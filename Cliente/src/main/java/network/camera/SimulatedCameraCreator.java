package network.camera;

/**
 * Creador concreto para la cámara simulada (Factory Method).
 */
public class SimulatedCameraCreator extends CameraCreator {
    @Override
    public CameraStrategy createCamera(int userId, String userName, String roomCode) {
        return new SimulatedCameraStrategy(userId, userName, roomCode);
    }
}
