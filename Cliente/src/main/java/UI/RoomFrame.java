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
import network.camera.*;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.concurrent.ConcurrentHashMap;
import UI.memento.ChatInputMemento;
import UI.memento.ChatHistoryCaretaker;

public class RoomFrame extends JFrame implements ClienteConexion.MensajeListener {
    private static RoomFrame activeInstance;

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

    // --- CARD 3b: Invitado admitido, esperando inicio de reunión ---
    private JLabel lblMensajeEsperaAdmitido;

    // --- CARD 4: Panel de Reunión Activa (Meeting View) ---
    private JLabel lblTituloReunion;
    private JPanel pnlChatMessages;
    private JScrollPane scrollChat;
    private Component chatFiller;
    private JTextField txtMensajeChat;
    private JButton btnEnviarChat;
    private JPanel pnlVideoGrid;
    private JPanel pnlVideoContainer;
    private JButton btnToggleCamera;
    private boolean camaraActiva = false;
    private java.util.Map<Integer, VideoFeedPanel> videoFeedPanels = new ConcurrentHashMap<>();
    private final java.util.Map<Integer, String> activeCameraStates = new ConcurrentHashMap<>();
    private CameraStrategy cameraStream;

    private Gson gson;
    private java.util.Map<String, java.io.FileOutputStream> descargasEnProgreso = new java.util.concurrent.ConcurrentHashMap<>();
    private final ChatHistoryCaretaker chatCaretaker = new ChatHistoryCaretaker();

    // Pool de hilos para decodificación de video y procesamiento de disco asíncrono
    private final java.util.concurrent.ExecutorService videoDecoderExecutor = 
        java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "VideoDecoderPool");
            t.setDaemon(true);
            return t;
        });

    public RoomFrame(int userId, String userName) {
        activeInstance = this;
        this.userId = userId;
        this.userName = userName;
        this.gson = new Gson();

        setTitle("Zoom Sockets - " + userName);
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Registrar oyente de red
        ClienteConexion.getInstancia().setListener(this);

        // Desconectarse al cerrar la ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salirDeSalaActual();
                ClienteConexion.getInstancia().desconectar();
                if (videoDecoderExecutor != null) {
                    videoDecoderExecutor.shutdown();
                }
                activeInstance = null;
            }
        });

        // Contenedor principal con CardLayout
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // Inicializar las 5 pantallas (Cards)
        inicializarPantallaSelector();
        inicializarPantallaHost();
        inicializarPantallaInvitado();
        inicializarPantallaInvitadoAdmitido();
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
                new EmptyBorder(20, 20, 20, 20)));

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
                new EmptyBorder(20, 20, 20, 20)));

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
    // 3b. CARD "INVITADO_ADMITIDO": Invitado admitido, esperando inicio de reunión
    // =========================================================================
    private void inicializarPantallaInvitadoAdmitido() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblLogo = new JLabel("Admitido", JLabel.CENTER);
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblLogo.setForeground(new Color(39, 174, 96));
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(lblLogo, gbc);

        lblMensajeEsperaAdmitido = new JLabel("Has sido admitido. Esperando que el anfitrión inicie la reunión...",
                JLabel.CENTER);
        lblMensajeEsperaAdmitido.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        gbc.gridy = 1;
        panel.add(lblMensajeEsperaAdmitido, gbc);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setForeground(new Color(39, 174, 96));
        gbc.gridy = 2;
        panel.add(progress, gbc);

        JButton btnCancelarEsperando = new JButton("Abandonar Sala");
        btnCancelarEsperando.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancelarEsperando.setBackground(new Color(192, 57, 43));
        btnCancelarEsperando.setForeground(Color.WHITE);
        btnCancelarEsperando.setFocusPainted(false);
        btnCancelarEsperando.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelarEsperando.addActionListener(e -> cancelarSolicitud());
        gbc.gridy = 3;
        panel.add(btnCancelarEsperando, gbc);

        mainContainer.add(panel, "INVITADO_ADMITIDO");
    }

    // =========================================================================
    // 4. CARD "REUNION": Pantalla de Reunión Activa (Chat y Video Grid)
    // =========================================================================
    private void inicializarPantallaReunion() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(30, 31, 34)); // Fondo Oscuro Premium (Estilo Zoom)
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header Superior
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(new Color(43, 45, 49));
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 57, 62), 1, true),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        lblTituloReunion = new JLabel("Sala de Reunión Activa: Código ------");
        lblTituloReunion.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTituloReunion.setForeground(Color.WHITE);
        pnlHeader.add(lblTituloReunion, BorderLayout.WEST);

        JPanel pnlHeaderButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlHeaderButtons.setOpaque(false);

        JButton btnArchivos = new JButton("📁 Archivos");
        btnArchivos.setBackground(new Color(45, 140, 255)); // Zoom Blue
        btnArchivos.setForeground(Color.WHITE);
        btnArchivos.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnArchivos.setFocusPainted(false);
        btnArchivos.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnArchivos.addActionListener(e -> solicitarListaArchivos());
        btnArchivos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnArchivos.setBackground(new Color(65, 155, 255));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnArchivos.setBackground(new Color(45, 140, 255));
            }
        });
        pnlHeaderButtons.add(btnArchivos);

        btnToggleCamera = new JButton("📹 Cámara: OFF");
        btnToggleCamera.setBackground(new Color(237, 66, 69)); // Rojo Zoom
        btnToggleCamera.setForeground(Color.WHITE);
        btnToggleCamera.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnToggleCamera.setFocusPainted(false);
        btnToggleCamera.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggleCamera.addActionListener(e -> toggleCamera());
        pnlHeaderButtons.add(btnToggleCamera);

        JButton btnSalir = new JButton("🚪 Abandonar Sala");
        btnSalir.setBackground(new Color(237, 66, 69)); // Rojo Zoom
        btnSalir.setForeground(Color.WHITE);
        btnSalir.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSalir.setFocusPainted(false);
        btnSalir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSalir.addActionListener(e -> abandonarReunion());
        btnSalir.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnSalir.setBackground(new Color(253, 92, 95));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnSalir.setBackground(new Color(237, 66, 69));
            }
        });
        pnlHeaderButtons.add(btnSalir);

        pnlHeader.add(pnlHeaderButtons, BorderLayout.EAST);

        panel.add(pnlHeader, BorderLayout.NORTH);

        // Panel Central Dividido
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(new Color(30, 31, 34));
        splitPane.setBorder(null);
        splitPane.setDividerLocation(480);
        splitPane.setResizeWeight(0.6);

        // Lado Izquierdo: Video Grid
        pnlVideoGrid = new JPanel(new BorderLayout());
        pnlVideoGrid.setBackground(new Color(30, 31, 34));
        pnlVideoGrid.setBorder(new LineBorder(new Color(55, 57, 62), 1, true));

        pnlVideoContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        pnlVideoContainer.setBackground(new Color(23, 23, 28));
        
        JScrollPane scrollVideo = new JScrollPane(pnlVideoContainer);
        scrollVideo.setBorder(null);
        scrollVideo.getVerticalScrollBar().setUnitIncrement(16);
        pnlVideoGrid.add(scrollVideo, BorderLayout.CENTER);

        splitPane.setLeftComponent(pnlVideoGrid);

        // Lado Derecho: Panel de Chat (Fase 4)
        JPanel pnlChat = new JPanel(new BorderLayout(5, 5));
        pnlChat.setBackground(new Color(43, 45, 49));
        pnlChat.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(55, 57, 62), 1, true),
                new EmptyBorder(10, 10, 10, 10)));

        pnlChatMessages = new JPanel();
        pnlChatMessages.setLayout(new BoxLayout(pnlChatMessages, BoxLayout.Y_AXIS));
        pnlChatMessages.setBackground(new Color(43, 45, 49));

        scrollChat = new JScrollPane(pnlChatMessages);
        scrollChat.setBorder(null);
        scrollChat.getVerticalScrollBar().setUnitIncrement(12);
        pnlChat.add(scrollChat, BorderLayout.CENTER);

        // Footer del Chat (Input)
        JPanel pnlChatInput = new JPanel(new BorderLayout(5, 0));
        pnlChatInput.setBackground(new Color(43, 45, 49));

        txtMensajeChat = new JTextField();
        txtMensajeChat.setBackground(new Color(30, 31, 34));
        txtMensajeChat.setForeground(Color.WHITE);
        txtMensajeChat.setCaretColor(Color.WHITE);
        txtMensajeChat.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtMensajeChat.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(60, 63, 65), 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        txtMensajeChat.addActionListener(e -> enviarMensajeChat());
        txtMensajeChat.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                    ChatInputMemento memento = chatCaretaker.getPrevious();
                    if (memento != null) {
                        txtMensajeChat.setText(memento.getText());
                    }
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
                    ChatInputMemento memento = chatCaretaker.getNext();
                    if (memento != null) {
                        txtMensajeChat.setText(memento.getText());
                    }
                }
            }
        });
        pnlChatInput.add(txtMensajeChat, BorderLayout.CENTER);

        JButton btnAdjuntar = new JButton("📎");
        btnAdjuntar.setToolTipText("Compartir archivo");
        btnAdjuntar.setBackground(new Color(43, 45, 49));
        btnAdjuntar.setForeground(Color.LIGHT_GRAY);
        btnAdjuntar.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btnAdjuntar.setFocusPainted(false);
        btnAdjuntar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdjuntar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btnAdjuntar.addActionListener(e -> seleccionarYSubirArchivo());
        btnAdjuntar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnAdjuntar.setBackground(new Color(55, 57, 62));
                btnAdjuntar.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnAdjuntar.setBackground(new Color(43, 45, 49));
                btnAdjuntar.setForeground(Color.LIGHT_GRAY);
            }
        });
        pnlChatInput.add(btnAdjuntar, BorderLayout.WEST);

        btnEnviarChat = new JButton("📤");
        btnEnviarChat.setToolTipText("Enviar mensaje");
        btnEnviarChat.setBackground(new Color(45, 140, 255));
        btnEnviarChat.setForeground(Color.WHITE);
        btnEnviarChat.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btnEnviarChat.setFocusPainted(false);
        btnEnviarChat.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEnviarChat.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        btnEnviarChat.addActionListener(e -> enviarMensajeChat());
        btnEnviarChat.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnEnviarChat.setBackground(new Color(65, 155, 255));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnEnviarChat.setBackground(new Color(45, 140, 255));
            }
        });
        pnlChatInput.add(btnEnviarChat, BorderLayout.EAST);

        pnlChat.add(pnlChatInput, BorderLayout.SOUTH);

        splitPane.setRightComponent(pnlChat);

        panel.add(splitPane, BorderLayout.CENTER);

        mainContainer.add(panel, "REUNION");
    }

    private void agregarMensajeAlChat(String remitente, int remitenteId, String texto, String hora) {
        boolean esPropio = (remitenteId == this.userId);
        
        JPanel rowPanel = new JPanel(new FlowLayout(esPropio ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 5));
        rowPanel.setOpaque(false);
        
        if ("SISTEMA".equalsIgnoreCase(remitente)) {
            JPanel sysRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            sysRow.setOpaque(false);
            
            JPanel bubble = new JPanel();
            bubble.setBackground(new Color(60, 60, 65));
            bubble.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
            
            JLabel lblTexto = new JLabel(texto);
            lblTexto.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblTexto.setForeground(Color.LIGHT_GRAY);
            bubble.add(lblTexto);
            
            sysRow.add(bubble);
            
            if (chatFiller != null) pnlChatMessages.remove(chatFiller);
            pnlChatMessages.add(sysRow);
            chatFiller = Box.createVerticalGlue();
            pnlChatMessages.add(chatFiller);
            
            pnlChatMessages.revalidate();
            pnlChatMessages.repaint();
            
            // Auto scroll
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollChat.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
            return;
        }

        // Burbuja de mensaje
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        
        if (esPropio) {
            bubble.setBackground(new Color(45, 140, 255)); // Zoom Blue
        } else {
            bubble.setBackground(new Color(60, 63, 65)); // Dark Gray Card
        }
        
        // Si no es propio, agregamos el nombre del remitente arriba
        if (!esPropio) {
            JLabel lblRemitente = new JLabel(remitente);
            lblRemitente.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblRemitente.setForeground(new Color(152, 170, 236)); // Color llamativo
            bubble.add(lblRemitente);
            bubble.add(Box.createRigidArea(new Dimension(0, 3)));
        }
        
        // Contenido del mensaje con Wrap de texto automático en JLabel
        JLabel lblTexto = new JLabel("<html><p style='width: 150px;'>" + texto.replace("\n", "<br>") + "</p></html>");
        lblTexto.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTexto.setForeground(Color.WHITE);
        bubble.add(lblTexto);
        bubble.add(Box.createRigidArea(new Dimension(0, 3)));
        
        // Hora
        JLabel lblHora = new JLabel(hora != null && !hora.isEmpty() ? hora : obtenerHoraActual());
        lblHora.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        lblHora.setForeground(esPropio ? new Color(200, 220, 255) : Color.LIGHT_GRAY);
        lblHora.setAlignmentX(Component.RIGHT_ALIGNMENT);
        bubble.add(lblHora);
        
        // Si no es propio, añadir avatar con inicial
        if (!esPropio) {
            JPanel pnlAvatar = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(114, 137, 218)); // Azul Discord
                    g2.fillOval(0, 0, 32, 32);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    String inicial = remitente.substring(0, Math.min(2, remitente.length())).toUpperCase();
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (32 - fm.stringWidth(inicial)) / 2;
                    int y = ((32 - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(inicial, x, y);
                }
            };
            pnlAvatar.setPreferredSize(new Dimension(32, 32));
            pnlAvatar.setOpaque(false);
            rowPanel.add(pnlAvatar);
        }
        
        rowPanel.add(bubble);
        
        // Remover el filler viejo antes de agregar el mensaje, luego agregar el filler nuevo al final
        if (chatFiller != null) {
            pnlChatMessages.remove(chatFiller);
        }
        pnlChatMessages.add(rowPanel);
        chatFiller = Box.createVerticalGlue();
        pnlChatMessages.add(chatFiller);
        
        pnlChatMessages.revalidate();
        pnlChatMessages.repaint();
        
        // Hacer scroll automático al final
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollChat.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private String formatearHora(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return obtenerHoraActual();
        }
        try {
            if (timestamp.contains(" ")) {
                String horaParte = timestamp.split(" ")[1];
                if (horaParte.contains(":")) {
                    String[] partes = horaParte.split(":");
                    return partes[0] + ":" + partes[1];
                }
            }
        } catch (Exception e) {
            // Ignorar y retornar original
        }
        return timestamp;
    }

    private String obtenerHoraActual() {
        java.time.LocalTime ahora = java.time.LocalTime.now();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        return ahora.format(dtf);
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
            JOptionPane.showMessageDialog(this, "El código de sala debe tener exactamente 6 caracteres.",
                    "Código Inválido", JOptionPane.WARNING_MESSAGE);
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
        if (roomCode == null)
            return;

        MensajeSocket msg = new MensajeSocket();
        msg.setType("START_MEETING_REQUEST");
        msg.setRoomCode(roomCode);
        msg.setUserId(userId);
        msg.setUserName(userName);
        msg.setMessage("REQUEST_START");

        ClienteConexion.getInstancia().enviarMensaje(msg);
        btnIniciarReunion.setEnabled(false);
        agregarMensajeAlChat("SISTEMA", 0, "Petición de inicio de reunión enviada. Esperando confirmación...", null);
    }

    private void entrarAReunion() {
        lblTituloReunion.setText("Sala de Reunión Activa: Código " + roomCode);
        pnlChatMessages.removeAll();
        pnlChatMessages.revalidate();
        pnlChatMessages.repaint();
        cardLayout.show(mainContainer, "REUNION");
        activeCameraStates.clear(); // Limpiar estados de cámara anteriores

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
            activeCameraStates.put(this.userId, "ON");
            updateVideoState(this.userId, this.userName, "ON");
            enviarEstadoCamara("ON");
            startCameraSource();
        } else {
            activeCameraStates.put(this.userId, "OFF");
            updateVideoState(this.userId, this.userName, "OFF");
            enviarEstadoCamara("OFF");
        }
    }

    private void cancelarSolicitud() {
        salirDeSalaActual();
        cardLayout.show(mainContainer, "SELECTOR");
    }

    private void abandonarReunion() {
        salirDeSalaActual();
        pnlChatMessages.removeAll();
        pnlChatMessages.revalidate();
        pnlChatMessages.repaint();
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
        if (texto.isEmpty())
            return;

        // Guardar estado de texto en el Caretaker (Memento)
        chatCaretaker.addMemento(new ChatInputMemento(texto));

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

            case "MEETING_STARTED":
                procesarMeetingStarted(mensaje);
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

            case "LEAVE_ROOM":
                procesarUsuarioSalio(mensaje);
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
                JOptionPane.showMessageDialog(this, "Error al crear sala: " + mensaje.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(this, "Error al unirse: " + mensaje.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void procesarWaitingRoomUpdate(MensajeSocket mensaje) {
        SwingUtilities.invokeLater(() -> {
            pnlListaEspera.removeAll();
            pnlListaEspera.revalidate();
            pnlListaEspera.repaint();

            // Deserializar la lista de solicitudes
            java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>() {
            }.getType();
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
                        new EmptyBorder(5, 10, 5, 10)));

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
                lblMensajeEsperaAdmitido.setText("¡Has sido admitido! Esperando que el anfitrión inicie la reunión...");
                cardLayout.show(mainContainer, "INVITADO_ADMITIDO");
                JOptionPane.showMessageDialog(this,
                        "¡El anfitrión ha aceptado tu solicitud! Espera a que inicie la reunión.", "Acceso Admitido",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "El anfitrión ha rechazado tu solicitud de ingreso.",
                        "Acceso Denegado", JOptionPane.WARNING_MESSAGE);
                this.roomCode = null;
                cardLayout.show(mainContainer, "SELECTOR");
            }
        });
    }

    private void procesarMeetingStarted(MensajeSocket mensaje) {
        SwingUtilities.invokeLater(() -> {
            this.roomCode = mensaje.getRoomCode();
            entrarAReunion();
            JOptionPane.showMessageDialog(this, "La reunión ha comenzado. Bienvenido(a).", "Reunión iniciada",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void procesarMensajeChat(MensajeSocket mensaje) {
        SwingUtilities.invokeLater(() -> {
            String remitente = mensaje.getUserName();
            String texto = mensaje.getMessage();
            int remitenteId = mensaje.getUserId() != null ? mensaje.getUserId() : 0;
            String hora = formatearHora(mensaje.getSentAt());
            agregarMensajeAlChat(remitente, remitenteId, texto, hora);
        });
    }

    private void procesarCameraFrame(MensajeSocket mensaje) {
        videoDecoderExecutor.submit(() -> {
            String payload = mensaje.getMessage();
            if (payload == null || payload.isEmpty())
                return;

            try {
                byte[] bytes = Base64.getDecoder().decode(payload);
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                BufferedImage img = ImageIO.read(bais);
                if (img == null)
                    return;

                Integer senderId = mensaje.getUserId();
                String senderName = mensaje.getUserName();

                SwingUtilities.invokeLater(() -> updateVideoFeed(senderId, senderName, img));
            } catch (Exception e) {
                System.err.println("[-] Error al procesar CAMERA_FRAME: " + e.getMessage());
            }
        });
    }

    private void procesarEstadoCamara(MensajeSocket mensaje) {
        if (mensaje == null)
            return;
        Integer senderId = mensaje.getUserId();
        String senderName = mensaje.getUserName();
        String estado = mensaje.getMessage();
        if (senderId == null || estado == null)
            return;

        activeCameraStates.put(senderId, estado);

        SwingUtilities.invokeLater(() -> updateVideoState(senderId, senderName, estado));
    }

    private void procesarUsuarioSalio(MensajeSocket mensaje) {
        if (mensaje == null)
            return;
        Integer leavingUserId = mensaje.getUserId();
        if (leavingUserId == null)
            return;

        SwingUtilities.invokeLater(() -> {
            JPanel panel = videoFeedPanels.remove(leavingUserId);
            activeCameraStates.remove(leavingUserId);
            if (panel != null) {
                pnlVideoContainer.remove(panel);
                pnlVideoContainer.revalidate();
                pnlVideoContainer.repaint();
            }
        });
    }

    public static RoomFrame getActiveInstance() {
        return activeInstance;
    }

    public void mostrarFrameLocal(BufferedImage img) {
        SwingUtilities.invokeLater(() -> updateVideoFeed(this.userId, this.userName, img));
    }

    private void updateVideoFeed(Integer senderId, String senderName, BufferedImage img) {
        if (senderId == null)
            return;

        // Si el estado de la cámara del remitente es OFF, ignorar el frame para evitar
        // condiciones de carrera
        String estado = activeCameraStates.get(senderId);
        if ("OFF".equalsIgnoreCase(estado)) {
            return;
        }

        VideoFeedPanel panel = getOrCreateVideoPanel(senderId, senderName);
        if (panel != null) {
            panel.setFrame(img);
        }
    }

    private void updateVideoState(Integer senderId, String senderName, String estado) {
        VideoFeedPanel panel = getOrCreateVideoPanel(senderId, senderName);
        if (panel != null) {
            panel.setCameraState(estado);
        }
    }

    private VideoFeedPanel getOrCreateVideoPanel(Integer senderId, String senderName) {
        VideoFeedPanel panel = videoFeedPanels.get(senderId);
        if (panel == null) {
            panel = new VideoFeedPanel(senderId, senderName);
            videoFeedPanels.put(senderId, panel);

            // Sincronizar con el estado de cámara conocido actual
            String currentState = activeCameraStates.getOrDefault(senderId, "OFF");
            panel.setCameraState(currentState);

            pnlVideoContainer.add(panel);
            pnlVideoContainer.revalidate();
            pnlVideoContainer.repaint();
        }
        return panel;
    }

    @Override
    public void onDesconexion() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Se ha perdido la conexión con el servidor. Saliendo...",
                    "Desconectado", JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(this, "El archivo supera el límite de 5 MB.", "Archivo muy grande",
                        JOptionPane.WARNING_MESSAGE);
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
                        byte[] tempBuf = bytesLeidos == buffer.length ? buffer
                                : java.util.Arrays.copyOf(buffer, bytesLeidos);
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
                        JOptionPane.showMessageDialog(this, "¡Archivo '" + nombreArchivo + "' enviado con éxito!",
                                "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    });

                } catch (Exception e) {
                    System.err.println("[-] Error al subir archivo: " + e.getMessage());
                    String msg = e.getMessage();
                    String userMsg = "Error al subir archivo: " + msg;
                    if (msg != null && (msg.contains("proveedor de archivos de nube") || msg.toLowerCase().contains("cloud file provider"))) {
                        userMsg = "El archivo seleccionado está en la nube (OneDrive/Google Drive) pero el proveedor de sincronización no se está ejecutando.\n\n"
                                + "Para solucionarlo:\n"
                                + "1. Abre e inicia sesión en OneDrive/Google Drive en tu computadora.\n"
                                + "2. O haz clic derecho sobre el archivo en el Explorador de archivos de Windows y selecciona 'Mantener siempre en este dispositivo'.\n"
                                + "3. O copia el archivo a una carpeta local fuera de la nube (por ejemplo, C:\\) e intenta subirlo nuevamente.";
                    }
                    final String finalUserMsg = userMsg;
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, finalUserMsg, "Error de archivo en la nube",
                                JOptionPane.ERROR_MESSAGE);
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
            java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>() {
            }.getType();
            List<Map<String, Object>> archivos = gson.fromJson(mensaje.getMessage(), listType);

            // Crear el diálogo modal para ver los archivos
            JDialog dialog = new JDialog(this, "Archivos Compartidos", true);
            dialog.setSize(600, 450);
            dialog.setLocationRelativeTo(this);
            dialog.setResizable(false);

            JPanel pnlMain = new JPanel(new BorderLayout(15, 15));
            pnlMain.setBackground(new Color(30, 31, 34)); // #1E1F22
            pnlMain.setBorder(new EmptyBorder(20, 20, 20, 20));

            JLabel lblTitulo = new JLabel("Archivos Compartidos en la Sala:");
            lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lblTitulo.setForeground(Color.WHITE);
            pnlMain.add(lblTitulo, BorderLayout.NORTH);

            JPanel pnlListaArchivos = new JPanel();
            pnlListaArchivos.setLayout(new BoxLayout(pnlListaArchivos, BoxLayout.Y_AXIS));
            pnlListaArchivos.setBackground(new Color(30, 31, 34)); // #1E1F22

            if (archivos == null || archivos.isEmpty()) {
                JLabel lblVacio = new JLabel("No se ha compartido ningún archivo en esta sala.", JLabel.CENTER);
                lblVacio.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                lblVacio.setForeground(Color.GRAY);
                lblVacio.setAlignmentX(Component.CENTER_ALIGNMENT);
                pnlListaArchivos.add(Box.createVerticalGlue());
                pnlListaArchivos.add(lblVacio);
                pnlListaArchivos.add(Box.createVerticalGlue());
            } else {
                for (Map<String, Object> arch : archivos) {
                    String nombreArchivo = (String) arch.get("nombreArchivo");
                    String rutaFisica = (String) arch.get("rutaArchivo");
                    String usuarioSubida = (String) arch.get("nombres");
                    String fechaSubida = (String) arch.get("fechaSubida");

                    JPanel card = new JPanel(new BorderLayout(15, 10));
                    card.setBackground(new Color(43, 45, 49)); // #2B2D31
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(60, 63, 65), 1, true),
                            BorderFactory.createEmptyBorder(10, 15, 10, 15)));
                    card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
                    card.setPreferredSize(new Dimension(0, 70));

                    // Determinar extensión e icono coloreado
                    String ext = "";
                    int dotIdx = nombreArchivo.lastIndexOf('.');
                    if (dotIdx > 0) {
                        ext = nombreArchivo.substring(dotIdx + 1).toLowerCase();
                    }

                    String emoji = "📁";
                    Color colorIcono = new Color(140, 140, 140); // Gris genérico

                    if ("pdf".equals(ext)) {
                        emoji = "📄";
                        colorIcono = new Color(237, 66, 69); // Rojo PDF
                    } else if ("xls".equals(ext) || "xlsx".equals(ext) || "csv".equals(ext)) {
                        emoji = "📊";
                        colorIcono = new Color(59, 165, 92); // Verde Excel
                    } else if ("doc".equals(ext) || "docx".equals(ext) || "txt".equals(ext)) {
                        emoji = "📝";
                        colorIcono = new Color(45, 140, 255); // Azul Word
                    } else if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext) || "bmp".equals(ext)) {
                        emoji = "🖼️";
                        colorIcono = new Color(155, 89, 182); // Morado/Púrpura Imagen
                    }

                    JLabel lblIcono = new JLabel(emoji, JLabel.CENTER);
                    lblIcono.setFont(new Font("Segoe UI", Font.PLAIN, 28));
                    lblIcono.setForeground(colorIcono);
                    lblIcono.setPreferredSize(new Dimension(40, 40));
                    card.add(lblIcono, BorderLayout.WEST);

                    JPanel pnlInfo = new JPanel(new GridLayout(2, 1, 2, 2));
                    pnlInfo.setOpaque(false);

                    JLabel lblNombreArch = new JLabel(nombreArchivo);
                    lblNombreArch.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    lblNombreArch.setForeground(Color.WHITE);

                    JLabel lblMeta = new JLabel("Compartido por: " + usuarioSubida + " • " + fechaSubida);
                    lblMeta.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    lblMeta.setForeground(Color.LIGHT_GRAY);

                    pnlInfo.add(lblNombreArch);
                    pnlInfo.add(lblMeta);
                    card.add(pnlInfo, BorderLayout.CENTER);

                    JButton btnDescargar = new JButton("⬇ Descargar");
                    btnDescargar.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    btnDescargar.setBackground(new Color(45, 140, 255)); // Zoom Blue
                    btnDescargar.setForeground(Color.WHITE);
                    btnDescargar.setFocusPainted(false);
                    btnDescargar.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    btnDescargar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

                    btnDescargar.addMouseListener(new java.awt.event.MouseAdapter() {
                        public void mouseEntered(java.awt.event.MouseEvent evt) {
                            btnDescargar.setBackground(new Color(65, 155, 255));
                        }
                        public void mouseExited(java.awt.event.MouseEvent evt) {
                            btnDescargar.setBackground(new Color(45, 140, 255));
                        }
                    });

                    btnDescargar.addActionListener(e -> {
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

                                JOptionPane.showMessageDialog(dialog, "Iniciando descarga de '" + nombreArchivo + "'...",
                                        "Descarga en curso", JOptionPane.INFORMATION_MESSAGE);
                                dialog.dispose();

                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(dialog, "Error al preparar archivo local: " + ex.getMessage(),
                                        "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });

                    card.add(btnDescargar, BorderLayout.EAST);
                    pnlListaArchivos.add(card);
                    pnlListaArchivos.add(Box.createRigidArea(new Dimension(0, 10)));
                }
            }

            JScrollPane scroll = new JScrollPane(pnlListaArchivos);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(12);
            scroll.getViewport().setBackground(new Color(30, 31, 34)); // #1E1F22
            pnlMain.add(scroll, BorderLayout.CENTER);

            dialog.getContentPane().add(pnlMain);
            dialog.setVisible(true);
        });
    }

    private void procesarInicioDescarga(MensajeSocket mensaje) {
        String payload = mensaje.getMessage();
        if (payload == null || !payload.contains("|"))
            return;

        String[] partes = payload.split("\\|", 2);
        String rutaFisica = partes[0];

        System.out.println("[*] Iniciando descarga local para: " + rutaFisica);
    }

    private void procesarChunkDescarga(MensajeSocket mensaje) {
        videoDecoderExecutor.submit(() -> {
            String payload = mensaje.getMessage();
            if (payload == null || !payload.contains("|"))
                return;

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
        });
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
                    JOptionPane.showMessageDialog(this,
                            "¡La descarga de '" + finalNombre + "' ha finalizado con éxito!", "Descarga Completada",
                            JOptionPane.INFORMATION_MESSAGE);
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
                activeCameraStates.put(this.userId, "ON");
                updateVideoState(this.userId, this.userName, "ON");
                enviarEstadoCamara("ON");
                startCameraSource();
            } else {
                activeCameraStates.put(this.userId, "OFF");
                updateVideoState(this.userId, this.userName, "OFF");
                enviarEstadoCamara("OFF");
                stopCameraSource();
            }
        }
    }

    private void enviarEstadoCamara(String estado) {
        if (roomCode == null)
            return;
        MensajeSocket estadoMsg = new MensajeSocket();
        estadoMsg.setType("CAMERA_STATE");
        estadoMsg.setRoomCode(roomCode);
        estadoMsg.setUserId(userId);
        estadoMsg.setUserName(userName);
        estadoMsg.setMessage(estado);
        ClienteConexion.getInstancia().enviarMensaje(estadoMsg);
    }

    private void startCameraSource() {
        btnToggleCamera.setEnabled(false);

        new Thread(() -> {
            // Verificar si el usuario apagó la cámara antes de empezar
            if (!camaraActiva || roomCode == null) {
                SwingUtilities.invokeLater(() -> btnToggleCamera.setEnabled(true));
                return;
            }

            // Instanciar el proxy utilizando el creador físico inicial (Factory Method &
            // Proxy)
            CameraCreator physicalCreator = new PhysicalCameraCreator();
            CameraProxy proxy = new CameraProxy(userId, userName, roomCode, physicalCreator);
            cameraStream = proxy;

            boolean started = proxy.start();

            // Verificar si la cámara fue apagada o se abandonó la sala mientras se
            // inicializaba
            if (!camaraActiva || roomCode == null) {
                proxy.stop();
                if (cameraStream == proxy) {
                    cameraStream = null;
                }
                SwingUtilities.invokeLater(() -> btnToggleCamera.setEnabled(true));
                return;
            }

            if (started) {
                SwingUtilities.invokeLater(() -> {
                    btnToggleCamera.setEnabled(true);

                    // Si se hizo fallback a la simulación, alertar al usuario
                    if (proxy.getRealSubject() instanceof SimulatedCameraStrategy) {
                        JOptionPane.showMessageDialog(
                                RoomFrame.this,
                                "No se pudo acceder a la cámara física.\n" +
                                        "Asegúrate de que no esté en uso por otra aplicación y de que los permisos de cámara\n"
                                        +
                                        "estén habilitados en la configuración de privacidad de Windows:\n" +
                                        "Configuración -> Privacidad y seguridad -> Cámara (permitir que las aplicaciones de escritorio accedan).\n\n"
                                        +
                                        "Se activará la cámara de simulación académica.",
                                "Advertencia de Cámara",
                                JOptionPane.WARNING_MESSAGE);
                    }
                });
            } else {
                SwingUtilities.invokeLater(() -> btnToggleCamera.setEnabled(true));
            }
        }, "CameraInitializerThread").start();
    }

    private void stopCameraSource() {
        if (cameraStream != null) {
            cameraStream.stop();
            cameraStream = null;
        }
    }

    public void forzarFallbackASimulacion() {
        SwingUtilities.invokeLater(() -> {
            if (cameraStream != null) {
                cameraStream.stop();
            }
            // Cambiar a simulado
            CameraCreator simulatedCreator = new SimulatedCameraCreator();
            cameraStream = simulatedCreator.createCamera(userId, userName, roomCode);
            cameraStream.start();
            System.out.println("[RoomFrame] Fallback forzado a cámara simulada debido a fallas consecutivas.");

            // Alertar al usuario
            JOptionPane.showMessageDialog(
                    this,
                    "Se interrumpió la conexión con la cámara física debido a fallas continuas.\n" +
                            "Se activará la cámara de simulación académica.",
                    "Advertencia de Cámara",
                    JOptionPane.WARNING_MESSAGE);
        });
    }

    // =========================================================================
    // COMPONENTE DE VIDEO FEED PERSONALIZADO (Estilo Moderno)
    // =========================================================================
    private class VideoFeedPanel extends JPanel {
        private final Integer senderId;
        private final String senderName;
        private BufferedImage currentFrame;
        private String cameraState = "OFF";

        public VideoFeedPanel(Integer senderId, String senderName) {
            this.senderId = senderId;
            this.senderName = senderName;
            setPreferredSize(new Dimension(320, 240));
            setLayout(null); // Layout absoluto para overlays manuales
            setBackground(new Color(30, 30, 35));
            setBorder(BorderFactory.createLineBorder(new Color(55, 55, 60), 2, true));
        }

        public void setFrame(BufferedImage img) {
            this.currentFrame = img;
            this.cameraState = "ON";
            repaint();
        }

        public void setCameraState(String state) {
            this.cameraState = state;
            if ("OFF".equalsIgnoreCase(state)) {
                this.currentFrame = null;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int w = getWidth();
            int h = getHeight();

            if ("ON".equalsIgnoreCase(cameraState) && currentFrame != null) {
                // Dibujar el cuadro de video escalado para llenar
                g2.drawImage(currentFrame, 0, 0, w, h, null);
            } else {
                // Dibujar fondo oscuro
                g2.setColor(new Color(23, 23, 28));
                g2.fillRect(0, 0, w, h);

                // Dibujar avatar circular
                int circleRadius = 60;
                int cx = (w - circleRadius) / 2;
                int cy = (h - circleRadius) / 2 - 10;
                
                g2.setColor(new Color(45, 140, 255, 40)); // Brillo semi-transparente
                g2.fillOval(cx - 4, cy - 4, circleRadius + 8, circleRadius + 8);
                
                g2.setColor(new Color(45, 140, 255)); // Azul Zoom
                g2.fillOval(cx, cy, circleRadius, circleRadius);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                String initials = getInitials(senderName);
                FontMetrics fm = g2.getFontMetrics();
                int tx = cx + (circleRadius - fm.stringWidth(initials)) / 2;
                int ty = cy + ((circleRadius - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initials, tx, ty);

                // Texto: Cámara apagada
                g2.setColor(new Color(231, 76, 60)); // Rojo suave
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                String statusText = "🔴 Cámara apagada";
                FontMetrics fmStatus = g2.getFontMetrics();
                int sx = (w - fmStatus.stringWidth(statusText)) / 2;
                int sy = cy + circleRadius + 30;
                g2.drawString(statusText, sx, sy);
            }

            // Dibujar overlay con nombre en la parte inferior
            g2.setColor(new Color(0, 0, 0, 150)); // Fondo negro translúcido
            g2.fillRoundRect(10, h - 35, w - 20, 25, 6, 6);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            String nameText = senderName + (senderId.equals(userId) ? " (Tú)" : "");
            FontMetrics fmName = g2.getFontMetrics();
            int ny = h - 35 + ((25 - fmName.getHeight()) / 2) + fmName.getAscent();
            g2.drawString(nameText, 20, ny);
            
            // Indicador de conexión verde si la cámara está activa
            if ("ON".equalsIgnoreCase(cameraState)) {
                g2.setColor(new Color(46, 204, 113)); // Verde activo
                g2.fillOval(w - 25, h - 27, 8, 8);
            }
        }

        private String getInitials(String name) {
            if (name == null || name.isEmpty()) return "?";
            String[] parts = name.split(" ");
            if (parts.length >= 2) {
                return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
            }
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
    }
}
