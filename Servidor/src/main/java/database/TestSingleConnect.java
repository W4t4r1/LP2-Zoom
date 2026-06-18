package database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class TestSingleConnect {
    public static void main(String[] args) {
        System.out.println("[*] Cargando configuración desde config.properties...");
        Properties config = new Properties();
        try (InputStream is = TestSingleConnect.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            config.load(is);
        } catch (Exception e) {
            System.err.println("Error al cargar config.properties: " + e.getMessage());
            return;
        }

        String host = config.getProperty("db.host");
        String port = config.getProperty("db.port");
        String dbName = config.getProperty("db.name");
        String user = config.getProperty("db.user");
        String password = config.getProperty("db.password");

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName + "?sslmode=require";

        System.out.println("[*] Probando conexión JDBC con los valores configurados:");
        System.out.println("URL: " + url);
        System.out.println("Usuario: " + user);
        System.out.println("Password: " + password);

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            if (conn != null) {
                System.out.println("\n[🎉 ÉXITO] ¡Conexión establecida exitosamente con Supabase!");
                System.out.println("Catálogo actual: " + conn.getCatalog());
            }
        } catch (Exception e) {
            System.err.println("\n[EXCEPCIÓN] Error de conexión:");
            e.printStackTrace();
        }
    }
}
