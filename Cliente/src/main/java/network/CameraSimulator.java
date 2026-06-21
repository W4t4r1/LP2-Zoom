package network;

import model.MensajeSocket;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/**
 * Simulador de cámara simple que genera imágenes dinámicas para pruebas.
 * Comprime a JPG, codifica en Base64 y envía `CAMERA_FRAME` por `ClienteConexion`.
 */
public class CameraSimulator {
    private final int userId;
    private final String userName;
    private final String roomCode;
    private final int width = 320;
    private final int height = 240;
    private final int fps = 5; // frames por segundo
    private ScheduledExecutorService executor;

    public CameraSimulator(int userId, String userName, String roomCode) {
        this.userId = userId;
        this.userName = userName;
        this.roomCode = roomCode;
    }

    public void start() {
        if (executor != null && !executor.isShutdown()) return;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CameraSimulator-" + userId);
            t.setDaemon(true);
            return t;
        });
        long period = 1000L / Math.max(1, fps);
        executor.scheduleAtFixedRate(this::tick, 0, period, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void tick() {
        try {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();

            // Fondo degradado simple
            Color c1 = new Color(30, 30, 40);
            Color c2 = new Color(60, 80, 120);
            for (int y = 0; y < height; y++) {
                float t = (float) y / (height - 1);
                int r = (int) (c1.getRed() * (1 - t) + c2.getRed() * t);
                int gr = (int) (c1.getGreen() * (1 - t) + c2.getGreen() * t);
                int b = (int) (c1.getBlue() * (1 - t) + c2.getBlue() * t);
                g.setColor(new Color(r, gr, b));
                g.fillRect(0, y, width, 1);
            }

            // Dibujar un círculo móvil para simular movimiento
            long now = System.currentTimeMillis();
            int cx = (int) ((width / 2) + Math.sin(now / 300.0) * (width / 4));
            int cy = height / 2;
            int radius = 30;
            g.setColor(new Color(240, 180, 70));
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

            // Texto con usuario y timestamp
            g.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g.setColor(Color.WHITE);
            String label = userName + " (" + userId + ")";
            g.drawString(label, 10, 20);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g.drawString(Instant.now().toString().substring(11, 19), 10, 40);

            g.dispose();

            // Comprimir a JPG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            byte[] jpg = baos.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(jpg);

            // Construir mensaje
            MensajeSocket msg = new MensajeSocket();
            msg.setType("CAMERA_FRAME");
            msg.setRoomCode(roomCode);
            msg.setUserId(userId);
            msg.setUserName(userName);
            msg.setMessage(base64);

            ClienteConexion.getInstancia().enviarMensaje(msg);

        } catch (Exception e) {
            System.err.println("[-] CameraSimulator error: " + e.getMessage());
        }
    }
}
