package database;

import java.util.List;
import java.util.Map;
import model.Usuario;
import model.MensajeSocket;

public class DBProxy implements DBStrategy {
    private final DBCreator creator;
    private DBStrategy realSubject;

    public DBProxy(DBCreator creator) {
        this.creator = creator;
    }

    private synchronized DBStrategy getRealSubject() {
        if (realSubject == null) {
            System.out.println("[DB Proxy] Inicialización perezosa de la base de datos (Virtual Proxy)...");
            realSubject = creator.createDatabase();
        }
        return realSubject;
    }

    @Override
    public synchronized Usuario login(String correo, String password) {
        System.out.println("[DB Proxy Logging] Intento de login para correo: " + correo);
        return getRealSubject().login(correo, password);
    }

    @Override
    public synchronized boolean registrar(String nombres, String correo, String password, String rol) {
        System.out.println("[DB Proxy Logging] Registrando usuario: " + correo + " (" + nombres + ")");
        return getRealSubject().registrar(nombres, correo, password, rol);
    }

    @Override
    public synchronized boolean crearSala(String codigoSala, String nombre, int idHost) {
        System.out.println("[DB Proxy Logging] Creando sala: " + codigoSala + " (" + nombre + ") por Host: " + idHost);
        return getRealSubject().crearSala(codigoSala, nombre, idHost);
    }

    @Override
    public synchronized int obtenerIdSalaPorCodigo(String codigoSala) {
        System.out.println("[DB Proxy Logging] Obteniendo ID de sala por código: " + codigoSala);
        return getRealSubject().obtenerIdSalaPorCodigo(codigoSala);
    }

    @Override
    public synchronized int obtenerHostIdPorCodigo(String codigoSala) {
        System.out.println("[DB Proxy Logging] Obteniendo ID de host para sala: " + codigoSala);
        return getRealSubject().obtenerHostIdPorCodigo(codigoSala);
    }

    @Override
    public synchronized boolean solicitarUnirseASala(String codigoSala, int idUsuario) {
        System.out.println("[DB Proxy Logging] Usuario: " + idUsuario + " solicita unirse a sala: " + codigoSala);
        return getRealSubject().solicitarUnirseASala(codigoSala, idUsuario);
    }

    @Override
    public synchronized boolean actualizarEstadoSolicitud(String codigoSala, int idUsuario, String nuevoEstado) {
        System.out.println("[DB Proxy Logging] Actualizando solicitud en sala: " + codigoSala + " para usuario: " + idUsuario + " a estado: " + nuevoEstado);
        return getRealSubject().actualizarEstadoSolicitud(codigoSala, idUsuario, nuevoEstado);
    }

    @Override
    public synchronized boolean agregarParticipante(String codigoSala, int idUsuario) {
        System.out.println("[DB Proxy Logging] Agregando participante: " + idUsuario + " a sala: " + codigoSala);
        return getRealSubject().agregarParticipante(codigoSala, idUsuario);
    }

    @Override
    public synchronized List<Map<String, Object>> obtenerSolicitudesPendientes(String codigoSala) {
        System.out.println("[DB Proxy Logging] Obteniendo solicitudes pendientes para sala: " + codigoSala);
        return getRealSubject().obtenerSolicitudesPendientes(codigoSala);
    }

    @Override
    public synchronized List<Integer> obtenerParticipantesActivos(String codigoSala) {
        System.out.println("[DB Proxy Logging] Obteniendo participantes activos para sala: " + codigoSala);
        return getRealSubject().obtenerParticipantesActivos(codigoSala);
    }

    @Override
    public synchronized boolean guardarMensaje(String codigoSala, int idUsuario, String contenido) {
        System.out.println("[DB Proxy Logging] Guardando mensaje en sala: " + codigoSala + " de usuario: " + idUsuario);
        return getRealSubject().guardarMensaje(codigoSala, idUsuario, contenido);
    }

    @Override
    public synchronized boolean guardarArchivo(String codigoSala, int idUsuario, String nombreArchivo, String rutaArchivo) {
        System.out.println("[DB Proxy Logging] Guardando metadatos de archivo: " + nombreArchivo + " en sala: " + codigoSala + " por usuario: " + idUsuario);
        return getRealSubject().guardarArchivo(codigoSala, idUsuario, nombreArchivo, rutaArchivo);
    }

    @Override
    public synchronized List<MensajeSocket> obtenerHistorialMensajes(String codigoSala) {
        System.out.println("[DB Proxy Logging] Cargando historial de mensajes para sala: " + codigoSala);
        return getRealSubject().obtenerHistorialMensajes(codigoSala);
    }

    @Override
    public synchronized List<Map<String, Object>> obtenerArchivosCompartidos(String codigoSala) {
        System.out.println("[DB Proxy Logging] Obteniendo archivos compartidos en sala: " + codigoSala);
        return getRealSubject().obtenerArchivosCompartidos(codigoSala);
    }
}
