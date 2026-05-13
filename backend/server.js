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

// Función para enviar correo usando la API de BREVO (Vía HTTP - Única que funciona en Render)
const sendVerificationEmail = async (email, code) => {
    console.log(`📧 Iniciando envío de código a: ${email}`);
    const BREVO_API_KEY = process.env.BREVO_API_KEY;

    if (!BREVO_API_KEY) {
        console.error('❌ ERROR: BREVO_API_KEY no configurada');
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
                // IMPORTANTE: El email del sender DEBE ser el mismo que verificaste en Brevo
                sender: { name: 'Salud Activa', email: 'andybrahian1996@gmail.com' },
                to: [{ email: email }],
                subject: `${code} es tu código de verificación - Salud Activa`,
                textContent: `Tu código de verificación para Salud Activa es: ${code}`,
                htmlContent: `
                    <div style="font-family: sans-serif; max-width: 500px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                        <h2 style="color: #007bff; text-align: center;">Verifica tu cuenta</h2>
                        <p>Hola, gracias por unirte a <strong>Salud Activa</strong>. Usa el siguiente código para completar tu registro:</p>
                        <div style="background: #f4f7ff; padding: 20px; text-align: center; font-size: 30px; font-weight: bold; color: #007bff; letter-spacing: 5px; border-radius: 8px; margin: 20px 0;">
                            ${code}
                        </div>
                        <p style="font-size: 12px; color: #777;">Si no solicitaste este código, puedes ignorar este correo.</p>
                    </div>
                `
            })
        });

        const data = await response.json();
        if (response.ok) {
            console.log(`✅ Correo enviado con éxito a ${email}. ID: ${data.messageId}`);
        } else {
            console.error('❌ Brevo rechazó el envío:', data);
        }
    } catch (err) {
        console.error('❌ Error de conexión al enviar correo:', err.message);
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

        // Enviamos el correo (sin esperar el await para no bloquear la respuesta)
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

app.get('/', (req, res) => res.send('🚀 Salud Activa Backend LIVE'));
app.listen(PORT, '0.0.0.0', () => console.log(`🚀 Servidor en puerto ${PORT}`));
