package client_view;

import javax.swing.*;
import java.awt.*;
import java.io.DataOutputStream;
import java.net.Socket;

public class WaitingRoomFrm extends JDialog {
    private Socket socket;
    private String roomId;
    
    // --- BIẾN ĐẾM NGƯỢC ---
    private Timer timer;
    private int count = 15; // 15 giây
    private JLabel lblCountDown;
    private boolean isQuickPlayMode = false;
    private Frame owner; // Lưu lại để dùng cho dialog hỏi lại

    public WaitingRoomFrm(Frame owner, Socket socket, String roomId, boolean isQuickPlay) {
        super(owner, "Đang đợi người chơi...", false); // false = Không chặn hoàn toàn (để còn update UI)
        this.owner = owner;
        this.socket = socket;
        this.roomId = roomId;
        this.isQuickPlayMode = isQuickPlay;
        
        this.setSize(350, 200);
        this.setLayout(new BorderLayout());
        this.setLocationRelativeTo(owner);
        // Khi đóng bằng X thì cũng coi như Hủy phòng
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); 
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                cancelRoom(); // Gọi hủy khi đóng
            }
        });

        // UI chính
        JLabel lblInfo = new JLabel("<html><center>Mã Phòng: <font color='red'>" + roomId + "</font><br>Đang chờ đối thủ...</center></html>", SwingConstants.CENTER);
        lblInfo.setFont(new Font("Arial", Font.BOLD, 16));
        this.add(lblInfo, BorderLayout.CENTER);

        // Nút Hủy thủ công
        JButton btnCancel = new JButton("Hủy Phòng");
        btnCancel.addActionListener(e -> {
            cancelRoom();
            this.dispose();
        });
        this.add(btnCancel, BorderLayout.SOUTH);
        
        // --- LOGIC ĐẾM NGƯỢC CHO CHẾ ĐỘ CHƠI NHANH ---
        if (isQuickPlayMode) {
            lblCountDown = new JLabel("Tự động hủy sau: 15s", SwingConstants.CENTER);
            lblCountDown.setForeground(Color.RED);
            lblCountDown.setFont(new Font("Arial", Font.ITALIC, 12));
            this.add(lblCountDown, BorderLayout.NORTH);
            
            startCountdown();
        }
    }
    
    private void startCountdown() {
        timer = new Timer(1000, e -> {
            count--;
            lblCountDown.setText("Tự động hủy sau: " + count + "s");
            
            if (count <= 0) {
                ((Timer)e.getSource()).stop();
                this.dispose(); // Đóng form chờ
                handleTimeout(); // Xử lý hết giờ
            }
        });
        timer.start();
    }
    
    // Hủy phòng trên Server
    private void cancelRoom() {
        if (timer != null && timer.isRunning()) timer.stop();
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("CANCEL_ROOM");
            out.flush();
        } catch (Exception ex) {}
    }
    
    // Xử lý khi hết 15s (Chỉ dùng cho Quick Play)
    private void handleTimeout() {
        // Hỏi người chơi muốn thử lại không
        int choice = JOptionPane.showConfirmDialog(owner, 
                "Không tìm thấy đối thủ!\nBạn có muốn thử tìm lại không?", 
                "Hết thời gian", JOptionPane.YES_NO_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            // Gửi lại lệnh Quick Play
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF("QUICK_PLAY");
                out.flush();
            } catch (Exception ex) {}
        }
        // Nếu chọn No thì thôi, về màn hình chính (GameFrm đã hiện sẵn ở dưới rồi)
    }
}