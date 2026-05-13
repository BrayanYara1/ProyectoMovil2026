require('dotenv').config();
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

// Función de envío por SMTP Seguro (Puerto 465)
const sendVerificationEmail = async (email, code) => {
    console.log(`📧 Intentando enviar código a: ${email}`);

    // Configuramos el transporte usando SSL directo (Puerto 465)
    const transporter = nodemailer.createTransport({
        host: 'smtp.gmail.com',
        port: 465,
        secure: true, // Uso de SSL
        auth: {
            user: process.env.EMAIL_USER,
            pass: process.env.EMAIL_PASS
        }
    });

    const mailOptions = {
        from: `"Salud Activa" <${process.env.EMAIL_USER}>`,
        to: email,
        subject: `${code} es tu código de verificación`,
        text: `Hola! Tu código de verificación para Salud Activa es: ${code}`,
        html: `<p>Hola! Tu código de verificación para <b>Salud Activa</b> es: <br><br> <span style="font-size: 24px; font-weight: bold; color: #007bff;">${code}</span></p>`
    };

    try {
        await transporter.sendMail(mailOptions);
        console.log('✅ Correo enviado exitosamente');
    } catch (err) {
        console.error('❌ Error de Nodemailer:', err.message);
        // Si falla el envío, al menos logueamos el código para que tú lo veas en Render
        console.log(`🔑 CÓDIGO DE EMERGENCIA PARA ${email}: ${code}`);
    }
};

// Modelos y Rutas
const User = require('./models/User');
const Turno = require('./models/Turno');
const Medicamento = require('./models/Medicamento');
const Estudio = require('./models/Estudio');

mongoose.connect(process.env.MONGODB_URI)
    .then(() => console.log('✅ MongoDB Atlas conectado'))
    .catch(err => console.error('❌ Error MongoDB:', err.message));

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
        res.status(500).json({ mensaje: "Error" });
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
        res.json({ mensaje: "OK", usuario: { id: user._id, nombre: user.nombre }, token });
    } catch (error) {
        res.status(500).json({ mensaje: "Error" });
    }
});

app.delete('/api/admin/reset-database', async (req, res) => {
    await User.deleteMany({});
    await Turno.deleteMany({});
    await Medicamento.deleteMany({});
    await Estudio.deleteMany({});
    res.json({ mensaje: "Base de datos reiniciada" });
});

app.get('/', (req, res) => res.send('🚀 Backend Online'));
app.listen(PORT, '0.0.0.0', () => console.log(`🚀 Puerto ${PORT}`));
