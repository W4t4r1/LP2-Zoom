-- ========================================================
-- SCHEMA SQL PARA SUPABASE - LP2-ZOOM
-- ========================================================

-- Eliminar tablas existentes para evitar conflictos al re-ejecutar
DROP TABLE IF EXISTS ArchivosCompartidos CASCADE;
DROP TABLE IF EXISTS Mensajes CASCADE;
DROP TABLE IF EXISTS SolicitudesSala CASCADE;
DROP TABLE IF EXISTS ParticipantesSala CASCADE;
DROP TABLE IF EXISTS Salas CASCADE;
DROP TABLE IF EXISTS Usuarios CASCADE;

-- 1. Tabla: Usuarios
CREATE TABLE Usuarios (
    IdUsuario SERIAL PRIMARY KEY,
    Nombres VARCHAR(150) NOT NULL,
    Correo VARCHAR(150) UNIQUE NOT NULL,
    PasswordHash VARCHAR(255) NOT NULL,
    Rol VARCHAR(50) NOT NULL DEFAULT 'USUARIO'
);

-- 2. Tabla: Salas
CREATE TABLE Salas (
    IdSala SERIAL PRIMARY KEY,
    CodigoSala VARCHAR(10) UNIQUE NOT NULL, -- Código de 6 letras/números generado por el Host
    Nombre VARCHAR(150) NOT NULL,
    IdHost INT NOT NULL REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE,
    Estado VARCHAR(50) NOT NULL DEFAULT 'ACTIVA' -- ACTIVA, FINALIZADA
);

-- 3. Tabla: ParticipantesSala (Miembros que han sido admitidos o estuvieron en la sala)
CREATE TABLE ParticipantesSala (
    IdParticipante SERIAL PRIMARY KEY,
    IdSala INT NOT NULL REFERENCES Salas(IdSala) ON DELETE CASCADE,
    IdUsuario INT NOT NULL REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE,
    Estado VARCHAR(50) NOT NULL DEFAULT 'ACTIVO', -- ACTIVO, SALIÓ
    FechaIngreso TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sala_usuario UNIQUE (IdSala, IdUsuario) -- Evita duplicados activos
);

-- 4. Tabla: SolicitudesSala (Cola de espera de invitados esperando aprobación del Host)
CREATE TABLE SolicitudesSala (
    IdSolicitud SERIAL PRIMARY KEY,
    IdSala INT NOT NULL REFERENCES Salas(IdSala) ON DELETE CASCADE,
    IdUsuario INT NOT NULL REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE,
    Estado VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE, ACEPTADO, RECHAZADO
    FechaSolicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_solicitud_sala_usuario UNIQUE (IdSala, IdUsuario)
);

-- 5. Tabla: Mensajes (Chat grupal en tiempo real)
CREATE TABLE Mensajes (
    IdMensaje SERIAL PRIMARY KEY,
    IdSala INT NOT NULL REFERENCES Salas(IdSala) ON DELETE CASCADE,
    IdUsuario INT NOT NULL REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE,
    Contenido TEXT NOT NULL,
    FechaEnvio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Tabla: ArchivosCompartidos (Metadatos de documentos compartidos)
CREATE TABLE ArchivosCompartidos (
    IdArchivo SERIAL PRIMARY KEY,
    IdSala INT NOT NULL REFERENCES Salas(IdSala) ON DELETE CASCADE,
    IdUsuario INT NOT NULL REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE,
    NombreArchivo VARCHAR(255) NOT NULL,
    RutaArchivo VARCHAR(500) NOT NULL, -- Ruta física en la carpeta del servidor
    FechaSubida TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ========================================================
-- DATOS DE PRUEBA INICIALES (OPCIONAL)
-- Nota: Las contraseñas en texto plano '123456' o similar deben guardarse hasheadas.
-- El hash mostrado abajo corresponde a un SHA-256 de '123456': 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92
-- ========================================================

-- Insertar un usuario Host de prueba
INSERT INTO Usuarios (Nombres, Correo, PasswordHash, Rol) VALUES 
('Host De Prueba', 'host@zoom.com', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'HOST'),
('Invitado De Prueba', 'invitado@zoom.com', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'INVITADO');
