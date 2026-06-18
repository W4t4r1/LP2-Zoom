package UI;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import model.MensajeSocket;
import network.ClienteConexion;

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

    private Gson gson;

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

        JButton btnSalir = new JButton("Abandonar Sala");
        btnSalir.setBackground(new Color(192, 57, 43));
        btnSalir.setForeground(Color.WHITE);
        btnSalir.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSalir.setFocusPainted(false);
        btnSalir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSalir.addActionListener(e -> abandonarReunion());
        pnlHeader.add(btnSalir, BorderLayout.EAST);

        panel.add(pnlHeader, BorderLayout.NORTH);

        // Panel Central Dividido
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(new Color(30, 30, 35));
        splitPane.setBorder(null);
        splitPane.setDividerLocation(480);
        splitPane.setResizeWeight(0.6);

        // Lado Izquierdo: Video Grid Placeholder (Fase 6)
        pnlVideoGrid = new JPanel(new BorderLayout());
        pnlVideoGrid.setBackground(new Color(20, 20, 25));
        pnlVideoGrid.setBorder(new LineBorder(new Color(45, 45, 50), 1, true));
        
        JLabel lblVideoPlaceholder = new JLabel("Cámaras Activas (Próxima Fase 6)", JLabel.CENTER);
        lblVideoPlaceholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblVideoPlaceholder.setForeground(Color.LIGHT_GRAY);
        pnlVideoGrid.add(lblVideoPlaceholder, BorderLayout.CENTER);

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
        // Transitamos al Host a la pantalla de reunión directamente
        lblTituloReunion.setText("Sala de Reunión Activa: Código " + roomCode);
        cardLayout.show(mainContainer, "REUNION");
        
        // Notificar en el chat del Host
        txtAreaChat.append("[SISTEMA] Has iniciado la reunión. ¡Bienvenidos!\n");
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
                double guestIdDouble = (double) sol.get("idUsuario");
                int guestId = (int) guestIdDouble;
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
                lblTituloReunion.setText("Sala de Reunión Activa: Código " + roomCode);
                cardLayout.show(mainContainer, "REUNION");
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

    @Override
    public void onDesconexion() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Se ha perdido la conexión con el servidor. Saliendo...", "Desconectado", JOptionPane.ERROR_MESSAGE);
            this.dispose();
            System.exit(0);
        });
    }
}
