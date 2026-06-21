package UI;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import model.MensajeSocket;
import network.ClienteConexion;
import network.CameraCapture;
import network.CameraSimulator;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.concurrent.ConcurrentHashMap;

public class RoomFrame extends JFrame implements ClienteConexion.MensajeListener {

    private int userId;
    private String userName;
    private String roomCode;

    // Componentes de CardLayout
    private CardLayout cardLayout;
    private JPanel mainContainer;

    // --- CARD 1: Selector de Rol ---
    private JTextField txtCodigoSala;
    private JButton btnCrearSala;
    private JButton btnUnirse;

    // --- CARD 2: Panel Host (Sala de Espera del Anfitrión) ---
    private JLabel lblCodigoSalaHost;
    private JPanel pnlListaEspera;
    private JButton btnIniciarReunion;

    // --- CARD 3: Panel Invitado (Pantalla de Espera) ---
    private JLabel lblMensajeEspera;
    private JButton btnCancelarSolicitud;

    // --- CARD 4: Panel de Reunión Activa (Meeting View) ---
    private JLabel lblTituloReunion;
    private JTextArea txtAreaChat;
    private JTextField txtMensajeChat;
    private JButton btnEnviarChat;
    private JPanel pnlVideoGrid;
    private JPanel pnlVideoContainer;
    private JButton btnToggleCamera;
    private boolean camaraActiva = true;
    private java.util.Map<Integer, JLabel> videoFeeds = new ConcurrentHashMap<>();
    private java.util.Map<Integer, JPanel> videoFeedPanels = new ConcurrentHashMap<>();
    private CameraCapture cameraCapture;
    private CameraSimulator cameraSimulator;
    private boolean usingCameraCapture = false;

    private Gson gson;
    private java.util.Map<String, java.io.FileOutputStream> descargasEnProgreso = new java.util.concurrent.ConcurrentHashMap<>();

    public RoomFrame(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.gson = new Gson();

        setTitle("Zoom Sockets - " + userName);
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Patrón Observer: Registrar oyente de red
        ClienteConexion.getInstancia().addListener(this);

        // Desconectarse al cerrar la ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salirDeSalaActual();
                ClienteConexion.getInstancia().desconectar();
            }
        });

        // Contenedor principal con CardLayout
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // Inicializar las 4 pantallas (Cards)
        inicializarPantallaSelector();
        inicializarPantallaHost();
        inicializarPantallaInvitado();
        inicializarPantallaReunion();

        add(mainContainer);

        // Mostrar por defecto la selección de rol
        cardLayout.show(mainContainer, "SELECTOR");
    }

    // =========================================================================
    // 1. CARD "SELECTOR": Selección de Rol / Entrada
    // =========================================================================
    private void inicializarPantallaSelector() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(new Color(245, 247, 250)); // Fondo gris claro moderno
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Título principal
        JLabel lblTitulo = new JLabel("Zoom Sockets", JLabel.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblTitulo.setForeground(new Color(41, 128, 185));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(lblTitulo, gbc);

        JLabel lblSub = new JLabel("Bienvenido " + userName + ". ¿Qué desea hacer hoy?", JLabel.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSub.setForeground(Color.GRAY);
        gbc.gridy = 1;
        panel.add(lblSub, gbc);

        // Separador visual
        gbc.gridy = 2;
        panel.add(Box.createRigidArea(new Dimension(0, 20)), gbc);

        // --- SECCIÓN HOST (Izquierda) ---
        JPanel pnlHost = new JPanel(new GridLayout(2, 1, 10, 10));
        pnlHost.setBackground(Color.WHITE);
        pnlHost.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 224, 230), 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel lblHostInfo = new JLabel("Iniciar como Anfitrión", JLabel.CENTER);
        lblHostInfo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        pnlHost.add(lblHostInfo);

        btnCrearSala = new JButton("Crear una Nueva Sala");
        btnCrearSala.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCrearSala.setBackground(new Color(41, 128, 185));
        btnCrearSala.setForeground(Color.WHITE);
        btnCrearSala.setFocusPainted(false);
        btnCrearSala.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCrearSala.addActionListener(e -> crearNuevaSala());
        pnlHost.add(btnCrearSala);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        panel.add(pnlHost, gbc);

        // --- SECCIÓN INVITADO (Derecha) ---
        JPanel pnlInvitado = new JPanel(new GridBagLayout());
        pnlInvitado.setBackground(Color.WHITE);
        pnlInvitado.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 224, 230), 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        GridBagConstraints gbcInv = new GridBagConstraints();
        gbcInv.insets = new Insets(5, 5, 5, 5);
        gbcInv.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblInvInfo = new JLabel("Unirse a una Sala Existente", JLabel.CENTER);
        lblInvInfo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbcInv.gridx = 0;
        gbcInv.gridy = 0;
        gbcInv.gridwidth = 2;
        pnlInvitado.add(lblInvInfo, gbcInv);

        txtCodigoSala = new JTextField();
        txtCodigoSala.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtCodigoSala.setHorizontalAlignment(JTextField.CENTER);
        txtCodigoSala.setToolTipText("Escribe el código de 6 caracteres");
        gbcInv.gridy = 1;
        pnlInvitado.add(txtCodigoSala, gbcInv);

        btnUnirse = new JButton("Solicitar Unirse");
        btnUnirse.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnUnirse.setBackground(new Color(39, 174, 96));
        btnUnirse.setForeground(Color.WHITE);
        btnUnirse.setFocusPainted(false);
        btnUnirse.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnUnirse.addActionListener(e -> solicitarUnirse());
        gbcInv.gridy = 2;
        pnlInvitado.add(btnUnirse, gbcInv);

        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(pnlInvitado, gbc);

        mainContainer.add(panel, "SELECTOR");
    }

    // =========================================================================
    // 2. CARD "HOST": Sala de Espera del Host
    // =========================================================================
    private void inicializarPantallaHost() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Header
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(Color.WHITE);

        JLabel lblInfo = new JLabel("Eres el Anfitrión de la sala");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblInfo.setForeground(Color.GRAY);
        pnlHeader.add(lblInfo, BorderLayout.NORTH);

        lblCodigoSalaHost = new JLabel("Código de Sala: ------");
        lblCodigoSalaHost.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblCodigoSalaHost.setForeground(new Color(41, 128, 185));
        pnlHeader.add(lblCodigoSalaHost, BorderLayout.CENTER);

        panel.add(pnlHeader, BorderLayout.NORTH);

        // Lista de Espera Central
        JPanel pnlCuerpo = new JPanel(new BorderLayout(5, 5));
        pnlCuerpo.setBackground(Color.WHITE);
        
        JLabel lblEsperaTitulo = new JLabel("Invitados en la Sala de Espera:");
        lblEsperaTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pnlCuerpo.add(lblEsperaTitulo, BorderLayout.NORTH);

        pnlListaEspera = new JPanel();
        pnlListaEspera.setLayout(new BoxLayout(pnlListaEspera, BoxLayout.Y_AXIS));
        pnlListaEspera.setBackground(new Color(245, 247, 250));

        JScrollPane scroll = new JScrollPane(pnlListaEspera);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 235, 240)));
        pnlCuerpo.add(scroll, BorderLayout.CENTER);

        panel.add(pnlCuerpo, BorderLayout.CENTER);

        // Botón Footer
        btnIniciarReunion = new JButton("Iniciar la Videoconferencia");
        btnIniciarReunion.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnIniciarReunion.setBackground(new Color(41, 128, 185));
        btnIniciarReunion.setForeground(Color.WHITE);
        btnIniciarReunion.setFocusPainted(false);
        btnIniciarReunion.setPreferredSize(new Dimension(0, 50));
        btnIniciarReunion.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnIniciarReunion.addActionListener(e -> iniciarReunionHost());
        panel.add(btnIniciarReunion, BorderLayout.SOUTH);

        mainContainer.add(panel, "HOST");
    }

    // =========================================================================
    // 3. CARD "INVITADO": Pantalla de Espera
    // =========================================================================
    private void inicializarPantallaInvitado() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblLogo = new JLabel("Sala de Espera", JLabel.CENTER);
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblLogo.setForeground(new Color(230, 126, 34)); // Color Naranja Alerta
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(lblLogo, gbc);

        lblMensajeEspera = new JLabel("Esperando aprobación del anfitrión...", JLabel.CENTER);
        lblMensajeEspera.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        gbc.gridy = 1;
        panel.add(lblMensajeEspera, gbc);

        // Barra de progreso de carga indeterminada
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setForeground(new Color(230, 126, 34));
        gbc.gridy = 2;
        panel.add(progress, gbc);

        btnCancelarSolicitud = new JButton("Cancelar Solicitud");
        btnCancelarSolicitud.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancelarSolicitud.setBackground(new Color(192, 57, 43));
        btnCancelarSolicitud.setForeground(Color.WHITE);
        btnCancelarSolicitud.setFocusPainted(false);
        btnCancelarSolicitud.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelarSolicitud.addActionListener(e -> cancelarSolicitud());
        gbc.gridy = 3;
        panel.add(btnCancelarSolicitud, gbc);

        mainContainer.add(panel, "INVITADO");
    }

    // =========================================================================
    // 4. CARD "REUNION": Pantalla de Reunión Activa (Chat y Video Grid)
    // =========================================================================
    private void inicializarPantallaReunion() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(30, 30, 35)); // Fondo Oscuro Premium (Estilo Zoom)
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header Superior
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(new Color(40, 40, 45));
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(55, 55, 60), 1, true),
                new EmptyBorder(10, 15, 10, 15)
        ));

        lblTituloReunion = new JLabel("Sala de Reunión Activa: Código ------");
        lblTituloReunion.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTituloReunion.setForeground(Color.WHITE);
        pnlHeader.add(lblTituloReunion, BorderLayout.WEST);

        JPanel pnlHeaderButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlHeaderButtons.setOpaque(false);

        JButton btnArchivos = new JButton("📂 Archivos");
        btnArchivos.setBackground(new Color(41, 128, 185));
        btnArchivos.setForeground(Color.WHITE);
        btnArchivos.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnArchivos.setFocusPainted(false);
        btnArchivos.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnArchivos.addActionListener(e -> solicitarListaArchivos());
        pnlHeaderButtons.add(btnArchivos);

        btnToggleCamera = new JButton("Cámara: ON");
        btnToggleCamera.setBackground(new Color(46, 204, 113));
        btnToggleCamera.setForeground(Color.WHITE);
        btnToggleCamera.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnToggleCamera.setFocusPainted(false);
        btnToggleCamera.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggleCamera.addActionListener(e -> toggleCamera());
        pnlHeaderButtons.add(btnToggleCamera);

        JButton btnSalir = new JButton("Abandonar Sala");
        btnSalir.setBackground(new Color(192, 57, 43));
        btnSalir.setForeground(Color.WHITE);
        btnSalir.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSalir.setFocusPainted(false);
        btnSalir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSalir.addActionListener(e -> abandonarReunion());
        pnlHeaderButtons.add(btnSalir);

        pnlHeader.add(pnlHeaderButtons, BorderLayout.EAST);

        panel.add(pnlHeader, BorderLayout.NORTH);

        // Panel Central Dividido
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(new Color(30, 30, 35));
        splitPane.setBorder(null);
        splitPane.setDividerLocation(480);
        splitPane.setResizeWeight(0.6);

// Lado Izquierdo: Video Grid
         pnlVideoGrid = new JPanel(new BorderLayout());
         pnlVideoGrid.setBackground(new Color(20, 20, 25));
         pnlVideoGrid.setBorder(new LineBorder(new Color(45, 45, 50), 1, true));
 
         pnlVideoContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
         pnlVideoContainer.setOpaque(false);
         pnlVideoGrid.add(pnlVideoContainer, BorderLayout.CENTER);

        splitPane.setLeftComponent(pnlVideoGrid);

        // Lado Derecho: Panel de Chat (Fase 4)
        JPanel pnlChat = new JPanel(new BorderLayout(5, 5));
        pnlChat.setBackground(new Color(40, 40, 45));
        pnlChat.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(45, 45, 50), 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        txtAreaChat = new JTextArea();
        txtAreaChat.setEditable(false);
        txtAreaChat.setBackground(new Color(50, 50, 55));
        txtAreaChat.setForeground(Color.WHITE);
        txtAreaChat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtAreaChat.setLineWrap(true);
        txtAreaChat.setWrapStyleWord(true);
        
        JScrollPane scrollChat = new JScrollPane(txtAreaChat);
        scrollChat.setBorder(null);
        pnlChat.add(scrollChat, BorderLayout.CENTER);

        // Footer del Chat (Input)
        JPanel pnlChatInput = new JPanel(new BorderLayout(5, 0));
        pnlChatInput.setBackground(new Color(40, 40, 45));

        txtMensajeChat = new JTextField();
        txtMensajeChat.setBackground(new Color(30, 30, 35));
        txtMensajeChat.setForeground(Color.WHITE);
        txtMensajeChat.setCaretColor(Color.WHITE);
        txtMensajeChat.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtMensajeChat.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(55, 55, 60), 1, true),
                new EmptyBorder(5, 8, 5, 8)
        ));
        txtMensajeChat.addActionListener(e -> enviarMensajeChat());
        pnlChatInput.add(txtMensajeChat, BorderLayout.CENTER);

        JButton btnAdjuntar = new JButton("📎");
        btnAdjuntar.setBackground(new Color(127, 140, 141));
        btnAdjuntar.setForeground(Color.WHITE);
        btnAdjuntar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAdjuntar.setFocusPainted(false);
        btnAdjuntar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdjuntar.addActionListener(e -> seleccionarYSubirArchivo());
        pnlChatInput.add(btnAdjuntar, BorderLayout.WEST);

        btnEnviarChat = new JButton("Enviar");
        btnEnviarChat.setBackground(new Color(41, 128, 185));
        btnEnviarChat.setForeground(Color.WHITE);
        btnEnviarChat.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnEnviarChat.setFocusPainted(false);
        btnEnviarChat.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEnviarChat.addActionListener(e -> enviarMensajeChat());
        pnlChatInput.add(btnEnviarChat, BorderLayout.EAST);

        pnlChat.add(pnlChatInput, BorderLayout.SOUTH);

        splitPane.setRightComponent(pnlChat);

        panel.add(splitPane, BorderLayout.CENTER);

        mainContainer.add(panel, "REUNION");
    }

    // =========================================================================
    // ACCIONES POR RED Y BOTONES
    // =========================================================================

    private void crearNuevaSala() {
        MensajeSocket msg = new MensajeSocket();
        msg.setType("CREATE_ROOM");
        msg.setUserId(userId);
        msg.setUserName(userName);
        msg.setMessage("Sala de " + userName);

        ClienteConexion.getInstancia().enviarMensaje(msg);
        System.out.println("[->] Enviada solicitud para crear sala...");
    }

    private void solicitarUnirse() {
        String codigo = txtCodigoSala.getText().trim().toUpperCase();
        if (codigo.length() != 6) {
            JOptionPane.showMessageDialog(this, "El código de sala debe tener exactamente 6 caracteres.", "Código Inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        MensajeSocket msg = new MensajeSocket();
        msg.setType("JOIN_ROOM_REQUEST");
        msg.setRoomCode(codigo);
        msg.setUserId(userId);
        msg.setUserName(userName);

        ClienteConexion.getInstancia().enviarMensaje(msg);
        System.out.println("[->] Enviada solicitud para unirse a sala: " + codigo);
    }

    private void responderSolicitud(int invitadoId, String nombresInvitado, String accion) {
        MensajeSocket msg = new MensajeSocket();
        msg.setType("ADMIT_USER");
        msg.setRoomCode(roomCode);
        msg.setUserId(invitadoId); // IdUsuario del invitado
        msg.setUserName(nombresInvitado);
        msg.setMessage(accion); // "ACEPTAR" o "RECHAZAR"

        ClienteConexion.getInstancia().enviarMensaje(msg);
        System.out.println("[->] Enviada respuesta al Invitado " + nombresInvitado + ": " + accion);
    }

    private void iniciarReunionHost() {
        // En este prototipo, el Host puede iniciar formalmente la reunión, llevando a todos los admitidos a la pantalla.
        entrarAReunion();
        
        // Notificar en el chat del Host
        txtAreaChat.append("[SISTEMA] Has iniciado la reunión. ¡Bienvenidos!\n");
    }

    private void entrarAReunion() {
        lblTituloReunion.setText("Sala de Reunión Activa: Código " + roomCode);
        txtAreaChat.setText(""); // Limpiar chat previo para evitar duplicados
        cardLayout.show(mainContainer, "REUNION");

        // Enviar solicitud de historial de chat al servidor
        MensajeSocket requestMsg = new MensajeSocket();
        requestMsg.setType("CHAT_MESSAGE");
        requestMsg.setRoomCode(roomCode);
        requestMsg.setUserId(userId);
        requestMsg.setUserName(userName);
        requestMsg.setMessage("REQUEST_HISTORY");
        ClienteConexion.getInstancia().enviarMensaje(requestMsg);

        // Notificar el estado de cámara actual al entrar a la reunión
        if (camaraActiva) {
            enviarEstadoCamara("ON");
            startCameraSource();
        } else {
            enviarEstadoCamara("OFF");
        }
    }

    private void cancelarSolicitud() {
        salirDeSalaActual();
        cardLayout.show(mainContainer, "SELECTOR");
    }

    private void abandonarReunion() {
        salirDeSalaActual();
        txtAreaChat.setText(""); // Limpiar chat
        cardLayout.show(mainContainer, "SELECTOR");
    }

    private void salirDeSalaActual() {
        if (roomCode != null) {
            MensajeSocket msg = new MensajeSocket();
            msg.setType("LEAVE_ROOM");
            msg.setRoomCode(roomCode);
            msg.setUserId(userId);
            msg.setUserName(userName);

            ClienteConexion.getInstancia().enviarMensaje(msg);
            System.out.println("[->] Enviado mensaje de salida de sala: " + roomCode);
            roomCode = null;
            // Detener simulador de cámara si está corriendo
            try {
                stopCameraSource();
            } catch (Exception ex) {
                System.err.println("[-] Error al detener la cámara: " + ex.getMessage());
            }
        }
    }

    private void enviarMensajeChat() {
        String texto = txtMensajeChat.getText().trim();
        if (texto.isEmpty()) return;

        MensajeSocket msg = new MensajeSocket();
        msg.setType("CHAT_MESSAGE");
        msg.setRoomCode(roomCode);
        msg.setUserId(userId);
        msg.setUserName(userName);
        msg.setMessage(texto);

        ClienteConexion.getInstancia().enviarMensaje(msg);
        txtMensajeChat.setText("");
    }

    // =========================================================================
    // OYENTES DE MENSAJES (OBSERVER PATTERN)
    // =========================================================================

    @Override
    public void onMensajeRecibido(MensajeSocket mensaje) {
        String tipo = mensaje.getType();

        switch (tipo) {
            case "CREATE_ROOM":
                procesarCreacionSala(mensaje);
                break;

            case "JOIN_ROOM_RESPONSE":
                procesarJoinResponse(mensaje);
                break;

            case "WAITING_ROOM_UPDATE":
                procesarWaitingRoomUpdate(mensaje);
                break;

            case "ADMIT_USER":
                procesarAdmisionUsuario(mensaje);
                break;

            case "CHAT_MESSAGE":
                procesarMensajeChat(mensaje);
                break;

            case "CAMERA_FRAME":
                procesarCameraFrame(mensaje);
                break;

            case "CAMERA_STATE":
                procesarEstadoCamara(mensaje);
                break;

            case "GET_FILES_RESPONSE":
                procesarGetFilesResponse(mensaje);
                break;
 
            case "FILE_START":
                procesarInicioDescarga(mensaje);
                break;
 
            case "FILE_CHUNK":
                procesarChunkDescarga(mensaje);
                break;
 
            case "FILE_END":
                procesarFinDescarga(mensaje);
                break;

            default:
                break;
        }
    }

    private void procesarCreacionSala(MensajeSocket mensaje) {
        SwingUtilities.invokeLater(() -> {
            if ("SUCCESS".equalsIgnoreCase(mensaje.getMessage())) {
                this.roomCode = mensaje.getRoomCode();
                lblCodigoSalaHost.setText("Código de Sala: " + this.roomCode);
                cardLayout.show(mainContainer, "HOST");
                System.out.println("[+] Sala creada con éxito: " + this.roomCode);
            } else {
                JOptionPane.showMessageDialog(this, "Error al crear sala: " + mensaje.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void procesarJoinResponse(MensajeSocket mensaje) {
        SwingUtilities.invokeLater(() -> {
            if ("PENDIENTE".equalsIgnoreCase(mensaje.getMessage())) {
                this.roomCode = mensaje.getRoomCode();
                lblMensajeEspera.setText("Tu solicitud para ingresar a la sala " + this.roomCode + " está PENDIENTE.");
                cardLayout.show(mainContainer, "INVITADO");
            } else {
                JOptionPane.showMessageDialog(this, "Error al unirse: " + mensaje.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void procesarWaitingRoomUpdate(MensajeSocket mensaje) {
        SwingUtilities.invokeLater(() -> {
            pnlListaEspera.removeAll();
            pnlListaEspera.revalidate();
            pnlListaEspera.repaint();

            // Deserializar la lista de solicitudes
            java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> solicitudes = gson.fromJson(mensaje.getMessage(), listType);

            if (solicitudes == null || solicitudes.isEmpty()) {
                JLabel lblVacio = new JLabel("No hay nadie esperando en este momento.", JLabel.CENTER);
                lblVacio.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                lblVacio.setAlignmentX(Component.CENTER_ALIGNMENT);
                pnlListaEspera.add(Box.createVerticalGlue());
                pnlListaEspera.add(lblVacio);
                pnlListaEspera.add(Box.createVerticalGlue());
                return;
            }

            for (Map<String, Object> sol : solicitudes) {
                // Parsear propiedades
                Number guestIdNum = (Number) sol.get("idUsuario");
                int guestId = guestIdNum.intValue();
                String guestName = (String) sol.get("nombres");

                // Crear fila visual para el invitado
                JPanel row = new JPanel(new BorderLayout(10, 0));
                row.setBackground(Color.WHITE);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
                row.setPreferredSize(new Dimension(0, 50));
                row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 240)),
                        new EmptyBorder(5, 10, 5, 10)
                ));

                JLabel lblNombre = new JLabel(guestName);
                lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 14));
                row.add(lblNombre, BorderLayout.CENTER);

                // Botones de control
                JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
                pnlBotones.setBackground(Color.WHITE);

                JButton btnAceptar = new JButton("Admitir");
                btnAceptar.setBackground(new Color(46, 204, 113));
                btnAceptar.setForeground(Color.WHITE);
                btnAceptar.setFont(new Font("Segoe UI", Font.BOLD, 11));
                btnAceptar.setFocusPainted(false);
                btnAceptar.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btnAceptar.addActionListener(e -> responderSolicitud(guestId, guestName, "ACEPTAR"));
                pnlBotones.add(btnAceptar);

                JButton btnRechazar = new JButton("Rechazar");
                btnRechazar.setBackground(new Color(231, 76, 60));
                btnRechazar.setForeground(Color.WHITE);
                btnRechazar.setFont(new Font("Segoe UI", Font.BOLD, 11));
                btnRechazar.setFocusPainted(false);
                btnRechazar.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btnRechazar.addActionListener(e -> responderSolicitud(guestId, guestName, "RECHAZAR"));
                pnlBotones.add(btnRechazar);

                row.add(pnlBotones, BorderLayout.EAST);
                pnlListaEspera.add(row);
            }

            pnlListaEspera.revalidate();
            pnlListaEspera.repaint();
        });
    }

    private void procesarAdmisionUsuario(MensajeSocket mensaje) {
        SwingUtilities.invokeLater(() -> {
            String decision = mensaje.getMessage(); // "ACCEPTED" o "REJECTED"
            if ("ACCEPTED".equalsIgnoreCase(decision)) {
                entrarAReunion();
                JOptionPane.showMessageDialog(this, "¡El anfitrión ha aceptado tu solicitud! Entrando a la sala...", "Acceso Admitido", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "El anfitrión ha rechazado tu solicitud de ingreso.", "Acceso Denegado", JOptionPane.WARNING_MESSAGE);
                this.roomCode = null;
                cardLayout.show(mainContainer, "SELECTOR");
            }
        });
    }

    private void procesarMensajeChat(MensajeSocket mensaje) {
        SwingUtilities.invokeLater(() -> {
            String remitente = mensaje.getUserName();
            String texto = mensaje.getMessage();
            txtAreaChat.append("[" + remitente + "]: " + texto + "\n");
            
            // Auto-scroll al final del chat
            txtAreaChat.setCaretPosition(txtAreaChat.getDocument().getLength());
        });
    }

    private void procesarCameraFrame(MensajeSocket mensaje) {
        String payload = mensaje.getMessage();
        if (payload == null || payload.isEmpty()) return;

        try {
            byte[] bytes = Base64.getDecoder().decode(payload);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BufferedImage img = ImageIO.read(bais);
            if (img == null) return;

            Integer senderId = mensaje.getUserId();
            String senderName = mensaje.getUserName();

            SwingUtilities.invokeLater(() -> updateVideoFeed(senderId, senderName, img));
        } catch (Exception e) {
            System.err.println("[-] Error al procesar CAMERA_FRAME: " + e.getMessage());
        }
    }

    private void procesarEstadoCamara(MensajeSocket mensaje) {
        if (mensaje == null) return;
        Integer senderId = mensaje.getUserId();
        String senderName = mensaje.getUserName();
        String estado = mensaje.getMessage();
        if (senderId == null || estado == null) return;

        SwingUtilities.invokeLater(() -> updateVideoState(senderId, senderName, estado));
    }

    private void updateVideoFeed(Integer senderId, String senderName, BufferedImage img) {
        if (senderId == null) return;

        JLabel lbl = getOrCreateFeedLabel(senderId, senderName);
        if (lbl == null) return;

        ImageIcon icon = new ImageIcon(img.getScaledInstance(320, 240, Image.SCALE_SMOOTH));
        lbl.setIcon(icon);
        lbl.setText("");

        JPanel panel = videoFeedPanels.get(senderId);
        if (panel != null) {
            panel.setBackground(new Color(20, 20, 25));
        }
    }

    private void updateVideoState(Integer senderId, String senderName, String estado) {
        JLabel lbl = getOrCreateFeedLabel(senderId, senderName);
        if (lbl == null) return;

        JPanel panel = videoFeedPanels.get(senderId);
        if (panel != null) {
            if ("OFF".equalsIgnoreCase(estado)) {
                panel.setBackground(Color.BLACK);
                lbl.setIcon(null);
                lbl.setText("Cámara apagada");
                lbl.setForeground(Color.WHITE);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
            } else {
                panel.setBackground(new Color(20, 20, 25));
                lbl.setText("");
            }
        }
    }

    private JLabel getOrCreateFeedLabel(Integer senderId, String senderName) {
        JLabel lbl = videoFeeds.get(senderId);
        JPanel panel = videoFeedPanels.get(senderId);

        if (panel == null) {
            panel = new JPanel(new BorderLayout());
            panel.setPreferredSize(new Dimension(320, 240));
            panel.setBackground(new Color(20, 20, 25));
            panel.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 80), 1, true));

            lbl = new JLabel();
            lbl.setHorizontalAlignment(JLabel.CENTER);
            lbl.setVerticalAlignment(JLabel.CENTER);
            lbl.setForeground(Color.WHITE);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            panel.add(lbl, BorderLayout.CENTER);

            JLabel nameLbl = new JLabel(senderName != null ? senderName : ("User " + senderId), JLabel.CENTER);
            nameLbl.setForeground(Color.WHITE);
            nameLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            panel.add(nameLbl, BorderLayout.SOUTH);

            pnlVideoContainer.add(panel);
            videoFeeds.put(senderId, lbl);
            videoFeedPanels.put(senderId, panel);
            pnlVideoContainer.revalidate();
            pnlVideoContainer.repaint();
        }

        return lbl;
    }

    @Override
    public void onDesconexion() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Se ha perdido la conexión con el servidor. Saliendo...", "Desconectado", JOptionPane.ERROR_MESSAGE);
            this.dispose();
            System.exit(0);
        });
    }

    private void seleccionarYSubirArchivo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar archivo para compartir");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = chooser.getSelectedFile();
            if (file.length() > 5 * 1024 * 1024) { // Límite de 5 MB
                JOptionPane.showMessageDialog(this, "El archivo supera el límite de 5 MB.", "Archivo muy grande", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Iniciar subida en hilo de fondo para no colgar el EDT
            String fileId = java.util.UUID.randomUUID().toString().substring(0, 8);
            String nombreArchivo = file.getName();
            
            new Thread(() -> {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                    // 1. Enviar FILE_START
                    MensajeSocket startMsg = new MensajeSocket();
                    startMsg.setType("FILE_START");
                    startMsg.setRoomCode(roomCode);
                    startMsg.setUserId(userId);
                    startMsg.setUserName(userName);
                    startMsg.setMessage(fileId + "|" + nombreArchivo);
                    ClienteConexion.getInstancia().enviarMensaje(startMsg);

                    byte[] buffer = new byte[64 * 1024]; // 64 KB chunks
                    int bytesLeidos;

                    // 2. Enviar FILE_CHUNKs
                    while ((bytesLeidos = fis.read(buffer)) != -1) {
                        byte[] tempBuf = bytesLeidos == buffer.length ? buffer : java.util.Arrays.copyOf(buffer, bytesLeidos);
                        String chunkBase64 = Base64.getEncoder().encodeToString(tempBuf);

                        MensajeSocket chunkMsg = new MensajeSocket();
                        chunkMsg.setType("FILE_CHUNK");
                        chunkMsg.setRoomCode(roomCode);
                        chunkMsg.setUserId(userId);
                        chunkMsg.setUserName(userName);
                        chunkMsg.setMessage(fileId + "|" + chunkBase64);
                        ClienteConexion.getInstancia().enviarMensaje(chunkMsg);
                        
                        Thread.sleep(20); // Pausa corta
                    }

                    // 3. Enviar FILE_END
                    MensajeSocket endMsg = new MensajeSocket();
                    endMsg.setType("FILE_END");
                    endMsg.setRoomCode(roomCode);
                    endMsg.setUserId(userId);
                    endMsg.setUserName(userName);
                    endMsg.setMessage(fileId);
                    ClienteConexion.getInstancia().enviarMensaje(endMsg);
                    
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "¡Archivo '" + nombreArchivo + "' enviado con éxito!", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    });

                } catch (Exception e) {
                    System.err.println("[-] Error al subir archivo: " + e.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Error al subir archivo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        }
    }

    private void solicitarListaArchivos() {
        MensajeSocket request = new MensajeSocket();
        request.setType("GET_FILES_REQUEST");
        request.setRoomCode(roomCode);
        request.setUserId(userId);
        request.setUserName(userName);
        ClienteConexion.getInstancia().enviarMensaje(request);
    }

    private void procesarGetFilesResponse(MensajeSocket mensaje) {
        SwingUtilities.invokeLater(() -> {
            java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> archivos = gson.fromJson(mensaje.getMessage(), listType);

            // Crear el diálogo modal para ver los archivos
            JDialog dialog = new JDialog(this, "Archivos Compartidos", true);
            dialog.setSize(550, 350);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout(10, 10));

            JPanel pnlCentral = new JPanel(new BorderLayout(5, 5));
            pnlCentral.setBorder(new EmptyBorder(10, 10, 10, 10));

            JLabel lblTitulo = new JLabel("Lista de Archivos de esta Sala:");
            lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
            pnlCentral.add(lblTitulo, BorderLayout.NORTH);

            // Tabla de archivos
            String[] columnas = {"Archivo", "Compartido por", "Fecha"};
            Object[][] datos = new Object[archivos.size()][3];
            for (int i = 0; i < archivos.size(); i++) {
                Map<String, Object> arch = archivos.get(i);
                datos[i][0] = arch.get("nombreArchivo");
                datos[i][1] = arch.get("nombres");
                datos[i][2] = arch.get("fechaSubida");
            }

            JTable tabla = new JTable(datos, columnas) {
                public boolean isCellEditable(int row, int column) { return false; }
            };
            tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            tabla.setRowHeight(25);
            JScrollPane scroll = new JScrollPane(tabla);
            pnlCentral.add(scroll, BorderLayout.CENTER);

            dialog.add(pnlCentral, BorderLayout.CENTER);

            // Footer con botón de descarga
            JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            JButton btnDescargar = new JButton("Descargar Seleccionado");
            btnDescargar.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnDescargar.setBackground(new Color(39, 174, 96));
            btnDescargar.setForeground(Color.WHITE);
            btnDescargar.setFocusPainted(false);
            btnDescargar.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btnDescargar.addActionListener(e -> {
                int filaSel = tabla.getSelectedRow();
                if (filaSel == -1) {
                    JOptionPane.showMessageDialog(dialog, "Seleccione un archivo de la lista.", "Selección requerida", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Map<String, Object> archSel = archivos.get(filaSel);
                String nombreArchivo = (String) archSel.get("nombreArchivo");
                String rutaFisica = (String) archSel.get("rutaArchivo");

                // Solicitar destino de guardado
                JFileChooser saveChooser = new JFileChooser();
                saveChooser.setSelectedFile(new java.io.File(nombreArchivo));
                saveChooser.setDialogTitle("Guardar archivo descargado");
                
                if (saveChooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                    java.io.File destFile = saveChooser.getSelectedFile();
                    try {
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(destFile);
                        descargasEnProgreso.put(rutaFisica, fos);

                        // Enviar solicitud de descarga
                        MensajeSocket downloadReq = new MensajeSocket();
                        downloadReq.setType("FILE_DOWNLOAD_REQUEST");
                        downloadReq.setRoomCode(roomCode);
                        downloadReq.setUserId(userId);
                        downloadReq.setUserName(userName);
                        downloadReq.setMessage(rutaFisica);
                        ClienteConexion.getInstancia().enviarMensaje(downloadReq);

                        JOptionPane.showMessageDialog(dialog, "Iniciando descarga de '" + nombreArchivo + "'...", "Descarga en curso", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "Error al preparar archivo local: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            pnlFooter.add(btnDescargar);
            dialog.add(pnlFooter, BorderLayout.SOUTH);

            dialog.setVisible(true);
        });
    }

    private void procesarInicioDescarga(MensajeSocket mensaje) {
        String payload = mensaje.getMessage();
        if (payload == null || !payload.contains("|")) return;

        String[] partes = payload.split("\\|", 2);
        String rutaFisica = partes[0];
        
        System.out.println("[*] Iniciando descarga local para: " + rutaFisica);
    }

    private void procesarChunkDescarga(MensajeSocket mensaje) {
        String payload = mensaje.getMessage();
        if (payload == null || !payload.contains("|")) return;

        String[] partes = payload.split("\\|", 2);
        String rutaFisica = partes[0];
        String chunkBase64 = partes[1];

        java.io.FileOutputStream fos = descargasEnProgreso.get(rutaFisica);
        if (fos != null) {
            try {
                byte[] bytes = Base64.getDecoder().decode(chunkBase64);
                fos.write(bytes);
            } catch (Exception e) {
                System.err.println("[-] Error al escribir chunk de descarga: " + e.getMessage());
            }
        }
    }

    private void procesarFinDescarga(MensajeSocket mensaje) {
        String rutaFisica = mensaje.getMessage();
        java.io.FileOutputStream fos = descargasEnProgreso.remove(rutaFisica);
        if (fos != null) {
            try {
                fos.close();
                String nombreArchivo = rutaFisica;
                if (nombreArchivo.contains(java.io.File.separator)) {
                    nombreArchivo = nombreArchivo.substring(nombreArchivo.lastIndexOf(java.io.File.separator) + 1);
                }
                if (nombreArchivo.contains("/")) {
                    nombreArchivo = nombreArchivo.substring(nombreArchivo.lastIndexOf("/") + 1);
                }
                if (nombreArchivo.contains("_")) {
                    nombreArchivo = nombreArchivo.substring(nombreArchivo.indexOf("_") + 1);
                }
                String finalNombre = nombreArchivo;
                
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "¡La descarga de '" + finalNombre + "' ha finalizado con éxito!", "Descarga Completada", JOptionPane.INFORMATION_MESSAGE);
                });
                System.out.println("[OK] Descarga finalizada con éxito.");
            } catch (Exception e) {
                System.err.println("[-] Error al finalizar descarga: " + e.getMessage());
            }
        }
    }

    private void toggleCamera() {
        camaraActiva = !camaraActiva;
        btnToggleCamera.setText(camaraActiva ? "Cámara: ON" : "Cámara: OFF");
        btnToggleCamera.setBackground(camaraActiva ? new Color(46, 204, 113) : new Color(192, 57, 43));
        if (roomCode != null) {
            if (camaraActiva) {
                enviarEstadoCamara("ON");
                startCameraSource();
            } else {
                enviarEstadoCamara("OFF");
                stopCameraSource();
            }
        }
    }

    private void enviarEstadoCamara(String estado) {
        if (roomCode == null) return;
        MensajeSocket estadoMsg = new MensajeSocket();
        estadoMsg.setType("CAMERA_STATE");
        estadoMsg.setRoomCode(roomCode);
        estadoMsg.setUserId(userId);
        estadoMsg.setUserName(userName);
        estadoMsg.setMessage(estado);
        ClienteConexion.getInstancia().enviarMensaje(estadoMsg);
    }

    private void startCameraSource() {
        if (cameraCapture == null) {
            cameraCapture = new CameraCapture(userId, userName, roomCode);
        }

        if (cameraCapture.start()) {
            usingCameraCapture = true;
            return;
        }

        // Si no se encuentra cámara física, usar simulador de respaldo.
        usingCameraCapture = false;
        if (cameraSimulator == null) {
            cameraSimulator = new CameraSimulator(userId, userName, roomCode);
        }
        cameraSimulator.start();
    }

    private void stopCameraSource() {
        if (cameraCapture != null) {
            cameraCapture.stop();
            cameraCapture = null;
        }
        if (cameraSimulator != null) {
            cameraSimulator.stop();
            cameraSimulator = null;
        }
        usingCameraCapture = false;
    }
}
