package database;

import java.util.List;
import java.util.Map;
import model.Usuario;
import model.MensajeSocket;

public interface DBStrategy {
    Usuario login(String correo, String password);
    boolean registrar(String nombres, String correo, String password, String rol);
    boolean existeCorreo(String correo);
    boolean crearSala(String codigoSala, String nombre, int idHost);
    int obtenerIdSalaPorCodigo(String codigoSala);
    int obtenerHostIdPorCodigo(String codigoSala);
    boolean solicitarUnirseASala(String codigoSala, int idUsuario);
    boolean actualizarEstadoSolicitud(String codigoSala, int idUsuario, String nuevoEstado);
    boolean agregarParticipante(String codigoSala, int idUsuario);
    List<Map<String, Object>> obtenerSolicitudesPendientes(String codigoSala);
    List<Integer> obtenerParticipantesActivos(String codigoSala);
    boolean guardarMensaje(String codigoSala, int idUsuario, String contenido);
    boolean guardarArchivo(String codigoSala, int idUsuario, String nombreArchivo, String rutaArchivo);
    List<MensajeSocket> obtenerHistorialMensajes(String codigoSala);
    List<Map<String, Object>> obtenerArchivosCompartidos(String codigoSala);
}
