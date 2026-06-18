package poo.cliente;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import model.MensajeSocket;

public class TestHeadlessClient {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java TestHeadlessClient [host|guest] [roomCode]");
            return;
        }

        String role = args[0];
        String targetRoomCode = args.length > 1 ? args[1] : null;

        String serverIp = "localhost";
        int serverPort = 5000;

        try {
            System.out.println("[*] " + role.toUpperCase() + ": Conectando a " + serverIp + ":" + serverPort + "...");
            Socket socket = new Socket(serverIp, serverPort);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Gson gson = new Gson();

            // Iniciar hilo para recibir mensajes del servidor
            new Thread(() -> {
                try {
                    String json;
                    while ((json = in.readLine()) != null) {
                        MensajeSocket msg = gson.fromJson(json, MensajeSocket.class);
                        System.out.println("\n[RECIBIDO] Tipo: " + msg.getType() + " | Msg: " + msg.getMessage() + " | Code: " + msg.getRoomCode() + " | User: " + msg.getUserName());
                        
                        if ("LOGIN_RESPONSE".equals(msg.getType())) {
                            if ("SUCCESS".equals(msg.getMessage())) {
                                System.out.println("[+] Login Exitoso para: " + msg.getUserName() + " (ID: " + msg.getUserId() + ")");
                                if ("host".equals(role)) {
                                    // Crear sala
                                    MensajeSocket createMsg = new MensajeSocket();
                                    createMsg.setType("CREATE_ROOM");
                                    createMsg.setUserId(msg.getUserId());
                                    createMsg.setUserName(msg.getUserName());
                                    createMsg.setMessage("Sala de pruebas headless");
                                    out.println(gson.toJson(createMsg));
                                } else {
                                    // Unirse a sala
                                    MensajeSocket joinMsg = new MensajeSocket();
                                    joinMsg.setType("JOIN_ROOM_REQUEST");
                                    joinMsg.setRoomCode(targetRoomCode);
                                    joinMsg.setUserId(msg.getUserId());
                                    joinMsg.setUserName(msg.getUserName());
                                    out.println(gson.toJson(joinMsg));
                                }
                            } else {
                                System.err.println("[-] Error de Login: " + msg.getMessage());
                            }
                        } else if ("CREATE_ROOM".equals(msg.getType())) {
                            System.out.println("[+] Sala creada con código: " + msg.getRoomCode());
                        } else if ("WAITING_ROOM_UPDATE".equals(msg.getType())) {
                            System.out.println("[+] Sala de espera actualizada: " + msg.getMessage());
                            if ("host".equals(role)) {
                                // Admitir al primer usuario después de 2 segundos para simular tiempo de reacción
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(2000);
                                        // Enviar mensaje de admisión para el usuario 2 (Invitado)
                                        MensajeSocket admitMsg = new MensajeSocket();
                                        admitMsg.setType("ADMIT_USER");
                                        admitMsg.setRoomCode(msg.getRoomCode());
                                        admitMsg.setUserId(2); // Invitado De Prueba tiene ID 2 típicamente
                                        admitMsg.setUserName("Invitado De Prueba");
                                        admitMsg.setMessage("ACEPTAR");
                                        out.println(gson.toJson(admitMsg));
                                        System.out.println("[->] Enviada admisión para Invitado De Prueba (ID 2)");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }).start();
                            }
                        } else if ("ADMIT_USER".equals(msg.getType())) {
                            System.out.println("[+] Admitido en la sala: " + msg.getMessage());
                            if ("ACCEPTED".equals(msg.getMessage())) {
                                // Enviar chat
                                MensajeSocket chatMsg = new MensajeSocket();
                                chatMsg.setType("CHAT_MESSAGE");
                                chatMsg.setRoomCode(msg.getRoomCode());
                                chatMsg.setUserId(2);
                                chatMsg.setUserName("Invitado Headless");
                                chatMsg.setMessage("Hola! Acabo de entrar a la videoconferencia headless.");
                                out.println(gson.toJson(chatMsg));
                            }
                        } else if ("CHAT_MESSAGE".equals(msg.getType())) {
                            System.out.println("[CHAT] " + msg.getUserName() + ": " + msg.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[-] Error en lectura de socket: " + e.getMessage());
                }
            }).start();

            // Realizar login
            MensajeSocket loginMsg = new MensajeSocket();
            loginMsg.setType("LOGIN_REQUEST");
            if ("host".equals(role)) {
                loginMsg.setUserName("host@zoom.com");
            } else {
                loginMsg.setUserName("invitado@zoom.com");
            }
            loginMsg.setMessage("123456");
            
            out.println(gson.toJson(loginMsg));

            // Mantenerse vivo
            Thread.sleep(15000);
            
            // Salir de la sala
            MensajeSocket leaveMsg = new MensajeSocket();
            leaveMsg.setType("LEAVE_ROOM");
            leaveMsg.setRoomCode(targetRoomCode);
            if ("host".equals(role)) {
                leaveMsg.setUserId(1);
                leaveMsg.setUserName("Host De Prueba");
            } else {
                leaveMsg.setUserId(2);
                leaveMsg.setUserName("Invitado De Prueba");
            }
            out.println(gson.toJson(leaveMsg));
            Thread.sleep(1000);
            socket.close();
            System.out.println("[*] Test terminado.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
