# Diseño de la Base de Datos - LP2-Zoom

Este documento especifica el modelado de datos físico y lógico alojado en nuestra base de datos relacional de **Supabase (PostgreSQL)**.

## 1. Detalles Técnicos
*   **Motor:** PostgreSQL.
*   **Servicio Cloud:** Supabase.
*   **Conectividad:** JDBC (`postgresql-42.7.4.jar`) con soporte para conexiones seguras SSL (`sslmode=require`).

---

## 2. Diagrama de Relaciones y Modelo Físico

El esquema está compuesto por 6 tablas principales con restricciones de clave primaria (PK) e integridad referencial (FK) con eliminación en cascada (`ON DELETE CASCADE`):

### A. Tabla: `Usuarios`
Almacena las cuentas registradas en el sistema.
*   `IdUsuario` (SERIAL, PK): Identificador numérico secuencial auto-incremental.
*   `Nombres` (VARCHAR(150), NOT NULL): Nombres y apellidos completos.
*   `Correo` (VARCHAR(150), UNIQUE, NOT NULL): Correo electrónico del usuario (llave alternativa de login).
*   `PasswordHash` (VARCHAR(255), NOT NULL): Hash SHA-256 de la contraseña (nunca se almacena en texto plano).
*   `Rol` (VARCHAR(50), NOT NULL DEFAULT 'USUARIO'): Rol de usuario (ej. 'HOST', 'INVITADO', 'USUARIO').

### B. Tabla: `Salas`
Almacena las salas creadas por los anfitriones.
*   `IdSala` (SERIAL, PK): Identificador secuencial auto-incremental.
*   `CodigoSala` (VARCHAR(10), UNIQUE, NOT NULL): Código único de 6 caracteres autogenerado por el Host para unirse a la sala.
*   `Nombre` (VARCHAR(150), NOT NULL): Nombre descriptivo de la videoconferencia.
*   `IdHost` (INT, FK $\rightarrow$ `Usuarios.IdUsuario`, ON DELETE CASCADE): ID del usuario que actúa como creador y administrador de la sala.
*   `Estado` (VARCHAR(50), NOT NULL DEFAULT 'ACTIVA'): Estado actual de la videoconferencia ('ACTIVA', 'FINALIZADA').

### C. Tabla: `ParticipantesSala`
Registra la membresía de usuarios admitidos en una videoconferencia.
*   `IdParticipante` (SERIAL, PK): Identificador único auto-incremental.
*   `IdSala` (INT, FK $\rightarrow$ `Salas.IdSala`, ON DELETE CASCADE): Referencia a la sala.
*   `IdUsuario` (INT, FK $\rightarrow$ `Usuarios.IdUsuario`, ON DELETE CASCADE): Referencia al usuario admitido.
*   `Estado` (VARCHAR(50), NOT NULL DEFAULT 'ACTIVO'): Estado de su participación ('ACTIVO', 'SALIÓ').
*   `FechaIngreso` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Fecha y hora en que fue admitido.
*   *Restricción:* Llave única compuesta `uq_sala_usuario` (`IdSala`, `IdUsuario`) para evitar duplicidad de registros activos.

### D. Tabla: `SolicitudesSala`
Cola temporal de espera para la moderación del Host.
*   `IdSolicitud` (SERIAL, PK): Identificador secuencial.
*   `IdSala` (INT, FK $\rightarrow$ `Salas.IdSala`, ON DELETE CASCADE): Referencia de sala.
*   `IdUsuario` (INT, FK $\rightarrow$ `Usuarios.IdUsuario`, ON DELETE CASCADE): Referencia del invitado postulante.
*   `Estado` (VARCHAR(50), NOT NULL DEFAULT 'PENDIENTE'): Estado de la solicitud ('PENDIENTE', 'ACEPTADO', 'RECHAZADO').
*   `FechaSolicitud` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Marca temporal de postulación.
*   *Restricción:* Llave única compuesta `uq_solicitud_sala_usuario` (`IdSala`, `IdUsuario`) para actualizar en lugar de duplicar peticiones repetidas.

### E. Tabla: `Mensajes`
Guarda el historial persistente de chats grupales de cada sala.
*   `IdMensaje` (SERIAL, PK): Identificador de mensaje.
*   `IdSala` (INT, FK $\rightarrow$ `Salas.IdSala`, ON DELETE CASCADE): Sala a la que pertenece el chat.
*   `IdUsuario` (INT, FK $\rightarrow$ `Usuarios.IdUsuario`, ON DELETE CASCADE): Remitente del mensaje.
*   `Contenido` (TEXT, NOT NULL): Mensaje de texto plano enviado.
*   `FechaEnvio` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Fecha y hora exacta de emisión.

### F. Tabla: `ArchivosCompartidos`
Guarda los metadatos y enlaces físicos de documentos subidos.
*   `IdArchivo` (SERIAL, PK): Identificador del archivo.
*   `IdSala` (INT, FK $\rightarrow$ `Salas.IdSala`, ON DELETE CASCADE): Sala asociada.
*   `IdUsuario` (INT, FK $\rightarrow$ `Usuarios.IdUsuario`, ON DELETE CASCADE): Usuario que comparte.
*   `NombreArchivo` (VARCHAR(255), NOT NULL): Nombre del archivo original (ej. 'presentacion.pdf').
*   `RutaArchivo` (VARCHAR(500), NOT NULL): Ubicación física y nombre único en el servidor (ej. 'uploads/fileId_presentacion.pdf').
*   `FechaSubida` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Fecha de carga.

---

## 3. Script SQL de Migración (DDL)

Puedes ejecutar el siguiente script directamente en el **SQL Editor** del panel de control de Supabase para inicializar el esquema y los datos iniciales de prueba:

```sql
-- DDL de Migración de Tablas
DROP TABLE IF EXISTS ArchivosCompartidos CASCADE;
DROP TABLE IF EXISTS Mensajes CASCADE;
DROP TABLE IF EXISTS SolicitudesSala CASCADE;
DROP TABLE IF EXISTS ParticipantesSala CASCADE;
DROP TABLE IF EXISTS Salas CASCADE;
DROP TABLE IF EXISTS Usuarios CASCADE;

-- Crear Tabla Usuarios
CREATE TABLE Usuarios (
    IdUsuario SERIAL PRIMARY KEY,
    Nombres VARCHAR(150) NOT NULL,
    Correo VARCHAR(150) UNIQUE NOT NULL,
    PasswordHash VARCHAR(255) NOT NULL,
    Rol VARCHAR(50) NOT NULL DEFAULT 'USUARIO'
);

-- Crear Tabla Salas
CREATE TABLE Salas (
    IdSala SERIAL PRIMARY KEY,
    CodigoSala VARCHAR(10) UNIQUE NOT NULL,
    Nombre VARCHAR(150) NOT NULL,
    IdHost INT NOT NULL REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE,
    Estado VARCHAR(50) NOT NULL DEFAULT 'ACTIVA'
);

-- Crear Tabla ParticipantesSala
CREATE TABLE ParticipantesSala (
    IdParticipante SERIAL PRIMARY KEY,
    IdSala INT NOT NULL REFERENCES Salas(IdSala) ON DELETE CASCADE,
    IdUsuario INT NOT NULL REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE,
    Estado VARCHAR(50) NOT NULL DEFAULT 'ACTIVO',
    FechaIngreso TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sala_usuario UNIQUE (IdSala, IdUsuario)
);

-- Crear Tabla SolicitudesSala
CREATE TABLE SolicitudesSala (
    IdSolicitud SERIAL PRIMARY KEY,
    IdSala INT NOT NULL REFERENCES Salas(IdSala) ON DELETE CASCADE,
    IdUsuario INT NOT NULL REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE,
    Estado VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE',
    FechaSolicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_solicitud_sala_usuario UNIQUE (IdSala, IdUsuario)
);

-- Crear Tabla Mensajes
CREATE TABLE Mensajes (
    IdMensaje SERIAL PRIMARY KEY,
    IdSala INT NOT NULL REFERENCES Salas(IdSala) ON DELETE CASCADE,
    IdUsuario INT NOT NULL REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE,
    Contenido TEXT NOT NULL,
    FechaEnvio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Crear Tabla ArchivosCompartidos
CREATE TABLE ArchivosCompartidos (
    IdArchivo SERIAL PRIMARY KEY,
    IdSala INT NOT NULL REFERENCES Salas(IdSala) ON DELETE CASCADE,
    IdUsuario INT NOT NULL REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE,
    NombreArchivo VARCHAR(255) NOT NULL,
    RutaArchivo VARCHAR(500) NOT NULL,
    FechaSubida TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- SEEDS: Cargar Usuarios Iniciales de Prueba
-- Contraseña texto plano: '123456'
-- Hash SHA-256: '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92'
INSERT INTO Usuarios (Nombres, Correo, PasswordHash, Rol) VALUES 
('Host De Prueba', 'host@zoom.com', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'HOST'),
('Invitado De Prueba', 'invitado@zoom.com', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'INVITADO');
```
