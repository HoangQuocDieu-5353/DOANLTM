package client_view; 

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class LoginUI extends JFrame implements ActionListener {
    // 1. Khai báo các linh kiện
    private JLabel lblTitle;
    private JLabel lblUser;
    private JLabel lblPass;
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnLogin;
    private JButton btnRegister;

    public LoginUI() {
        initUI();
    }

    // Hàm dựng giao diện (Giữ nguyên như bro viết, rất đẹp!)
    private void initUI() {
        this.setTitle("Đăng Nhập Game Caro");
        this.setSize(400, 300);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(null);

        // --- Tiêu đề ---
        lblTitle = new JLabel("GAME CARO ONLINE");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitle.setForeground(Color.BLUE);
        lblTitle.setBounds(90, 20, 250, 30);
        this.add(lblTitle);

        // --- Ô Username ---
        lblUser = new JLabel("Tài khoản:");
        lblUser.setBounds(30, 80, 80, 25);
        this.add(lblUser);

        txtUser = new JTextField();
        txtUser.setBounds(110, 80, 230, 25);
        this.add(txtUser);

        // --- Ô Password ---
        lblPass = new JLabel("Mật khẩu:");
        lblPass.setBounds(30, 120, 80, 25);
        this.add(lblPass);

        txtPass = new JPasswordField();
        txtPass.setBounds(110, 120, 230, 25);
        this.add(txtPass);

        // --- Nút Đăng nhập ---
        btnLogin = new JButton("Đăng Nhập");
        btnLogin.setBounds(110, 170, 100, 30);
        btnLogin.addActionListener(this);
        this.add(btnLogin);

        // --- Nút Đăng ký ---
        btnRegister = new JButton("Đăng Ký");
        btnRegister.setBounds(240, 170, 100, 30);
        btnRegister.addActionListener(this);
        this.add(btnRegister);
        
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnLogin) {
            xuLyDangNhap();
        } else if (e.getSource() == btnRegister) {
            JOptionPane.showMessageDialog(this, "Chức năng đăng ký đang phát triển!");
        }
    }

    // --- PHẦN QUAN TRỌNG ĐÃ CẬP NHẬT ---
    private void xuLyDangNhap() {
        try {
            String user = txtUser.getText();
            String pass = new String(txtPass.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Chưa nhập tài khoản/mật khẩu!");
                return;
            }

            // 1. Kết nối Server
            // Lưu ý: Nếu Server chạy máy khác thì đổi "localhost" thành IP Server
            Socket socket = new Socket("localhost", 5555);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // 2. Gửi lệnh Login
            out.writeUTF("LOGIN|" + user + "|" + pass);
            out.flush();

            // 3. Nhận kết quả
            String response = in.readUTF(); 
            String[] parts = response.split("\\|");
            
            // 4. Xử lý kết quả
            if (parts[0].equals("LOGIN_OK")) {
                String myName = parts[1];
                JOptionPane.showMessageDialog(this, "Đăng nhập thành công! Xin chào " + myName);
                
                // --- CHUYỂN CẢNH ---
                this.dispose(); // Đóng cửa sổ Login lại
                
                // Mở màn hình Game và TRAO SOCKET cho nó
                // (Đảm bảo bro đã tạo file GameFrm.java như hướng dẫn trước nhé)
                new GameFrm(socket, myName); 
                
                // LƯU Ý: KHÔNG ĐƯỢC socket.close() Ở ĐÂY!!!
                
            } else {
                JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu!");
                socket.close(); // Chỉ đóng kết nối khi thất bại
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối Server! Kiểm tra lại xem Server bật chưa?");
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new LoginUI();
    }
}