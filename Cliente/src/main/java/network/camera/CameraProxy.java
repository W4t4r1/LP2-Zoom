package network.camera;

/**
 * Proxy inteligente para controlar el acceso, registro y fallback del origen de video (Proxy).
 */
public class CameraProxy implements CameraStrategy {
    private final int userId;
    private final String userName;
    private final String roomCode;
    private CameraCreator creator;
    private CameraStrategy realSubject;

    // Protection Proxy: Flag de control de acceso para simular permisos
    private static boolean permissionGranted = true;

    public CameraProxy(int userId, String userName, String roomCode, CameraCreator creator) {
        this.userId = userId;
        this.userName = userName;
        this.roomCode = roomCode;
        this.creator = creator;
    }

    public static void setPermissionGranted(boolean granted) {
        permissionGranted = granted;
    }

    public static boolean isPermissionGranted() {
        return permissionGranted;
    }

    @Override
    public boolean start() {
        // Logging Proxy: Registrar el intento de encendido
        System.out.println("[Proxy] Interceptando llamada a start() para el usuario: " + userName);

        // Protection Proxy: Validar permisos antes de delegar
        if (!permissionGranted) {
            System.err.println("[Proxy] [ACCESO RECHAZADO] No se puede iniciar la cámara. Permiso denegado.");
            return false;
        }

        // Virtual Proxy: Inicialización perezosa (Lazy Initialization)
        if (realSubject == null) {
            System.out.println("[Proxy] Inicialización perezosa: Creando instancia real de la cámara...");
            realSubject = creator.createCamera(userId, userName, roomCode);
        }

        System.out.println("[Proxy] Delegando start() al sujeto real: " + realSubject.getClass().getSimpleName());
        boolean success = realSubject.start();

        // Mecanismo de Fallback Transparente en el Proxy
        if (!success && creator instanceof PhysicalCameraCreator) {
            System.out.println("[Proxy] Fallo al iniciar la cámara física. Conmutando automáticamente al simulador...");
            // Cambiar creador y recrear sujeto real con estrategia simulada
            this.creator = new SimulatedCameraCreator();
            this.realSubject = creator.createCamera(userId, userName, roomCode);
            
            System.out.println("[Proxy] Intentando arrancar la cámara simulada de respaldo...");
            success = this.realSubject.start();
        }

        return success;
    }

    @Override
    public void stop() {
        System.out.println("[Proxy] Interceptando llamada a stop()");
        if (realSubject != null) {
            System.out.println("[Proxy] Delegando stop() al sujeto real: " + realSubject.getClass().getSimpleName());
            realSubject.stop();
        } else {
            System.out.println("[Proxy] El sujeto real no estaba inicializado. Nada que detener.");
        }
    }

    @Override
    public boolean isActive() {
        return realSubject != null && realSubject.isActive();
    }

    /**
     * Obtiene el sujeto real actualmente en uso (útil para que la UI sepa si hubo fallback).
     * 
     * @return La estrategia real de cámara delegada.
     */
    public CameraStrategy getRealSubject() {
        return realSubject;
    }
}
