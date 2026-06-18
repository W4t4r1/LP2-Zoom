package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

public class ListTables {
    public static void main(String[] args) {
        System.out.println("[*] Recuperando listado de tablas físicas en Supabase por JDBC...");
        try (Connection conn = ConexionBD.conectar()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String[] types = {"TABLE"};
            try (ResultSet rs = metaData.getTables(null, "public", "%", types)) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    System.out.println("Tabla encontrada: '" + tableName + "'");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
