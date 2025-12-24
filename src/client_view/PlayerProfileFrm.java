package client_view;

import javax.swing.*;
import java.awt.*;

public class PlayerProfileFrm extends JFrame {

    public PlayerProfileFrm(String username, int win, int lose, int draw, int score) {
        this.setTitle("Hồ sơ người chơi: " + username);
        this.setSize(400, 500);
        this.setLocationRelativeTo(null); // Ra giữa màn hình
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Đóng form này không tắt app
        this.setLayout(new BorderLayout());

        int totalGames = win + lose + draw;
        float winRate = totalGames == 0 ? 0 : ((float) win / totalGames) * 100;

        JPanel pnlHeader = new JPanel(new GridLayout(2, 1));
        pnlHeader.setBackground(new Color(230, 240, 255)); // Màu nền xanh nhạt
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // Icon Avatar (Tạm thời dùng chữ cái đầu)
        JLabel lblAvatar = new JLabel(String.valueOf(username.charAt(0)).toUpperCase(), SwingConstants.CENTER);
        lblAvatar.setFont(new Font("Arial", Font.BOLD, 60));
        lblAvatar.setForeground(new Color(0, 102, 204));
        
        // Tên User
        JLabel lblName = new JLabel(username, SwingConstants.CENTER);
        lblName.setFont(new Font("Arial", Font.BOLD, 24));
        lblName.setForeground(Color.BLACK);

        pnlHeader.add(lblAvatar);
        pnlHeader.add(lblName);

        // --- BODY (THÔNG SỐ CHI TIẾT) ---
        JPanel pnlBody = new JPanel(new GridLayout(6, 1, 10, 10));
        pnlBody.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40)); // Căn lề 2 bên
        pnlBody.setBackground(Color.WHITE);

        pnlBody.add(createStatRow("Điểm số (Rank):", String.valueOf(score), new Color(0, 153, 0))); // Xanh lá
        pnlBody.add(createStatRow("Tổng số trận:", String.valueOf(totalGames), Color.BLACK));
        pnlBody.add(createStatRow("Số trận thắng:", String.valueOf(win), Color.BLUE));
        pnlBody.add(createStatRow("Số trận thua:", String.valueOf(lose), Color.RED));
        pnlBody.add(createStatRow("Số trận hòa:", String.valueOf(draw), Color.GRAY));
        pnlBody.add(createStatRow("Tỉ lệ thắng:", String.format("%.1f%%", winRate), new Color(255, 102, 0))); // Cam

        // --- FOOTER (NÚT ĐÓNG) ---
        JPanel pnlFooter = new JPanel();
        pnlFooter.setBackground(Color.WHITE);
        pnlFooter.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Arial", Font.BOLD, 14));
        btnClose.setPreferredSize(new Dimension(100, 35));
        btnClose.addActionListener(e -> this.dispose());
        
        pnlFooter.add(btnClose);

        // --- ADD VÀO FRAME ---
        this.add(pnlHeader, BorderLayout.NORTH);
        this.add(pnlBody, BorderLayout.CENTER);
        this.add(pnlFooter, BorderLayout.SOUTH);

        this.setVisible(true);
    }

    // Hàm phụ để tạo dòng hiển thị cho đẹp
    private JPanel createStatRow(String title, String value, Color valueColor) {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240))); // Kẻ gạch dưới mờ

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Arial", Font.PLAIN, 16));
        lblTitle.setForeground(Color.GRAY);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Arial", Font.BOLD, 18));
        lblValue.setForeground(valueColor);

        pnl.add(lblTitle, BorderLayout.WEST);
        pnl.add(lblValue, BorderLayout.EAST);
        
        return pnl;
    }
    
    // Test chạy thử giao diện một mình
    public static void main(String[] args) {
        new PlayerProfileFrm("TestUser", 15, 5, 2, 1250);
    }
}