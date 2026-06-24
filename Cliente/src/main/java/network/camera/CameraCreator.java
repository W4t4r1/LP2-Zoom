package network.camera;

/**
 * Clase base creadora para el patrón Factory Method.
 */
public abstract class CameraCreator {
    /**
     * Método de fábrica para instanciar una estrategia de cámara.
     * 
     * @param userId   ID del usuario.
     * @param userName Nombre del usuario.
     * @param roomCode Código de la sala.
     * @return Una instancia de CameraStrategy.
     */
    public abstract CameraStrategy createCamera(int userId, String userName, String roomCode);
}
