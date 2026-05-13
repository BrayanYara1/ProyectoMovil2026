require('dotenv').config();
const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');

const app = express();
const PORT = process.env.PORT || 10000;
const SECRET_KEY = process.env.JWT_SECRET || 'SaludActiva_Secret_Key_2024';

app.use(cors());
app.use(express.json());

// --- MIDDLEWARE DE AUTENTICACIÓN ---
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) return res.status(401).json({ mensaje: "Token no proporcionado" });

    jwt.verify(token, SECRET_KEY, (err, user) => {
        if (err) return res.status(403).json({ mensaje: "Token inválido o expirado" });
        req.userId = user.userId;
        next();
    });
};

// --- FUNCIÓN DE ENVÍO DE CORREO (BREVO API) ---
const sendVerificationEmail = async (email, code) => {
    console.log(`📧 Enviando código [${code}] a: ${email}`);
    const BREVO_API_KEY = process.env.BREVO_API_KEY;

    if (!BREVO_API_KEY) {
        console.error('❌ Error: BREVO_API_KEY no configurada');
        return;
    }

    try {
        const response = await fetch('https://api.brevo.com/v3/smtp/email', {
            method: 'POST',
            headers: {
                'accept': 'application/json',
                'api-key': BREVO_API_KEY,
                'content-type': 'application/json'
            },
            body: JSON.stringify({
                sender: { name: 'Salud Activa', email: 'andybrahian1996@gmail.com' },
                to: [{ email: email }],
                subject: `Tu código de acceso: ${code}`,
                textContent: `Tu código de verificación es: ${code}`,
                htmlContent: `<h3>Código de verificación: <b style="color: #007bff;">${code}</b></h3>`
            })
        });

        if (response.ok) {
            console.log(`✅ Brevo aceptó el correo para ${email}`);
        } else {
            const errData = await response.json();
            console.error('❌ Brevo rechazó el envío:', errData);
        }
    } catch (err) {
        console.error('❌ Error de conexión API Brevo:', err.message);
    }
};

// --- MODELOS ---
const User = require('./models/User');
const Turno = require('./models/Turno');
const Medicamento = require('./models/Medicamento');
const Estudio = require('./models/Estudio');

// --- CONEXIÓN MONGODB ---
mongoose.connect(process.env.MONGODB_URI)
    .then(() => console.log('✅ MongoDB Atlas conectado'))
    .catch(err => console.error('❌ Error MongoDB:', err.message));

// --- RUTAS DE AUTENTICACIÓN ---

app.post('/api/auth/register', async (req, res) => {
    try {
        const { nombre, email, telefono, contrasena } = req.body;
        const existe = await User.findOne({ email });
        if (existe) return res.status(400).json({ mensaje: "El email ya está registrado" });

        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(contrasena, salt);
        const verificationCode = Math.floor(100000 + Math.random() * 900000).toString();

        const nuevoUsuario = new User({
            nombre, email, telefono, contrasena: hashedPassword, verificationCode, isVerified: false
        });
        await nuevoUsuario.save();

        console.log(`🔑 CÓDIGO PARA ${email}: ${verificationCode}`);
        sendVerificationEmail(email, verificationCode);

        res.status(201).json({ mensaje: "OK", email: nuevoUsuario.email });
    } catch (error) {
        res.status(500).json({ mensaje: "Error en el servidor" });
    }
});

app.post('/api/auth/verify', async (req, res) => {
    try {
        const { email, code } = req.body;
        const user = await User.findOne({ email });
        if (!user) return res.status(404).json({ mensaje: "Usuario no encontrado" });
        if (user.verificationCode === code) {
            user.isVerified = true;
            user.verificationCode = null;
            await user.save();
            res.json({ mensaje: "Cuenta verificada" });
        } else {
            res.status(400).json({ mensaje: "Código incorrecto" });
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

        const verificationCode = Math.floor(100000 + Math.random() * 900000).toString();
        user.verificationCode = verificationCode;
        await user.save();

        sendVerificationEmail(email, verificationCode);
        res.json({ mensaje: "Código reenviado" });
    } catch (error) {
        res.status(500).json({ mensaje: "Error al reenviar" });
    }
});

app.post('/api/auth/login', async (req, res) => {
    try {
        const { email, contrasena } = req.body;
        const user = await User.findOne({ email });
        if (!user || !await bcrypt.compare(contrasena, user.contrasena)) {
            return res.status(401).json({ mensaje: "Credenciales inválidas" });
        }
        if (!user.isVerified) return res.status(403).json({ mensaje: "Cuenta no verificada", email: user.email });

        const token = jwt.sign({ userId: user._id }, SECRET_KEY, { expiresIn: '30d' });
        res.json({
            mensaje: "OK",
            usuario: {
                id: user._id,
                nombre: user.nombre,
                email: user.email,
                telefono: user.telefono
            },
            token
        });
    } catch (error) {
        res.status(500).json({ mensaje: "Error en el servidor" });
    }
});

app.put('/api/auth/profile', authenticateToken, async (req, res) => {
    try {
        const { nombre, telefono } = req.body;
        const user = await User.findByIdAndUpdate(req.userId, { nombre, telefono }, { new: true });
        res.json({ id: user._id, nombre: user.nombre, email: user.email, telefono: user.telefono });
    } catch (error) {
        res.status(500).json({ mensaje: "Error al actualizar perfil" });
    }
});

app.post('/api/auth/fcm-token', authenticateToken, async (req, res) => {
    try {
        const { token } = req.body;
        await User.findByIdAndUpdate(req.userId, { fcmToken: token });
        res.status(200).send();
    } catch (error) {
        res.status(500).json({ mensaje: "Error FCM" });
    }
});

// --- RUTAS DE TURNOS ---

app.get('/api/turnos', authenticateToken, async (req, res) => {
    try {
        const turnos = await Turno.find({ usuarioId: req.userId });
        res.json(turnos);
    } catch (error) {
        res.status(500).json({ mensaje: "Error al obtener turnos" });
    }
});

app.post('/api/turnos', authenticateToken, async (req, res) => {
    try {
        const nuevoTurno = new Turno({ ...req.body, usuarioId: req.userId });
        await nuevoTurno.save();
        res.status(201).json(nuevoTurno);
    } catch (error) {
        res.status(500).json({ mensaje: "Error al crear turno" });
    }
});

app.delete('/api/turnos/:id', authenticateToken, async (req, res) => {
    try {
        await Turno.findOneAndDelete({ _id: req.params.id, usuarioId: req.userId });
        res.status(200).send();
    } catch (error) {
        res.status(500).json({ mensaje: "Error al eliminar" });
    }
});

// --- RUTAS DE MEDICAMENTOS ---

app.get('/api/medicamentos', authenticateToken, async (req, res) => {
    try {
        const medicamentos = await Medicamento.find({ usuarioId: req.userId });
        res.json(medicamentos);
    } catch (error) {
        res.status(500).json({ mensaje: "Error" });
    }
});

app.post('/api/medicamentos', authenticateToken, async (req, res) => {
    try {
        const med = new Medicamento({ ...req.body, usuarioId: req.userId });
        await med.save();
        res.status(201).json(med);
    } catch (error) {
        res.status(500).json({ mensaje: "Error" });
    }
});

app.delete('/api/medicamentos/:id', authenticateToken, async (req, res) => {
    try {
        await Medicamento.findOneAndDelete({ _id: req.params.id, usuarioId: req.userId });
        res.status(200).send();
    } catch (error) {
        res.status(500).json({ mensaje: "Error" });
    }
});

// --- RUTAS DE ESTUDIOS ---

app.get('/api/estudios', authenticateToken, async (req, res) => {
    try {
        const estudios = await Estudio.find({ usuarioId: req.userId });
        res.json(estudios);
    } catch (error) {
        res.status(500).json({ mensaje: "Error" });
    }
});

app.post('/api/estudios', authenticateToken, async (req, res) => {
    try {
        const estudio = new Estudio({ ...req.body, usuarioId: req.userId });
        await estudio.save();
        res.status(201).json(estudio);
    } catch (error) {
        res.status(500).json({ mensaje: "Error" });
    }
});

app.delete('/api/estudios/:id', authenticateToken, async (req, res) => {
    try {
        await Estudio.findOneAndDelete({ _id: req.params.id, usuarioId: req.userId });
        res.status(200).send();
    } catch (error) {
        res.status(500).json({ mensaje: "Error" });
    }
});

// --- OTROS ---

app.delete('/api/admin/reset-database', async (req, res) => {
    await User.deleteMany({});
    await Turno.deleteMany({});
    await Medicamento.deleteMany({});
    await Estudio.deleteMany({});
    res.json({ mensaje: "Base de datos reiniciada" });
});

app.get('/', (req, res) => res.send('🚀 Backend Online y Funcional'));
app.listen(PORT, '0.0.0.0', () => console.log(`🚀 Puerto ${PORT}`));
