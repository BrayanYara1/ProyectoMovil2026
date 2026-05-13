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
const { Resend } = require('resend');

// Configuración de Resend (Mucho más fiable en Render que SMTP)
const resend = new Resend(process.env.RESEND_API_KEY);

// Modelos
const User = require('./models/User');
const Turno = require('./models/Turno');
const Medicamento = require('./models/Medicamento');
const Estudio = require('./models/Estudio');

const app = express();
const PORT = process.env.PORT || 10000;
const SECRET_KEY = process.env.JWT_SECRET || 'SaludActiva_Secret_Key_2024';

app.use(cors());
app.use(express.json());

// Función para enviar correo de verificación usando la API de Resend
const sendVerificationEmail = async (email, code) => {
    console.log(`📧 Intentando enviar correo vía RESEND a: ${email} con código: ${code}`);
    try {
        const { data, error } = await resend.emails.send({
            from: 'Salud Activa <onboarding@resend.dev>',
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
                </div>
            `
        });

        if (error) {
            console.error('❌ Error devuelto por Resend API:', error);
            throw new Error(error.message);
        }

        console.log('✅ Correo aceptado por Resend. ID:', data.id);
        return data;
    } catch (err) {
        console.error('❌ Fallo crítico en sendVerificationEmail:', err.message);
        throw err;
    }
};

// Ruta de estado
app.get('/', (req, res) => res.send('🚀 Salud Activa Backend is RUNNING (v3.1.0)'));

// Conexión a MongoDB
mongoose.connect(process.env.MONGODB_URI)
    .then(() => console.log('✅ Conectado exitosamente a MongoDB Atlas'))
    .catch(err => console.error('❌ Error de conexión a MongoDB:', err.message));

// Middleware para logging
app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    next();
});

// --- RUTAS DE AUTENTICACIÓN ---

app.post('/api/auth/register', async (req, res) => {
    try {
        const { nombre, email, telefono, contrasena } = req.body;
        if (!nombre || !email || !contrasena) return res.status(400).json({ mensaje: "Campos incompletos" });

        const existe = await User.findOne({ email });
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
            nombre, email, telefono, contrasena: hashedPassword, verificationCode, isVerified: false
        });
        await nuevoUsuario.save();

        sendVerificationEmail(email, verificationCode).catch(e => console.error("Error correo:", e.message));
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

app.post('/api/auth/resend-code', async (req, res) => {
    try {
        const { email } = req.body;
        const user = await User.findOne({ email });
        if (!user) return res.status(404).json({ mensaje: "Usuario no encontrado" });
        if (user.isVerified) return res.status(400).json({ mensaje: "La cuenta ya está verificada" });

        const newCode = Math.floor(100000 + Math.random() * 900000).toString();
        user.verificationCode = newCode;
        await user.save();
        sendVerificationEmail(email, newCode).catch(e => console.error("Error re-envío:", e.message));
        res.json({ mensaje: "Código reenviado con éxito" });
    } catch (error) {
        res.status(500).json({ mensaje: "Error al reenviar código" });
    }
});

app.post('/api/auth/login', async (req, res) => {
    try {
        const { email, contrasena } = req.body;
        const user = await User.findOne({ email });
        if (!user) return res.status(401).json({ mensaje: "Usuario no encontrado" });
        const esValida = await bcrypt.compare(contrasena, user.contrasena);
        if (!esValida) return res.status(401).json({ mensaje: "Contraseña incorrecta" });
        if (!user.isVerified) return res.status(403).json({ mensaje: "Cuenta no verificada", requiresVerification: true, email: user.email });

        const token = jwt.sign({ userId: user._id, email: user.email }, SECRET_KEY, { expiresIn: '30d' });
        res.json({ mensaje: "OK", usuario: { id: user._id, nombre: user.nombre, email: user.email }, token });
    } catch (error) {
        res.status(500).json({ mensaje: "Error en el servidor" });
    }
});

// --- OTRAS RUTAS (TURNOS, MEDICAMENTOS, ETC) ---
// (Se mantienen igual pero simplificadas para este bloque)

app.get('/api/turnos', async (req, res) => { /* ... */ });
app.post('/api/turnos', async (req, res) => { /* ... */ });
// ... (resto de rutas)

app.use((req, res) => res.status(404).json({ mensaje: "Ruta no encontrada" }));

app.listen(PORT, '0.0.0.0', () => console.log(`🚀 Salud Activa Backend v3.1.0 en puerto ${PORT}`));

module.exports = app;
