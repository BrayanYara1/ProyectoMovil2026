const mongoose = require('mongoose');

const UserSchema = new mongoose.Schema({
    nombre: { type: String, required: true },
    email: { type: String, required: true, unique: true },
    telefono: { type: String, default: "" },
    contrasena: { type: String, required: true },
    fcmToken: { type: String, default: null },
    fechaCreacion: { type: Date, default: Date.now }
});

module.exports = mongoose.model('User', UserSchema);
