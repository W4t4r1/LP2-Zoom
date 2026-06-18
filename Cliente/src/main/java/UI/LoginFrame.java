package UI;

import com.google.gson.Gson;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.MensajeSocket;

public class LoginFrame extends JFrame {
    
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnIngresar;
    
    // Variables de red
    private Socket socket;
    private PrintWriter salida;
    private Gson gson;

    public LoginFrame() {
        // Configuración básica de la ventana
        setTitle("Zoom Sockets - Iniciar Sesión");
        setSize(380, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        setResizable(false); // Evitamos que se deforme el diseño

        gson = new Gson();

        // --- INYECCIÓN DE DISEÑO MODERNO ---
        // Panel principal con fondo blanco y márgenes estilo "Tarjeta"
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(40, 40, 40, 40));

        // 1. Título y Subtítulo
        JLabel lblTitulo = new JLabel("Bienvenido");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitulo.setForeground(new Color(41, 128, 185)); // Azul institucional
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitulo = new JLabel("Ingresa tus credenciales para continuar");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
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

        // 5. Botón de Ingreso Moderno
        btnIngresar = new JButton("Ingresar a la Sala");
        btnIngresar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnIngresar.setBackground(new Color(41, 128, 185));
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFocusPainted(false);
        btnIngresar.setBorderPainted(false);
        btnIngresar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnIngresar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Efecto visual Hover para el botón
        btnIngresar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnIngresar.setBackground(new Color(52, 152, 219)); // Azul más claro
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnIngresar.setBackground(new Color(41, 128, 185)); // Regresa al original
            }
        });

        // 6. Ensamblaje del panel con separadores espaciadores
        mainPanel.add(lblTitulo);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(lblSubtitulo);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 40)));
        
        mainPanel.add(pnlUsrLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(txtUsuario);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        mainPanel.add(pnlPassLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(txtPassword);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 40)));
        
        mainPanel.add(btnIngresar);

        add(mainPanel);

        // --- LÓGICA DE RED Y EVENTOS ---
        conectarAlServidor();

        btnIngresar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ejecutarLogin();
            }
        });
    }

    private void conectarAlServidor() {
        try {
            socket = new Socket("localhost", 5000);
            salida = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("[+] Conectado exitosamente al servidor de Sockets.");
        } catch (Exception e) {
            System.err.println("[-] No se pudo conectar al servidor: " + e.getMessage());
        }
    }

    private void ejecutarLogin() {
        String usuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (usuario.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, complete todos los campos.", "Campos Vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Empaquetamos los datos usando la clase del paquete modelo
        MensajeSocket solicitudLogin = new MensajeSocket();
        solicitudLogin.setType("LOGIN_REQUEST");
        solicitudLogin.setUserName(usuario);
        solicitudLogin.setMessage(password);

        // Convertimos el objeto Java a una cadena JSON
        String jsonPayload = gson.toJson(solicitudLogin);

        // Enviamos el JSON al servidor
        if (salida != null) {
            salida.println(jsonPayload);
            System.out.println("[->] Enviado al Servidor: " + jsonPayload);
        } else {
            JOptionPane.showMessageDialog(this, "Sin conexión con el servidor. Enciéndalo primero.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}