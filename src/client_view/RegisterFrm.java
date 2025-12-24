package client_view;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class RegisterFrm extends JFrame {
    private JTextField txtUser;
    private JTextField txtNickname; 
    private JPasswordField txtPass;
    private JButton btnRegister, btnLogin;

    public RegisterFrm() {
        this.setTitle("Đăng Ký Tài Khoản");
        this.setSize(400, 350); 
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(null);
        JLabel lblTitle = new JLabel("ĐĂNG KÝ");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitle.setForeground(Color.RED);
        lblTitle.setBounds(150, 20, 100, 30);
        this.add(lblTitle);
        JLabel lblUser = new JLabel("Tài khoản:");
        lblUser.setBounds(50, 70, 80, 25);
        this.add(lblUser);
        txtUser = new JTextField();
        txtUser.setBounds(130, 70, 200, 25);
        this.add(txtUser);
        JLabel lblNick = new JLabel("Nickname:");
        lblNick.setBounds(50, 110, 80, 25);
        this.add(lblNick);
        txtNickname = new JTextField();
        txtNickname.setBounds(130, 110, 200, 25);
        this.add(txtNickname);

        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setBounds(50, 150, 80, 25);
        this.add(lblPass);
        txtPass = new JPasswordField();
        txtPass.setBounds(130, 150, 200, 25);
        this.add(txtPass);

        // --- 4. Các nút bấm ---
        btnRegister = new JButton("Đăng Ký");
        btnRegister.setBounds(130, 200, 90, 30);
        this.add(btnRegister);

        btnLogin = new JButton("Về đăng nhập");
        btnLogin.setBounds(230, 200, 110, 30);
        this.add(btnLogin);

        // --- SỰ KIỆN ---
        btnRegister.addActionListener(e -> xuLyDangKy());

        btnLogin.addActionListener(e -> {
            this.dispose();
            new LoginUI(); 
        });

        this.setVisible(true);
    }

    private void xuLyDangKy() {
        try {
            String user = txtUser.getText().trim();
            String nick = txtNickname.getText().trim(); 
            String pass = new String(txtPass.getPassword()).trim();

            if (user.isEmpty() || nick.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!");
                return;
            }

            // 1. Cấu hình TrustStore (Để tin tưởng Server SSL)
            System.setProperty("javax.net.ssl.trustStore", "carokeystore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "123456789");

            // 2. Tạo kết nối SSL (Thay cho Socket thường)
            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) sslFactory.createSocket("localhost", 5555); // Port 5555
            
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // 3. Gửi lệnh đăng ký
            out.writeUTF("REGISTER|" + user + "|" + pass + "|" + nick);
            out.flush();

            // 4. Nhận phản hồi
            String response = in.readUTF();
            
            if (response.equals("REGISTER_OK")) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công! Hãy đăng nhập.");
                this.dispose();
                new LoginUI(); 
            } else if (response.equals("EXISTED")) {
                JOptionPane.showMessageDialog(this, "Tài khoản đã tồn tại!");
            } else {
                JOptionPane.showMessageDialog(this, "Đăng ký thất bại: " + response);
            }
            
            socket.close(); 
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối SSL: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}