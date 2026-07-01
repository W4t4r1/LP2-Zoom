# Guion de Exposición Detallado por Diapositiva: Caso de Uso Principal (LP2-Zoom)

Este guion está optimizado para una presentación de **6 minutos** sincronizada diapositiva por diapositiva con el archivo **[presentacion_caso_uso.pptx](file:///c:/Users/lorox/OneDrive/Desktop/LP2-Zoom/LP2-Zoom/docs/presentacion_caso_uso.pptx)**.

---

## RESUMEN DE TIEMPOS Y EXPOSITORES

*   **Diapositivas 1 a 4:** Marco Huanca (Minuto 0:00 - 2:00)
*   **Diapositivas 5 a 8:** Jonathan Leon (Minuto 2:00 - 4:00)
*   **Diapositivas 9 a 11:** Jeanpier Robles (Minuto 4:00 - 6:00)

---

## EXPOSITOR 1: MARCO HUANCA (MINUTO 0:00 - 2:00)

### Diapositiva 1: Portada
*   **Título en Pantalla:** LP2-ZOOM - Caso de Uso Principal: Gestión de Admisión y Videoconferencia en Tiempo Real
*   **Tiempo:** [0:00 - 0:30]
*   **Visual:** Pantalla de bienvenida en modo oscuro con nombres de los integrantes y logo del curso.
*   **Narración (Voz en off):**
    > *"Buenas tardes, docente y compañeros. Hoy les presentaremos la especificación técnica del caso de uso principal y más crítico de nuestro proyecto **LP2-Zoom**, titulado: 'Gestión de Sala de Espera, Admisión e Inicio de Videoconferencia en Tiempo Real'. Mi grupo está conformado por Jonathan Leon, Jeanpier Robles y quien les habla, Marco Huanca. El núcleo de este proyecto no recae en usar frameworks o servidores web embebidos de alto nivel, sino en construir un sistema concurrente distribuido de baja latencia utilizando Sockets TCP nativos en Java SE y base de datos en la nube."*

---

### Diapositiva 2: 1. Introducción al Caso de Uso
*   **Título en Pantalla:** 1. Introducción al Caso de Uso (¿Por qué es el flujo central?)
*   **Tiempo:** [0:30 - 1:00]
*   **Visual:** Viñetas detallando la integración completa y la regla de oro.
*   **Narración (Voz en off):**
    > *"Este caso de uso representa la columna vertebral del prototipo por tres razones fundamentales. Primero, integra todos los componentes y capas del sistema: el cliente gráfico Swing, el protocolo por sockets TCP, el pool concurrente del servidor y la base de datos de Supabase. Segundo, modela la transición de estados interactiva de los usuarios desde la selección de rol hasta la reunión activa. Y tercero, nos permite validar en un escenario real la regla de oro de aislamiento de persistencia de datos en sistemas distribuidos cliente-servidor."*

---

### Diapositiva 3: 2. Regla de Oro: Aislamiento de Persistencia
*   **Título en Pantalla:** 2. Regla de Oro: Aislamiento de Persistencia
*   **Tiempo:** [1:00 - 1:30]
*   **Visual:** Esquema de aislamiento y seguridad (JDBC exclusivo del Servidor).
*   **Narración (Voz en off):**
    > *"Nuestra regla de oro establece que el cliente Swing jamás realiza conexiones JDBC directas ni almacena credenciales de la base de datos en su código local, lo que previene que sean vulneradas por ingeniería inversa o descompilación. En su lugar, toda transacción de moderación viaja cifrada y estructurada en formato JSON a través de sockets TCP persistentes hacia el Servidor. Este actúa como el middleware exclusivo de persistencia, aplicando criptografía SHA-256 a las contraseñas antes de interactuar de forma segura con Supabase."*

---

### Diapositiva 4: 3. Diagrama de Casos de Uso (UML General)
*   **Título en Pantalla:** 3. Diagrama de Casos de Uso (UML General)
*   **Tiempo:** [1:30 - 2:00]
*   **Visual:** Diagrama de Casos de Uso con los actores Anfitrión, Invitado y Servidor.
*   **Narración (Voz en off):**
    > *"Como se aprecia en nuestro Diagrama de Casos de Uso, el sistema separa de manera limpia las facultades de cada rol. El Anfitrión es el único con permisos para crear salas, moderar y admitir invitados en la sala de espera, y emitir la orden de inicio oficial. El Invitado solicita unirse mediante un código de 6 caracteres y queda retenido de forma segura en cola hasta ser admitido. Ambos convergen en los módulos comunes de la reunión activa, como el chat y la cámara. A continuación, mi compañero Jonathan Leon explicará los flujos dinámicos de red y la mensajería del protocolo."*

---

## EXPOSITOR 2: JONATHAN LEON (MINUTO 2:00 - 4:00)

### Diapositiva 5: 4. El Protocolo: Tramas JSON sobre Sockets
*   **Título en Pantalla:** 4. El Protocolo: Tramas JSON sobre Sockets
*   **Tiempo:** [2:00 - 2:30]
*   **Visual:** Estructura de la clase `MensajeSocket` con sus atributos JSON.
*   **Narración (Voz en off):**
    > *"Gracias, Marco. Para que esta comunicación distribuida funcione de forma integrada, diseñamos un protocolo estándar basado en tramas JSON estructuradas por la clase `MensajeSocket`. Cada paquete enviado cuenta con campos específicos: el atributo 'type' define el comando de negocio a ejecutar, 'roomCode' asocia el mensaje a la reunión correspondiente, 'userId' y 'userName' identifican al remitente, y 'message' transporta el texto del chat o los fragmentos de imagen de la webcam."*

---

### Diapositiva 6: 5. Diagrama de Secuencia: Admisión y Sesión
*   **Título en Pantalla:** 5. Diagrama de Secuencia: Admisión y Sesión
*   **Tiempo:** [2:30 - 3:00]
*   **Visual:** Diagrama de Secuencia mostrando la mensajería entre Invitado, Servidor y Host.
*   **Narración (Voz en off):**
    > *"El Diagrama de Secuencia ilustra esta coreografía temporal de la red. Cuando el Invitado solicita unirse, envía un `JOIN_ROOM_REQUEST`. El servidor lo recibe, escribe el estado PENDIENTE en Supabase e informa al Host con una `WAITING_ROOM_UPDATE` para poblar su lista en el EDT de Swing. Cuando el Host da clic en 'Admitir', se despacha una trama `ADMIT_USER` con estado `ACCEPTED`, cambiando el estado del Invitado a admitido. Finalmente, al presionar 'Iniciar reunión', el servidor propaga simultáneamente la señal `MEETING_STARTED`, conmutando a todos a la reunión."*

---

### Diapositiva 7: 6. Diagrama de Actividades: Flujo del Usuario
*   **Título en Pantalla:** 6. Diagrama de Actividades: Flujo del Usuario
*   **Tiempo:** [3:00 - 3:30]
*   **Visual:** Diagrama de Actividades con los carriles de decisión de los usuarios.
*   **Narración (Voz en off):**
    > *"El ciclo de control se modela detalladamente en el Diagrama de Actividades. Tras validar el inicio de sesión del usuario en Supabase con hash SHA-256, el flujo se bifurca: el Host genera el código de sala e ingresa al ciclo de espera de candidatos, mientras que el Invitado introduce el código de 6 caracteres y queda en cola en estado PENDIENTE. Si es rechazado por el Host, regresa al selector inicial; si es aceptado, tras emitirse la señal de inicio de reunión, ambos hilos de ejecución convergen en la pantalla interactiva activando en paralelo el chat y la transmisión de video."*

---

### Diapositiva 8: 7. Diagrama de Estados (CardLayout de Pantallas)
*   **Título en Pantalla:** 7. Diagrama de Estados (CardLayout de Pantallas)
*   **Tiempo:** [3:30 - 4:00]
*   **Visual:** Máquina de estados visual del CardLayout (SELECTOR $\rightarrow$ HOST_WAITING/GUEST_PENDING $\rightarrow$ REUNION).
*   **Narración (Voz en off):**
    > *"Este comportamiento gráfico es controlado con precisión por un gestor `CardLayout` en la ventana principal del cliente. La interfaz transiciona entre 4 tarjetas bien definidas: `SELECTOR` para iniciar, `HOST_WAITING` para que el anfitrión administre la lista en tiempo real, `GUEST_PENDING` que bloquea la pantalla del invitado con una barra de progreso en espera de admisión, y la tarjeta unificada `REUNION`. Ahora, mi compañero Jeanpier Robles detallará los patrones de diseño aplicados y la arquitectura de concurrencia del sistema."*

---

## EXPOSITOR 3: JEANPIER ROBLES (MINUTO 4:00 - 6:00)

### Diapositiva 9: 8. Patrones de Diseño en la Admisión
*   **Título en Pantalla:** 8. Patrones de Diseño en la Admisión
*   **Tiempo:** [4:00 - 4:45]
*   **Visual:** Nombres y justificaciones de Singleton, Bridge, Proxy y DBProxy.
*   **Narración (Voz en off):**
    > *"Gracias, Jonathan. Para dar soporte a esta arquitectura distribuida, aplicamos patrones de diseño orientados a objetos. El **Singleton** en `ClienteConexion` asegura una única instancia de conexión física abierta. El **Bridge** en `ProtocolBridge` separa la red física de la serialización JSON con Gson. Y el patrón **Proxy** se usa simétricamente: en el servidor, un `DBProxy` maneja la conexión JDBC con Lazy Loading y logs de auditoría; y en el cliente, un `CameraProxy` controla los permisos y realiza un fallback transparente a cámara simulada si el hardware de la webcam física está bloqueado o falla, previniendo congelamientos de pantalla."*

---

### Diapositiva 10: 9. Arquitectura de Hilos (Swing EDT vs Red)
*   **Título en Pantalla:** 9. Arquitectura de Hilos (Swing EDT vs Red)
*   **Tiempo:** [4:45 - 5:30]
*   **Visual:** Esquema de hilos mostrando EDT, HiloEscuchaCliente y pools de hilos secundarios.
*   **Narración (Voz en off):**
    > *"Uno de los mayores desafíos de Swing es que el Event Dispatch Thread, o EDT, es monocanal y cualquier lectura bloqueante de red lo congelaría por completo. Lo solucionamos implementando un hilo secundario independiente de escucha de sockets. Cuando este hilo recibe las tramas del servidor, delega la renderización gráfica de vuelta al EDT de manera segura usando `SwingUtilities.invokeLater(...)`. Asimismo, la decodificación de las imágenes en Base64 de la cámara corre en un pool de hilos daemon secundario (`videoDecoderExecutor`) para no entorpecer el flujo principal de la red."*

---

### Diapositiva 11: 10. Tolerancia a Fallos y Conclusiones
*   **Título en Pantalla:** 10. Tolerancia a Fallos y Conclusiones
*   **Tiempo:** [5:30 - 6:00]
*   **Visual:** Viñetas sobre desconexión abrupta y conclusiones del proyecto.
*   **Narración (Voz en off):**
    > *"El sistema es altamente tolerante a fallos: ante una desconexión abrupta de red de un cliente, el servidor captura la excepción física de I/O, libera de inmediato el socket físico, actualiza el estado de la sesión en Supabase a 'SALIÓ' y notifica a los demás clientes de la sala para apagar los feeds de video activos, evitando fugas de memoria. 
    > En conclusión, la combinación de Sockets nativos y Patrones de Diseño nos permitió construir un software distribuido robusto, modular y altamente reutilizable, demostrando los beneficios de la programación a bajo nivel en entornos concurrentes. Con esto concluimos nuestra exposición. Quedamos atentos a sus preguntas. Muchas gracias."*
