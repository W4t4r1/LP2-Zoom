package network.camera;

/**
 * Interfaz común para todas las estrategias de origen de video (Estrategia).
 */
public interface CameraStrategy {
    /**
     * Inicia la captura/transmisión de video.
     * 
     * @return true si se inició correctamente, false en caso contrario.
     */
    boolean start();

    /**
     * Detiene la captura/transmisión de video.
     */
    void stop();

    /**
     * Indica si el flujo de video está activo actualmente.
     * 
     * @return true si está activo, false en caso contrario.
     */
    boolean isActive();
}
