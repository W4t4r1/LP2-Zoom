package network;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import model.MensajeSocket;

public class ClienteConexion {
    private static ClienteConexion instancia;
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private Gson gson;
    private Thread hiloEscucha;
    private boolean conectado = false;

    // Interfaz para escuchar los mensajes del servidor
    public interface MensajeListener {
        void onMensajeRecibido(MensajeSocket mensaje);
        void onDesconexion();
    }

    private final List<MensajeListener> listeners = new ArrayList<>();

    private ClienteConexion() {
        this.gson = new Gson();
    }

    /**
     * Retorna la instancia única de la conexión de red (Patrón Singleton).
     */
    public static synchronized ClienteConexion getInstancia() {
        if (instancia == null) {
            instancia = new ClienteConexion();
        }
        return instancia;
    }

    /**
     * Establece la conexión física con el servidor de sockets de Java.
     */
    public synchronized boolean conectar(String host, int puerto) {
        if (conectado) return true;
        try {
            socket = new Socket(host, puerto);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);
            conectado = true;
            
            // Iniciar hilo de escucha asíncrono para no congelar la UI
            hiloEscucha = new Thread(this::escucharServidor);
            hiloEscucha.setName("HiloEscuchaCliente");
            hiloEscucha.start();
            
            System.out.println("[+] ClienteConexion: Conectado a " + host + ":" + puerto);
            return true;
        } catch (Exception e) {
            System.err.println("[-] ClienteConexion: Error de conexión: " + e.getMessage());
            return false;
        }
    }

    private void escucharServidor() {
        try {
            String linea;
            // Lectura continua de mensajes JSON enviados por el servidor
            while (conectado && (linea = entrada.readLine()) != null) {
                try {
                    MensajeSocket mensaje = gson.fromJson(linea, MensajeSocket.class);
                    if (mensaje != null) {
                        notificarListeners(mensaje);
                    }
                } catch (Exception e) {
                    System.err.println("[-] ClienteConexion: Error al deserializar JSON: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[-] ClienteConexion: Hilo de escucha detenido: " + e.getMessage());
        } finally {
            desconectar();
        }
    }

    /**
     * Cierra de manera limpia la conexión del socket.
     */
    public synchronized void desconectar() {
        if (!conectado) return;
        conectado = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.err.println("[-] Error al cerrar socket: " + e.getMessage());
        }
        notificarDesconexion();
        listeners.clear();
        System.out.println("[-] ClienteConexion: Desconectado del servidor.");
    }

    /**
     * Serializa y envía un objeto MensajeSocket por el canal TCP.
     */
    public void enviarMensaje(MensajeSocket mensaje) {
        if (!conectado) {
            System.err.println("[-] ClienteConexion: Imposible enviar, no hay conexión activa.");
            return;
        }
        try {
            String json = gson.toJson(mensaje);
            salida.println(json);
        } catch (Exception e) {
            System.err.println("[-] ClienteConexion: Error al enviar: " + e.getMessage());
        }
    }

    public synchronized void addListener(MensajeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(MensajeListener listener) {
        listeners.remove(listener);
    }

    private synchronized void notificarListeners(MensajeSocket mensaje) {
        List<MensajeListener> copia = new ArrayList<>(listeners);
        for (MensajeListener listener : copia) {
            listener.onMensajeRecibido(mensaje);
        }
    }

    private synchronized void notificarDesconexion() {
        List<MensajeListener> copia = new ArrayList<>(listeners);
        for (MensajeListener listener : copia) {
            listener.onDesconexion();
        }
    }

    public boolean isConectado() {
        return conectado;
    }
}
