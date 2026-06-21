# Contrato de Protocolo y Mensajería TCP - LP2-Zoom

Este documento especifica el formato exacto de las tramas de datos JSON transmitidas en los canales de entrada y salida de los sockets TCP de **LP2-Zoom**.

## 1. Estructura Estándar de la Trama JSON

Todos los mensajes que circulan por los sockets se mapean a la clase [MensajeSocket](file:///c:/Users/Jeanpier/OneDrive/Desktop/LP2-Zoom/Cliente/src/main/java/model/MensajeSocket.java) y deben representarse como una única línea de texto finalizada con un carácter de salto de línea `\n`.

### Campos del Objeto JSON

| Campo | Tipo | Requerido | Descripción |
| :--- | :---: | :---: | :--- |
| `type` | String | Sí | Tipo de transacción o evento. Funciona como el enrutador en el backend y el frontend. |
| `roomCode` | String | Condicional | Código alfanumérico único de 6 caracteres asignado a la sala activa. |
| `userId` | Integer | Condicional | Identificador numérico del usuario emisor o del destinatario. |
| `userName` | String | Condicional | Nombre en texto claro o correo electrónico del usuario. |
| `message` | String | Condicional | Payload flexible (texto de chat, credenciales encriptadas,Base64, o arreglos JSON). |
| `sentAt` | String | No | Fecha y hora en formato ISO o Timestamp. |

---

## 2. Definición de Transacciones y Payloads

### A. Autenticación (Login)

#### 1. `LOGIN_REQUEST` (Cliente $\rightarrow$ Servidor)
Petición para validar las credenciales de un usuario.
*   `userName`: Correo del usuario.
*   `message`: Contraseña en texto plano (el servidor la hashea y valida).

```json
{
  "type": "LOGIN_REQUEST",
  "userName": "host@zoom.com",
  "message": "123456"
}
```

#### 2. `LOGIN_RESPONSE` (Servidor $\rightarrow$ Cliente)
Respuesta a la validación de credenciales.
*   `message`: `"SUCCESS"` en caso de éxito; o un mensaje explicativo de error.
*   `userId`: ID del usuario asignado en la BD (toma valor al ingresar exitosamente).
*   `userName`: Nombre completo del usuario para la sesión.

```json
{
  "type": "LOGIN_RESPONSE",
  "message": "SUCCESS",
  "userId": 1,
  "userName": "Host De Prueba"
}
```

---

### B. Gestión de Salas y Aprobación

#### 1. `CREATE_ROOM` (Cliente $\rightarrow$ Servidor)
Solicitud del Host para crear una videoconferencia.
*   `userId`: Identificador del Host.
*   `message`: Nombre de la videoconferencia.

```json
{
  "type": "CREATE_ROOM",
  "userId": 1,
  "message": "Videoconferencia de Redes"
}
```

#### 2. `CREATE_ROOM` (Servidor $\rightarrow$ Cliente Host)
Confirmación de creación de sala por el servidor.
*   `roomCode`: Código alfanumérico único de 6 caracteres generado (ej: `"17F082"`).
*   `message`: `"SUCCESS"`.

```json
{
  "type": "CREATE_ROOM",
  "roomCode": "17F082",
  "message": "SUCCESS"
}
```

#### 3. `JOIN_ROOM_REQUEST` (Invitado $\rightarrow$ Servidor)
Petición para entrar en cola a una sala existente.
*   `roomCode`: Código de la sala a unirse.
*   `userId`: ID del invitado postulante.
*   `userName`: Nombre del invitado.

```json
{
  "type": "JOIN_ROOM_REQUEST",
  "roomCode": "17F082",
  "userId": 2,
  "userName": "Invitado De Prueba"
}
```

#### 4. `JOIN_ROOM_RESPONSE` (Servidor $\rightarrow$ Invitado)
Respuesta a la postulación del invitado.
*   `roomCode`: Código de la sala.
*   `message`: `"PENDIENTE"` (solicitud en cola en espera del host) o `"ERROR: La sala no existe"`.

```json
{
  "type": "JOIN_ROOM_RESPONSE",
  "roomCode": "17F082",
  "message": "PENDIENTE"
}
```

#### 5. `WAITING_ROOM_UPDATE` (Servidor $\rightarrow$ Host)
Actualización periódica enviada al Host para refrescar su cola visual.
*   `roomCode`: Código de la sala.
*   `message`: Arreglo serializado en JSON con las solicitudes en estado `PENDIENTE`.

```json
{
  "type": "WAITING_ROOM_UPDATE",
  "roomCode": "17F082",
  "message": "[{\"fechaSolicitud\":\"2026-06-19 23:43:51\",\"idUsuario\":2,\"correo\":\"invitado@zoom.com\",\"nombres\":\"Invitado De Prueba\"}]"
}
```

#### 6. `ADMIT_USER` (Host $\rightarrow$ Servidor $\rightarrow$ Invitado)
*   **Host $\rightarrow$ Servidor:** El host envía la decisión (`"ACEPTAR"` o `"RECHAZAR"`).
    *   `userId`: ID del invitado a admitir/rechazar.
```json
{
  "type": "ADMIT_USER",
  "roomCode": "17F082",
  "userId": 2,
  "message": "ACEPTAR"
}
```
*   **Servidor $\rightarrow$ Invitado:** El servidor notifica el resultado (`"ACCEPTED"` o `"REJECTED"`).
```json
{
  "type": "ADMIT_USER",
  "roomCode": "17F082",
  "message": "ACCEPTED"
}
```

---

### C. Chat en Tiempo Real

#### `CHAT_MESSAGE` (Bidireccional)
*   **Emisión Cliente:** Envía el texto escrito.
*   **Difusión Servidor:** Reenvía el texto a todos los miembros de la sala.
*   **Comando Especial:** Si `message` es `"REQUEST_HISTORY"`, el servidor intercepta el comando y responde enviando el historial de mensajes de la base de datos uno a uno, solo al socket emisor.

```json
{
  "type": "CHAT_MESSAGE",
  "roomCode": "17F082",
  "userId": 2,
  "userName": "Invitado De Prueba",
  "message": "Hola a todos, esta es una prueba de integración!"
}
```

---

### D. Compartición de Documentos (Archivos)

#### 1. `FILE_START` (Cliente $\rightarrow$ Servidor)
Inicia la transferencia de un archivo compartido.
*   `message`: Formato `fileId|nombreArchivo`.

```json
{
  "type": "FILE_START",
  "roomCode": "17F082",
  "message": "a7b8c9d0|presentacion.pdf"
}
```

#### 2. `FILE_CHUNK` (Cliente $\rightarrow$ Servidor)
Envía un bloque binario del archivo.
*   `message`: Formato `fileId|chunkBase64` (datos codificados en texto).

```json
{
  "type": "FILE_CHUNK",
  "roomCode": "17F082",
  "message": "a7b8c9d0|JVBERi0xLjQKJcOkw7zDpzcKMSAwIG9iagogIDw8IC9UeXBlIC9DYXRh..."
}
```

#### 3. `FILE_END` (Cliente $\rightarrow$ Servidor)
Finaliza la subida de un archivo.
*   `message`: El `fileId` único asignado al archivo.

```json
{
  "type": "FILE_END",
  "roomCode": "17F082",
  "message": "a7b8c9d0"
}
```

#### 4. `GET_FILES_REQUEST` (Cliente $\rightarrow$ Servidor)
Solicita la lista de documentos subidos en esta sala.

```json
{
  "type": "GET_FILES_REQUEST",
  "roomCode": "17F082"
}
```

#### 5. `GET_FILES_RESPONSE` (Servidor $\rightarrow$ Cliente)
Retorna la lista de archivos de la sala serializada en JSON en el campo `message`.

```json
{
  "type": "GET_FILES_RESPONSE",
  "roomCode": "17F082",
  "message": "[{\"nombreArchivo\":\"presentacion.pdf\",\"rutaArchivo\":\"uploads/a7b8c9d0_presentacion.pdf\",\"nombres\":\"Invitado De Prueba\",\"fechaSubida\":\"2026-06-19 23:44:00\"}]"
}
```

#### 6. `FILE_DOWNLOAD_REQUEST` (Cliente $\rightarrow$ Servidor)
Solicita la descarga de un archivo enviando su ruta física del servidor.
*   `message`: Ruta física del archivo en el servidor.

```json
{
  "type": "FILE_DOWNLOAD_REQUEST",
  "roomCode": "17F082",
  "message": "uploads/a7b8c9d0_presentacion.pdf"
}
```

*(Nota: La descarga física del archivo desde el servidor hacia el cliente se realiza en reversa utilizando la secuencia FILE\_START, FILE\_CHUNK y FILE\_END desde el servidor).*

---

### E. Transmisión de Cámara Básica

#### `CAMERA_FRAME` (Cliente $\rightarrow$ Servidor $\rightarrow$ Invitados de Sala)
Envío periódico de imágenes de webcam locales. El servidor retransmite el frame a todos en la sala menos al remitente para optimizar la red.
*   `message`: Fotograma en formato JPG codificado en Base64.
*   *Recomendación de Resolución:* 320x240 o inferior para prevenir sobrecarga de sockets.

```json
{
  "type": "CAMERA_FRAME",
  "roomCode": "17F082",
  "userId": 2,
  "message": "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQ..."
}
```

#### `CAMERA_STATE` (Cliente → Servidor → Invitados de Sala)
Notificación de activación/desactivación de cámara. El servidor retransmite el mensaje a los demás participantes de la sala (excluyendo al remitente) para que su UI muestre el estado de cámara apagada o encendida.
*   `message`: `"ON"` o `"OFF"`.
*   `roomCode`: Código de la sala activa.
*   `userId`: ID del usuario que cambió el estado de cámara.

```json
{
  "type": "CAMERA_STATE",
  "roomCode": "17F082",
  "userId": 2,
  "message": "OFF"
}
```
