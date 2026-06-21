package network;

import com.github.sarxos.webcam.Webcam;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import model.MensajeSocket;

/**
 * Captura la webcam física usando webcam-capture y envía frames como
 * mensajes CAMERA_FRAME. Si no hay cámara disponible, el método start()
 * devolverá false para permitir un fallback al simulador.
 */
public class CameraCapture {

    private final int userId;
    private final String userName;
    private final String roomCode;
    private final int width = 320;
    private final int height = 240;
    private final int fps = 5;
    private Webcam webcam;
    private ScheduledExecutorService executor;

    public CameraCapture(int userId, String userName, String roomCode) {
        this.userId = userId;
        this.userName = userName;
        this.roomCode = roomCode;
    }

    public boolean start() {
        if (executor != null && !executor.isShutdown()) return true;

        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                System.err.println("[-] No se encontró ninguna webcam física.");
                return false;
            }

            webcam.setViewSize(new java.awt.Dimension(width, height));
            webcam.open(true);

            executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "CameraCapture-" + userId);
                t.setDaemon(true);
                return t;
            });

            long period = 1000L / Math.max(1, fps);
            executor.scheduleAtFixedRate(this::tick, 0, period, TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            System.err.println("[-] Error al iniciar la webcam: " + e.getMessage());
            stop();
            return false;
        }
    }

    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (webcam != null) {
            try {
                webcam.close();
            } catch (Exception ignored) {
            }
            webcam = null;
        }
    }

    private void tick() {
        try {
            if (webcam == null || !webcam.isOpen()) return;

            BufferedImage img = webcam.getImage();
            if (img == null) return;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

            MensajeSocket msg = new MensajeSocket();
            msg.setType("CAMERA_FRAME");
            msg.setRoomCode(roomCode);
            msg.setUserId(userId);
            msg.setUserName(userName);
            msg.setMessage(base64);
            ClienteConexion.getInstancia().enviarMensaje(msg);
        } catch (Exception e) {
            System.err.println("[-] Error al capturar frame de webcam: " + e.getMessage());
        }
    }
}
