package network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import model.MensajeSocket;
import network.bridge.ProtocolBridge;
import network.bridge.JSONProtocolBridge;

public class ClienteConexion {
    private static ClienteConexion instancia;
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private Thread hiloEscucha;
    private boolean conectado = false;
    private String hostActual;

    // Bridge Abstraction: Delegar serialización al implementador del Bridge
    private final ProtocolBridge protocolBridge;

    // Interfaz de callback simple para recibir mensajes
    public interface MensajeListener {
        void onMensajeRecibido(MensajeSocket mensaje);
        void onDesconexion();
    }

    private MensajeListener listener;

    private ClienteConexion(ProtocolBridge bridge) {
        this.protocolBridge = bridge;
    }

    /**
     * Retorna la instancia única de la conexión de red (Patrón Singleton).
     */
    public static synchronized ClienteConexion getInstancia() {
        if (instancia == null) {
            instancia = new ClienteConexion(new JSONProtocolBridge());
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
            hostActual = host;
            
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
                    // Utiliza el Bridge para deserializar
                    MensajeSocket mensaje = protocolBridge.deserialize(linea);
                    if (mensaje != null) {
                        notificarListener(mensaje);
                    }
                } catch (Exception e) {
                    System.err.println("[-] ClienteConexion: Error al deserializar datos: " + e.getMessage());
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
        hostActual = null;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.err.println("[-] Error al cerrar socket: " + e.getMessage());
        }
        notificarDesconexion();
        listener = null;
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
            // Utiliza el Bridge para serializar
            String data = protocolBridge.serialize(mensaje);
            salida.println(data);
        } catch (Exception e) {
            System.err.println("[-] ClienteConexion: Error al enviar: " + e.getMessage());
        }
    }

    public synchronized void setListener(MensajeListener listener) {
        this.listener = listener;
    }

    private synchronized void notificarListener(MensajeSocket mensaje) {
        if (listener != null) {
            listener.onMensajeRecibido(mensaje);
        }
    }

    private synchronized void notificarDesconexion() {
        if (listener != null) {
            listener.onDesconexion();
        }
    }

    public boolean isConectado() {
        return conectado;
    }

    public String getHostActual() {
        return hostActual;
    }
}
