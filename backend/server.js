require('dotenv').config();
const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { Resend } = require('resend');

const app = express();
const PORT = process.env.PORT || 10000;
const SECRET_KEY = process.env.JWT_SECRET || 'SaludActiva_Secret_Key_2024';

// Inicializar Resend con la API KEY de tus variables de entorno
const resend = new Resend(process.env.RESEND_API_KEY);

app.use(cors());
app.use(express.json());

// Función para enviar correo usando la API de RESEND (HTTP - No se bloquea en Render)
const sendVerificationEmail = async (email, code) => {
    console.log(`📧 Intentando enviar correo vía RESEND a: ${email}`);

    try {
        const { data, error } = await resend.emails.send({
            from: 'Salud Activa <onboarding@resend.dev>',
            to: email,
            subject: 'Tu código de verificación: ' + code,
            html: `
                <div style="font-family: sans-serif; padding: 20px; border: 1px solid #eee; border-radius: 10px; max-width: 500px; margin: auto;">
                    <h2 style="color: #007bff; text-align: center;">Verifica tu cuenta</h2>
                    <p>Hola, usa el siguiente código para completar tu registro en <strong>Salud Activa</strong>:</p>
                    <div style="background: #f4f7ff; padding: 20px; text-align: center; font-size: 30px; font-weight: bold; color: #007bff; letter-spacing: 5px; border-radius: 8px; margin: 20px 0;">
                        ${code}
                    </div>
                    <p style="font-size: 12px; color: #777; text-align: center;">Si no solicitaste este código, puedes ignorar este correo.</p>
                </div>
            `
        });

        if (error) {
            console.error('❌ Error de Resend:', error);
        } else {
            console.log('✅ Correo enviado con éxito. ID:', data.id);
        }
    } catch (err) {
        console.error('❌ Error de conexión con Resend:', err.message);
    }
};

// Modelos
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

        // Enviar correo
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
            res.json({ mensaje: "Cuenta verificada con éxito" });
        } else {
            res.status(400).json({ mensaje: "Código incorrecto" });
        }
    } catch (error) {
        res.status(500).json({ mensaje: "Error servidor" });
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

app.delete('/api/admin/reset-database', async (req, res) => {
    try {
        await User.deleteMany({});
        await Turno.deleteMany({});
        await Medicamento.deleteMany({});
        await Estudio.deleteMany({});
        res.json({ mensaje: "Base de datos reiniciada" });
    } catch (error) {
        res.status(500).json({ mensaje: "Error" });
    }
});

app.get('/', (req, res) => res.send('🚀 Salud Activa Backend LIVE with Resend'));
app.listen(PORT, '0.0.0.0', () => console.log(`🚀 Servidor en puerto ${PORT}`));
