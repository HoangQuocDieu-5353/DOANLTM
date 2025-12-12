package client_view;

import javax.swing.*;
import java.awt.*;
import java.io.DataOutputStream;
import java.net.Socket;

public class GameBoardFrm extends JFrame {
    private Socket socket;
    private String myName;
    private String competitorName;
    
    // Logic game
    private String mySide; // "X" hoặc "O"
    private boolean isMyTurn; 
    
    // --- THÊM BIẾN TÍNH ĐIỂM ---
    private int myScore = 0;
    private int competitorScore = 0;
    private final int MAX_SCORE = 5; // Chạm 5 là thắng cả trận

    // UI Components
    private JButton[][] buttons = new JButton[20][20];
    private JLabel lblStatus;
    private JLabel lblScore; // Hiển thị tỉ số

    public GameBoardFrm(Socket socket, String myName, String competitorName, String mySide) {
        this.socket = socket;
        this.myName = myName;
        this.competitorName = competitorName;
        this.mySide = mySide;
        
        // X đi trước
        this.isMyTurn = mySide.equals("X");
        
        initUI();
    }

    private void initUI() {
        this.setTitle("Caro: " + myName + " vs " + competitorName);
        this.setSize(850, 700); // Tăng size chút cho rộng
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout());

        // --- Panel Thông tin & Tỉ số ---
        JPanel pnlInfo = new JPanel(new GridLayout(2, 1));
        
        // Dòng 1: Tên + Phe
        JPanel pnlNames = new JPanel(new GridLayout(1, 2));
        JLabel lblMe = new JLabel("Bạn: " + myName + " (" + mySide + ")");
        lblMe.setFont(new Font("Arial", Font.BOLD, 15));
        lblMe.setForeground(Color.BLUE);
        pnlNames.add(lblMe);
        
        lblStatus = new JLabel(isMyTurn ? " -> ĐẾN LƯỢT BẠN" : " -> ĐỢI ĐỐI THỦ...");
        lblStatus.setFont(new Font("Arial", Font.BOLD, 15));
        lblStatus.setForeground(isMyTurn ? Color.RED : Color.BLACK);
        pnlNames.add(lblStatus);
        
        // Dòng 2: Tỉ số
        JPanel pnlScore = new JPanel(new FlowLayout());
        lblScore = new JLabel("Tỉ số: 0 - 0");
        lblScore.setFont(new Font("Arial", Font.BOLD, 20));
        lblScore.setForeground(new Color(0, 100, 0)); // Màu xanh lá đậm
        pnlScore.add(lblScore);

        pnlInfo.add(pnlNames);
        pnlInfo.add(pnlScore);
        this.add(pnlInfo, BorderLayout.NORTH);

        // --- Panel Bàn cờ ---
        JPanel pnlBanCo = new JPanel(new GridLayout(20, 20));
        
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                JButton btn = new JButton("");
                btn.setBackground(Color.WHITE);
                btn.setFocusable(false);
                btn.setFont(new Font("Arial", Font.BOLD, 18));
                btn.setMargin(new Insets(0, 0, 0, 0));
                
                btn.putClientProperty("x", i);
                btn.putClientProperty("y", j);
                
                // --- SỰ KIỆN CLICK ---
                btn.addActionListener(e -> {
                    if (!isMyTurn || !btn.getText().equals("")) return; 
                    
                    // 1. Đánh dấu
                    btn.setText(mySide);
                    btn.setForeground(mySide.equals("X") ? Color.RED : Color.BLUE);
                    
                    // 2. Khóa lượt
                    isMyTurn = false;
                    lblStatus.setText(" -> ĐỢI ĐỐI THỦ...");
                    lblStatus.setForeground(Color.BLACK);
                    this.setTitle("Đang chờ đối thủ...");
                    
                    try {
                        int x = (int) btn.getClientProperty("x");
                        int y = (int) btn.getClientProperty("y");
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        out.writeUTF("CARO|" + x + "|" + y + "|" + competitorName); 
                        out.flush();
                        
                        // 3. Check thắng thua (Luật mới)
                        if (checkWinStrict(x, y, mySide)) {
                            increaseMyScore();
                        }
                        
                    } catch (Exception ex) { ex.printStackTrace(); }
                });
                
                buttons[i][j] = btn;
                pnlBanCo.add(btn);
            }
        }
        this.add(pnlBanCo, BorderLayout.CENTER);
        this.setVisible(true); 
    }
    
    // --- XỬ LÝ KHI ĐỐI THỦ ĐÁNH ---
    public void addCompetitorMove(int x, int y) {
        SwingUtilities.invokeLater(() -> {
            String competitorSide = mySide.equals("X") ? "O" : "X";
            
            if(x >= 0 && x < 20 && y >= 0 && y < 20) {
                buttons[x][y].setText(competitorSide);
                buttons[x][y].setForeground(competitorSide.equals("X") ? Color.RED : Color.BLUE);
                
                // Check xem đối thủ có thắng không
                if (checkWinStrict(x, y, competitorSide)) {
                    increaseCompetitorScore();
                    return; // Dừng, không mở khóa lượt nữa
                }
                
                isMyTurn = true;
                lblStatus.setText(" -> ĐẾN LƯỢT BẠN");
                lblStatus.setForeground(Color.RED);
                this.setTitle("Đến lượt bạn đánh!");
            }
        });
    }

    // ========================================================================
    // --- THUẬT TOÁN CHECK WIN: 5 QUÂN, KHÔNG BỊ CHẶN 2 ĐẦU ---
    // ========================================================================
    private boolean checkWinStrict(int x, int y, String value) {
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}}; // Ngang, Dọc, Chéo chính, Chéo phụ

        for (int[] dir : directions) {
            int dx = dir[0];
            int dy = dir[1];

            // 1. Đếm số quân liên tiếp về 2 phía
            int count = 1; // Tính cả quân vừa đánh
            
            // Đếm về phía dương (Positive direction)
            int i = 1;
            while (true) {
                int nx = x + i * dx;
                int ny = y + i * dy;
                if (nx < 0 || nx >= 20 || ny < 0 || ny >= 20 || !buttons[nx][ny].getText().equals(value)) {
                    break;
                }
                count++;
                i++;
            }
            // Lưu lại vị trí chặn đầu này để check block
            int headX = x + i * dx;
            int headY = y + i * dy;

            // Đếm về phía âm (Negative direction)
            int j = 1;
            while (true) {
                int nx = x - j * dx;
                int ny = y - j * dy;
                if (nx < 0 || nx >= 20 || ny < 0 || ny >= 20 || !buttons[nx][ny].getText().equals(value)) {
                    break;
                }
                count++;
                j++;
            }
            // Lưu lại vị trí chặn đuôi này
            int tailX = x - j * dx;
            int tailY = y - j * dy;

            // --- LUẬT: CHÍNH XÁC 5 QUÂN VÀ KHÔNG BỊ CHẶN 2 ĐẦU ---
            if (count == 5) { // Nếu đúng 5 quân (4 hoặc 6 không tính)
                boolean headBlocked = isBlocked(headX, headY, value);
                boolean tailBlocked = isBlocked(tailX, tailY, value);

                // Nếu KHÔNG bị chặn cả 2 đầu thì mới thắng
                if (!(headBlocked && tailBlocked)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Hàm kiểm tra xem ô đó có phải là "Vật cản" không
    // Vật cản = Hết bàn cờ HOẶC Quân của đối thủ
    private boolean isBlocked(int x, int y, String myValue) {
        // Ra khỏi bàn cờ coi như bị chặn
        if (x < 0 || x >= 20 || y < 0 || y >= 20) {
            return true;
        }
        String cellValue = buttons[x][y].getText();
        // Nếu ô đó trống -> Không chặn
        if (cellValue.equals("")) return false;
        
        // Nếu ô đó khác quân mình -> Chặn
        return !cellValue.equals(myValue);
    }

    // ========================================================================
    // --- XỬ LÝ ĐIỂM SỐ VÀ RESET VÁN ---
    // ========================================================================
    
    private void increaseMyScore() {
        myScore++;
        updateScoreUI();
        
        if (myScore >= MAX_SCORE) {
            JOptionPane.showMessageDialog(this, "CHÚC MỪNG! BẠN ĐÃ CHIẾN THẮNG CHUNG CUỘC!");
            disableBoard(); // Khóa game, hết trận
        } else {
            JOptionPane.showMessageDialog(this, "Bạn thắng ván này! Tỉ số: " + myScore + " - " + competitorScore + "\nBắt đầu ván mới!");
            resetBoard(); // Reset bàn cờ chơi ván tiếp
        }
    }

    private void increaseCompetitorScore() {
        competitorScore++;
        updateScoreUI();
        
        if (competitorScore >= MAX_SCORE) {
            JOptionPane.showMessageDialog(this, "THẤT BẠI! ĐỐI THỦ ĐÃ THẮNG CHUNG CUỘC!");
            disableBoard();
        } else {
            JOptionPane.showMessageDialog(this, "Bạn thua ván này! Tỉ số: " + myScore + " - " + competitorScore + "\nBắt đầu ván mới!");
            resetBoard();
        }
    }
    
    private void updateScoreUI() {
        lblScore.setText("Tỉ số: " + myScore + " - " + competitorScore);
    }
    
    // Hàm xóa bàn cờ để chơi ván mới
    private void resetBoard() {
        // Xóa hết quân cờ
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
            }
        }
        
        // Reset lượt đi: Theo luật thông thường thì người thua đi trước hoặc X đi trước.
        // Để đơn giản, ta giữ nguyên quy tắc ban đầu: X luôn đi trước
        isMyTurn = mySide.equals("X");
        
        // Cập nhật lại label thông báo
        lblStatus.setText(isMyTurn ? " -> ĐẾN LƯỢT BẠN" : " -> ĐỢI ĐỐI THỦ...");
        lblStatus.setForeground(isMyTurn ? Color.RED : Color.BLACK);
        this.setTitle("Caro: " + myName + " vs " + competitorName);
    }

    private void disableBoard() {
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                buttons[i][j].setEnabled(false);
            }
        }
    }
}