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
    private int consecutiveFailures = 0;

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
            java.util.List<Webcam> webcams = Webcam.getWebcams(3000);
            if (webcams == null || webcams.isEmpty()) {
                System.err.println("[-] [PhysicalCamera] No se encontró ninguna webcam física.");
                return false;
            }

            System.out.println("[*] [PhysicalCamera] Listando webcams detectadas:");
            for (Webcam w : webcams) {
                System.out.println("  -> " + w.getName());
            }

            // Intentar abrir la primera cámara disponible que funcione (evitando IR/Hello o bloqueadas)
            boolean opened = false;
            java.util.Set<String> failedCameras = new java.util.HashSet<>();

            for (Webcam w : webcams) {
                // Saltar cámaras de infrarrojos (IR) si es posible por nombre
                String nameLower = w.getName().toLowerCase();
                if (nameLower.contains("ir camera") || nameLower.contains("hello") || nameLower.contains("virtual") || nameLower.contains("voice") || nameLower.contains("audio") || nameLower.contains("control")) {
                    System.out.println("[*] [PhysicalCamera] Saltando cámara sospechosa de ser IR/Virtual/Audio: " + w.getName());
                    continue;
                }
                
                System.out.println("[*] [PhysicalCamera] Intentando abrir: " + w.getName());
                try {
                    // No forzamos setViewSize para evitar errores de mismatch de buffer nativos
                    
                    // Ejecutar open y primer testImg en un executor con timeout de 2000ms
                    java.util.concurrent.ExecutorService openExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
                    java.util.concurrent.Future<Boolean> openFuture = openExecutor.submit(() -> {
                        if (w.open()) {
                            // Intentar capturar un frame
                            for (int i = 0; i < 3; i++) {
                                java.awt.image.BufferedImage img = w.getImage();
                                if (img != null) return true;
                                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                            }
                        }
                        return false;
                    });
                    
                    boolean success = false;
                    try {
                        success = openFuture.get(2000, java.util.concurrent.TimeUnit.MILLISECONDS);
                    } catch (java.util.concurrent.TimeoutException te) {
                        System.err.println("[-] [PhysicalCamera] Timeout al intentar abrir/probar la cámara " + w.getName());
                        openFuture.cancel(true);
                    } catch (Exception e) {
                        System.err.println("[-] [PhysicalCamera] Excepción al probar la cámara: " + e.getMessage());
                    } finally {
                        openExecutor.shutdownNow();
                    }
                    
                    if (success) {
                        webcam = w;
                        opened = true;
                        System.out.println("[+] [PhysicalCamera] Cámara abierta con éxito y capturando frames: " + w.getName());
                        break;
                    } else {
                        System.err.println("[-] [PhysicalCamera] No se pudo abrir o capturar frames de " + w.getName() + ". Cerrando...");
                        failedCameras.add(w.getName());
                        try { w.close(); } catch (Exception ignored) {}
                    }
                } catch (Throwable t) {
                    System.err.println("[-] [PhysicalCamera] Error al abrir " + w.getName() + ": " + t.getMessage());
                    failedCameras.add(w.getName());
                }
            }



            if (!opened || webcam == null) {
                System.err.println("[-] [PhysicalCamera] No se pudo abrir ninguna webcam física.");
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
        } catch (Throwable e) {
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
            if (img == null) {
                consecutiveFailures++;
                if (consecutiveFailures >= 5) {
                    System.err.println("[-] [PhysicalCamera] Detectadas 5 fallas consecutivas (frame nulo). Forzando fallback a simulación...");
                    UI.RoomFrame activeFrame = UI.RoomFrame.getActiveInstance();
                    if (activeFrame != null) {
                        activeFrame.forzarFallbackASimulacion();
                    }
                }
                return;
            }

            // Si hay éxito, resetear contador
            consecutiveFailures = 0;

            // Renderizar localmente en la interfaz
            UI.RoomFrame activeFrame = UI.RoomFrame.getActiveInstance();
            if (activeFrame != null) {
                activeFrame.mostrarFrameLocal(img);
            }

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
            consecutiveFailures++;
            if (consecutiveFailures >= 5) {
                System.err.println("[-] [PhysicalCamera] Detectadas 5 fallas consecutivas por excepción. Forzando fallback a simulación...");
                UI.RoomFrame activeFrame = UI.RoomFrame.getActiveInstance();
                if (activeFrame != null) {
                    activeFrame.forzarFallbackASimulacion();
                }
            }
        }
    }

}
