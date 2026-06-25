# Control de Fases del Proyecto - LP2-Zoom

Este documento registra las fases de desarrollo del proyecto, el estado actual de cada componente (Backend vs. Frontend) y las tareas específicas completadas y pendientes.

---

## 📊 Resumen General del Avance

| Fase de Desarrollo | Estado Actual | % Avance | Detalle de lo Implementado / Pendiente |
| :--- | :---: | :---: | :--- |
| **Infraestructura y BD** | 🟢 Completado | 100% | Archivo `.gitignore` limpio, carpeta `/db` con script DDL `schema.sql` y JDBC validado con éxito. |
| **Fase 1: Sockets Multicliente** | 🟢 Completado | 100% | Servidor multihilo con `CachedThreadPool`. Cliente asíncrono con [ClienteConexion](../Cliente/src/main/java/network/ClienteConexion.java) (Singleton/Observer) que evita el congelamiento de la UI. |
| **Fase 2: Registro y Login** | 🟢 Completado | 100% | Hashing SHA-256 en [HashUtils](../Servidor/src/main/java/database/HashUtils.java), CRUD en [DBService](../Servidor/src/main/java/database/DBService.java) y login reactivo asíncrono en [LoginFrame](../Cliente/src/main/java/UI/LoginFrame.java). |
| **Fase 3: Gestión de Salas** | 🟢 Completado | 100% | Creación de salas, unión a salas, sala de espera con admisión interactiva (Admitir/Rechazar), estado de espera admitido y arranque de reunión controlado por el Host. |
| **Fase 4: Chat en Tiempo Real** | 🟢 Completado | 100% | Difusión de mensajes en tiempo real, persistencia en Supabase, y carga automática del historial de chats al ingresar a la reunión. |
| **Fase 5: Envío de Documentos** | 🟢 Completado | 100% | Compartición de archivos fragmentados por socket, listado interactivo en modal, descarga asíncrona segura con validación de rutas en servidor. |
| **Fase 6: Transmisión de Cámara** | 🟢 Completado | 100% | Servidor y cliente listos. Retransmisión de fotogramas de video (`CAMERA_FRAME`) y estados de cámara (`CAMERA_STATE`) activa. Cliente integra captura de webcam física con la librería `webcam-capture` y simulación académica de video (conmutación y fallback automáticos en el Proxy de Cámara). Grid visual y controles integrados en `RoomFrame`. |

---

## 🛠️ Detalle de Tareas por Fase

### 📂 Infraestructura y Base de Datos
*   [x] Configuración de dependencias de Maven en ambos módulos (Gson, PostgreSQL Driver, etc.).
*   [x] Configuración limpia del archivo [.gitignore](../.gitignore).
*   [x] Estructura del script DDL SQL ([schema.sql](../db/schema.sql)) para Supabase.
*   [x] Pruebas de conectividad JDBC exitosas en servidor.

---

### 📡 Fase 1: Sockets Multicliente
*   [x] Arquitectura de red asíncrona no bloqueante para el cliente.
*   [x] Implementación de patrón Singleton y Observer en [ClienteConexion](../Cliente/src/main/java/network/ClienteConexion.java) para notificaciones asíncronas en UI.
*   [x] Servidor principal (`MainServidor`) escuchando en puerto `5000` con `CachedThreadPool`.
*   [x] Manejador de hilos por cliente (`ManejadorCliente`) con lectura en bucle de tramas JSON.

---

### 🔐 Fase 2: Registro y Login con Hashing
*   [x] Métodos de persistencia en base de datos (`login` y `registrar`) en [DBService](../Servidor/src/main/java/database/DBService.java).
*   [x] Algoritmo SHA-256 nativo implementado para hash de contraseñas.
*   [x] Formulario moderno de inicio de sesión en Swing ([LoginFrame](../Cliente/src/main/java/UI/LoginFrame.java)).
*   [x] Conexión reactiva que redirige a [RoomFrame](../Cliente/src/main/java/UI/RoomFrame.java) tras login exitoso.

---

### 🚪 Fase 3: Gestión de Salas y Espera
*   **Backend (100%):**
    *   [x] Crear sala (`CREATE_ROOM`) con código único autogenerado y registro en BD.
    *   [x] Solicitar unión (`JOIN_ROOM_REQUEST`) con código de sala, dejando al usuario en estado `PENDIENTE`.
    *   [x] Notificar actualizaciones de sala de espera al Host en tiempo real (`WAITING_ROOM_UPDATE`).
    *   [x] Lógica de aceptación o rechazo (`ADMIT_USER`) con actualización en cascada de la tabla `ParticipantesSala`.
*   **Frontend (100%):**
    *   [x] Habilitar el flujo en [RoomFrame](../Cliente/src/main/java/UI/RoomFrame.java) para redirigir dinámicamente según la respuesta del host.
    *   [x] Refrescar dinámicamente la lista de solicitudes en el panel del Host usando la data JSON de `WAITING_ROOM_UPDATE`.
    *   [x] Implementar botones Admitir/Rechazar en la UI del Host que envíen `ADMIT_USER`.
    *   [x] Gestionar en el Invitado la transición automática a la pantalla de reunión activa al recibir `ACCEPTED`.

---

### 💬 Fase 4: Chat en Tiempo Real
*   **Backend (100%):**
    *   [x] Recepción de mensajes de texto en servidor.
    *   [x] Persistencia directa del historial de chats en la tabla `Mensajes` en Supabase.
    *   [x] Retransmisión grupal (`retransmitirMensaje`) a todos los clientes asociados a la sala.
*   **Frontend (100%):**
    *   [x] Conectar la caja de texto `txtMensajeChat` y el botón `btnEnviarChat` de [RoomFrame](../Cliente/src/main/java/UI/RoomFrame.java) para emitir el evento `CHAT_MESSAGE`.
    *   [x] Pintar en tiempo real los mensajes recibidos en el área de texto `txtAreaChat`.
    *   [x] Implementar formateador visual o autoscroll para que los mensajes de chat nuevos siempre sean visibles.
    *   [x] Carga automática del historial de mensajes previo al entrar a la sala.

---

### 📄 Fase 5: Envío de Documentos
*   **Backend (100%):**
    *   [x] Servidor maneja la trama `FILE_START` para inicializar el stream físico de escritura local.
    *   [x] Servidor acumula datos en binario recibidos en Base64 mediante tramas `FILE_CHUNK`.
    *   [x] Servidor consolida y cierra el archivo físico en `uploads/` al recibir `FILE_END`.
    *   [x] Registro de metadatos (ruta física, nombre de archivo, sala y usuario) en la tabla `ArchivosCompartidos`.
    *   [x] Envío de notificación automática tipo chat avisando a los usuarios que se ha compartido un archivo.
*   **Frontend (100%):**
    *   [x] Añadir botón para seleccionar archivos locales (`JFileChooser`).
    *   [x] Implementar lógica para fragmentar el archivo seleccionado en chunks binarios y enviarlos a través del socket.
    *   [x] Agregar panel o lista visual de archivos compartidos dentro de la sala de videoconferencia.
    *   [x] Habilitar opción para descargar/guardar los archivos compartidos solicitándolos al servidor.

---

### 📷 Fase 6: Transmisión de Cámara Básica
*   **Backend (100%):**
    *   [x] Retransmisión selectiva de frames de imagen en Base64 (`CAMERA_FRAME`).
    *   [x] Exclusión automática del emisor en la retransmisión para ahorrar ancho de banda y evitar auto-renderizado.
*   **Frontend (100%):**
    *   [x] Configurar captura periódica de la webcam local (o generar una transmisión simulada de imágenes locales si no hay webcam física).
    *   [x] Rutinas para redimensionar fotos a 320x240, comprimir a JPG y codificar a Base64.
    *   [x] Diseñar e implementar el grid de video (`pnlVideoGrid`) en [RoomFrame](../Cliente/src/main/java/UI/RoomFrame.java) para acomodar y refrescar dinámicamente los paneles de cada usuario activo con su correspondiente imagen.
    *   [x] Añadir control de cámara ON/OFF en la UI y cambio de estado para iniciar/detener la transmisión.
