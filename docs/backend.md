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
Cuando un socket cliente se desconecta (ya sea por enviar un mensaje de `LEAVE_ROOM` de forma explícita o por una pérdida abrupta de la conexión física TCP que produce un fin de stream o error de E/S), se invoca el método `desconectar()`:

```java
// Limpieza de recursos al cerrar la sesión de un cliente
private void desconectar() {
    try {
        if (this.userId != null) {
            // 1. Remover del mapa de conexiones activas
            MainServidor.clientesActivos.remove(this.userId);
            
            // 2. Si estaba en una sala activa, marcar su salida de la BD
            if (this.roomCode != null) {
                DBService.actualizarEstadoSolicitud(this.roomCode, this.userId, "RECHAZADO");
                
                // 3. Notificar a los demás integrantes de la sala sobre la salida
                MensajeSocket alerta = new MensajeSocket("CHAT_MESSAGE", this.roomCode, 0, "SISTEMA", this.userName + " se ha desconectado.", null);
                MainServidor.retransmitirMensaje(alerta, null);
            }
        }
        // 4. Cerrar sockets y flujos de datos físicos
        if (socketCliente != null && !socketCliente.isClosed()) {
            socketCliente.close();
        }
    } catch (Exception e) {
        System.err.println("[-] Error cerrando socket para " + this.userName + ": " + e.getMessage());
    }
}
```
Esto garantiza que no existan fugas de memoria (hilos huérfanos que consuman recursos) ni inconsistencias en las interfaces de los demás participantes que siguen conectados en la sala.
