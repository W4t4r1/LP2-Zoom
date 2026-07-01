# Guion de Exposición Definitivo: Sustentación Basada en Diagramas (3 Expositores)

Este guion está diseñado para una exposición de **6 minutos** basada en la visualización directa del reporte del proyecto (**informe.md** o **informe.docx**) en pantalla.

---

## ESTRUCTURA Y ACCIONES DE PANTALLA

*   **Expositor 1: Marco Huanca (TÚ)** (Minuto 0:00 - 2:00)
    *   *Acción:* Compartir pantalla con el archivo **[informe.md](file:///c:/Users/lorox/OneDrive/Desktop/LP2-Zoom/LP2-Zoom/docs/informe.md)**. Mostrar en pantalla el **Diagrama de Casos de Uso (UML)** (línea 244) y luego desplazarse hacia abajo para explicar detalladamente la **Ficha/Tabla de Especificación del CU-02** (línea 268).
*   **Expositor 2: Jonathan Leon** (Minuto 2:00 - 4:00)
    *   *Acción:* Desplazarse en pantalla hacia abajo para proyectar el **Diagrama de Secuencia (Handshake de red)** (línea 341) y señalar cada mensaje del socket TCP.
*   **Expositor 3: Jeanpier Robles** (Minuto 4:00 - 6:00)
    *   *Acción:* Desplazarse en pantalla para proyectar el **Diagrama de Actividades (General)** (línea 268) y el **Diagrama de Transición de Estados** para explicar la lógica y la gestión de excepciones de sockets y hilos.

---

## EXPOSITOR 1: MARCO HUANCA (MINUTO 0:00 - 2:00)
### PARTE 1: DIAGRAMA DE CASOS DE USO Y TABLA DE ESPECIFICACIÓN CU-02

*   **Pantalla en exhibición:** El archivo **informe.md** (o Word) en la sección **"Diagrama de Casos de Uso"**.

> **[0:00 - 0:45] Sustentación del Diagrama de Casos de Uso**
> *"Buenas tardes, docente y compañeros. Para sustentar la arquitectura distribuida del proyecto **LP2-Zoom**, nos guiaremos exclusivamente por la documentación y diagramas de nuestro informe técnico.
> Como se observa aquí en pantalla, en el **Diagrama de Casos de Uso UML**, las funcionalidades se dividen rigurosamente por actores. El **Usuario Invitado** realiza operaciones básicas como iniciar sesión, unirse a sala, chatear y enviar archivos. 
> Por su parte, el **Anfitrión o Host** posee el control de moderación y administración física de la videoconferencia: Crear Sala, Iniciar Reunión Oficial y el caso de uso central que sustentaremos hoy: **Admitir / Rechazar Invitados**."*

*   **Acción en Pantalla:** Hace scroll hacia abajo para posicionar y encuadrar completamente la **Tabla de Especificación del Caso de Uso (CU-02)**.

> **[0:45 - 1:30] Explicación de la Tabla de Especificación (Ficha Técnica)**
> *"Esta acción de moderación está documentada formalmente en la **Ficha Técnica** que ven en pantalla. 
> * **Actor Principal:** Es el Anfitrión, y como **Actores Secundarios** intervienen el Invitado en espera, el Servidor y la Base de Datos.
> * **Objetivo:** Moderar el acceso a la sala de espera en tiempo real.
> * **Precondición:** El Host debe encontrarse en la pantalla de espera (`HOST_WAITING`) y el Invitado debe haber postulado con el código de 6 caracteres, quedando registrado como `'PENDIENTE'` en la tabla `SolicitudesSala`.
> * **Postcondición:** Al admitirse, la base de datos Supabase se actualiza pasando la solicitud a `'ACCEPTED'` e insertando al Invitado en `ParticipantesSala` como `'ACTIVO'`. A nivel de interfaz, el gestor gráfico `CardLayout` conmuta a la pantalla `'REUNION'` e inicia la decodificación de video."*

> **[1:30 - 2:00] Cierre y Transición**
> *"Esta ficha técnica y el diagrama que acabamos de ver definen las reglas lógicas. Para comprender cómo interactúan físicamente los hilos del socket TCP y la base de datos Supabase para cumplir esta especificación, mi compañero Jonathan Leon sustentará a continuación el Diagrama de Secuencia."*

---

## EXPOSITOR 2: JONATHAN LEON (MINUTO 2:00 - 4:00)
### PARTE 2: DIAGRAMA DE SECUENCIA (HANDSHAKE Y SALA DE ESPERA)

*   **Pantalla en exhibición:** Desplaza el documento hacia abajo y muestra el **"Diagrama de Secuencia (Handshake y Flujo de Sala de Espera)"** en pantalla completa. Señala las flechas de mensajes con el cursor.

> **[2:00 - 3:00] Sustentación de la Secuencia de Red**
> *"Gracias, Marco. Pasemos a sustentar el **Diagrama de Secuencia**, que ilustra la coreografía de red sobre sockets TCP. 
> El flujo se inicia cuando el Invitado solicita unirse enviando la trama JSON `JOIN_ROOM_REQUEST`. El servidor, mediante `ManejadorCliente`, recibe el mensaje, interactúa con `DBProxy` para registrar la solicitud en Supabase con estado `'PENDIENTE'` y responde con la trama `JOIN_ROOM_RESPONSE`. 
> Seguidamente, el servidor envía `WAITING_ROOM_UPDATE` al Host para refrescar asíncronamente su lista gráfica en el EDT mediante `SwingUtilities.invokeLater`. 
> Cuando el Host hace clic en 'Admitir', el cliente despacha `ADMIT_USER` (`ACEPTAR`). El servidor actualiza Supabase, registra al Invitado como `'ACTIVO'` en `ParticipantesSala` y envía al Invitado la confirmación `ADMIT_USER` (`ACCEPTED`)."*

> **[3:00 - 3:45] Flujos Alternativos y Sincronización de Inicio**
> *"El diagrama también detalla los flujos alternativos: si el Host decide rechazar al candidato, el cliente envía `ADMIT_USER` (`RECHARZAR`). El servidor actualiza Supabase a `'RECHAZADO'` y notifica al Invitado con la trama `ADMIT_USER` (`REJECTED`) para que regrese al selector.
> Para iniciar la sesión, el Host envía `START_MEETING_REQUEST`. El servidor difunde la trama `MEETING_STARTED` a todos los participantes activos de la sala, forzando a los hilos de red de los clientes a cambiar sus pantallas a la tarjeta `'REUNION'` e iniciar los hilos multimedia."*

> **[3:45 - 4:00] Transición**
> *"A continuación, mi compañero Jeanpier Robles sustentará la lógica de control gráfico y la resiliencia mediante el diagrama de actividades."*

---

## EXPOSITOR 3: JEANPIER ROBLES (MINUTO 4:00 - 6:00)
### PARTE 3: DIAGRAMA DE ACTIVIDADES Y EXCEPCIONES CONCURRENTES

*   **Pantalla en exhibición:** Desplaza el reporte hacia el **"Diagrama de Actividades (General)"** y el **"Diagrama de Transición de Estados"**.

> **[4:00 - 4:45] Sustentación de Actividades y Estados**
> *"Gracias, Jonathan. Como se observa en el **Diagrama de Actividades**, el sistema gestiona un ciclo con múltiples bifurcaciones concurrentes.
> Tras la autenticación, el flujo se divide: el Host genera el código de 6 caracteres y entra en el ciclo de escucha de moderación, mientras que el Invitado introduce el código de sala y queda en cola en estado `'PENDIENTE'`.
> Al conmutar a la reunión activa por la señal `MEETING_STARTED`, los flujos se ejecutan de forma paralela en hilos secundarios: el envío de chats (Módulo Chat), la captura y decodificación de Base64 de la cámara (Módulo Cámara) y la fragmentación en chunks de archivos (Módulo Archivos)."*

> **[4:45 - 5:30] Relación con las Reglas y Excepciones**
> *"Este flujo responde a reglas técnicas muy claras: la **Regla de Oro (RN-01)** impide conexiones directas de base de datos desde los hilos clientes, centralizando la persistencia en el servidor. 
> Además, ante fallas críticas de red, el diagrama de actividades y de secuencia muestran cómo el servidor reacciona ante excepciones de I/O de sockets cerrados: ejecuta el método `desconectar()`, remueve al participante de la cola y notifica a los demás miembros para apagar su grid de video. En caso de falla física de la webcam, el proxy de cámara realiza la conmutación."*

> **[5:30 - 6:00] Conclusión de la Exposición**
> *"En conclusión, la concordancia entre nuestros diagramas de Casos de Uso, Secuencia y Actividades demuestra la solidez de **LP2-Zoom**. La programación de sockets TCP de bajo nivel combinada con patrones de diseño nos ha permitido construir un sistema concurrente distribuido seguro, modular y tolerante a fallos. Muchas gracias."*
