# Especificación del Backend - LP2-Zoom

Este documento contiene la especificación de diseño, paquetes y reglas de negocio del servidor multihilo de sockets de **LP2-Zoom**.

## 1. Stack Tecnológico del Backend

El backend se ejecuta como una aplicación de consola en **Java SE** utilizando APIs nativas para la comunicación y el manejo de concurrencia:

*   **Red Nativa:** `java.net.ServerSocket` para escuchar conexiones físicas y `java.net.Socket` para interactuar con los clientes individuales.
*   **Manejo de I/O:** `java.io.BufferedReader` (lectura orientada a líneas de caracteres) y `java.io.PrintWriter` (escritura con auto-flush).
*   **Concurrencia:** `java.util.concurrent.ExecutorService` utilizando un Thread Pool creado a través de `Executors.newCachedThreadPool()`. Esto permite crear hilos dinámicos según se necesiten y reutilizarlos.
*   **Librerías de Terceros:** **Gson** de Google para serializar y deserializar los objetos de tramas JSON.

---

## 2. Estructura de Paquetes (Módulo Servidor)

El código fuente del Servidor se encuentra estructurado bajo los siguientes paquetes:

```text
Servidor/src/main/java/
├── database/                   # Capa de Persistencia y Conectividad
│   ├── ConexionBD.java         # Proveedor estático de conexiones JDBC
│   ├── DBService.java          # Métodos CRUD parametrizados con Supabase
│   └── HashUtils.java          # Criptografía nativa (hashing SHA-256)
├── model/                      # Modelos de Datos compartidos
│   ├── MensajeSocket.java      # Estructura del protocolo JSON de comunicación
│   └── Usuario.java            # Datos del usuario autenticado
└── network/                    # Capa de Comunicación por Red
    ├── MainServidor.java       # Punto de entrada y loop principal ServerSocket
    └── ManejadorCliente.java   # HiloRunnable que maneja el ciclo de vida de cada socket
```

---

## 3. Módulos y Lógica Principal

### A. Servidor Escucha (MainServidor)
El archivo [MainServidor](file:///c:/Users/Jeanpier/OneDrive/Desktop/LP2-Zoom/Servidor/src/main/java/network/MainServidor.java) corre un bucle continuo de aceptación de sockets.

```java
// Hilo principal que ejecuta el bucle de escucha del ServerSocket
try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
    while (running) {
        Socket socketCliente = serverSocket.accept();
        // Inicializa el hilo runnable y lo pasa al Thread Pool para ejecución paralela
        ManejadorCliente manejador = new ManejadorCliente(socketCliente);
        poolHilos.submit(manejador);
    }
}
```

### B. Enrutamiento Basado en Tipo de Mensaje (ManejadorCliente)
Cada hilo de cliente lee continuamente tramas serializadas en JSON. El enrutamiento se ejecuta a través de un bloque `switch-case` evaluando la propiedad `type` del objeto `MensajeSocket`:

```java
// Procesamiento de tramas de sockets según tipo
private void procesarMensaje(MensajeSocket mensaje) {
    String tipo = mensaje.getType();
    switch (tipo) {
        case "LOGIN_REQUEST":
            ejecutarLogin(mensaje);
            break;
        case "CREATE_ROOM":
            ejecutarCrearSala(mensaje);
            break;
        case "JOIN_ROOM_REQUEST":
            ejecutarSolicitudUnirse(mensaje);
            break;
        case "ADMIT_USER":
            ejecutarAdmitirUsuario(mensaje);
            break;
        case "CHAT_MESSAGE":
            ejecutarMensajeChat(mensaje);
            break;
        case "FILE_START":
            ejecutarInicioArchivo(mensaje);
            break;
        case "FILE_CHUNK":
            ejecutarChunkArchivo(mensaje);
            break;
        case "FILE_END":
            ejecutarFinArchivo(mensaje);
            break;
        case "GET_FILES_REQUEST":
            ejecutarListarArchivos(mensaje);
            break;
        case "FILE_DOWNLOAD_REQUEST":
            ejecutarDescargarArchivo(mensaje);
            break;
        case "CAMERA_FRAME":
            ejecutarFrameCamara(mensaje);
            break;
        case "LEAVE_ROOM":
            ejecutarSalirSala(mensaje);
            break;
    }
}
```

---

## 4. Reglas de Negocio y Ciclo de Vida de Conexión

### A. Registro de Conexiones Activas
El servidor almacena los usuarios autenticados en un mapa concurrente global (`MainServidor.clientesActivos` de tipo `ConcurrentHashMap<Integer, ManejadorCliente>`). Esto permite mapear el `userId` numérico del usuario con su respectivo hilo de comunicación activa, facilitando la retransmisión de mensajes.

### B. Desconexión Limpia y Robusta
Cuando un socket cliente se desconecta (ya sea por enviar un mensaje de `LEAVE_ROOM` de forma explícita o por una pérdida abrupta de la conexión física TCP que produce un fin de stream o error de E/S), se invoca el método `desconectar()` o se procesa `ejecutarSalirSala()`.

Para asegurar que las cámaras de los demás participantes dejen de procesarse y no se queden congeladas:
1. **Difusión de Apagado de Cámara:** Antes de remover al participante de la sala y de la base de datos, el hilo de su socket genera y retransmite un mensaje con tipo `CAMERA_STATE` y contenido `"OFF"` dirigido a todos los demás integrantes activos en la sala.
2. **Remoción de Conexiones Activas:** Se retira al usuario del mapa concurrente global `MainServidor.clientesActivos`.
3. **Persistencia del Estado:** Se actualiza el estado de la solicitud en Supabase a `"RECHAZADO"` (desconectado).
4. **Mensaje de Sistema:** Se envía un mensaje tipo `CHAT_MESSAGE` con remitente `"SISTEMA"` informando sobre la salida o desconexión del usuario.
5. **Cierre Físico:** Se cierra el canal `Socket` del cliente de forma segura.

Esto garantiza que no existan fugas de memoria (hilos huérfanos que consuman recursos) ni inconsistencias visuales (como pantallas congeladas en el último fotograma) en las pantallas de los demás participantes que siguen conectados en la sala.
