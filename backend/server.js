require('dotenv').config();
// Robust polyfill for crypto in Node.js 18+ for older libraries
if (typeof crypto === 'undefined') {
    global.crypto = require('node:crypto').webcrypto;
} else if (!crypto.subtle && require('node:crypto').webcrypto) {
    global.crypto = require('node:crypto').webcrypto;
}

const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const nodemailer = require('nodemailer');

// Modelos
const User = require('./models/User');
const Turno = require('./models/Turno');
const Medicamento = require('./models/Medicamento');
const Estudio = require('./models/Estudio');

const app = express();
const PORT = process.env.PORT || 3000;
const SECRET_KEY = process.env.JWT_SECRET || 'SaludActiva_Secret_Key_2024';

app.use(cors());
app.use(express.json());

// Configuración de Nodemailer mejorada con logs
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.EMAIL_USER,
        pass: process.env.EMAIL_PASS
    }
});

// Verificar conexión con el servidor de correos al arrancar
transporter.verify((error, success) => {
    if (error) {
        console.error('❌ Error en configuración de correo:', error.message);
    } else {
        console.log('✅ Servidor de correos listo para enviar mensajes');
    }
});

// Función para enviar correo de verificación
const sendVerificationEmail = async (email, code) => {
    console.log(`📧 Intentando enviar correo a: ${email} con código: ${code}`);
    const mailOptions = {
        from: `"Salud Activa" <${process.env.EMAIL_USER}>`,
        to: email,
        subject: 'Código de Verificación - Salud Activa',
        html: `
            <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
                <h2 style="color: #007bff;">¡Bienvenido a Salud Activa!</h2>
                <p>Gracias por registrarte. Para activar tu cuenta, ingresa el siguiente código en la aplicación:</p>
                <div style="background: #f4f4f4; padding: 15px; font-size: 24px; font-weight: bold; text-align: center; border-radius: 8px; letter-spacing: 5px;">
                    ${code}
                </div>
                <p>Este código expirará en 24 horas.</p>
                <p>Si no creaste esta cuenta, puedes ignorar este correo.</p>
            </div>
        `
    };

    try {
        const info = await transporter.sendMail(mailOptions);
        console.log('✅ Correo enviado con éxito:', info.messageId);
        return info;
    } catch (err) {
        console.error('❌ Error REAL al enviar correo:', err.message);
        throw err;
    }
};

// Ruta de estado
app.get('/', (req, res) => res.send('🚀 Salud Activa Backend is RUNNING (v3.0.3)'));
app.get('/api/status', (req, res) => res.json({ status: "online", version: "3.0.7", database: mongoose.connection.readyState === 1 ? "connected" : "disconnected" }));

// Conexión a MongoDB
mongoose.connect(process.env.MONGODB_URI)
    .then(() => console.log('✅ Conectado exitosamente a MongoDB Atlas'))
    .catch(err => console.error('❌ Error de conexión a MongoDB:', err.message));

// Middleware para logging
app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    next();
});

// --- MIDDLEWARE DE AUTENTICACIÓN ---
const authenticate = async (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ mensaje: "No autorizado. Inicie sesión." });
    }

    const token = authHeader.split(' ')[1];
    try {
        const decoded = jwt.verify(token, SECRET_KEY);
        const user = await User.findById(decoded.userId);
        if (!user) return res.status(401).json({ mensaje: "Usuario no encontrado" });
        req.user = user;
        next();
    } catch (e) {
        return res.status(401).json({ mensaje: "Token inválido o expirado" });
    }
};

// --- RUTAS DE AUTENTICACIÓN ---

app.post('/api/auth/register', async (req, res) => {
    try {
        const { nombre, email, telefono, contrasena } = req.body;
        if (!nombre || !email || !contrasena) return res.status(400).json({ mensaje: "Campos incompletos" });

        const existe = await User.findOne({ email });

        // SI YA EXISTE PERO NO ESTÁ VERIFICADO, PERMITIMOS RE-ENVIAR CÓDIGO
        if (existe && !existe.isVerified) {
            const newCode = Math.floor(100000 + Math.random() * 900000).toString();
            existe.verificationCode = newCode;
            await existe.save();
            sendVerificationEmail(email, newCode).catch(e => console.error("Fallo re-envío:", e.message));
            return res.json({ mensaje: "OK", requiresVerification: true, email: existe.email });
        }

        if (existe) return res.status(400).json({ mensaje: "El email ya está registrado" });

        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(contrasena, salt);

        const verificationCode = Math.floor(100000 + Math.random() * 900000).toString();

        const nuevoUsuario = new User({
            nombre,
            email,
            telefono,
            contrasena: hashedPassword,
            verificationCode,
            isVerified: false
        });
        await nuevoUsuario.save();

        // ENVIAR CORREO EN SEGUNDO PLANO (Evita Timeout)
        sendVerificationEmail(email, verificationCode).catch(mailError => {
            console.error('Error enviando correo:', mailError.message);
        });

        res.status(201).json({ mensaje: "OK", requiresVerification: true, email: nuevoUsuario.email });
    } catch (error) {
        res.status(500).json({ mensaje: "Error en el servidor", error: error.message });
    }
});

app.post('/api/auth/verify', async (req, res) => {
    try {
        const { email, code } = req.body;
        const user = await User.findOne({ email });

        if (!user) return res.status(404).json({ mensaje: "Usuario no encontrado" });
        if (user.isVerified) return res.status(400).json({ mensaje: "La cuenta ya está verificada" });

        if (user.verificationCode === code) {
            user.isVerified = true;
            user.verificationCode = null;
            await user.save();
            res.json({ mensaje: "Cuenta verificada con éxito" });
        } else {
            res.status(400).json({ mensaje: "Código de verificación incorrecto" });
        }
    } catch (error) {
        res.status(500).json({ mensaje: "Error en el servidor" });
    }
});

app.post('/api/auth/login', async (req, res) => {
    try {
        const { email, contrasena } = req.body;
        const user = await User.findOne({ email });
        if (!user) return res.status(401).json({ mensaje: "Usuario no encontrado" });

        const esValida = await bcrypt.compare(contrasena, user.contrasena);
        if (!esValida) return res.status(401).json({ mensaje: "Contraseña incorrecta" });

        if (!user.isVerified) {
            return res.status(403).json({
                mensaje: "Cuenta no verificada. Revisa tu correo.",
                requiresVerification: true,
                email: user.email
            });
        }

        const token = jwt.sign({ userId: user._id, email: user.email }, SECRET_KEY, { expiresIn: '30d' });
        const userSafe = { id: user._id, _id: user._id, nombre: user.nombre, email: user.email, telefono: user.telefono };
        res.json({ mensaje: "OK", usuario: userSafe, token });
    } catch (error) {
        res.status(500).json({ mensaje: "Error en el servidor" });
    }
});

app.post('/api/auth/fcm-token', authenticate, async (req, res) => {
    try {
        const { token } = req.body;
        await User.findByIdAndUpdate(req.user._id, { fcmToken: token });
        res.json({ mensaje: "Token actualizado" });
    } catch (error) {
        res.status(500).json({ mensaje: "Error al actualizar token" });
    }
});

app.put('/api/auth/profile', authenticate, async (req, res) => {
    try {
        const { nombre, telefono } = req.body;
        const user = await User.findByIdAndUpdate(req.user._id, { nombre, telefono }, { new: true });
        if (!user) return res.status(404).json({ mensaje: "Usuario no encontrado" });
        const userSafe = { id: user._id, _id: user._id, nombre: user.nombre, email: user.email, telefono: user.telefono };
        res.json(userSafe);
    } catch (error) {
        res.status(500).json({ mensaje: "Error al actualizar perfil" });
    }
});

// --- RUTAS DE TURNOS ---

app.get('/api/turnos', authenticate, async (req, res) => {
    try {
        const turnos = await Turno.find({ usuarioId: req.user._id });
        res.json(turnos.map(t => ({ ...t._doc, id: t._id, _id: t._id })));
    } catch (error) {
        res.status(500).json({ mensaje: "Error al obtener turnos" });
    }
});

app.post('/api/turnos', authenticate, async (req, res) => {
    try {
        const { nombre, fecha, hora, motivo, especialidad, doctor } = req.body;
        const nuevoTurno = new Turno({ usuarioId: req.user._id, pacienteNombre: nombre, fecha, hora, motivo, especialidad, doctor });
        await nuevoTurno.save();
        res.status(201).json({ ...nuevoTurno._doc, id: nuevoTurno._id, _id: nuevoTurno._id });
    } catch (error) {
        res.status(500).json({ mensaje: "Error al crear turno" });
    }
});

app.delete('/api/turnos/:id', authenticate, async (req, res) => {
    try {
        const resultado = await Turno.findOneAndDelete({ _id: req.params.id, usuarioId: req.user._id });
        if (!resultado) return res.status(404).json({ mensaje: "Turno no encontrado" });
        res.status(204).send();
    } catch (error) {
        res.status(500).json({ mensaje: "Error al eliminar turno" });
    }
});

// --- RUTAS DE MEDICAMENTOS ---

app.get('/api/medicamentos', authenticate, async (req, res) => {
    try {
        const meds = await Medicamento.find({ usuarioId: req.user._id });
        res.json(meds.map(m => ({ ...m._doc, id: m._id, _id: m._id })));
    } catch (error) {
        res.status(500).json({ mensaje: "Error al obtener medicamentos" });
    }
});

app.post('/api/medicamentos', authenticate, async (req, res) => {
    try {
        const { nombre, dosis, frecuencia, proximaToma, notas } = req.body;
        const med = new Medicamento({ usuarioId: req.user._id, nombre, dosis, frecuencia, proximaToma, notas });
        await med.save();
        res.status(201).json({ ...med._doc, id: med._id, _id: med._id });
    } catch (error) {
        res.status(500).json({ mensaje: "Error al guardar medicamento" });
    }
});

app.delete('/api/medicamentos/:id', authenticate, async (req, res) => {
    try {
        const resultado = await Medicamento.findOneAndDelete({ _id: req.params.id, usuarioId: req.user._id });
        if (!resultado) return res.status(404).json({ mensaje: "Medicamento no encontrado" });
        res.status(204).send();
    } catch (error) {
        res.status(500).json({ mensaje: "Error al eliminar medicamento" });
    }
});

// --- RUTAS DE ESTUDIOS ---

app.get('/api/estudios', authenticate, async (req, res) => {
    try {
        const estudios = await Estudio.find({ usuarioId: req.user._id });
        res.json(estudios.map(e => ({ ...e._doc, id: e._id, _id: e._id })));
    } catch (error) {
        res.status(500).json({ mensaje: "Error al obtener estudios" });
    }
});

app.post('/api/estudios', authenticate, async (req, res) => {
    try {
        const { titulo, fecha, tipo, resultadoBreve, urlDocumento, notas } = req.body;
        const nuevoEstudio = new Estudio({ usuarioId: req.user._id, titulo, fecha, tipo, resultadoBreve, urlDocumento, notas });
        await nuevoEstudio.save();
        res.status(201).json({ ...nuevoEstudio._doc, id: nuevoEstudio._id, _id: nuevoEstudio._id });
    } catch (error) {
        res.status(500).json({ mensaje: "Error al guardar estudio" });
    }
});

app.delete('/api/estudios/:id', authenticate, async (req, res) => {
    try {
        const resultado = await Estudio.findOneAndDelete({ _id: req.params.id, usuarioId: req.user._id });
        if (!resultado) return res.status(404).json({ mensaje: "Estudio no encontrado" });
        res.status(204).send();
    } catch (error) {
        res.status(500).json({ mensaje: "Error al eliminar estudio" });
    }
});

app.use((req, res) => {
    res.status(404).json({ mensaje: "Ruta no encontrada" });
});

if (require.main === module) {
    app.listen(PORT, '0.0.0.0', () => {
        console.log(`🚀 Salud Activa CLOUD Backend v3.0.3 en puerto ${PORT}`);
    });
}

module.exports = app;
