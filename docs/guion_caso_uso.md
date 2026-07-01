# Guion de Exposición Detallado: Caso de Uso "Admitir / Rechazar Invitados" (CU-02)

Este guion de exposición está diseñado para durar **6 minutos** en total y divide la explicación de la ficha técnica de moderación en sala de espera de **LP2-Zoom** de forma equitativa entre **3 personas**.

---

## RESUMEN DE COMPARTICIÓN
1. **Expositor 1: Marco Huanca** (Introducción, Ficha del CU, Objetivos, Precondiciones y Postcondiciones)
2. **Expositor 2: Jonathan Leon** (Flujo de Eventos: Escenario Principal y Flujo Alternativo de Rechazo)
3. **Expositor 3: Jeanpier Robles** (Reglas de Negocio, Datos de Entrada/Salida, Excepciones y Conclusiones)

---

## PARTE 1: FICHA TÉCNICA, ACTORES Y CONDICIONES
### **Expositor 1: Marco Huanca (Minuto 0:00 - 2:00)**

*   **Pacing:** Claro, introductorio, sentando las bases del caso de uso.
*   **Apoyo Visual sugerido:** Diapositiva con los datos de la ficha técnica, los actores y las condiciones.

> **[0:00 - 0:45] Introducción y Ficha Técnica**
> *"Buenas tardes, docente y compañeros. Hoy detallaremos la especificación formal del caso de uso principal de nuestro proyecto **LP2-Zoom**, denominado **CU-02: Admitir / Rechazar Invitados**. 
> Este caso de uso asocia de forma íntegra a los dos actores de nuestra aplicación distribuida: el **Anfitrión (Host)**, que modera las peticiones; y el **Invitado (Guest)**, que postula para ingresar a la sala. El objetivo principal de este flujo es permitir al Anfitrión moderar el ingreso de los Invitados en sala de espera en tiempo real, decidiendo si aprueba o deniega su acceso antes de que inicie oficialmente la videoconferencia."*

> **[0:45 - 1:30] Precondiciones y Postcondiciones**
> *"Para que este caso de uso inicie de forma correcta, se deben cumplir dos precondiciones críticas. Primero, el Anfitrión debe haber creado una sala activa en el sistema y estar en el panel de espera del Host. Segundo, uno o más Invitados deben haber enviado una solicitud de unión con el código de 6 caracteres, quedando registrados en estado `'PENDIENTE'` en la tabla `SolicitudesSala` de Supabase.
> Al finalizar de forma exitosa, la base de datos se actualiza: la solicitud cambia a estado `'ACCEPTED'` y el Invitado se inserta en `ParticipantesSala` como `'ACTIVO'`. Gráficamente, el Invitado transiciona al estado `'INVITADO_ADMITIDO'` y queda en espera de la orden de inicio oficial para cambiar de pantalla mediante `CardLayout`."*

> **[1:30 - 2:00] Transición a los flujos**
> *"Para conocer en detalle cómo viajan los mensajes JSON por el canal de socket y cómo interactúan las interfaces, daré paso a mi compañero Jonathan Leon, quien detallará el flujo de eventos principal y el escenario alternativo."*

---

## PARTE 2: FLUJO PRINCIPAL Y ESCENARIOS ALTERNATIVOS
### **Expositor 2: Jonathan Leon (Minuto 2:00 - 4:00)**

*   **Pacing:** Dinámico y descriptivo, centrado en el comportamiento de red y de la UI.
*   **Apoyo Visual sugerido:** Diagrama de Secuencia y el paso a paso del flujo principal en pantalla.

> **[2:00 - 3:00] Flujo Principal Paso a Paso**
> *"Gracias, Marco. El flujo principal comienza cuando el Invitado solicita unirse enviando la trama `JOIN_ROOM_REQUEST`. El Servidor registra la solicitud en Supabase como `'PENDIENTE'` en la tabla `SolicitudesSala` y notifica al Anfitrión con una trama `WAITING_ROOM_UPDATE`. 
> La interfaz del Anfitrión recibe la trama y actualiza de forma segura la lista gráfica `JList` con los nombres de los candidatos pendientes dentro del EDT de Swing mediante `SwingUtilities.invokeLater`.
> El Anfitrión selecciona al Invitado y hace clic en 'Admitir'. Esto despacha una trama `ADMIT_USER` con el mensaje `'ACEPTAR'` por el socket TCP. 
> El servidor recibe la trama, actualiza el registro en Supabase a `'ACCEPTED'`, inserta al participante en `ParticipantesSala` como `'ACTIVO'` y notifica al Invitado con la trama `ADMIT_USER` (`ACCEPTED`) para cambiar su interfaz a admitido."*

> **[3:00 - 3:45] Flujos Alternativos**
> *"El sistema también contempla la moderación de rechazos. Si el Anfitrión decide hacer clic en el botón 'Rechazar', el cliente envía la trama `ADMIT_USER` con el mensaje `'RECHAZAR'`. 
> El servidor recibe el comando, actualiza el registro en la tabla `SolicitudesSala` a estado `'RECHAZADO'` en Supabase y le notifica al Invitado con la trama `ADMIT_USER` (`REJECTED`). El cliente del Invitado recibe la notificación, sale de la pantalla de bloqueo y regresa al selector principal con el mensaje 'Solicitud rechazada'."*

> **[3:45 - 4:00] Transición a Reglas de Negocio**
> *"Para sostener esta robusta lógica distribuida, el sistema aplica reglas estrictas de persistencia, tipado de datos y tolerancia a fallas. A continuación, mi compañero Jeanpier Robles detallará estas reglas y las excepciones técnicas."*

---

## PARTE 3: REGLAS DE NEGOCIO, DATOS Y EXCEPCIONES
### **Expositor 3: Jeanpier Robles (Minuto 4:00 - 6:00)**

*   **Pacing:** Técnico, riguroso, enfocado en las reglas de la arquitectura del informe y la resiliencia.
*   **Apoyo Visual sugerido:** Lista de reglas de negocio, estructura JSON de tramas y excepciones de red.

> **[4:00 - 4:45] Reglas de Negocio y Datos**
> *"Gracias, Jonathan. Para que este flujo sea mantenible y seguro, implementamos las **Reglas de Negocio** definidas en el reporte técnico del proyecto. 
> La primera es la **Regla de Oro de Persistencia (RN-01)**: las credenciales de la base de datos no existen en el cliente; toda modificación de estados en `SolicitudesSala` y `ParticipantesSala` se realiza en el Servidor a través del proxy `DBProxy`. 
> La segunda es la **Regla de Concurrencia de Swing (RN-02)**: las notificaciones asíncronas de red de tipo `WAITING_ROOM_UPDATE` se gestionan en segundo plano por el `HiloEscuchaCliente` y se pintan en la interfaz mediante `SwingUtilities.invokeLater()` para no bloquear el EDT gráfico.
> Los datos de entrada y salida del protocolo viajan en objetos del tipo `MensajeSocket` serializados a JSON, con tipos como `JOIN_ROOM_REQUEST`, `WAITING_ROOM_UPDATE` y `ADMIT_USER`."*

> **[4:45 - 5:30] Excepciones y Tolerancia a Fallos**
> *"A nivel técnico, manejamos las excepciones de red descritas en la sección 7 de nuestro informe. 
> La primera es la **Desconexión abrupta de un Invitado en espera (EX-01)**: si el socket del Invitado se cierra inesperadamente en cola, el Servidor captura la excepción de I/O, ejecuta `desconectar()` para liberar recursos, elimina la solicitud pendiente de `SolicitudesSala` y difunde una nueva trama `WAITING_ROOM_UPDATE` al Anfitrión para remover al candidato huérfano del `JList` visual de inmediato.
> Esto garantiza que no queden solicitudes colgadas ni paneles de video vacíos."*

> **[5:30 - 6:00] Conclusiones**
> *"Como conclusiones, el caso de uso de moderación y admisión de **LP2-Zoom** demuestra que la combinación de Sockets nativos y Patrones de Diseño nos permite construir software concurrente distribuido seguro, robusto y modular. Muchas gracias."*
