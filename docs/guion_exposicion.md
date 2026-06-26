# GUION DE EXPOSICIÓN TRABAJO FINAL: LP2-ZOOM

---

## Estructura General de la Presentación
*   **Tiempo total estimado:** ~9 minutos (aprox. 3 minutos por expositor).
*   **Expositores:** 
    1.  **Marco Huanca** (Introducción, Arquitectura de Red y Concurrencia)
    2.  **Jonathan Leon** (Patrones de Diseño y Estructura UML)
    3.  **Jeanpier Robles** (Base de Datos, Protocolo de Sockets, Implementación y Conclusiones)

---

## DIAPOSITIVA 1: PORTADA E INTRODUCCIÓN

### **Expositor 1: Marco Huanca (Minuto 0:00 - 3:00)**

*(Pacing: Calmado, seguro, estableciendo el tono académico y la relevancia del proyecto).*

**[0:00 - 0:45] Introducción y Contexto**
> *"Buenas tardes, docente y compañeros. Hoy les presentaremos **LP2-Zoom**, un prototipo académico de videoconferencia y mensajería distribuida en tiempo real desarrollado enteramente en Java SE y PostgreSQL en la nube mediante Supabase. El desafío principal que aborda este proyecto es la construcción de un sistema concurrente distribuido de baja latencia sin depender de frameworks comerciales de alto nivel como WebSockets o servidores web embebidos. En su lugar, hemos bajado al nivel más fundamental de la red: los Sockets TCP nativos, lo que nos ha permitido entender y controlar de primera mano el flujo de bytes, la sincronización de hilos y la arquitectura cliente-servidor."*

---

## DIAPOSITIVA 2: REGLA DE ORO Y ARQUITECTURA DE ALTO NIVEL

**[0:45 - 1:30] Aislamiento de Persistencia y Regla de Oro**
*(Señalando el diagrama de componentes / vista general)*
> *"Para garantizar la integridad y robustez del sistema, definimos una **Regla de Oro en la Arquitectura**: el cliente jamás tiene contacto directo con la base de datos a través de JDBC. Si expusiéramos las credenciales en el cliente, nuestro sistema sería vulnerable a la descompilación. En su lugar, toda interacción con Supabase se realiza a través de nuestro servidor central. Este actúa como un middleware y orquestador seguro, aplicando hashing criptográfico SHA-256 a las contraseñas antes de consultar o escribir en la base de datos."*

---

## DIAPOSITIVA 3: ARQUITECTURA DEL CLIENTE (UI vs. RED)

**[1:30 - 2:15] Threading en el Cliente y EDT**
> *"En el desarrollo de interfaces con Java Swing, nos enfrentamos a un reto crítico: el Event Dispatch Thread, o EDT, es monocanal. Si ejecutáramos la lectura de datos de red bloqueante en el mismo hilo gráfico, la interfaz del usuario se congelaría por completo al esperar una trama. Para solucionarlo, implementamos un desacoplamiento de hilos: un hilo secundario independiente (`HiloEscuchaCliente`) gestiona la escucha en segundo plano del Socket. Al recibir un paquete JSON, delega su renderización en la pantalla mediante `SwingUtilities.invokeLater`. Asimismo, para la visualización del video, usamos un pool de decodificación asíncrono para que la decodificación de las imágenes en Base64 no sobrecargue la lectura de los sockets principales."*

---

## DIAPOSITIVA 4: ARQUITECTURA DEL SERVIDOR Y CONCURRENCIA

**[2:15 - 3:00] Servidor Multicliente**
> *"En la parte del servidor, la concurrencia es manejada mediante un pool de hilos dinámico (`CachedThreadPool`). Cada cliente que se conecta físicamente al puerto 5000 genera una instancia de `ManejadorCliente` que se ejecuta en su propio hilo asíncrono. El pool reutiliza hilos inactivos y destruye aquellos que quedan ociosos por más de 60 segundos, optimizando el consumo de recursos de hardware. Ahora daré el pase a mi compañero Jonathan Leon, quien explicará en detalle los patrones de diseño aplicados y la estructura de clases del proyecto."*

---

## DIAPOSITIVA 5: PATRONES DE DISEÑO EN LP2-ZOOM

### **Expositor 2: Jonathan Leon (Minuto 3:00 - 6:00)**

*(Pacing: Dinámico, estructurado, enfocándose en la modularidad y el desacoplamiento de clases).*

**[3:00 - 3:45] Patrones de Creación y Estructura Global**
> *"Gracias, Marco. Para que un sistema de esta naturaleza sea escalable y fácil de mantener, aplicamos transversalmente **6 patrones de diseño**. El primero es el patrón **Singleton**, utilizado en la clase `ClienteConexion`. Este asegura que a lo largo de toda la ejecución del cliente exista un único socket físico TCP abierto hacia el servidor. En segundo lugar, aplicamos **Factory Method** en conjunto con el patrón **Strategy** para la captura de video. Contamos con un `CameraCreator` que decide dinámicamente si instanciar una cámara física a través de la biblioteca WebCam de hardware, o una cámara simulada si el dispositivo no cuenta con hardware de captura, aislando esta lógica de la interfaz gráfica principal."*

---

## DIAPOSITIVA 6: PATRONES ESTRUCTURALES Y DE COMPORTAMIENTO

**[3:45 - 4:45] Proxy, Bridge y Memento**
> *"Para la base de datos, implementamos un **Proxy Virtual** (`DBProxy`). Este realiza una carga perezosa o 'lazy loading' del driver de Supabase, acelerando sustancialmente el inicio del servidor, además de actuar como un **Logging Proxy** que audita las consultas en consola.
> En la comunicación de red, aplicamos el patrón **Bridge**, separando la abstracción de transmisión física en `ClienteConexion` de la serialización del protocolo. La interfaz `ProtocolBridge` delega la conversión a JSON. Si el día de mañana decidimos migrar a tramas binarias o XML, solo cambiamos el Bridge, sin tocar el código del socket ni la UI.
> Finalmente, a nivel de interfaz de chat, implementamos el patrón **Memento**. Con la ayuda de un `ChatHistoryCaretaker`, guardamos el estado del texto que el usuario escribe en la sala de chat como borrador, permitiéndole navegar entre borradores anteriores y siguientes usando las flechas de dirección (arriba y abajo), simulando la experiencia de terminales avanzadas."*

---

## DIAPOSITIVA 7: DIAGRAMAS UML Y ESTRUCTURA DE CLASES

**[4:45 - 6:00] UML: Diagramas de Casos de Uso, Clases y Secuencia**
*(Señalando los diagramas correspondientes de Clases y Secuencia en la pantalla)*
> *"En pantalla pueden observar nuestro **Diagrama de Clases**, donde se aprecia claramente la relación de composición de `ClienteConexion` con la interfaz del Bridge, y cómo las diferentes pantallas implementan la interfaz observadora `MensajeListener`.
> Asimismo, el **Diagrama de Secuencia** ilustra el flujo de admisión asíncrono. Cuando un invitado solicita ingresar a una sala, el servidor registra su estado como 'PENDIENTE' y notifica al Host en tiempo real. La interfaz del Host se actualiza reactivamente mediante el Event Dispatch Thread, permitiéndole admitir o denegar el acceso. Tras la admisión, el Host inicia la reunión y el servidor propaga simultáneamente la orden de arranque a todos los invitados. A continuación, Jeanpier Robles explicará el diseño de base de datos, el protocolo de comunicación y la demostración práctica del software."*

---

## DIAPOSITIVA 8: DISEÑO DE BASE DE DATOS Y PROTOCOLO

### **Expositor 3: Jeanpier Robles (Minuto 6:00 - 9:00)**

*(Pacing: Técnico, práctico, remarcando la seguridad, la gestión de errores y el cierre formal).*

**[6:00 - 6:45] Modelo Relacional y Protocolo JSON**
> *"Gracias, Jonathan. El diseño de nuestra base de datos en Supabase sigue la tercera forma normal. Está compuesto por 6 tablas clave: `Usuarios`, `Salas`, `ParticipantesSala`, `SolicitudesSala`, `Mensajes` y `ArchivosCompartidos`, con restricciones de clave única para prevenir solicitudes duplicadas y borrado en cascada para evitar la persistencia de datos huérfanos.
> Para la mensajería, diseñamos un protocolo estándar estructurado en tramas JSON representadas por la clase `MensajeSocket`. Cada paquete tiene un encabezado de tipo (`type`) que el servidor procesa usando una estructura polimórfica para rumbear el mensaje. Ejemplos de ello son el flujo de transmisión de archivos, segmentados en chunks codificados en Base64 para no desbordar la memoria de la JVM, y la cámara, donde el flujo de bytes de imagen se codifica y transmite continuamente."*

---

## DIAPOSITIVA 9: FUNCIONAMIENTO, TOLERANCIA A FALLOS Y CONCLUSIONES

**[6:45 - 8:00] Implementación de Módulos y Gestión de Desconexiones**
> *"A nivel de funcionalidad e implementación, el sistema maneja la seguridad de accesos mediante hash SHA-256 e implementa una cola de moderación dinámica para las salas. Uno de los mayores retos técnicos fue la **tolerancia a fallos y la liberación de recursos**. Ante una desconexión abrupta de red de un cliente, el servidor atrapa la excepción de I/O, libera de inmediato el socket físico, actualiza el estado del participante a 'SALIÓ' en la base de datos relacional y notifica instantáneamente a los demás miembros de la sala para apagar los feeds de video activos, previniendo fugas de memoria y bloqueos."*

**[8:00 - 9:00] Conclusiones y Trabajo Futuro**
> *"Como conclusiones del proyecto, destacamos dos puntos principales:
> Primero, la combinación de Sockets nativos y Patrones de Diseño nos permitió construir un software distribuido robusto, modular y altamente reutilizable, demostrando los beneficios de la Programación Orientada a Objetos en entornos concurrentes.
> Segundo, el aislamiento del cliente respecto a JDBC representa una arquitectura de seguridad estándar de nivel industrial.
> Como trabajo futuro, se proyecta la transición de la transmisión de video a streams nativos RTP/RTSP para optimizar el ancho de banda y la compresión del canal.
> Con esto concluimos nuestra exposición. Quedamos atentos a sus preguntas. Muchas gracias."*
