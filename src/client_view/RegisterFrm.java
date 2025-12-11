package client_view;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class RegisterFrm extends JFrame {
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnRegister, btnLogin;

    public RegisterFrm() {
        this.setTitle("Đăng Ký Tài Khoản");
        this.setSize(400, 300);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(null);

        // Title
        JLabel lblTitle = new JLabel("ĐĂNG KÝ");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitle.setForeground(Color.RED);
        lblTitle.setBounds(150, 20, 100, 30);
        this.add(lblTitle);

        // User
        JLabel lblUser = new JLabel("Tài khoản:");
        lblUser.setBounds(50, 80, 80, 25);
        this.add(lblUser);
        txtUser = new JTextField();
        txtUser.setBounds(130, 80, 200, 25);
        this.add(txtUser);

        // Pass
        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setBounds(50, 120, 80, 25);
        this.add(lblPass);
        txtPass = new JPasswordField();
        txtPass.setBounds(130, 120, 200, 25);
        this.add(txtPass);

        // Button Register
        btnRegister = new JButton("Đăng Ký");
        btnRegister.setBounds(130, 170, 90, 30);
        this.add(btnRegister);

        // Button Back to Login
        btnLogin = new JButton("Đã có tài khoản");
        btnLogin.setBounds(230, 170, 100, 30);
        this.add(btnLogin);

        
        // 1. Bấm nút Đăng Ký
        btnRegister.addActionListener(e -> xuLyDangKy());

        // 2. Bấm nút Quay lại đăng nhập
        btnLogin.addActionListener(e -> {
            this.dispose();
            new LoginUI(); // Mở lại màn hình Login
        });

        this.setVisible(true);
    }

    private void xuLyDangKy() {
        try {
            String user = txtUser.getText();
            String pass = new String(txtPass.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không được để trống!");
                return;
            }

            // Kết nối Server (Nhớ sửa IP nếu chạy khác máy)
            Socket socket = new Socket("localhost", 5555);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // Gửi lệnh: REGISTER|user|pass
            out.writeUTF("REGISTER|" + user + "|" + pass);
            out.flush();

            // Nhận phản hồi
            String response = in.readUTF();
            
            if (response.equals("REGISTER_OK")) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công! Hãy đăng nhập.");
                this.dispose();
                new LoginUI(); // Chuyển về màn hình đăng nhập
            } else {
                JOptionPane.showMessageDialog(this, "Tài khoản đã tồn tại!");
            }
            
            socket.close(); // Đăng ký xong thì đóng kết nối luôn
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối Server!");
            ex.printStackTrace();
        }
    }
}