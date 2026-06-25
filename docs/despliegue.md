# Guía de Despliegue y Configuración - LP2-Zoom

Este documento detalla los requisitos del entorno, la configuración de parámetros de la base de datos y el proceso de compilación y empaquetado del sistema **LP2-Zoom**.

## 1. Requisitos Previos del Sistema

Antes de iniciar el servidor y los clientes, asegúrese de contar con las siguientes herramientas en su entorno:

*   **Java Development Kit (JDK):** Versión 17 o superior (Java 26 es compatible y está validado en desarrollo).
*   **Gestor de Proyectos:** **Maven** (para la resolución automática de dependencias y empaquetado).
*   **Base de Datos:** Instancia activa de PostgreSQL en Supabase.
*   **Entorno Gráfico:** Sistema operativo con soporte para Swing (Windows, macOS o Linux con X11 habilitado).

---

## 2. Configuración de Variables y Propiedades

El Servidor lee las credenciales de conectividad JDBC desde el archivo de configuración [config.properties](../Servidor/src/main/resources/config.properties). 

### Formato de Variables (Ejemplo de `config.properties`)
Debe crearse un archivo en la ruta `Servidor/src/main/resources/config.properties` con los siguientes parámetros relacionales (use sus credenciales obtenidas desde la consola de Supabase):

```ini
# Configuración de Base de Datos - Supabase PostgreSQL
db.host=aws-1-us-east-2.pooler.supabase.com
db.port=5432
db.name=postgres
db.user=postgres.your-supabase-project-id
db.password=your-database-password
```

> [!CAUTION]
> **Seguridad de Credenciales**
> Nunca suba el archivo `config.properties` con contraseñas reales a repositorios públicos de GitHub. Asegúrese de añadir esta ruta en su archivo `.gitignore`.

---

## 3. Configuración de la Cadena de Conexión JDBC

La conexión es provista por la clase [ConexionBD](../Servidor/src/main/java/database/ConexionBD.java). La cadena JDBC se arma dinámicamente forzando el uso de SSL para la seguridad de Supabase:

```java
// Construcción de la URL JDBC con SSL requerido para Supabase
String url = "jdbc:postgresql://" + props.getProperty("db.host") + ":"
        + props.getProperty("db.port") + "/" + props.getProperty("db.name")
        + "?sslmode=require";

return DriverManager.getConnection(url, props.getProperty("db.user"), props.getProperty("db.password"));
```

---

## 4. Compilación y Empaquetamiento con Maven

El repositorio está estructurado como un proyecto modular multiproyecto o con poms independientes para Cliente y Servidor.

### A. Construir el Servidor
Para limpiar la compilación y empaquetar un archivo `.jar` ejecutable con todas las dependencias del servidor:

```bash
cd Servidor/
mvn clean package
```
*   **Artefacto generado:** `Servidor/target/Servidor-1.0-SNAPSHOT.jar`

### B. Construir el Cliente
Para limpiar y compilar los archivos de interfaz gráfica y la red asíncrona del cliente:

```bash
cd ../Cliente/
mvn clean package
```
*   **Artefacto generado:** `Cliente/target/Cliente-1.0-SNAPSHOT.jar`

---

## 5. Ejecución en Producción (Consola)

Una vez compilados los JARs, puede iniciar los módulos usando la consola de comandos de Java:

### Iniciar el Servidor de Sockets:
```bash
java -jar Servidor/target/Servidor-1.0-SNAPSHOT.jar
```

### Iniciar una Instancia de Cliente (Login):
```bash
java -jar Cliente/target/Cliente-1.0-SNAPSHOT.jar
```
*(Nota: Puede ejecutar el JAR del cliente múltiples veces en la misma o en distintas computadoras de la red local para simular la conexión de múltiples participantes).*
