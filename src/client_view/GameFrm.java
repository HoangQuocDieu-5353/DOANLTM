package client_view;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.Socket;

public class GameFrm extends JFrame {
    private Socket socket;
    private String username;

    // Constructor nhận Socket và Tên từ Login gửi sang
    public GameFrm(Socket socket, String username) {
        this.socket = socket;
        this.username = username;
        
        initUI(); // Vẽ giao diện
    }

    private void initUI() {
        this.setTitle("Sảnh Chờ Game Caro - " + username);
        this.setSize(500, 400);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(null);

        // --- Tiêu đề Chào mừng ---
        JLabel lblWelcome = new JLabel("Xin chào, " + username + "!");
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 18));
        lblWelcome.setForeground(Color.RED);
        lblWelcome.setBounds(30, 20, 300, 30);
        this.add(lblWelcome);

        // --- Nút Chức năng (Tạo khung thôi, chưa code logic) ---
        JButton btnTaoPhong = new JButton("Tạo Phòng");
        btnTaoPhong.setBounds(150, 100, 200, 40);
        this.add(btnTaoPhong);

        JButton btnTimTran = new JButton("Tìm Trận Nhanh");
        btnTimTran.setBounds(150, 160, 200, 40);
        this.add(btnTimTran);
        
        // --- Nút Đăng Xuất ---
        JButton btnDangXuat = new JButton("Đăng Xuất");
        btnDangXuat.setBounds(350, 300, 100, 30);
        this.add(btnDangXuat);

        // Xử lý nút Đăng Xuất
        btnDangXuat.addActionListener(e -> {
            try {
                socket.close(); // Đóng kết nối
                this.dispose(); // Tắt bảng này
                new LoginUI(); // Quay lại màn hình Login
            } catch (IOException ex) {}
        });

        this.setVisible(true);
    }
}