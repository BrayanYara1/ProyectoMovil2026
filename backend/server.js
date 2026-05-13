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

const app = express();
const PORT = process.env.PORT || 10000;
const SECRET_KEY = process.env.JWT_SECRET || 'SaludActiva_Secret_Key_2024';

app.use(cors());
app.use(express.json());

// Función para enviar correo usando la API de BREVO (HTTP - No se bloquea en Render)
const sendVerificationEmail = async (email, code) => {
    console.log(`📧 Intentando enviar correo a: ${email}`);
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
                subject: 'Código de Verificación - Salud Activa',
                htmlContent: `
                    <div style="font-family: Arial, sans-serif; padding: 20px; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee;">
                        <h2 style="color: #007bff; text-align: center;">¡Bienvenido a Salud Activa!</h2>
                        <p>Gracias por registrarte. Tu código de verificación es:</p>
                        <div style="background: #f8f9fa; padding: 20px; font-size: 32px; font-weight: bold; text-align: center; border-radius: 10px; border: 2px dashed #007bff; letter-spacing: 10px; color: #007bff;">
                            ${code}
                        </div>
                        <p style="margin-top: 20px;">Copia este código en la aplicación para activar tu cuenta.</p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                        <p style="font-size: 12px; color: #777; text-align: center;">Este es un correo automático, por favor no lo respondas.</p>
                    </div>
                `
            })
        });

        const data = await response.json();
        if (!response.ok) throw new Error(data.message || 'Error en API de Brevo');

        console.log('✅ Correo aceptado por Brevo. ID:', data.messageId);
        return data;
    } catch (err) {
        console.error('❌ Error en envío de email:', err.message);
        // No lanzamos error para que el registro no falle si falla el correo
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

        // Enviamos el correo pero no esperamos a que termine para responder al cliente
        sendVerificationEmail(email, verificationCode);

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

        sendVerificationEmail(email, newCode);
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

app.get('/', (req, res) => res.send('🚀 Salud Activa Backend API Edition is RUNNING'));
app.listen(PORT, '0.0.0.0', () => console.log(`🚀 Servidor en puerto ${PORT}`));

module.exports = app;
