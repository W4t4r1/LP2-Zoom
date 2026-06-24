package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Usuario;
import model.MensajeSocket;

public class DBService implements DBStrategy {

    /**
     * Valida credenciales contra la tabla de Usuarios.
     * 
     * @param correo Correo electrónico.
     * @param password Contraseña en texto plano.
     * @return El objeto Usuario si las credenciales son válidas, null en caso contrario.
     */
    @Override
    public Usuario login(String correo, String password) {
        String query = "SELECT IdUsuario, Nombres, Correo, PasswordHash, Rol FROM Usuarios WHERE Correo = ?";
        String hashIngresado = HashUtils.hashPassword(password);
        
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hashGuardado = rs.getString("PasswordHash");
                    if (hashGuardado.equalsIgnoreCase(hashIngresado)) {
                        return new Usuario(
                            rs.getInt("IdUsuario"),
                            rs.getString("Nombres"),
                            rs.getString("Correo"),
                            rs.getString("Rol")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error en login: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Registra un nuevo usuario en Supabase con su contraseña hasheada.
     */
    @Override
    public boolean registrar(String nombres, String correo, String password, String rol) {
        String query = "INSERT INTO Usuarios (Nombres, Correo, PasswordHash, Rol) VALUES (?, ?, ?, ?)";
        String hash = HashUtils.hashPassword(password);
        
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, nombres);
            ps.setString(2, correo);
            ps.setString(3, hash);
            ps.setString(4, rol);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al registrar usuario: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Registra una sala nueva en la base de datos.
     */
    @Override
    public boolean crearSala(String codigoSala, String nombre, int idHost) {
        String query = "INSERT INTO Salas (CodigoSala, Nombre, IdHost, Estado) VALUES (?, ?, ?, 'ACTIVA')";
        
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, codigoSala);
            ps.setString(2, nombre);
            ps.setInt(3, idHost);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al crear sala: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Obtiene el ID numérico de una sala a partir de su código único.
     */
    @Override
    public int obtenerIdSalaPorCodigo(String codigoSala) {
        String query = "SELECT IdSala FROM Salas WHERE CodigoSala = ? AND Estado = 'ACTIVA'";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, codigoSala);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("IdSala");
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al obtener ID de sala: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Obtiene el ID del anfitrión (Host) de una sala a partir de su código único.
     */
    @Override
    public int obtenerHostIdPorCodigo(String codigoSala) {
        String query = "SELECT IdHost FROM Salas WHERE CodigoSala = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, codigoSala);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("IdHost");
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al obtener ID del Host: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Registra una solicitud de ingreso a una sala en estado PENDIENTE.
     */
    @Override
    public boolean solicitarUnirseASala(String codigoSala, int idUsuario) {
        int idSala = obtenerIdSalaPorCodigo(codigoSala);
        if (idSala == -1) {
            return false;
        }
        
        String query = "INSERT INTO SolicitudesSala (IdSala, IdUsuario, Estado) VALUES (?, ?, 'PENDIENTE') " +
                       "ON CONFLICT (IdSala, IdUsuario) DO UPDATE SET Estado = 'PENDIENTE'";
        
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, idSala);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al solicitar unirse a sala: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Actualiza el estado de la solicitud y, si es ACEPTADO, agrega al usuario a la lista de participantes activos.
     */
    @Override
    public boolean actualizarEstadoSolicitud(String codigoSala, int idUsuario, String nuevoEstado) {
        int idSala = obtenerIdSalaPorCodigo(codigoSala);
        if (idSala == -1) {
            return false;
        }
        
        String updateQuery = "UPDATE SolicitudesSala SET Estado = ? WHERE IdSala = ? AND IdUsuario = ?";
        try (Connection conn = ConexionBD.conectar()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUpdate = conn.prepareStatement(updateQuery)) {
                psUpdate.setString(1, nuevoEstado);
                psUpdate.setInt(2, idSala);
                psUpdate.setInt(3, idUsuario);
                
                int rows = psUpdate.executeUpdate();
                if (rows > 0) {
                    if ("ACEPTADO".equalsIgnoreCase(nuevoEstado)) {
                        String insertPartQuery = "INSERT INTO ParticipantesSala (IdSala, IdUsuario, Estado) VALUES (?, ?, 'ACTIVO') " +
                                                 "ON CONFLICT (IdSala, IdUsuario) DO UPDATE SET Estado = 'ACTIVO'";
                        try (PreparedStatement psInsert = conn.prepareStatement(insertPartQuery)) {
                            psInsert.setInt(1, idSala);
                            psInsert.setInt(2, idUsuario);
                            psInsert.executeUpdate();
                        }
                    } else if ("RECHAZADO".equalsIgnoreCase(nuevoEstado)) {
                        // Opcional: Eliminar o desactivar participación
                        String deletePartQuery = "DELETE FROM ParticipantesSala WHERE IdSala = ? AND IdUsuario = ?";
                        try (PreparedStatement psDelete = conn.prepareStatement(deletePartQuery)) {
                            psDelete.setInt(1, idSala);
                            psDelete.setInt(2, idUsuario);
                            psDelete.executeUpdate();
                        }
                    }
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al actualizar estado de solicitud: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Agrega directamente a un participante a la sala.
     */
    @Override
    public boolean agregarParticipante(String codigoSala, int idUsuario) {
        int idSala = obtenerIdSalaPorCodigo(codigoSala);
        if (idSala == -1) {
            return false;
        }
        String query = "INSERT INTO ParticipantesSala (IdSala, IdUsuario, Estado) VALUES (?, ?, 'ACTIVO') " +
                       "ON CONFLICT (IdSala, IdUsuario) DO UPDATE SET Estado = 'ACTIVO'";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, idSala);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al agregar participante: " + e.getMessage());
        }
        return false;
    }

    /**
     * Obtiene la lista de solicitudes pendientes para que el Host las visualice.
     * Retorna una estructura serializable (lista de mapas).
     */
    @Override
    public List<Map<String, Object>> obtenerSolicitudesPendientes(String codigoSala) {
        List<Map<String, Object>> lista = new ArrayList<>();
        int idSala = obtenerIdSalaPorCodigo(codigoSala);
        if (idSala == -1) {
            return lista;
        }
        
        String query = "SELECT u.IdUsuario, u.Nombres, u.Correo, s.FechaSolicitud " +
                       "FROM SolicitudesSala s " +
                       "JOIN Usuarios u ON s.IdUsuario = u.IdUsuario " +
                       "WHERE s.IdSala = ? AND s.Estado = 'PENDIENTE' " +
                       "ORDER BY s.FechaSolicitud ASC";
        
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, idSala);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> solicitud = new HashMap<>();
                    solicitud.put("idUsuario", rs.getInt("IdUsuario"));
                    solicitud.put("nombres", rs.getString("Nombres"));
                    solicitud.put("correo", rs.getString("Correo"));
                    solicitud.put("fechaSolicitud", rs.getTimestamp("FechaSolicitud").toString());
                    lista.add(solicitud);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al obtener solicitudes pendientes: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Obtiene los IDs de los participantes activos de una sala admitida.
     */
    @Override
    public List<Integer> obtenerParticipantesActivos(String codigoSala) {
        List<Integer> participantes = new ArrayList<>();
        int idSala = obtenerIdSalaPorCodigo(codigoSala);
        if (idSala == -1) {
            return participantes;
        }

        String query = "SELECT IdUsuario FROM ParticipantesSala WHERE IdSala = ? AND Estado = 'ACTIVO'";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, idSala);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    participantes.add(rs.getInt("IdUsuario"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al obtener participantes activos: " + e.getMessage());
            e.printStackTrace();
        }
        return participantes;
    }

    /**
     * Persiste un mensaje de chat enviado por un usuario en una sala.
     */
    @Override
    public boolean guardarMensaje(String codigoSala, int idUsuario, String contenido) {
        int idSala = obtenerIdSalaPorCodigo(codigoSala);
        if (idSala == -1) {
            return false;
        }
        
        String query = "INSERT INTO Mensajes (IdSala, IdUsuario, Contenido) VALUES (?, ?, ?)";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, idSala);
            ps.setInt(2, idUsuario);
            ps.setString(3, contenido);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al guardar mensaje: " + e.getMessage());
        }
        return false;
    }

    /**
     * Registra los metadatos de un archivo compartido y su ubicación física en el Servidor.
     */
    @Override
    public boolean guardarArchivo(String codigoSala, int idUsuario, String nombreArchivo, String rutaArchivo) {
        int idSala = obtenerIdSalaPorCodigo(codigoSala);
        if (idSala == -1) {
            return false;
        }
        
        String query = "INSERT INTO ArchivosCompartidos (IdSala, IdUsuario, NombreArchivo, RutaArchivo) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, idSala);
            ps.setInt(2, idUsuario);
            ps.setString(3, nombreArchivo);
            ps.setString(4, rutaArchivo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al guardar archivo: " + e.getMessage());
        }
        return false;
    }

    /**
     * Obtiene el historial de mensajes de una sala ordenados cronológicamente.
     */
    @Override
    public List<MensajeSocket> obtenerHistorialMensajes(String codigoSala) {
        List<MensajeSocket> historial = new ArrayList<>();
        int idSala = obtenerIdSalaPorCodigo(codigoSala);
        if (idSala == -1) {
            return historial;
        }

        String query = "SELECT m.IdMensaje, u.IdUsuario, u.Nombres, m.Contenido, m.FechaEnvio " +
                       "FROM Mensajes m " +
                       "JOIN Usuarios u ON m.IdUsuario = u.IdUsuario " +
                       "WHERE m.IdSala = ? " +
                       "ORDER BY m.FechaEnvio ASC";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, idSala);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MensajeSocket msg = new MensajeSocket();
                    msg.setType("CHAT_MESSAGE");
                    msg.setRoomCode(codigoSala);
                    msg.setUserId(rs.getInt("IdUsuario"));
                    msg.setUserName(rs.getString("Nombres"));
                    msg.setMessage(rs.getString("Contenido"));
                    msg.setSentAt(rs.getTimestamp("FechaEnvio").toString());
                    historial.add(msg);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al obtener historial de mensajes: " + e.getMessage());
            e.printStackTrace();
        }
        return historial;
    }

    /**
     * Obtiene la lista de archivos compartidos en una sala.
     */
    @Override
    public List<Map<String, Object>> obtenerArchivosCompartidos(String codigoSala) {
        List<Map<String, Object>> lista = new ArrayList<>();
        int idSala = obtenerIdSalaPorCodigo(codigoSala);
        if (idSala == -1) {
            return lista;
        }

        String query = "SELECT a.NombreArchivo, a.RutaArchivo, u.Nombres, a.FechaSubida " +
                       "FROM ArchivosCompartidos a " +
                       "JOIN Usuarios u ON a.IdUsuario = u.IdUsuario " +
                       "WHERE a.IdSala = ? " +
                       "ORDER BY a.FechaSubida DESC";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, idSala);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> archivo = new HashMap<>();
                    archivo.put("nombreArchivo", rs.getString("NombreArchivo"));
                    archivo.put("rutaArchivo", rs.getString("RutaArchivo"));
                    archivo.put("nombres", rs.getString("Nombres"));
                    archivo.put("fechaSubida", rs.getTimestamp("FechaSubida").toString());
                    lista.add(archivo);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al obtener archivos compartidos: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }
}
