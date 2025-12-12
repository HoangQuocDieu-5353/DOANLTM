package client_view;

import client_controller.ClientListener;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataOutputStream; // Import thêm cái này để gửi tin
import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

public class GameFrm extends JFrame {
    private Socket socket;
    private String username;
    
    // Linh kiện danh sách
    private JList<String> listOnline;
    private DefaultListModel<String> listModel;

    public GameFrm(Socket socket, String username) {
        this.socket = socket;
        this.username = username;
        
        initUI();
        
        // Kích hoạt luồng lắng nghe
        ClientListener listener = new ClientListener(socket, this);
        listener.start();
    }

    private void initUI() {
        this.setTitle("Sảnh Chờ Game Caro - " + username);
        this.setSize(600, 450);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(null);
        this.setResizable(false);

        // --- CỘT TRÁI: DANH SÁCH ONLINE ---
        JLabel lblList = new JLabel("Danh sách người chơi:");
        lblList.setFont(new Font("Arial", Font.BOLD, 14));
        lblList.setBounds(20, 20, 200, 20);
        this.add(lblList);

        listModel = new DefaultListModel<>();
        listOnline = new JList<>(listModel);
        listOnline.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // Sự kiện click đúp để thách đấu nhanh
        listOnline.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    guiLoiMoi(); // Gọi hàm gửi lời mời
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(listOnline);
        scrollPane.setBounds(20, 50, 230, 320);
        this.add(scrollPane);

        // --- CỘT PHẢI: CHỨC NĂNG ---
        JLabel lblWelcome = new JLabel("Xin chào, " + username + "!");
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 18));
        lblWelcome.setForeground(Color.RED);
        lblWelcome.setBounds(280, 20, 300, 30);
        this.add(lblWelcome);

        JButton btnTaoPhong = new JButton("Tạo Phòng");
        btnTaoPhong.setBounds(280, 80, 200, 40);
        btnTaoPhong.setFont(new Font("Arial", Font.PLAIN, 14));
        this.add(btnTaoPhong);

        JButton btnTimTran = new JButton("Tìm Trận Nhanh");
        btnTimTran.setBounds(280, 140, 200, 40);
        btnTimTran.setFont(new Font("Arial", Font.PLAIN, 14));
        this.add(btnTimTran);
        
        // --- NÚT THÁCH ĐẤU (ĐÃ CẬP NHẬT LOGIC) ---
        JButton btnThachDau = new JButton("Thách Đấu");
        btnThachDau.setBounds(280, 200, 200, 40);
        btnThachDau.setFont(new Font("Arial", Font.PLAIN, 14));
        btnThachDau.addActionListener(e -> guiLoiMoi());
        this.add(btnThachDau);
        
        // Nút Đăng Xuất
        JButton btnDangXuat = new JButton("Đăng Xuất");
        btnDangXuat.setBounds(280, 330, 200, 40);
        btnDangXuat.setBackground(Color.PINK);
        this.add(btnDangXuat);

        btnDangXuat.addActionListener(e -> {
            try {
                socket.close();
                this.dispose();
                new LoginUI();
            } catch (IOException ex) {}
        });

        this.setVisible(true);
    }
    
    // --- HÀM GỬI LỜI MỜI (Tách riêng ra cho gọn) ---
    private void guiLoiMoi() {
        String userSelected = listOnline.getSelectedValue();
        
        if(userSelected == null) {
            JOptionPane.showMessageDialog(this, "Bạn chưa chọn người chơi nào!");
            return;
        }
        
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            
            // Gửi lệnh: INVITE|Tên_Người_Bị_Mời
            out.writeUTF("INVITE|" + userSelected);
            out.flush();
            
            JOptionPane.showMessageDialog(this, "Đã gửi lời mời tới " + userSelected + ".\nĐang chờ phản hồi...");
            
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi lời mời!");
        }
    }
    
    // Hàm cập nhật danh sách (Được gọi từ ClientListener)
    public void updateOnlineList(Vector<String> users) {
        listModel.clear();
        for (String u : users) {
            if(!u.equals(username)){
                listModel.addElement(u);
            }
        }
    }
    public String getUsername() {
        return this.username;
    }
}