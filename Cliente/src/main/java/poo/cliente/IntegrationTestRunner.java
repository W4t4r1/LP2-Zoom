package poo.cliente;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import model.MensajeSocket;

public class IntegrationTestRunner {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO PRUEBA DE INTEGRACIÓN AUTOMATIZADA ===");
        
        CountDownLatch roomCreatedLatch = new CountDownLatch(1);
        CountDownLatch guestJoinedLatch = new CountDownLatch(1);
        CountDownLatch chatReceivedLatch = new CountDownLatch(2); // Host and Guest should receive the chat msg
        
        AtomicReference<String> roomCodeRef = new AtomicReference<>();
        
        String serverIp = "localhost";
        int serverPort = 5000;
        Gson gson = new Gson();

        // 1. Iniciar HOST
        Thread hostThread = new Thread(() -> {
            try (Socket socket = new Socket(serverIp, serverPort);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                 
                System.out.println("[HOST] Conectado. Enviando login...");
                MensajeSocket login = new MensajeSocket();
                login.setType("LOGIN_REQUEST");
                login.setUserName("host@zoom.com");
                login.setMessage("123456");
                out.println(gson.toJson(login));

                String json;
                while ((json = in.readLine()) != null) {
                    MensajeSocket msg = gson.fromJson(json, MensajeSocket.class);
                    System.out.println("[HOST RECIBE] Tipo: " + msg.getType() + " | Msg: " + msg.getMessage());
                    
                    if ("LOGIN_RESPONSE".equals(msg.getType()) && "SUCCESS".equals(msg.getMessage())) {
                        System.out.println("[HOST] Login exitoso. Creando sala...");
                        MensajeSocket create = new MensajeSocket();
                        create.setType("CREATE_ROOM");
                        create.setUserId(msg.getUserId());
                        create.setUserName(msg.getUserName());
                        create.setMessage("Sala de Integración");
                        out.println(gson.toJson(create));
                    }
                    else if ("CREATE_ROOM".equals(msg.getType()) && "SUCCESS".equals(msg.getMessage())) {
                        String code = msg.getRoomCode();
                        System.out.println("[HOST] Sala creada con código: " + code);
                        roomCodeRef.set(code);
                        roomCreatedLatch.countDown();
                    }
                    else if ("WAITING_ROOM_UPDATE".equals(msg.getType())) {
                        System.out.println("[HOST] Solicitud de unión recibida en sala de espera. Admitiendo al invitado...");
                        // Admitir al invitado (ID 2)
                        MensajeSocket admit = new MensajeSocket();
                        admit.setType("ADMIT_USER");
                        admit.setRoomCode(roomCodeRef.get());
                        admit.setUserId(2); // Invitado De Prueba
                        admit.setUserName("Invitado De Prueba");
                        admit.setMessage("ACEPTAR");
                        out.println(gson.toJson(admit));
                    }
                    else if ("CHAT_MESSAGE".equals(msg.getType())) {
                        System.out.println("[HOST CHAT] " + msg.getUserName() + ": " + msg.getMessage());
                        chatReceivedLatch.countDown();
                    }
                }
            } catch (Exception e) {
                System.err.println("[HOST ERROR] " + e.getMessage());
            }
        });
        hostThread.start();

        // Esperar a que la sala sea creada
        try {
            if (!roomCreatedLatch.await(30, TimeUnit.SECONDS)) {
                System.err.println("[FALLO] Excedido el tiempo de espera para crear la sala.");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }

        String roomCode = roomCodeRef.get();
        System.out.println("[RUNNER] Código de sala obtenido: " + roomCode + ". Iniciando GUEST...");

        // 2. Iniciar GUEST
        Thread guestThread = new Thread(() -> {
            try (Socket socket = new Socket(serverIp, serverPort);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                 
                System.out.println("[GUEST] Conectado. Enviando login...");
                MensajeSocket login = new MensajeSocket();
                login.setType("LOGIN_REQUEST");
                login.setUserName("invitado@zoom.com");
                login.setMessage("123456");
                out.println(gson.toJson(login));

                String json;
                while ((json = in.readLine()) != null) {
                    MensajeSocket msg = gson.fromJson(json, MensajeSocket.class);
                    System.out.println("[GUEST RECIBE] Tipo: " + msg.getType() + " | Msg: " + msg.getMessage());
                    
                    if ("LOGIN_RESPONSE".equals(msg.getType()) && "SUCCESS".equals(msg.getMessage())) {
                        System.out.println("[GUEST] Login exitoso. Solicitando unirse a sala: " + roomCode);
                        MensajeSocket join = new MensajeSocket();
                        join.setType("JOIN_ROOM_REQUEST");
                        join.setRoomCode(roomCode);
                        join.setUserId(msg.getUserId());
                        join.setUserName(msg.getUserName());
                        out.println(gson.toJson(join));
                    }
                    else if ("JOIN_ROOM_RESPONSE".equals(msg.getType()) && "PENDIENTE".equals(msg.getMessage())) {
                        System.out.println("[GUEST] Solicitud registrada como PENDIENTE. Esperando aprobación...");
                    }
                    else if ("ADMIT_USER".equals(msg.getType()) && "ACCEPTED".equals(msg.getMessage())) {
                        System.out.println("[GUEST] ¡Admitido en la sala! Enviando mensaje de chat...");
                        guestJoinedLatch.countDown();
                        
                        MensajeSocket chat = new MensajeSocket();
                        chat.setType("CHAT_MESSAGE");
                        chat.setRoomCode(roomCode);
                        chat.setUserId(2);
                        chat.setUserName("Invitado De Prueba");
                        chat.setMessage("Hola a todos, esta es una prueba de integración!");
                        out.println(gson.toJson(chat));
                    }
                    else if ("CHAT_MESSAGE".equals(msg.getType())) {
                        System.out.println("[GUEST CHAT] " + msg.getUserName() + ": " + msg.getMessage());
                        chatReceivedLatch.countDown();
                    }
                }
            } catch (Exception e) {
                System.err.println("[GUEST ERROR] " + e.getMessage());
            }
        });
        guestThread.start();

        // Esperar a que todos los eventos se cumplan
        try {
            boolean joined = guestJoinedLatch.await(30, TimeUnit.SECONDS);
            boolean chatReceived = chatReceivedLatch.await(30, TimeUnit.SECONDS);
            
            if (joined && chatReceived) {
                System.out.println("\n[🎉 PRUEBA COMPLETADA CON ÉXITO]");
                System.out.println("1. Conexión y login de Host y Guest verificados.");
                System.out.println("2. Creación de sala por el Host verificado.");
                System.out.println("3. Solicitud de ingreso de Guest verificado.");
                System.out.println("4. Sala de espera y admisión verificado.");
                System.out.println("5. Chat en tiempo real bidireccional verificado.");
                System.exit(0);
            } else {
                System.err.println("\n[❌ FALLO EN LA PRUEBA]");
                System.err.println("Unión exitosa: " + joined);
                System.err.println("Chat recibido por ambos: " + chatReceived);
                System.exit(1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
