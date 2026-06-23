package network.camera;

import com.github.sarxos.webcam.Webcam;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import model.MensajeSocket;
import network.ClienteConexion;

/**
 * Estrategia de cámara que captura la webcam física (Concrete Strategy).
 */
public class PhysicalCameraStrategy implements CameraStrategy {

    private final int userId;
    private final String userName;
    private final String roomCode;
    private final int width = 320;
    private final int height = 240;
    private final int fps = 5;
    private Webcam webcam;
    private ScheduledExecutorService executor;

    public PhysicalCameraStrategy(int userId, String userName, String roomCode) {
        this.userId = userId;
        this.userName = userName;
        this.roomCode = roomCode;
    }

    @Override
    public synchronized boolean start() {
        if (executor != null && !executor.isShutdown()) return true;

        try {
            // Timeout de 3000ms para evitar cuelgues indefinidos en DirectShow
            webcam = Webcam.getDefault(3000);
            if (webcam == null) {
                System.err.println("[-] [PhysicalCamera] No se encontró ninguna webcam física.");
                return false;
            }

            webcam.setViewSize(new java.awt.Dimension(width, height));
            
            // Abrir de forma síncrona para verificar si realmente se pudo inicializar la cámara
            boolean opened = webcam.open();
            if (!opened) {
                System.err.println("[-] [PhysicalCamera] No se pudo abrir la webcam física (open() retornó false).");
                return false;
            }

            executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "PhysicalCameraCapture-" + userId);
                t.setDaemon(true);
                return t;
            });

            long period = 1000L / Math.max(1, fps);
            executor.scheduleAtFixedRate(this::tick, 0, period, TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            System.err.println("[-] [PhysicalCamera] Error al iniciar la webcam: " + e.getMessage());
            stop();
            return false;
        }
    }

    @Override
    public synchronized void stop() {
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

    @Override
    public synchronized boolean isActive() {
        return executor != null && !executor.isShutdown() && webcam != null && webcam.isOpen();
    }

    private void tick() {
        try {
            Webcam tempWebcam = this.webcam;
            if (tempWebcam == null || !tempWebcam.isOpen()) return;

            BufferedImage img = tempWebcam.getImage();
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
            System.err.println("[-] [PhysicalCamera] Error al capturar frame de webcam: " + e.getMessage());
        }
    }
}
