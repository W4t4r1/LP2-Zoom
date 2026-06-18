package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import model.MensajeSocket;

public class MainServidor {
    
    // Puerto estándar para nuestro proyecto
    private static final int PUERTO = 5000;
    
    // Pool de hilos dinámico para manejar múltiples clientes concurrentemente
    private static final ExecutorService poolHilos = Executors.newCachedThreadPool();

    // Registro global de clientes autenticados activos (IdUsuario -> Manejador)
    public static final Map<Integer, ManejadorCliente> clientesActivos = new ConcurrentHashMap<>();

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

                // Creamos el manejador y lo enviamos al pool de hilos para ejecución en paralelo
                ManejadorCliente manejador = new ManejadorCliente(socketCliente);
                poolHilos.submit(manejador);
            }

        } catch (IOException e) {
            System.err.println("[ERROR] No se pudo iniciar el servidor: " + e.getMessage());
        } finally {
            // Cerramos el pool de hilos de forma ordenada al detener el servidor
            poolHilos.shutdown();
        }
    }

    /**
     * Retransmite un mensaje a todos los miembros de una sala específica (excluyendo opcionalmente al remitente).
     * 
     * @param mensaje El mensaje a transmitir.
     * @param excluirId Opcional. ID del usuario a excluir (remitente). Si es null, se envía a todos.
     */
    public static void retransmitirMensaje(MensajeSocket mensaje, Integer excluirId) {
        if (mensaje.getRoomCode() == null) return;
        
        for (Map.Entry<Integer, ManejadorCliente> entry : clientesActivos.entrySet()) {
            Integer userId = entry.getKey();
            ManejadorCliente manejador = entry.getValue();
            
            // Filtramos por sala y por exclusión de remitente
            if (mensaje.getRoomCode().equalsIgnoreCase(manejador.getRoomCode())) {
                if (excluirId == null || !userId.equals(excluirId)) {
                    manejador.enviarMensaje(mensaje);
                }
            }
        }
    }
}