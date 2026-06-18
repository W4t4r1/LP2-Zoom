package database; // o database, según tu paquete final

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionBD {

    public static Connection conectar() throws SQLException {
        Properties props = new Properties();
        try (InputStream is = ConexionBD.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            props.load(is);
        } catch (Exception e) {
            throw new SQLException("No se pudo leer config.properties", e);
        }

        String url = "jdbc:postgresql://" + props.getProperty("db.host") + ":"
                + props.getProperty("db.port") + "/" + props.getProperty("db.name")
                + "?sslmode=require";

        return DriverManager.getConnection(url, props.getProperty("db.user"),
                props.getProperty("db.password"));
    }
}