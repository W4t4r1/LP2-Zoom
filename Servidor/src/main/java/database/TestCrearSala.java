package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestCrearSala {
    public static void main(String[] args) {
        System.out.println("[*] TestCrearSala: Iniciando...");
        String codigoSala = "TEST01";
        String nombre = "Sala Test Headless";
        int idHost = 1;

        try {
            System.out.println("[*] Conectando a la base de datos...");
            try (Connection conn = ConexionBD.conectar()) {
                System.out.println("[*] Conectado. Verificando si existe la sala...");
                
                // Primero verifiquemos si el host existe
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM Usuarios WHERE IdUsuario = ?")) {
                    ps.setInt(1, idHost);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("[+] Host encontrado: " + rs.getString("Nombres"));
                        } else {
                            System.err.println("[-] Host no encontrado!");
                            return;
                        }
                    }
                }

                // Intentar borrar la sala si ya existe
                System.out.println("[*] Eliminando sala de test anterior si existe...");
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Salas WHERE CodigoSala = ?")) {
                    ps.setString(1, codigoSala);
                    ps.executeUpdate();
                }

                System.out.println("[*] Insertando nueva sala...");
                String query = "INSERT INTO Salas (CodigoSala, Nombre, IdHost, Estado) VALUES (?, ?, ?, 'ACTIVA')";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, codigoSala);
                    ps.setString(2, nombre);
                    ps.setInt(3, idHost);
                    int rows = ps.executeUpdate();
                    System.out.println("[+] Filas insertadas: " + rows);
                }
            }
            System.out.println("[🎉 ÉXITO] TestCrearSala finalizado con éxito.");
        } catch (Exception e) {
            System.err.println("[-] Error durante la prueba:");
            e.printStackTrace();
        }
    }
}
