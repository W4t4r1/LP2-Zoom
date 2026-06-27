package database; // o database, según tu paquete final

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionBD {
    private static Connection connection;
    private static Properties props;

    private static synchronized void loadProps() throws SQLException {
        if (props == null) {
            Properties tempProps = new Properties();
            try (InputStream is = ConexionBD.class.getClassLoader()
                    .getResourceAsStream("config.properties")) {
                if (is == null) {
                    throw new SQLException("El archivo config.properties no se encontro en el classpath.");
                }
                tempProps.load(is);
            } catch (Exception e) {
                throw new SQLException("No se pudo leer config.properties", e);
            }
            props = tempProps;
        }
    }

    public static synchronized Connection conectar() throws SQLException {
        loadProps();
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                }
            }
            String url = "jdbc:postgresql://" + props.getProperty("db.host") + ":"
                    + props.getProperty("db.port") + "/" + props.getProperty("db.name")
                    + "?sslmode=require";

            connection = DriverManager.getConnection(url, props.getProperty("db.user"),
                    props.getProperty("db.password"));
            System.out.println("[DB] Nueva conexión física establecida con Supabase.");
        }

        // Retornar un proxy de la conexión que ignore close() para que los bloques try-with-resources no cierren el socket físico
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> {
                if ("close".equals(method.getName())) {
                    // Ignorar el cierre
                    return null;
                }
                try {
                    return method.invoke(connection, args);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        );
    }
}