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

// Función de envío vía API de Brevo (HTTP - La única que NO se bloquea en Render)
const sendVerificationEmail = async (email, code) => {
    console.log(`📧 Iniciando envío de código a: ${email}`);
    const BREVO_API_KEY = process.env.BREVO_API_KEY;

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
                subject: `${code} es tu codigo de verificacion`,
                textContent: `Hola! Tu codigo de verificacion para Salud Activa es: ${code}`,
                htmlContent: `<h3>Tu codigo de verificacion es: <b style="color: #007bff;">${code}</b></h3>`
            })
        });

        const data = await response.json();
        if (response.ok) {
            console.log(`✅ Correo aceptado por Brevo para: ${email}`);
        } else {
            console.error('❌ Brevo rechazó el envío:', data);
        }
    } catch (err) {
        console.error('❌ Error de conexión con la API:', err.message);
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
