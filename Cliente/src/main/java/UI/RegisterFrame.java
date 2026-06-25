package UI;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.MensajeSocket;
import network.ClienteConexion;

public class RegisterFrame extends JFrame implements ClienteConexion.MensajeListener {

    private JTextField txtNombres;
    private JTextField txtCorreo;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JButton btnRegistrar;
    private JButton btnVolver;
    private LoginFrame parent;

    public RegisterFrame(LoginFrame parent) {
        this.parent = parent;

        // Configuración básica de la ventana
        setTitle("Zoom Sockets - Registrarse");
        setSize(400, 620);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Al cerrar desde el botón de la ventana (X), regresamos al login
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                volverAlLogin();
            }
        });

        // --- INYECCIÓN DE DISEÑO MODERNO (Estilo Zoom) ---
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        // 1. Título y Subtítulo
        JLabel lblTitulo = new JLabel("Crear Cuenta");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitulo.setForeground(new Color(41, 128, 185)); // Azul institucional
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitulo = new JLabel("Regístrate para empezar a usar la aplicación");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubtitulo.setForeground(Color.GRAY);
        lblSubtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        Font fontLabels = new Font("Segoe UI", Font.BOLD, 11);
        Font fontCampos = new Font("Segoe UI", Font.PLAIN, 13);
        Color colorTexto = new Color(50, 50, 50);

        // 2. Campo: Nombres Completos
        JPanel pnlNombresLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlNombresLabel.setBackground(Color.WHITE);
        JLabel lblNombres = new JLabel("NOMBRES COMPLETOS");
        lblNombres.setFont(fontLabels);
        lblNombres.setForeground(Color.DARK_GRAY);
        pnlNombresLabel.add(lblNombres);

        txtNombres = new JTextField();
        txtNombres.setFont(fontCampos);
        txtNombres.setForeground(colorTexto);
        txtNombres.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // 3. Campo: Correo Electrónico
        JPanel pnlCorreoLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlCorreoLabel.setBackground(Color.WHITE);
        JLabel lblCorreo = new JLabel("CORREO ELECTRÓNICO");
        lblCorreo.setFont(fontLabels);
        lblCorreo.setForeground(Color.DARK_GRAY);
        pnlCorreoLabel.add(lblCorreo);

        txtCorreo = new JTextField();
        txtCorreo.setFont(fontCampos);
        txtCorreo.setForeground(colorTexto);
        txtCorreo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // 4. Campo: Contraseña
        JPanel pnlPassLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlPassLabel.setBackground(Color.WHITE);
        JLabel lblPassword = new JLabel("CONTRASEÑA (MÍNIMO 8 CARACTERES)");
        lblPassword.setFont(fontLabels);
        lblPassword.setForeground(Color.DARK_GRAY);
        pnlPassLabel.add(lblPassword);

        txtPassword = new JPasswordField();
        txtPassword.setFont(fontCampos);
        txtPassword.setForeground(colorTexto);
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // 5. Campo: Confirmar Contraseña
        JPanel pnlConfPassLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlConfPassLabel.setBackground(Color.WHITE);
        JLabel lblConfPassword = new JLabel("CONFIRMAR CONTRASEÑA");
        lblConfPassword.setFont(fontLabels);
        lblConfPassword.setForeground(Color.DARK_GRAY);
        pnlConfPassLabel.add(lblConfPassword);

        txtConfirmPassword = new JPasswordField();
        txtConfirmPassword.setFont(fontCampos);
        txtConfirmPassword.setForeground(colorTexto);
        txtConfirmPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // 6. Botón: Crear Cuenta
        btnRegistrar = new JButton("Crear cuenta");
        btnRegistrar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnRegistrar.setBackground(new Color(41, 128, 185));
        btnRegistrar.setForeground(Color.WHITE);
        btnRegistrar.setFocusPainted(false);
        btnRegistrar.setBorderPainted(false);
        btnRegistrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnRegistrar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnRegistrar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (btnRegistrar.isEnabled()) {
                    btnRegistrar.setBackground(new Color(52, 152, 219));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (btnRegistrar.isEnabled()) {
                    btnRegistrar.setBackground(new Color(41, 128, 185));
                }
            }
        });

        // 7. Botón: Volver al Login
        btnVolver = new JButton("Volver al Login");
        btnVolver.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnVolver.setForeground(Color.GRAY);
        btnVolver.setBorder(null);
        btnVolver.setContentAreaFilled(false);
        btnVolver.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVolver.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnVolver.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnVolver.setForeground(Color.DARK_GRAY);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnVolver.setForeground(Color.GRAY);
            }
        });

        // Ensamblar paneles
        mainPanel.add(lblTitulo);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(lblSubtitulo);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 25)));

        mainPanel.add(pnlNombresLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(txtNombres);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        mainPanel.add(pnlCorreoLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(txtCorreo);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        mainPanel.add(pnlPassLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(txtPassword);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        mainPanel.add(pnlConfPassLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(txtConfirmPassword);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 25)));

        mainPanel.add(btnRegistrar);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(btnVolver);

        add(mainPanel);

        // --- REGISTRO DE EVENTOS DE RED ---
        ClienteConexion.getInstancia().setListener(this);

        btnRegistrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ejecutarRegistro();
            }
        });

        btnVolver.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                volverAlLogin();
            }
        });
    }

    private void ejecutarRegistro() {
        String nombres = txtNombres.getText().trim();
        String correo = txtCorreo.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String confirmPassword = new String(txtConfirmPassword.getPassword()).trim();

        // 1. Validar campos vacíos
        if (nombres.isEmpty() || correo.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, complete todos los campos.", "Campos vacíos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Validar formato de correo electrónico
        if (!correo.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            JOptionPane.showMessageDialog(this, "El formato del correo electrónico ingresado no es válido.", "Formato inválido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 3. Validar longitud mínima de la contraseña
        if (password.length() < 8) {
            JOptionPane.showMessageDialog(this, "La contraseña debe tener un mínimo de 8 caracteres.", "Contraseña débil",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 4. Validar coincidencia de contraseñas
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden. Verifique e intente de nuevo.", "Error de coincidencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 5. Validar que la conexión esté activa
        if (!ClienteConexion.getInstancia().isConectado()) {
            JOptionPane.showMessageDialog(this, "No hay conexión activa con el servidor. Por favor, vuelva al login para reconectarse.", "Error de red",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Desactivar UI temporalmente
        btnRegistrar.setEnabled(false);
        btnRegistrar.setText("Registrando...");

        // Construir el mensaje de registro
        MensajeSocket req = new MensajeSocket();
        req.setType("REGISTER_REQUEST");
        req.setUserName(correo);
        req.setMessage(password);
        req.setFullName(nombres);

        // Enviar
        ClienteConexion.getInstancia().enviarMensaje(req);
        System.out.println("[->] Enviando REGISTER_REQUEST para: " + correo);
    }

    private void volverAlLogin() {
        if (parent != null) {
            parent.setVisible(true);
            ClienteConexion.getInstancia().setListener(parent);
        }
        this.dispose();
    }

    @Override
    public void onMensajeRecibido(MensajeSocket mensaje) {
        if ("REGISTER_RESPONSE".equalsIgnoreCase(mensaje.getType())) {
            SwingUtilities.invokeLater(() -> {
                btnRegistrar.setEnabled(true);
                btnRegistrar.setText("Crear cuenta");

                if ("SUCCESS".equalsIgnoreCase(mensaje.getMessage())) {
                    JOptionPane.showMessageDialog(this,
                            "✓ Cuenta creada correctamente\n\nAhora puedes iniciar sesión.",
                            "Registro Exitoso",
                            JOptionPane.INFORMATION_MESSAGE);
                    volverAlLogin();
                } else if ("EMAIL_ALREADY_EXISTS".equalsIgnoreCase(mensaje.getMessage())) {
                    JOptionPane.showMessageDialog(this,
                            "El correo electrónico ya se encuentra registrado. Utilice otro.",
                            "Correo Duplicado",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Error al registrar: " + mensaje.getMessage(),
                            "Error de Registro",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    @Override
    public void onDesconexion() {
        SwingUtilities.invokeLater(() -> {
            btnRegistrar.setEnabled(true);
            btnRegistrar.setText("Crear cuenta");
            JOptionPane.showMessageDialog(this,
                    "Se ha perdido la conexión con el servidor.",
                    "Desconectado",
                    JOptionPane.WARNING_MESSAGE);
            volverAlLogin();
        });
    }
}
