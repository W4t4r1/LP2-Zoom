% PROYECTO FINAL: LP2-ZOOM
% Integrantes: Marco Huanca, Jonathan Leon, Jeanpier Robles
% Docente: Yan Eduardo Cisneros Napravnik

# CAPÍTULO 1: INTRODUCCIÓN Y ARQUITECTURA DE RED (Expositor: Marco Huanca)

## LP2-Zoom: Prototipo Académico de Videoconferencia y Mensajería
- **Desafío Principal:** Construcción de un sistema distribuido multicliente de tiempo real.
- **Fundamentos de Red:** Comunicación directa sobre Sockets TCP nativos en Java SE.
- **Concurrencia Manual:** Gestión de flujos de datos sin frameworks comerciales de alto nivel.
- **Aislamiento Seguro:** Integración con base de datos PostgreSQL remota (Supabase).

---

## Regla de Oro de la Arquitectura (Aislamiento de Persistencia)
- **Principio:** El cliente jamás realiza conexiones directas JDBC a la base de datos.
- **Justificación:**
  1. *Seguridad:* Evita la exposición de credenciales mediante ingeniería inversa/descompilación.
  2. *Modularidad:* Los cambios en el motor de persistencia se aíslan al backend.
  3. *Control de Reglas de Negocio:* Validación centralizada en el servidor de sockets.

![](images/vista_general.png){width=80%}

---

## Arquitectura del Cliente (Threading y Event Dispatch Thread)
- **Reto Gráfico (Swing):** El EDT es monocanal. Operaciones bloqueantes de red congelan la UI.
- **Solución implementada:**
  - *Hilo de Escucha (Background):* Lee de forma asíncrona y bloqueante el stream del socket.
  - *Actualización del EDT:* Delegada mediante `SwingUtilities.invokeLater(...)`.
  - *Patrón Observer:* Notificaciones asíncronas vía callback (`MensajeListener`).
  - *Decodificación de Video:* Delegada a un pool concurrente secundario daemon.

---

## Arquitectura del Servidor (ServerSocket y CachedThreadPool)
- **Escucha del Servidor:** Bucle principal bloqueante en `serverSocket.accept()` (Puerto 5000).
- **Concurrencia Multicliente:**
  - Uso de `Executors.newCachedThreadPool()` para gestionar hilos de forma dinámica.
  - Creación dinámica de un hilo por cada socket cliente físico (`ManejadorCliente`).
  - Reutilización de hilos inactivos y destrucción tras 60 segundos de inactividad.

---

# CAPÍTULO 2: PATRONES DE DISEÑO Y ESTRUCTURA UML (Expositor: Jonathan Leon)

## Patrones de Diseño Aplicados (Creacionales y Comportamiento)
- **Singleton (`ClienteConexion`):**
  - Asegura un único canal físico TCP abierto durante el ciclo de vida del cliente.
- **Factory Method y Strategy (`CameraCreator` & `CameraStrategy`):**
  - Desacopla la captura física (webcam) de la simulación de video en fallback.
- **Memento (`ChatInputMemento` & `ChatHistoryCaretaker`):**
  - Captura e introduce historial de borradores en la caja de chat con las flechas direccionales.

---

## Patrones de Diseño Aplicados (Estructurales)
- **Proxy (`DBProxy` & `CameraProxy`):**
  - *Virtual/Lazy-load Proxy:* Carga la conexión JDBC solo en la primera consulta.
  - *Logging Proxy:* Audita y registra las transacciones en la consola.
  - *Fallback Proxy:* Conmutación automática a cámara simulada si falla el hardware.
- **Bridge (`ProtocolBridge`):**
  - Separa la capa de red del formato de serialización (conversión JSON con Gson).

---

## Diagrama de Clases UML del Proyecto

![](images/clases.png){width=85%}

---

## Diagrama de Secuencia (Sala de Espera y Moderación)

![](images/secuencia.png){width=70%}

---

# CAPÍTULO 3: BASE DE DATOS, PROTOCOLO Y CONCLUSIONES (Expositor: Jeanpier Robles)

## Diseño de Base de Datos (Modelo Entidad-Relación)
- **Tablas Principales:** `Usuarios`, `Salas`, `ParticipantesSala`, `SolicitudesSala`, `Mensajes`, `ArchivosCompartidos`.
- **Integridad y Seguridad:**
  - Borrado en cascada (`ON DELETE CASCADE`) para prevenir registros huérfanos.
  - Restricciones únicas para colas de admisión y sesiones concurrentes.

![](images/modelo_er.png){width=55%}

---

## Protocolo de Mensajería sobre Sockets (Trama JSON)
- **Estructura Estándar (`MensajeSocket`):** Contrato común en formato JSON.
  - Campos: `type`, `roomCode`, `userId`, `userName`, `message`, `sentAt`.
- **Flujo de Archivos Fragmentados:**
  - Transferencia segmentada en chunks codificados en Base64.
  - Previene desbordamiento de memoria de la JVM.
- **Flujo de Video:**
  - Transmisión continua de fotogramas (frames codificados) a través del canal de chat de video.

---

## Tolerancia a Fallos y Conclusiones
- **Manejo de Desconexión Abrupta:**
  - Cierre físico del socket, liberación de memoria en el servidor.
  - Actualización automática de la BD a estado `SALIÓ` y notificación general a la sala.
- **Conclusiones del Proyecto:**
  1. *Modularidad:* Los patrones de diseño estructuran código escalable y reutilizable.
  2. *Seguridad Industrial:* El aislamiento de BD previene la fuga de credenciales.
- **Trabajo Futuro:** Transición a streams nativos de video RTP/RTSP.
