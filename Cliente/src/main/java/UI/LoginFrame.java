package UI;

import com.google.gson.Gson;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.MensajeSocket;
import network.ClienteConexion;

public class LoginFrame extends JFrame implements ClienteConexion.MensajeListener {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JTextField txtIp;
    private JButton btnIngresar;
    private JButton btnRegistrarse;
    private JButton btnConectar;
    private JLabel lblEstadoConexion;

    // Variables de red
    private Gson gson;
    private boolean cambiandoIp = false;
    private boolean desconexionVoluntaria = false;

    public LoginFrame() {
        // Configuración básica de la ventana
        setTitle("Zoom Sockets - Iniciar Sesión");
        setSize(380, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false); // Evitamos que se deforme el diseño

        gson = new Gson();

        // --- INYECCIÓN DE DISEÑO MODERNO ---
        // Panel principal con fondo blanco y márgenes estilo "Tarjeta"
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        // 1. Título y Subtítulo
        JLabel lblTitulo = new JLabel("Bienvenido");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitulo.setForeground(new Color(41, 128, 185)); // Azul institucional
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitulo = new JLabel("Ingresa tus credenciales para continuar");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubtitulo.setForeground(Color.GRAY);
        lblSubtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 2. Definición de estilos para los campos
        Font fontLabels = new Font("Segoe UI", Font.BOLD, 12);
        Font fontCampos = new Font("Segoe UI", Font.PLAIN, 14);
        Color colorTexto = new Color(50, 50, 50);

        // 3. Campo de Usuario
        JPanel pnlUsrLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlUsrLabel.setBackground(Color.WHITE);
        JLabel lblUsuario = new JLabel("USUARIO O CORREO");
        lblUsuario.setFont(fontLabels);
        lblUsuario.setForeground(Color.DARK_GRAY);
        pnlUsrLabel.add(lblUsuario);

        txtUsuario = new JTextField();
        txtUsuario.setFont(fontCampos);
        txtUsuario.setForeground(colorTexto);
        txtUsuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // 4. Campo de Contraseña
        JPanel pnlPassLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlPassLabel.setBackground(Color.WHITE);
        JLabel lblPassword = new JLabel("CONTRASEÑA");
        lblPassword.setFont(fontLabels);
        lblPassword.setForeground(Color.DARK_GRAY);
        pnlPassLabel.add(lblPassword);

        txtPassword = new JPasswordField();
        txtPassword.setFont(fontCampos);
        txtPassword.setForeground(colorTexto);
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // 4b. Campo de IP del Servidor
        JPanel pnlIpLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlIpLabel.setBackground(Color.WHITE);
        JLabel lblIp = new JLabel("IP DEL SERVIDOR");
        lblIp.setFont(fontLabels);
        lblIp.setForeground(Color.DARK_GRAY);
        pnlIpLabel.add(lblIp);

        txtIp = new JTextField("localhost");
        txtIp.setFont(fontCampos);
        txtIp.setForeground(colorTexto);
        txtIp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // Botón de Conexión
        btnConectar = new JButton("Conectar al Servidor");
        btnConectar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConectar.setBackground(new Color(52, 73, 94));
        btnConectar.setForeground(Color.WHITE);
        btnConectar.setFocusPainted(false);
        btnConectar.setBorderPainted(false);
        btnConectar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnConectar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnConectar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (btnConectar.isEnabled()) {
                    if ("Conectar al Servidor".equals(btnConectar.getText())) {
                        btnConectar.setBackground(new Color(44, 62, 80));
                    } else {
                        btnConectar.setBackground(new Color(192, 57, 43)); // Rojo al hover en Desconectar
                    }
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (btnConectar.isEnabled()) {
                    if ("Conectar al Servidor".equals(btnConectar.getText())) {
                        btnConectar.setBackground(new Color(52, 73, 94));
                    } else {
                        btnConectar.setBackground(new Color(231, 76, 60)); // Rojo suave
                    }
                }
            }
        });

        // Etiqueta de Estado de Conexión
        JPanel pnlEstado = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        pnlEstado.setBackground(Color.WHITE);
        JLabel lblEstadoText = new JLabel("Estado:");
        lblEstadoText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEstadoText.setForeground(Color.GRAY);
        
        lblEstadoConexion = new JLabel("● Desconectado");
        lblEstadoConexion.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblEstadoConexion.setForeground(new Color(231, 76, 60)); // Rojo inicial
        pnlEstado.add(lblEstadoText);
        pnlEstado.add(lblEstadoConexion);

        // 5. Botón de Ingreso Moderno (Deshabilitado inicialmente)
        btnIngresar = new JButton("Ingresar a la Sala");
        btnIngresar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnIngresar.setBackground(new Color(41, 128, 185));
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFocusPainted(false);
        btnIngresar.setBorderPainted(false);
        btnIngresar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnIngresar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnIngresar.setEnabled(false);

        // Efecto visual Hover para el botón
        btnIngresar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (btnIngresar.isEnabled()) {
                    btnIngresar.setBackground(new Color(52, 152, 219));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (btnIngresar.isEnabled()) {
                    btnIngresar.setBackground(new Color(41, 128, 185));
                }
            }
        });

        // 6. Ensamblaje del panel con separadores espaciadores
        mainPanel.add(lblTitulo);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(lblSubtitulo);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        mainPanel.add(pnlUsrLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(txtUsuario);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        mainPanel.add(pnlPassLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(txtPassword);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        mainPanel.add(pnlIpLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(txtIp);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        mainPanel.add(btnConectar);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(pnlEstado);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        mainPanel.add(btnIngresar);

        // Panel para Registrarse (estilo Zoom moderno)
        JPanel pnlRegistrarse = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        pnlRegistrarse.setBackground(Color.WHITE);
        JLabel lblNoCuenta = new JLabel("¿No tienes cuenta?");
        lblNoCuenta.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblNoCuenta.setForeground(Color.GRAY);
        
        btnRegistrarse = new JButton("Registrarse");
        btnRegistrarse.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRegistrarse.setBorder(null);
        btnRegistrarse.setContentAreaFilled(false);
        btnRegistrarse.setCursor(new Cursor(Cursor.HAND_CURSOR));
        setRegistrarseEnabled(false); // Deshabilitado inicialmente
        
        btnRegistrarse.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (btnRegistrarse.isEnabled()) {
                    btnRegistrarse.setForeground(new Color(52, 152, 219));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (btnRegistrarse.isEnabled()) {
                    btnRegistrarse.setForeground(new Color(41, 128, 185));
                }
            }
        });
        
        pnlRegistrarse.add(lblNoCuenta);
        pnlRegistrarse.add(btnRegistrarse);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(pnlRegistrarse);

        add(mainPanel);

        // --- REGISTRO DE EVENTOS ---
        btnConectar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!ClienteConexion.getInstancia().isConectado()) {
                    iniciarConexion();
                } else {
                    desconectarServidor();
                }
            }
        });

        btnIngresar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ejecutarLogin();
            }
        });

        btnRegistrarse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnRegistrarse.isEnabled()) {
                    RegisterFrame registerFrame = new RegisterFrame(LoginFrame.this);
                    registerFrame.setVisible(true);
                    LoginFrame.this.setVisible(false);
                }
            }
        });

        // Oyente de cambios de IP para reiniciar conexión si se modifica la IP
        txtIp.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { resetearConexion(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { resetearConexion(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { resetearConexion(); }
            
            private void resetearConexion() {
                if (ClienteConexion.getInstancia().isConectado()) {
                    desconectarServidor();
                }
            }
        });
    }

    private void setRegistrarseEnabled(boolean enabled) {
        btnRegistrarse.setEnabled(enabled);
        if (enabled) {
            btnRegistrarse.setForeground(new Color(41, 128, 185));
        } else {
            btnRegistrarse.setForeground(Color.LIGHT_GRAY);
        }
    }

    private void iniciarConexion() {
        String ip = txtIp.getText().trim();
        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese la IP del servidor.", "IP Vacía",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Cambiar interfaz a Conectando...
        btnConectar.setEnabled(false);
        btnConectar.setText("Conectando...");
        lblEstadoConexion.setText("● Conectando...");
        lblEstadoConexion.setForeground(new Color(241, 196, 15)); // Amarillo
        txtIp.setEnabled(false);

        new Thread(() -> {
            ClienteConexion.getInstancia().setListener(this);
            boolean exito = ClienteConexion.getInstancia().conectar(ip, 5000);
            
            SwingUtilities.invokeLater(() -> {
                btnConectar.setEnabled(true);
                if (exito) {
                    btnConectar.setText("Desconectar");
                    btnConectar.setBackground(new Color(231, 76, 60)); // Rojo
                    lblEstadoConexion.setText("● Conectado");
                    lblEstadoConexion.setForeground(new Color(46, 204, 113)); // Verde
                    
                    btnIngresar.setEnabled(true);
                    setRegistrarseEnabled(true);
                } else {
                    btnConectar.setText("Conectar al Servidor");
                    btnConectar.setBackground(new Color(52, 73, 94));
                    lblEstadoConexion.setText("● Desconectado");
                    lblEstadoConexion.setForeground(new Color(231, 76, 60)); // Rojo
                    txtIp.setEnabled(true);
                    
                    btnIngresar.setEnabled(false);
                    setRegistrarseEnabled(false);
                    
                    JOptionPane.showMessageDialog(this,
                            "✗ No se pudo conectar al servidor en " + ip + ".\n\n" +
                            "Verifique si:\n" +
                            "- El servidor está encendido.\n" +
                            "- La IP ingresada es correcta.\n" +
                            "- Hay problemas de red o firewall.",
                            "Error de Conexión",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    private void desconectarServidor() {
        desconexionVoluntaria = true;
        ClienteConexion.getInstancia().desconectar();
    }

    private void ejecutarLogin() {
        String usuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (usuario.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese usuario y contraseña.", "Campos Vacíos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!ClienteConexion.getInstancia().isConectado()) {
            JOptionPane.showMessageDialog(this, "Debe conectarse al servidor primero.", "Sin Conexión",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Desactivar botón temporalmente para evitar doble clic
        btnIngresar.setEnabled(false);
        btnIngresar.setText("Verificando...");

        // Empaquetamos los datos usando la clase del paquete modelo
        MensajeSocket solicitudLogin = new MensajeSocket();
        solicitudLogin.setType("LOGIN_REQUEST");
        solicitudLogin.setUserName(usuario);
        solicitudLogin.setMessage(password);

        // Enviamos el JSON al servidor a través del manejador único
        ClienteConexion.getInstancia().enviarMensaje(solicitudLogin);
        System.out.println("[->] Enviada solicitud de login para: " + usuario);
    }

    @Override
    public void onMensajeRecibido(MensajeSocket mensaje) {
        if ("LOGIN_RESPONSE".equalsIgnoreCase(mensaje.getType())) {
            // Regresar al hilo de Swing para actualizar la UI
            SwingUtilities.invokeLater(() -> {
                btnIngresar.setEnabled(true);
                btnIngresar.setText("Ingresar a la Sala");

                if ("SUCCESS".equalsIgnoreCase(mensaje.getMessage())) {
                    JOptionPane.showMessageDialog(this,
                            "¡Inicio de sesión exitoso! Bienvenido " + mensaje.getUserName(),
                            "Acceso Permitido",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Abrir la sala principal pasando el ID y Nombre del usuario
                    RoomFrame roomFrame = new RoomFrame(mensaje.getUserId(), mensaje.getUserName());
                    roomFrame.setVisible(true);
                    this.dispose();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Error: " + mensaje.getMessage(),
                            "Acceso Denegado",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    @Override
    public void onDesconexion() {
        SwingUtilities.invokeLater(() -> {
            btnConectar.setEnabled(true);
            btnConectar.setText("Conectar al Servidor");
            btnConectar.setBackground(new Color(52, 73, 94));
            lblEstadoConexion.setText("● Desconectado");
            lblEstadoConexion.setForeground(new Color(231, 76, 60)); // Rojo
            txtIp.setEnabled(true);
            
            btnIngresar.setEnabled(false);
            btnIngresar.setText("Ingresar a la Sala");
            setRegistrarseEnabled(false);

            if (!desconexionVoluntaria && !cambiandoIp) {
                JOptionPane.showMessageDialog(this,
                        "Se ha perdido la conexión con el servidor.",
                        "Desconectado",
                        JOptionPane.WARNING_MESSAGE);
            }
            desconexionVoluntaria = false;
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}