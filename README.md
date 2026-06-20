# LP2-Zoom - Prototipo Académico de Videoconferencia

**LP2-Zoom** es un prototipo educativo de sistema de videoconferencia y mensajería en tiempo real desarrollado en **Java SE (nativos)** y **Supabase (PostgreSQL Cloud)**.

El propósito del proyecto es ejemplificar de forma conceptual y práctica la programación de red distribuida multicliente empleando sockets TCP nativos en Java y persistencia remota, aislando al cliente del acceso directo a la base de datos.

---

##Arquitectura y Regla de Oro

> [!IMPORTANT]
> **Esquema Estricto Cliente-Servidor**
> Los clientes **nunca** realizan conexiones JDBC ni consultas directas a la base de datos de Supabase. 
> 
> Todo el tráfico de autenticación, chats, salas de espera, metadatos y transmisión de archivos viaja cifrado en tramas **JSON** a través de sockets TCP persistentes dirigidos al **Servidor de Sockets**. El Servidor actúa como el único orquestador relacional interactuando con la base de datos.

---

##Índice de Documentación Técnica

Para conocer a fondo el diseño, el contrato de datos o la configuración del entorno, consulta las guías detalladas en la carpeta [docs/](file:///c:/Users/Jeanpier/OneDrive/Desktop/LP2-Zoom/docs):

*   **[Arquitectura General](file:///c:/Users/Jeanpier/OneDrive/Desktop/LP2-Zoom/docs/arquitectura.md):** Visión global del flujo de transacciones, componentes (Cliente, Servidor, DB) y estrategias ante la latencia y fallas de hilos.
*   **[Especificación del Backend](file:///c:/Users/Jeanpier/OneDrive/Desktop/LP2-Zoom/docs/backend.md):** Detalles sobre el manejo de concurrencia (`CachedThreadPool`), enrutamiento por tipo de trama y ciclo de vida de los sockets.
*   **[Especificación del Frontend](file:///c:/Users/Jeanpier/OneDrive/Desktop/LP2-Zoom/docs/frontend.md):** Manejo de hilos gráficos en Swing (EDT) en paralelo al Hilo de Escucha de Red secundario y control de pantallas (`CardLayout`).
*   **[Diseño de Base de Datos](file:///c:/Users/Jeanpier/OneDrive/Desktop/LP2-Zoom/docs/base-de-datos.md):** Estructura del esquema PostgreSQL en Supabase, diagrama entidad-relación y scripts de migración (DDL) con semillas de prueba.
*   **[Contrato de Mensajería JSON](file:///c:/Users/Jeanpier/OneDrive/Desktop/LP2-Zoom/docs/endpoints.md):** Formato y ejemplos exactos de los payloads JSON utilizados sobre los sockets TCP para cada caso de negocio.
*   **[Guía de Despliegue y Maven](file:///c:/Users/Jeanpier/OneDrive/Desktop/LP2-Zoom/docs/despliegue.md):** Configuración de variables, driver JDBC con SSL y comandos Maven para construir y ejecutar los archivos JAR.
*   **[Control de Fases del Proyecto](file:///c:/Users/Jeanpier/OneDrive/Desktop/LP2-Zoom/docs/fases.md):** Mapa de ruta de desarrollo de las 6 fases funcionales del prototipo y porcentaje de avance.

---

## 🛠️ Stack Tecnológico

*   **Lenguaje Base:** Java SE 17+ (JDK 26 validado).
*   **Gestión del Proyecto:** Maven.
*   **Base de Datos Cloud:** Supabase (PostgreSQL).
*   **Serializador JSON:** Gson (Google).
*   **Librería Gráfica:** Java Swing.
*   **Concurrencia:** Java Executors Framework (`ExecutorService`).

---

##Guía Rápida de Ejecución

Si deseas iniciar el proyecto localmente para pruebas, ejecuta los siguientes comandos desde tu consola en el directorio raíz de la aplicación:

### 1. Iniciar el Servidor de Sockets
```powershell
java -cp "bin_servidor;C:\Users\Jeanpier\.m2\repository\org\postgresql\postgresql\42.7.4\postgresql-42.7.4.jar;C:\Users\Jeanpier\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar" network.MainServidor
```

### 2. Iniciar Clientes
Abre terminales independientes por cada usuario que desees conectar (Host y Guests) y ejecuta:
```powershell
java -cp "bin_cliente;C:\Users\Jeanpier\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar" UI.LoginFrame
```

*   **Credenciales de Anfitrión (Host):** `host@zoom.com` / `123456`
*   **Credenciales de Invitado (Guest):** `invitado@zoom.com` / `123456`
