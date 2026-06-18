package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static final String HOST = "aws-1-us-east-2.pooler.supabase.com";
    private static final String PUERTO = "5432";
    private static final String BASE = "postgres";
    private static final String USUARIO = "postgres.fdwfbqrtowdvlufihsty";
    private static final String PASSWORD = "5349090Tiend@"; // reemplaza con tu password real

    private static final String URL =
        "jdbc:postgresql://" + HOST + ":" + PUERTO + "/" + BASE + "?sslmode=require";

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }

    // Prueba rápida de conexión
    public static void main(String[] args) {
        try (Connection con = conectar()) {
            if (con != null && !con.isClosed()) {
                System.out.println("Conexion exitosa a Supabase.");
            }
        } catch (SQLException e) {
            System.out.println("Error al conectar: " + e.getMessage());
        }
    }
}