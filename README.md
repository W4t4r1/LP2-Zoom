# LP2-Zoom - Prototipo Académico de Videoconferencia

**LP2-Zoom** es un prototipo educativo de sistema de videoconferencia y mensajería en tiempo real desarrollado en **Java SE (nativos)** y **Supabase (PostgreSQL Cloud)**.

El propósito del proyecto es ejemplificar de forma conceptual y práctica la programación de red distribuida multicliente empleando sockets TCP nativos en Java y persistencia remota, aislando al cliente del acceso directo a la base de datos.

---

## Arquitectura y Regla de Oro

> [!IMPORTANT]
> **Esquema Estricto Cliente-Servidor**
> Los clientes **nunca** realizan conexiones JDBC ni consultas directas a la base de datos de Supabase. 
> 
> Todo el tráfico de autenticación, chats, salas de espera, metadatos y transmisión de archivos viaja cifrado en tramas **JSON** a través de sockets TCP persistentes dirigidos al **Servidor de Sockets**. El Servidor actúa como el único orquestador relacional interactuando con la base de datos.

---

## Índice de Documentación Técnica

Para conocer a fondo el diseño, el contrato de datos o la configuración del entorno, consulta las guías detalladas en la carpeta [docs/](docs/):

*   **[Arquitectura General](docs/arquitectura.md):** Visión global del flujo de transacciones, componentes (Cliente, Servidor, DB) y estrategias ante la latencia y fallas de hilos.
*   **[Especificación del Backend](docs/backend.md):** Detalles sobre el manejo de concurrencia (`CachedThreadPool`), enrutamiento por tipo de trama y ciclo de vida de los sockets.
*   **[Especificación del Frontend](docs/frontend.md):** Manejo de hilos gráficos en Swing (EDT) en paralelo al Hilo de Escucha de Red secundario y control de pantallas (`CardLayout`).
*   **[Diseño de Base de Datos](docs/base-de-datos.md):** Estructura del esquema PostgreSQL en Supabase, diagrama entidad-relación y scripts de migración (DDL) con semillas de prueba.
*   **[Contrato de Mensajería JSON](docs/endpoints.md):** Formato y ejemplos exactos de los payloads JSON utilizados sobre los sockets TCP para cada caso de negocio.
*   **[Guía de Despliegue y Maven](docs/despliegue.md):** Configuración de variables, driver JDBC con SSL y comandos Maven para construir y ejecutar los archivos JAR.
*   **[Control de Fases del Proyecto](docs/fases.md):** Mapa de ruta de desarrollo de las 6 fases funcionales del prototipo y porcentaje de avance.
*   **[Caso de Uso Principal](docs/caso_de_uso_principal.md):** Especificación técnica detallada del flujo de admisión en sala de espera y transmisión de videoconferencia en tiempo real con sus respectivos diagramas generales.
*   **[Guion de Exposición de Caso de Uso](docs/guion_caso_uso.md):** Guion expositivo estructurado para 3 personas para la presentación oral del caso de uso principal.
*   **[Presentación del Caso de Uso (Marp)](docs/presentacion_caso_uso.md):** Estructura y diapositivas de la presentación en formato Markdown/Marp.
*   **[Presentación del Caso de Uso (PowerPoint)](docs/presentacion_caso_uso_final.pptx):** Diapositivas físicas en formato PPTX con **imágenes de los diagramas UML insertadas** (Use Case, Secuencia, Actividades y Regla de Oro) y diseño a doble columna.

---

## 🛠️ Stack Tecnológico

*   **Lenguaje Base:** Java SE 17+ (JDK 26 validado).
*   **Gestión del Proyecto:** Maven.
*   **Base de Datos Cloud:** Supabase (PostgreSQL).
*   **Serializador JSON:** Gson (Google).
*   **Librería Gráfica:** Java Swing.
*   **Concurrencia:** Java Executors Framework (`ExecutorService`).

---

## Guía Rápida de Ejecución

Si deseas iniciar el proyecto localmente para pruebas, tienes dos métodos genéricos que funcionarán en cualquier computadora (sin importar la ruta de usuario):

### Método 1: Ejecución con Java CLI (Recomendado/Rápido)
Este método utiliza los archivos ya compilados (`bin_servidor` y `bin_cliente`) y variables de entorno genéricas.

#### 1. Iniciar el Servidor de Sockets
Abre una terminal y ejecuta el comando según tu consola:
*   **En Windows (PowerShell):**
    ```powershell
    java -cp "bin_servidor;Servidor/src/main/resources;$env:USERPROFILE\.m2\repository\org\postgresql\postgresql\42.7.4\postgresql-42.7.4.jar;$env:USERPROFILE\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar" network.MainServidor
    ```
*   **En Windows (CMD / Command Prompt):**
    ```cmd
    java -cp "bin_servidor;Servidor/src/main/resources;%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.4\postgresql-42.7.4.jar;%USERPROFILE%\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar" network.MainServidor
    ```

#### 2. Iniciar Clientes
Abre terminales independientes por cada usuario y ejecuta:
*   **En Windows (PowerShell):**
    ```powershell
    java -cp "bin_cliente;$env:USERPROFILE\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar" UI.LoginFrame
    ```
*   **En Windows (CMD / Command Prompt):**
    ```cmd
    java -cp "bin_cliente;%USERPROFILE%\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar" UI.LoginFrame
    ```

---

### Método 2: Ejecución Directa con Maven (Si tienes Maven instalado globalmente)
Si tienes el comando `mvn` configurado en tus variables de entorno, puedes compilar y ejecutar directamente desde el código fuente:

#### 1. Iniciar el Servidor de Sockets
```bash
mvn -f Servidor/pom.xml compile exec:java -Dexec.mainClass="network.MainServidor"
```

#### 2. Iniciar Clientes
Abre terminales independientes por cada usuario y ejecuta:
```bash
mvn -f Cliente/pom.xml compile exec:java -Dexec.mainClass="UI.LoginFrame"
```

---

### Credenciales de Prueba
*   **Anfitrión (Host):** `host@zoom.com` / `123456`
*   **Invitado (Guest):** `invitado@zoom.com` / `123456`
