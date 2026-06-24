package network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import database.DBStrategy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import model.MensajeSocket;
import model.Usuario;

public class ManejadorCliente implements Runnable {
    
    private Socket socketCliente;
    private BufferedReader entrada;
    private PrintWriter salida;
    private Gson gson;

    // Estado de la conexión
    private Integer userId;
    private String userName;
    private String roomCode;
    private boolean admittedToRoom = false;

    // Directorio de almacenamiento de archivos
    private static final String DIRECTORIO_UPLOADS = "uploads";

    // Almacenamiento temporal para transferencias de archivos concurrentes (FileId -> FileOutputStream)
    private static final Map<String, FileOutputStream> archivosEnProgreso = new ConcurrentHashMap<>();
    // Mapa para mapear FileId a Nombre de archivo
    private static final Map<String, String> nombresArchivos = new ConcurrentHashMap<>();

    // Constructor que recibe el enchufe (socket) del cliente recién conectado
    public ManejadorCliente(Socket socketCliente) {
        this.socketCliente = socketCliente;
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try {
            System.out.println("[+] Hilo iniciado para el cliente: " + socketCliente.getInetAddress().getHostAddress());
            
            // Preparar canales de comunicación
            entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
            salida = new PrintWriter(socketCliente.getOutputStream(), true);
            
            String mensajeJSON;

            // Bucle infinito para escuchar a este cliente hasta que se desconecte
            while ((mensajeJSON = entrada.readLine()) != null) {
                try {
                    MensajeSocket mensaje = gson.fromJson(mensajeJSON, MensajeSocket.class);
                    if (mensaje == null || mensaje.getType() == null) continue;

                    procesarMensaje(mensaje);

                } catch (Exception e) {
                    System.err.println("[-] Error al procesar JSON: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("[-] Cliente desconectado o error de red: " + e.getMessage());
        } finally {
            desconectar();
        }
    }

    /**
     * Envia un mensaje serializado en JSON al socket del cliente.
     */
    public void enviarMensaje(MensajeSocket mensaje) {
        try {
            if (salida != null) {
                String json = gson.toJson(mensaje);
                salida.println(json);
            }
        } catch (Exception e) {
            System.err.println("[-] Error al enviar mensaje a usuario " + userId + ": " + e.getMessage());
        }
    }

    private void procesarMensaje(MensajeSocket mensaje) {
        String tipo = mensaje.getType();

        switch (tipo) {
            case "LOGIN_REQUEST":
                ejecutarLogin(mensaje);
                break;

            case "CREATE_ROOM":
                ejecutarCrearSala(mensaje);
                break;

            case "JOIN_ROOM_REQUEST":
                ejecutarSolicitudUnirse(mensaje);
                break;

            case "ADMIT_USER":
                ejecutarAdmitirUsuario(mensaje);
                break;

            case "START_MEETING_REQUEST":
                ejecutarIniciarReunion(mensaje);
                break;

            case "CHAT_MESSAGE":
                ejecutarMensajeChat(mensaje);
                break;

            case "CAMERA_FRAME":
                ejecutarFrameCamara(mensaje);
                break;

            case "CAMERA_STATE":
                ejecutarEstadoCamara(mensaje);
                break;

            case "LEAVE_ROOM":
                ejecutarSalirSala(mensaje);
                break;

            case "FILE_START":
                ejecutarInicioArchivo(mensaje);
                break;

            case "FILE_CHUNK":
                ejecutarChunkArchivo(mensaje);
                break;

            case "FILE_END":
                ejecutarFinArchivo(mensaje);
                break;

            case "GET_FILES_REQUEST":
                ejecutarListarArchivos(mensaje);
                break;

            case "FILE_DOWNLOAD_REQUEST":
                ejecutarDescargarArchivo(mensaje);
                break;

            default:
                System.out.println("[!] Tipo de mensaje no soportado: " + tipo);
                break;
        }
    }

    private void ejecutarLogin(MensajeSocket mensaje) {
        String correo = mensaje.getUserName();
        String password = mensaje.getMessage(); // Contraseña enviada en el campo mensaje

        System.out.println("[*] Intento de login para: " + correo);
        Usuario usuario = MainServidor.database.login(correo, password);

        MensajeSocket respuesta = new MensajeSocket();
        respuesta.setType("LOGIN_RESPONSE");

        if (usuario != null) {
            this.userId = usuario.getIdUsuario();
            this.userName = usuario.getNombres();
            
            // Registrar cliente en el mapa global
            MainServidor.clientesActivos.put(this.userId, this);
            
            respuesta.setUserId(usuario.getIdUsuario());
            respuesta.setUserName(usuario.getNombres());
            respuesta.setMessage("SUCCESS");
            System.out.println("[OK] Login exitoso. Usuario ID: " + this.userId);
        } else {
            respuesta.setMessage("ERROR: Credenciales incorrectas o problemas de base de datos.");
            System.out.println("[-] Login fallido para: " + correo);
        }
        enviarMensaje(respuesta);
    }

    private void ejecutarCrearSala(MensajeSocket mensaje) {
        if (this.userId == null) return;

        String nombreSala = mensaje.getMessage() != null ? mensaje.getMessage() : "Sala de " + this.userName;
        // Generar un código único de sala de 6 dígitos
        String codigoSala = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        System.out.println("[*] Creando sala " + codigoSala + " solicitada por Host ID: " + this.userId);
        boolean creada = MainServidor.database.crearSala(codigoSala, nombreSala, this.userId);

        MensajeSocket respuesta = new MensajeSocket();
        respuesta.setType("CREATE_ROOM");

        if (creada) {
            this.roomCode = codigoSala;
            // Registrar al anfitrión directamente como participante
            MainServidor.database.agregarParticipante(codigoSala, this.userId);
            this.admittedToRoom = true;
            
            respuesta.setRoomCode(codigoSala);
            respuesta.setMessage("SUCCESS");
            System.out.println("[OK] Sala creada con éxito: " + codigoSala);
        } else {
            respuesta.setMessage("ERROR: No se pudo crear la sala en la base de datos.");
            System.out.println("[-] Error al crear sala para Host ID: " + this.userId);
        }
        enviarMensaje(respuesta);
    }

    private void ejecutarSolicitudUnirse(MensajeSocket mensaje) {
        if (this.userId == null) return;

        String codigo = mensaje.getRoomCode();
        System.out.println("[*] Usuario " + this.userName + " (ID: " + this.userId + ") solicita unirse a sala: " + codigo);

        boolean registrada = MainServidor.database.solicitarUnirseASala(codigo, this.userId);

        MensajeSocket respuesta = new MensajeSocket();
        respuesta.setType("JOIN_ROOM_RESPONSE"); // o WAITING_ROOM_UPDATE para mantener la coherencia
        respuesta.setRoomCode(codigo);

        if (registrada) {
            this.roomCode = codigo; // Queda asignada la sala del socket temporalmente
            this.admittedToRoom = false;
            respuesta.setMessage("PENDIENTE");
            System.out.println("[OK] Solicitud registrada como PENDIENTE para " + this.userName);
            
            // Notificar al Host de la sala para que actualice su UI de sala de espera
            notificarActualizacionSalaEspera(codigo);
        } else {
            respuesta.setMessage("ERROR: La sala no existe o no se encuentra activa.");
            System.out.println("[-] Solicitud de unión fallida para sala: " + codigo);
        }
        enviarMensaje(respuesta);
    }

    private void ejecutarAdmitirUsuario(MensajeSocket mensaje) {
        if (this.userId == null) return;

        String codigo = mensaje.getRoomCode();
        // El host envía el IdUsuario del invitado en el campo userId y la acción ("ACEPTAR" / "RECHAZAR") en message
        Integer invitadoId = mensaje.getUserId();
        String accion = mensaje.getMessage();

        if (invitadoId == null || accion == null) return;

        // Verificar que quien lo solicita sea efectivamente el Host de la sala
        int hostId = MainServidor.database.obtenerHostIdPorCodigo(codigo);
        if (hostId != this.userId) {
            System.err.println("[WARNING] Intento no autorizado de admisión en sala " + codigo + " por usuario ID " + this.userId);
            return;
        }

        String nuevoEstado = ("ACEPTADO".equalsIgnoreCase(accion) || "ACEPTAR".equalsIgnoreCase(accion)) ? "ACEPTADO" : "RECHAZADO";
        System.out.println("[*] Host (ID: " + this.userId + ") actualizó solicitud de Invitado ID: " + invitadoId + " a: " + nuevoEstado);
        
        boolean ok = MainServidor.database.actualizarEstadoSolicitud(codigo, invitadoId, nuevoEstado);

        if (ok) {
            // Notificar al Invitado sobre la decisión si está conectado
            ManejadorCliente invitadoManejador = MainServidor.clientesActivos.get(invitadoId);
            if (invitadoManejador != null) {
                MensajeSocket notifInvitado = new MensajeSocket();
                notifInvitado.setType("ADMIT_USER");
                notifInvitado.setRoomCode(codigo);
                notifInvitado.setMessage(nuevoEstado.equals("ACEPTADO") ? "ACCEPTED" : "REJECTED");
                invitadoManejador.enviarMensaje(notifInvitado);
                
                if (nuevoEstado.equals("ACEPTADO")) {
                    invitadoManejador.setRoomCode(codigo); // Se confirma su inclusión en la sala
                    invitadoManejador.setAdmittedToRoom(true);
                } else {
                    invitadoManejador.setRoomCode(null);  // Remueve sala temporal
                    invitadoManejador.setAdmittedToRoom(false);
                }
            }
            // Enviar la lista de espera actualizada al Host
            notificarActualizacionSalaEspera(codigo);
        }
    }

    private void ejecutarIniciarReunion(MensajeSocket mensaje) {
        if (this.userId == null) return;

        String codigo = mensaje.getRoomCode();
        int hostId = MainServidor.database.obtenerHostIdPorCodigo(codigo);
        if (hostId != this.userId) {
            System.err.println("[WARNING] Usuario ID " + this.userId + " intentó iniciar reunión sin ser host en sala " + codigo);
            return;
        }

        // Notificar al host para que entre a la reunión
        MensajeSocket notifHost = new MensajeSocket();
        notifHost.setType("MEETING_STARTED");
        notifHost.setRoomCode(codigo);
        notifHost.setMessage("STARTED");
        this.enviarMensaje(notifHost);

        List<Integer> participantes = MainServidor.database.obtenerParticipantesActivos(codigo);
        for (Integer participanteId : participantes) {
            ManejadorCliente participante = MainServidor.clientesActivos.get(participanteId);
            if (participante != null) {
                MensajeSocket notif = new MensajeSocket();
                notif.setType("MEETING_STARTED");
                notif.setRoomCode(codigo);
                notif.setMessage("STARTED");
                participante.enviarMensaje(notif);
            }
        }

        System.out.println("[OK] Reunión iniciada en sala " + codigo + " por host ID " + this.userId + ". Participantes notificados: " + participantes.size());
    }

    private void ejecutarMensajeChat(MensajeSocket mensaje) {
        if (this.userId == null || this.roomCode == null) return;

        // Si es una solicitud de historial, no la persistimos ni retransmitimos; enviamos los mensajes previos
        if ("REQUEST_HISTORY".equals(mensaje.getMessage())) {
            System.out.println("[CHAT] Solicitando historial de mensajes para sala: " + this.roomCode);
            List<MensajeSocket> historial = MainServidor.database.obtenerHistorialMensajes(this.roomCode);
            for (MensajeSocket msg : historial) {
                enviarMensaje(msg);
            }
            return;
        }

        System.out.println("[CHAT] Mensaje recibido de " + this.userName + " en sala " + this.roomCode + ": " + mensaje.getMessage());
        
        // Guardar mensaje en base de datos
        MainServidor.database.guardarMensaje(this.roomCode, this.userId, mensaje.getMessage());

        // Retransmitir mensaje a todos los miembros activos en la sala
        MainServidor.retransmitirMensaje(mensaje, null);
    }

    private void ejecutarFrameCamara(MensajeSocket mensaje) {
        if (this.userId == null || this.roomCode == null) return;

        // Retransmitir frame a todos en la sala menos al remitente
        MainServidor.retransmitirMensaje(mensaje, this.userId);
    }

    private void ejecutarEstadoCamara(MensajeSocket mensaje) {
        if (this.userId == null || this.roomCode == null) return;

        // Retransmitir estado de cámara a todos los miembros de la sala menos al remitente
        MainServidor.retransmitirMensaje(mensaje, this.userId);
    }

    private void ejecutarSalirSala(MensajeSocket mensaje) {
        if (this.userId == null || this.roomCode == null) return;

        System.out.println("[-] Usuario " + this.userName + " sale de la sala " + this.roomCode);
        
        // Notificar estado de cámara OFF a los demás antes de salir
        MensajeSocket camOffMsg = new MensajeSocket();
        camOffMsg.setType("CAMERA_STATE");
        camOffMsg.setRoomCode(this.roomCode);
        camOffMsg.setUserId(this.userId);
        camOffMsg.setUserName(this.userName);
        camOffMsg.setMessage("OFF");
        MainServidor.retransmitirMensaje(camOffMsg, this.userId);

        // Remover de la base de datos de participantes activos
        MainServidor.database.actualizarEstadoSolicitud(this.roomCode, this.userId, "RECHAZADO");
        
        String codigoSalida = this.roomCode;
        
        // Notificar a los demás que el usuario salió (USER_LEFT)
        MensajeSocket leftMsg = new MensajeSocket();
        leftMsg.setType("USER_LEFT");
        leftMsg.setRoomCode(codigoSalida);
        leftMsg.setUserId(this.userId);
        leftMsg.setUserName(this.userName);
        MainServidor.retransmitirMensaje(leftMsg, this.userId);
        
        this.roomCode = null;

        // Notificar a los demás que el usuario abandonó
        MensajeSocket alerta = new MensajeSocket("CHAT_MESSAGE", codigoSalida, 0, "SISTEMA", this.userName + " ha salido de la sala.", null);
        MainServidor.retransmitirMensaje(alerta, null);
    }

    private void ejecutarInicioArchivo(MensajeSocket mensaje) {
        if (this.userId == null || this.roomCode == null) return;

        try {
            // El host/invitado envía en el campo message los datos formateados del archivo: "fileId|nombreArchivo"
            String payload = mensaje.getMessage();
            if (payload == null || !payload.contains("|")) return;

            String[] partes = payload.split("\\|", 2);
            String fileId = partes[0];
            String nombreArchivo = partes[1];

            // Asegurar que exista la carpeta uploads
            File directorio = new File(DIRECTORIO_UPLOADS);
            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            // Crear archivo físico en servidor con un nombre único para evitar colisiones
            String rutaFisica = DIRECTORIO_UPLOADS + File.separator + fileId + "_" + nombreArchivo;
            FileOutputStream fos = new FileOutputStream(rutaFisica);

            archivosEnProgreso.put(fileId, fos);
            nombresArchivos.put(fileId, nombreArchivo);
            
            System.out.println("[*] Iniciando recepción de archivo: " + nombreArchivo + " (FileId: " + fileId + ")");

        } catch (Exception e) {
            System.err.println("[-] Error al iniciar archivo: " + e.getMessage());
        }
    }

    private void ejecutarChunkArchivo(MensajeSocket mensaje) {
        // payload: "fileId|chunkBase64"
        String payload = mensaje.getMessage();
        if (payload == null || !payload.contains("|")) return;

        String[] partes = payload.split("\\|", 2);
        String fileId = partes[0];
        String chunkBase64 = partes[1];

        FileOutputStream fos = archivosEnProgreso.get(fileId);
        if (fos != null) {
            try {
                byte[] bytes = Base64.getDecoder().decode(chunkBase64);
                fos.write(bytes);
            } catch (Exception e) {
                System.err.println("[-] Error al escribir chunk de archivo ID " + fileId + ": " + e.getMessage());
            }
        }
    }

    private void ejecutarFinArchivo(MensajeSocket mensaje) {
        String fileId = mensaje.getMessage();
        FileOutputStream fos = archivosEnProgreso.remove(fileId);
        String nombreArchivo = nombresArchivos.remove(fileId);

        if (fos != null && nombreArchivo != null) {
            try {
                fos.close();
                String rutaFisica = DIRECTORIO_UPLOADS + File.separator + fileId + "_" + nombreArchivo;
                System.out.println("[OK] Archivo recibido por completo: " + nombreArchivo);

                // Guardar registro de archivo en base de datos
                MainServidor.database.guardarArchivo(this.roomCode, this.userId, nombreArchivo, rutaFisica);

                // Notificar en el chat sobre la disponibilidad del nuevo archivo
                MensajeSocket alertaChat = new MensajeSocket();
                alertaChat.setType("CHAT_MESSAGE");
                alertaChat.setRoomCode(this.roomCode);
                alertaChat.setUserId(0); // Sistema
                alertaChat.setUserName("SISTEMA");
                alertaChat.setMessage("El usuario '" + this.userName + "' compartió el archivo: " + nombreArchivo);
                MainServidor.retransmitirMensaje(alertaChat, null);

            } catch (Exception e) {
                System.err.println("[-] Error al cerrar archivo: " + e.getMessage());
            }
        }
    }

    private void notificarActualizacionSalaEspera(String codigoSala) {
        int hostId = MainServidor.database.obtenerHostIdPorCodigo(codigoSala);
        ManejadorCliente hostManejador = MainServidor.clientesActivos.get(hostId);
        
        if (hostManejador != null) {
            List<Map<String, Object>> solicitudes = MainServidor.database.obtenerSolicitudesPendientes(codigoSala);
            String jsonSolicitudes = gson.toJson(solicitudes);
            
            MensajeSocket notifHost = new MensajeSocket();
            notifHost.setType("WAITING_ROOM_UPDATE");
            notifHost.setRoomCode(codigoSala);
            notifHost.setMessage(jsonSolicitudes);
            hostManejador.enviarMensaje(notifHost);
            System.out.println("[->] Enviada actualización de sala de espera al Host (ID: " + hostId + ")");
        }
    }

    private void desconectar() {
        try {
            if (this.userId != null) {
                MainServidor.clientesActivos.remove(this.userId);
                System.out.println("[-] Usuario " + this.userName + " (ID: " + this.userId + ") desconectado del registro.");
                
                // Si estaba en una sala, notificar
                if (this.roomCode != null) {
                    // Notificar estado de cámara OFF a los demás por desconexión abrupta
                    MensajeSocket camOffMsg = new MensajeSocket();
                    camOffMsg.setType("CAMERA_STATE");
                    camOffMsg.setRoomCode(this.roomCode);
                    camOffMsg.setUserId(this.userId);
                    camOffMsg.setUserName(this.userName);
                    camOffMsg.setMessage("OFF");
                    MainServidor.retransmitirMensaje(camOffMsg, this.userId);

                    MainServidor.database.actualizarEstadoSolicitud(this.roomCode, this.userId, "RECHAZADO");

                    // Notificar a los demás que el usuario salió (USER_LEFT)
                    MensajeSocket leftMsg = new MensajeSocket();
                    leftMsg.setType("USER_LEFT");
                    leftMsg.setRoomCode(this.roomCode);
                    leftMsg.setUserId(this.userId);
                    leftMsg.setUserName(this.userName);
                    MainServidor.retransmitirMensaje(leftMsg, this.userId);

                    MensajeSocket alerta = new MensajeSocket("CHAT_MESSAGE", this.roomCode, 0, "SISTEMA", this.userName + " se ha desconectado.", null);
                    MainServidor.retransmitirMensaje(alerta, null);
                }
            }
            if (socketCliente != null && !socketCliente.isClosed()) {
                socketCliente.close();
            }
        } catch (Exception e) {
            System.err.println("[-] Error al cerrar recursos del socket: " + e.getMessage());
        }
    }

    private void ejecutarListarArchivos(MensajeSocket mensaje) {
        if (this.userId == null || this.roomCode == null) return;

        List<Map<String, Object>> archivos = MainServidor.database.obtenerArchivosCompartidos(this.roomCode);
        String jsonArchivos = gson.toJson(archivos);

        MensajeSocket respuesta = new MensajeSocket();
        respuesta.setType("GET_FILES_RESPONSE");
        respuesta.setRoomCode(this.roomCode);
        respuesta.setMessage(jsonArchivos);
        enviarMensaje(respuesta);
        System.out.println("[->] Enviada lista de archivos al usuario " + this.userName);
    }

    private void ejecutarDescargarArchivo(MensajeSocket mensaje) {
        if (this.userId == null || this.roomCode == null) return;

        String rutaFisica = mensaje.getMessage(); // La ruta del archivo en el servidor
        if (rutaFisica == null || rutaFisica.isEmpty()) return;

        // --- SEGURIDAD: Evitar Directory Traversal ---
        File file = new File(rutaFisica);
        try {
            String pathAbsoluto = file.getCanonicalPath();
            File dirUploads = new File(DIRECTORIO_UPLOADS);
            String uploadsAbsoluto = dirUploads.getCanonicalPath();

            if (!pathAbsoluto.startsWith(uploadsAbsoluto)) {
                System.err.println("[SECURITY WARNING] Intento de Directory Traversal por usuario ID " + this.userId + " con ruta: " + rutaFisica);
                return;
            }
        } catch (Exception e) {
            System.err.println("[-] Error de validación de seguridad: " + e.getMessage());
            return;
        }

        if (!file.exists() || !file.isFile()) {
            System.err.println("[-] El archivo solicitado no existe o no es válido: " + rutaFisica);
            return;
        }

        // Obtener el nombre original quitando el prefijo "fileId_"
        String nombreArchivo = file.getName();
        if (nombreArchivo.contains("_")) {
            nombreArchivo = nombreArchivo.substring(nombreArchivo.indexOf("_") + 1);
        }

        System.out.println("[*] Iniciando transmisión de descarga para: " + nombreArchivo + " (" + rutaFisica + ")");

        // Iniciar un hilo secundario para enviar el archivo en chunks y no bloquear el ManejadorCliente
        String finalNombreArchivo = nombreArchivo;
        new Thread(() -> {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                // 1. Enviar FILE_START al cliente
                MensajeSocket startMsg = new MensajeSocket();
                startMsg.setType("FILE_START");
                startMsg.setRoomCode(this.roomCode);
                startMsg.setMessage(rutaFisica + "|" + finalNombreArchivo);
                enviarMensaje(startMsg);

                byte[] buffer = new byte[64 * 1024]; // 64 KB chunks
                int bytesLeidos;

                // 2. Enviar FILE_CHUNKs
                while ((bytesLeidos = fis.read(buffer)) != -1) {
                    byte[] tempBuf = bytesLeidos == buffer.length ? buffer : java.util.Arrays.copyOf(buffer, bytesLeidos);
                    String chunkBase64 = Base64.getEncoder().encodeToString(tempBuf);

                    MensajeSocket chunkMsg = new MensajeSocket();
                    chunkMsg.setType("FILE_CHUNK");
                    chunkMsg.setRoomCode(this.roomCode);
                    chunkMsg.setMessage(rutaFisica + "|" + chunkBase64);
                    enviarMensaje(chunkMsg);
                    
                    // Un pequeño retardo para evitar saturar el buffer TCP
                    Thread.sleep(10);
                }

                // 3. Enviar FILE_END
                MensajeSocket endMsg = new MensajeSocket();
                endMsg.setType("FILE_END");
                endMsg.setRoomCode(this.roomCode);
                endMsg.setMessage(rutaFisica);
                enviarMensaje(endMsg);
                
                System.out.println("[OK] Transmisión de descarga completada para: " + finalNombreArchivo);

            } catch (Exception e) {
                System.err.println("[-] Error durante transmisión de descarga: " + e.getMessage());
            }
        }).start();
    }

    // --- GETTERS Y SETTERS ---
    public Integer getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public boolean isAdmittedToRoom() { return admittedToRoom; }
    public void setAdmittedToRoom(boolean admittedToRoom) { this.admittedToRoom = admittedToRoom; }
}