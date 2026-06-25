package network.camera;

/**
 * Creador concreto para la cámara física (Factory Method).
 */
public class PhysicalCameraCreator extends CameraCreator {
    @Override
    public CameraStrategy createCamera(int userId, String userName, String roomCode) {
        return new PhysicalCameraStrategy(userId, userName, roomCode);
    }
}
