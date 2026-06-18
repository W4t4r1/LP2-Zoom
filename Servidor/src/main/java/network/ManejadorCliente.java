package network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ManejadorCliente implements Runnable {
    
    private Socket socketCliente;

    // Constructor que recibe el enchufe (socket) del cliente recién conectado
    public ManejadorCliente(Socket socketCliente) {
        this.socketCliente = socketCliente;
    }

    @Override
    public void run() {
        try {
            System.out.println("[+] Hilo iniciado para el cliente: " + socketCliente.getInetAddress().getHostAddress());
            
            // Preparar el lector para escuchar lo que dice el cliente
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
            String mensajeJSON;

            // Bucle infinito para escuchar a este cliente hasta que se desconecte
            while ((mensajeJSON = entrada.readLine()) != null) {
                System.out.println("Mensaje recibido del cliente: " + mensajeJSON);
                
                // TODO: Aquí usaremos Gson para convertir este String a tu clase MensajeSocket
                // y luego conectaremos con Supabase para hacer el Login o Chat.
            }

        } catch (Exception e) {
            System.err.println("[-] Cliente desconectado o error de red: " + e.getMessage());
        } finally {
            try {
                socketCliente.close(); // Liberamos el recurso si hay error
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}