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
const authenticateToken = async (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) return res.status(401).json({ mensaje: "Token no proporcionado" });

    jwt.verify(token, SECRET_KEY, async (err, decoded) => {
        if (err) return res.status(403).json({ mensaje: "Token inválido o expirado" });

        try {
            const user = await User.findById(decoded.userId);
            if (!user) return res.status(401).json({ mensaje: "Usuario ya no existe" });

            req.userId = decoded.userId;
            req.user = user;
            next();
        } catch (error) {
            res.status(500).json({ mensaje: "Error de servidor en autenticación" });
        }
    });
};

// --- MODELOS ---
const User = require('./models/User');
const Turno = require('./models/Turno');
const Medicamento = require('./models/Medicamento');
const Estudio = require('./models/Estudio');

// --- CONEXIÓN MONGODB ---
if (!process.env.MONGODB_URI) {
    console.error('❌ ERROR: MONGODB_URI no definida en .env');
    process.exit(1);
}

mongoose.connect(process.env.MONGODB_URI)
    .then(() => console.log('✅ MongoDB Atlas conectado a:', mongoose.connection.name))
    .catch(err => {
        console.error('❌ Error MongoDB:', err.message);
        process.exit(1);
    });

// --- RUTAS DE AUTENTICACIÓN ---

app.post('/api/auth/register', async (req, res) => {
    try {
        const { nombre, email, telefono, contrasena } = req.body;
        if (!nombre || !email || !contrasena) {
            return res.status(400).json({ mensaje: "Faltan datos obligatorios" });
        }

        const existe = await User.findOne({ email });
        if (existe) return res.status(400).json({ mensaje: "El email ya está registrado" });

        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(contrasena, salt);

        const nuevoUsuario = new User({
            nombre, email, telefono, contrasena: hashedPassword, verificationCode: null, isVerified: true
        });
        await nuevoUsuario.save();

        console.log(`👤 Usuario registrado: ${email}`);
        res.status(201).json({ mensaje: "OK", email: nuevoUsuario.email });
    } catch (error) {
        console.error("Error en register:", error);
        res.status(500).json({ mensaje: "Error en el servidor durante el registro" });
    }
});

app.post('/api/auth/login', async (req, res) => {
    try {
        const { email, contrasena } = req.body;
        const user = await User.findOne({ email });
        if (!user || !await bcrypt.compare(contrasena, user.contrasena)) {
            return res.status(401).json({ mensaje: "Credenciales inválidas" });
        }

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
        console.error("Error en login:", error);
        res.status(500).json({ mensaje: "Error en el servidor" });
    }
});

app.put('/api/auth/profile', authenticateToken, async (req, res) => {
    try {
        const { nombre, telefono } = req.body;
        const user = await User.findByIdAndUpdate(req.userId, { nombre, telefono }, { new: true });
        if (!user) return res.status(404).json({ mensaje: "Usuario no encontrado" });
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

// Rutas Mock para compatibilidad con el Frontend (Si se requiere verificación luego)
app.post('/api/auth/verify', async (req, res) => res.status(200).json({ mensaje: "OK" }));
app.post('/api/auth/resend-code', async (req, res) => res.status(200).json({ mensaje: "OK" }));

// --- RUTAS DE TURNOS ---

app.get('/api/turnos', authenticateToken, async (req, res) => {
    try {
        const turnos = await Turno.find({ usuarioId: req.userId }).sort({ fecha: 1, hora: 1 });
        res.json(turnos);
    } catch (error) {
        res.status(500).json({ mensaje: "Error al obtener turnos" });
    }
});

app.get('/api/turnos/check-availability', authenticateToken, async (req, res) => {
    try {
        const { fecha, hora } = req.query;
        // Buscamos cualquier turno (de cualquier usuario) que coincida en fecha y hora
        // Excepto los cancelados
        const turnoExistente = await Turno.findOne({
            fecha,
            hora,
            estado: { $nin: ["Cancelado", "cancelled", "CANCELADO"] }
        });
        res.json({ disponible: !turnoExistente });
    } catch (error) {
        res.status(500).json({ mensaje: "Error al verificar disponibilidad" });
    }
});

app.post('/api/turnos', authenticateToken, async (req, res) => {
    try {
        const { fecha, hora } = req.body;

        // Verificación de seguridad en Backend (evitar solapamientos reales)
        const ocupado = await Turno.findOne({
            fecha,
            hora,
            estado: { $nin: ["Cancelado", "cancelled", "CANCELADO"] }
        });

        if (ocupado) {
            return res.status(400).json({ mensaje: "Este horario ya fue tomado por otro paciente" });
        }

        const nuevoTurno = new Turno({ ...req.body, usuarioId: req.userId });
        await nuevoTurno.save();
        res.status(201).json(nuevoTurno);
    } catch (error) {
        console.error("❌ Error al crear turno:", error);
        res.status(500).json({ mensaje: "Error al crear turno", error: error.message });
    }
});

app.delete('/api/turnos/:id', authenticateToken, async (req, res) => {
    try {
        const result = await Turno.findOneAndDelete({ _id: req.params.id, usuarioId: req.userId });
        if (!result) return res.status(404).json({ mensaje: "Turno no encontrado" });
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
    const adminKey = req.headers['x-admin-key'];
    if (!adminKey || adminKey !== process.env.ADMIN_KEY) {
        console.warn(`⚠️ Intento de reset no autorizado desde IP: ${req.ip}`);
        return res.status(403).json({ mensaje: "No autorizado" });
    }

    try {
        await User.deleteMany({});
        await Turno.deleteMany({});
        await Medicamento.deleteMany({});
        await Estudio.deleteMany({});
        res.json({ mensaje: "Base de datos reiniciada con éxito" });
    } catch (error) {
        res.status(500).json({ mensaje: "Error al reiniciar base de datos" });
    }
});

app.get('/', (req, res) => res.send('🚀 Salud Activa Backend Online (v1.1)'));
app.listen(PORT, '0.0.0.0', () => console.log(`🚀 Servidor corriendo en el puerto ${PORT}`));
