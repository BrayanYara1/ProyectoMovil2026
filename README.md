# SaludActiva - Gestión de Turnos Médicos

## Descripción
SaludActiva es una plataforma integral para la gestión de turnos médicos. El proyecto incluye una aplicación móvil Android nativa y un backend escalable desplegado en la nube de AWS.

## Arquitectura del Proyecto
El proyecto sigue principios de **Clean Architecture** y **Clean Code**:

1.  **App Android (`/app`):** 
    - **MVVM:** Separación clara entre la lógica de negocio (ViewModels) y la interfaz (Fragments).
    - **Repository Pattern:** Centralización del acceso a datos (API y Caché Offline).
    - **Data Binding / View Binding:** Eliminación de `findViewById` para un código más limpio.
    - Retrofit + OkHttp para la comunicación con el backend.
    - Kotlin Coroutines para operaciones asíncronas.
    - Coil para la carga de imágenes.
    - Material Design 3 para la interfaz de usuario.

2.  **Backend (`/backend`):**
    - Node.js + Express.
    - MongoDB Atlas como base de datos de documentos.
    - Autenticación mediante JSON Web Tokens (JWT).
    - Despliegue mediante Docker.

3.  **Infraestructura (`/terraform_aws`):**
    - Infraestructura como Código (IaC) usando Terraform.
    - Despliegue en AWS ECS Fargate (Serverless Containers).
    - Base de Datos Relacional RDS (PostgreSQL).
    - Load Balancer (ALB) para distribución de tráfico y alta disponibilidad.
    - ECR para almacenamiento de imágenes de contenedor.

## Containerización
El backend está completamente containerizado para asegurar la paridad entre entornos.
- **Dockerfile:** Utiliza una construcción multi-etapa basada en Node-Alpine para minimizar el tamaño de la imagen.
- **Dockerignore:** Excluye archivos locales pesados o sensibles.

## Ejecución Local

### Backend
1. Entra a la carpeta `backend/`.
2. Instala las dependencias: `npm install`.
3. Crea un archivo `.env` con las variables `MONGODB_URI` y `JWT_SECRET`.
4. Ejecuta: `npm start`.

### Android
1. Abre el proyecto en Android Studio.
2. Sincroniza Gradle.
3. Asegúrate de que el `BASE_URL` en `RetrofitClient.kt` sea el correcto.
4. Ejecuta la app en un emulador o dispositivo físico.

## Infraestructura en AWS
Para desplegar la infraestructura:
1. Ve a `terraform_aws/`.
2. Crea un archivo `terraform.tfvars` basado en el `.example`.
3. Ejecuta `terraform init`.
4. Ejecuta `terraform apply`.

## Pruebas
- **Android:** Pruebas unitarias en `src/test` y pruebas de instrumentación en `src/androidTest`.
- **Backend:** Pruebas de integración para la API (usando Jest y Supertest).
