package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServidor {
    
    // Puerto estándar para nuestro proyecto
    private static final int PUERTO = 5000;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   INICIANDO SERVIDOR ZOOM SOCKETS       ");
        System.out.println("=========================================");

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("[OK] Servidor escuchando en el puerto " + PUERTO + "...");

            // Bucle infinito: el servidor nunca se apaga, siempre espera clientes
            while (true) {
                // El código se pausa en esta línea hasta que un cliente toca la puerta
                Socket socketCliente = serverSocket.accept();
                System.out.println("\n[!] ¡Nuevo cliente intentando conectar!");

                // Creamos el hilo para este cliente y lo iniciamos
                ManejadorCliente manejador = new ManejadorCliente(socketCliente);
                Thread hiloCliente = new Thread(manejador);
                hiloCliente.start(); // Esto hace que el método run() del manejador se ejecute en paralelo
            }

        } catch (IOException e) {
            System.err.println("[ERROR] No se pudo iniciar el servidor: " + e.getMessage());
        }
    }
}