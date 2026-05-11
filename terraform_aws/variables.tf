variable "mongodb_uri" {
  description = "URI de conexión a MongoDB"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Secreto para JWT"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "Contraseña de la base de datos RDS"
  type        = string
  sensitive   = true
}
