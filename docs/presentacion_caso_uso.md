---
marp: true
theme: gaia
_class: lead
paginate: true
backgroundColor: #0F172A
color: #F8FAFC
style: |
  section {
    font-family: 'Outfit', sans-serif;
    padding: 40px;
  }
  h1 {
    color: #38BDF8;
  }
  h2 {
    color: #F1F5F9;
  }
  footer {
    font-size: 0.5em;
    color: #64748B;
  }
---

# LP2-ZOOM
## Caso de Uso Principal: Gestión de Admisión y Videoconferencia en Tiempo Real

**Expositores:** Marco Huanca, Jonathan Leon, Jeanpier Robles
**Curso:** Lenguaje de Programación II (SW403-U)
*Universidad Nacional de Ingeniería*

---

## 1. Introducción al Caso de Uso
### ¿Por qué es el flujo central del sistema?

*   **Integración Completa:** Conecta el cliente visual, el protocolo de red por sockets, el pool concurrente del servidor y la base de datos Supabase.
*   **Gestión de Estados:** Modela la transición interactiva desde la solicitud en espera hasta la videoconferencia activa.
*   **Regla de Oro de Seguridad:** Demuestra la contención de persistencia en el backend sin exponer accesos JDBC en el cliente.

*Expositor: Marco Huanca*

---

## 2. Regla de Oro: Aislamiento de Persistencia

*   **Principio:** El cliente Swing jamás se conecta directamente a PostgreSQL (Supabase) ni maneja credenciales de la BD.
*   **Flujo de Datos Seguro:** Toda consulta de moderación, chat o archivos viaja cifrada en tramas JSON por sockets TCP.
*   **Middleware Servidor:** El servidor de sockets ejecuta las consultas y el hashing de contraseñas de manera centralizada.

*Expositor: Marco Huanca*

---

## 3. Diagrama de Casos de Uso (UML General)

```
        ┌────────────────────────────────────────────────────────┐
        │                 LP2-Zoom (Sistema Sockets)            │
        │                                                        │
        │      ┌─────────────────────┐    ┌───────────────────┐  │
 ┌──────┼─────▶│   Crear Sala   │    │ Moderar Invitados │◀─┼──────┐
 │      │      └─────────────────────┘    └───────────────────┘  │      │
 │      │                 ▲                         ▲            │      │
 │      │                 │                         │            │      │
 │      │                 └────────────┬────────────┘            │      │
 │      │                              │                         │      │
 │      │      ┌─────────────────────┐ │  ┌───────────────────┐  │      │
(Anfitrión)───▶│   Iniciar Reunión   │ ┼─▶│ Transmitir Video  │◀─┼──(Invitado)
 │      │      └─────────────────────┘    └───────────────────┘  │      │
 │      │                                           ▲            │      │
 │      │                                           │            │      │
 │      │      ┌─────────────────────┐              │            │      │
 └──────┼─────▶│    Enviar Chat      │──────────────┘            │      │
        │      └─────────────────────┘                           │      │
        │      ┌─────────────────────┐                           │      │
        │      │ Unirse con Código   │◀──────────────────────────┼──────┘
        │      └─────────────────────┘                           │
        └────────────────────────────────────────────────────────┘
```
*Expositor: Marco Huanca*

---

## 4. El Protocolo: Tramas JSON sobre Sockets

*   **Estructura Base (`MensajeSocket`):** Contrato común unificado para agrupar metadatos del transporte.
*   **Atributos Clave:**
    *   `type`: Identificador de acción (`JOIN_ROOM_REQUEST`, `ADMIT_USER`, `MEETING_STARTED`).
    *   `roomCode`: Código de 6 dígitos que identifica la reunión activa.
    *   `userId` y `userName`: Identificación del emisor.
    *   `message`: Payload de datos (texto, chunk Base64 o comandos).

*Expositor: Jonathan Leon*

---

## 5. Diagrama de Secuencia: Admisión y Sesión

```
Invitado (Swing)         Servidor (Java Sockets)       Anfitrión (Swing)
       │                            │                           │
       ├──── JOIN_ROOM_REQUEST ────▶│                           │
       │                            ├─── WAITING_ROOM_UPDATE ──▶│ (Actualiza EDT)
       │                            │                           │
       │                            │◀────── ADMIT_USER ────────┤ (Admitir)
       │◄─────── ADMIT_USER ────────┤                           │ (Estado: Aceptado)
       │                            │                           │
       │                            │◀── START_MEETING_REQUEST ─┤ (Iniciar)
       │◄───── MEETING_STARTED ─────┼───── MEETING_STARTED ────▶│ (Cambia CardLayout)
       │                            │                           │
```
*Expositor: Jonathan Leon*

---

## 6. Diagrama de Actividades: Flujo del Usuario

```
[Dashboard Selector] ──▶ ¿Crear o Unirse?
   │
   ├── Crear Sala ──▶ [Sala Espera Host] ──▶ ¿Admitir? ──▶ [Iniciar Reunión]
   │                                                             │
   └── Unirse Sala ──▶ [Espera Invitado] ──▶ ¿Aceptado? ─────────┤
                                                                 ▼
                                                         [Reunión Activa]
                                                                 │
                                                ┌────────────────┴────────────────┐
                                                ▼                                 ▼
                                        [Cámara & Video Grid]             [Chat & Archivos]
```
*Expositor: Jonathan Leon*

---

## 7. Diagrama de Estados (CardLayout de Pantallas)

El panel principal conmuta dinámicamente de acuerdo al flujo de red:

1.  **SELECTOR:** Pantalla de Login / Selección de rol (Crear/Unirse).
2.  **HOST_WAITING:** Cola de moderación en tiempo real para el Host.
3.  **GUEST_PENDING:** Bloqueo de UI (pantalla de espera indeterminada).
4.  **REUNION:** Panel unificado de chat, video grid y archivos compartidos.

*Expositor: Jonathan Leon*

---

## 8. Patrones de Diseño en la Admisión

*   **Singleton (`ClienteConexion`):** Canal de socket único abierto.
*   **Bridge (`ProtocolBridge`):** Desacopla la red de la serialización JSON.
*   **Proxy de Persistencia (`DBProxy`):**
    *   *Virtual Proxy:* Carga perezosa del driver JDBC en el servidor.
    *   *Logging Proxy:* Audita las consultas relacionales en consola.
*   **Proxy de Cámara (`CameraProxy`):** Valida permisos de webcam y genera fallback automático a video simulado ante fallos físicos.

*Expositor: Jeanpier Robles*

---

## 9. Arquitectura de Hilos (Swing EDT vs Red)

*   **Conflicto de Bloqueo:** El Event Dispatch Thread (EDT) es monocanal. Escuchar sockets de forma síncrona congelaría la ventana.
*   **Solución Asíncrona:**
    *   **Hilo de Escucha (Red):** Lee de forma continua y bloqueante el stream TCP.
    *   **SwingUtilities.invokeLater():** Envía de forma segura la renderización de datos de vuelta al EDT.
    *   **videoDecoderExecutor:** Pool de hilos daemon secundario para decodificar frames Base64 sin sobrecargar la red.

*Expositor: Jeanpier Robles*

---

## 10. Tolerancia a Fallos y Conclusiones

*   **Limpieza de Desconexiones:** Si un socket se cierra abruptamente, el Servidor captura la excepción de I/O, libera los recursos, actualiza Supabase a `SALIÓ` y notifica a los demás clientes para apagar el feed.
*   **Conclusiones:**
    *   La programación a bajo nivel en Java (Sockets TCP nativos) otorga control total de sincronización y flujo.
    *   La arquitectura modular y el aislamiento de persistencia configuran un sistema seguro a nivel industrial.

*Expositor: Jeanpier Robles*
