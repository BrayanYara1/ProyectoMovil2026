require('dotenv').config();
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

const app = express();
const PORT = process.env.PORT || 10000;
const SECRET_KEY = process.env.JWT_SECRET || 'SaludActiva_Secret_Key_2024';

app.use(cors());
app.use(express.json());

// Función para enviar correo usando Nodemailer (Más confiable para Gmail/Institucionales)
const sendVerificationEmail = async (email, code) => {
    console.log(`📧 Intentando enviar correo a: ${email}`);

    const transporter = nodemailer.createTransport({
        service: 'gmail',
        auth: {
            user: process.env.EMAIL_USER,
            pass: process.env.EMAIL_PASS
        }
    });

    const mailOptions = {
        from: `"Salud Activa" <${process.env.EMAIL_USER}>`,
        to: email,
        subject: 'Código de Verificación - Salud Activa',
        html: `
            <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
                <h2 style="color: #007bff;">¡Bienvenido a Salud Activa!</h2>
                <p>Tu código de verificación es:</p>
                <div style="background: #f4f4f4; padding: 15px; font-size: 24px; font-weight: bold; text-align: center; border-radius: 8px; letter-spacing: 5px;">
                    ${code}
                </div>
                <p>Usa este código en la aplicación para activar tu cuenta.</p>
            </div>
        `
    };

    try {
        const info = await transporter.sendMail(mailOptions);
        console.log('✅ Correo enviado con éxito. ID:', info.messageId);
        return info;
    } catch (err) {
        console.error('❌ Error crítico en sendVerificationEmail:', err.message);
        throw err;
    }
};

// Modelos
const User = require('./models/User');
const Turno = require('./models/Turno');
const Medicamento = require('./models/Medicamento');
const Estudio = require('./models/Estudio');

// Conexión a MongoDB
mongoose.connect(process.env.MONGODB_URI)
    .then(() => console.log('✅ MongoDB Atlas conectado'))
    .catch(err => console.error('❌ Error MongoDB:', err.message));

// Rutas de autenticación
app.post('/api/auth/register', async (req, res) => {
    try {
        const { nombre, email, telefono, contrasena } = req.body;
        if (!nombre || !email || !contrasena) return res.status(400).json({ mensaje: "Campos incompletos" });

        const existe = await User.findOne({ email });
        if (existe) return res.status(400).json({ mensaje: "El email ya está registrado" });

        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(contrasena, salt);
        const verificationCode = Math.floor(100000 + Math.random() * 900000).toString();

        const nuevoUsuario = new User({
            nombre, email, telefono, contrasena: hashedPassword, verificationCode, isVerified: false
        });
        await nuevoUsuario.save();

        sendVerificationEmail(email, verificationCode).catch(e => console.error("Error envío inicial:", e.message));
        res.status(201).json({ mensaje: "OK", email: nuevoUsuario.email });
    } catch (error) {
        res.status(500).json({ mensaje: "Error en el servidor", error: error.message });
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
            res.json({ mensaje: "Cuenta verificada con éxito" });
        } else {
            res.status(400).json({ mensaje: "Código incorrecto" });
        }
    } catch (error) {
        res.status(500).json({ mensaje: "Error servidor" });
    }
});

app.post('/api/auth/resend-code', async (req, res) => {
    try {
        const { email } = req.body;
        const user = await User.findOne({ email });
        if (!user) return res.status(404).json({ mensaje: "Usuario no encontrado" });

        const newCode = Math.floor(100000 + Math.random() * 900000).toString();
        user.verificationCode = newCode;
        await user.save();

        sendVerificationEmail(email, newCode).catch(e => console.error("Error re-envío:", e.message));
        res.json({ mensaje: "Código reenviado" });
    } catch (error) {
        res.status(500).json({ mensaje: "Error al reenviar" });
    }
});

app.post('/api/auth/login', async (req, res) => {
    try {
        const { email, contrasena } = req.body;
        const user = await User.findOne({ email });
        if (!user) return res.status(401).json({ mensaje: "Usuario no encontrado" });

        const esValida = await bcrypt.compare(contrasena, user.contrasena);
        if (!esValida) return res.status(401).json({ mensaje: "Contraseña incorrecta" });

        if (!user.isVerified) return res.status(403).json({ mensaje: "Cuenta no verificada", email: user.email });

        const token = jwt.sign({ userId: user._id }, SECRET_KEY, { expiresIn: '30d' });
        res.json({ mensaje: "OK", usuario: { id: user._id, nombre: user.nombre, email: user.email }, token });
    } catch (error) {
        res.status(500).json({ mensaje: "Error servidor" });
    }
});

// --- RUTA DE EMERGENCIA PARA LIMPIAR BASE DE DATOS ---
app.delete('/api/admin/reset-database', async (req, res) => {
    try {
        await User.deleteMany({});
        await Turno.deleteMany({});
        await Medicamento.deleteMany({});
        await Estudio.deleteMany({});
        console.log('💥 BASE DE DATOS LIMPIADA POR COMPLETO');
        res.json({ mensaje: "Base de datos reiniciada. Todas las cuentas y datos han sido eliminados." });
    } catch (error) {
        res.status(500).json({ mensaje: "Error al reiniciar base de datos", error: error.message });
    }
});

app.get('/', (req, res) => res.send('🚀 Salud Activa Backend BREVO Edition is RUNNING'));
app.listen(PORT, '0.0.0.0', () => console.log(`🚀 Servidor en puerto ${PORT}`));

module.exports = app;
