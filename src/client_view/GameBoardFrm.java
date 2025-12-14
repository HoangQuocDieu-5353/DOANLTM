package client_view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.net.Socket;

public class GameBoardFrm extends JFrame {
    private Socket socket;
    private GameFrm gameFrm; // Giữ tham chiếu tới sảnh chờ để back về
    private String myName;
    private String competitorName;
    
    // Logic game
    private String mySide; // "X" hoặc "O"
    private boolean isMyTurn; 
    
    // --- BIẾN TÍNH ĐIỂM ---
    private int myScore = 0;
    private int competitorScore = 0;
    private final int MAX_SCORE = 5; 

    // --- BIẾN TIMER ---
    private Timer turnTimer;
    private int timeLeft = 30; // 30 giây suy nghĩ
    
    // --- UI Components ---
    private JButton[][] buttons = new JButton[20][20];
    private JLabel lblStatus;
    private JLabel lblScore; 
    private JLabel lblTimer; 
    
    // --- CHAT COMPONENTS ---
    private JTextArea txtChatArea;
    private JTextField txtMessageInput;
    private JButton btnSend;
    
    // --- NÚT ĐẦU HÀNG ---
    private JButton btnSurrender;

    // --- CẬP NHẬT CONSTRUCTOR: THÊM GameFrm gameFrm ---
    public GameBoardFrm(Socket socket, GameFrm gameFrm, String myName, String competitorName, String mySide) {
        this.socket = socket;
        this.gameFrm = gameFrm; // Lưu lại sảnh chờ
        this.myName = myName;
        this.competitorName = competitorName;
        this.mySide = mySide;
        
        // X đi trước
        this.isMyTurn = mySide.equals("X");
        
        initUI();
        initTimer(); 
        
        if (isMyTurn) {
            startTurnTimer();
        }
    }

    private void initUI() {
        this.setTitle("Caro Online: " + myName + " vs " + competitorName);
        this.setSize(1100, 750);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout());

        // ====================================================================
        // 1. PANEL TRÁI: BÀN CỜ
        // ====================================================================
        JPanel pnlBanCo = new JPanel(new GridLayout(20, 20));
        
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                JButton btn = new JButton("");
                btn.setBackground(Color.WHITE);
                btn.setFocusable(false);
                btn.setFont(new Font("Arial", Font.BOLD, 16));
                btn.setMargin(new Insets(0, 0, 0, 0));
                
                btn.putClientProperty("x", i);
                btn.putClientProperty("y", j);
                
                btn.addActionListener(e -> {
                    if (!isMyTurn || !btn.getText().equals("")) return; 
                    
                    btn.setText(mySide);
                    btn.setForeground(mySide.equals("X") ? Color.RED : Color.BLUE);
                    
                    isMyTurn = false;
                    updateStatus("Đang chờ đối thủ...");
                    stopTurnTimer(); 
                    
                    try {
                        int x = (int) btn.getClientProperty("x");
                        int y = (int) btn.getClientProperty("y");
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        out.writeUTF("CARO|" + x + "|" + y + "|" + competitorName); 
                        out.flush();
                        
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

        // ====================================================================
        // 2. PANEL PHẢI: THÔNG TIN + CHAT
        // ====================================================================
        JPanel pnlRight = new JPanel();
        pnlRight.setLayout(new BorderLayout());
        pnlRight.setPreferredSize(new Dimension(320, 0)); 
        pnlRight.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, Color.BLACK)); 

        // --- A. Thông tin ---
        JPanel pnlInfo = new JPanel(new GridLayout(5, 1, 5, 5));
        pnlInfo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlInfo.setBackground(new Color(240, 240, 240));

        JLabel lblMe = new JLabel("Bạn: " + myName + " (" + mySide + ")");
        lblMe.setFont(new Font("Arial", Font.BOLD, 15));
        lblMe.setForeground(Color.BLUE);
        
        JLabel lblCompetitor = new JLabel("Đối thủ: " + competitorName);
        lblCompetitor.setFont(new Font("Arial", Font.BOLD, 14));

        lblStatus = new JLabel(isMyTurn ? "ĐẾN LƯỢT BẠN" : "Đợi đối thủ...");
        lblStatus.setFont(new Font("Arial", Font.BOLD, 14));
        lblStatus.setForeground(isMyTurn ? Color.RED : Color.BLACK);
        
        lblScore = new JLabel("Tỉ số: 0 - 0");
        lblScore.setFont(new Font("Arial", Font.BOLD, 18));
        lblScore.setForeground(new Color(0, 100, 0)); 
        
        lblTimer = new JLabel("Thời gian: 30s");
        lblTimer.setFont(new Font("Arial", Font.BOLD, 22));
        lblTimer.setForeground(Color.RED);
        
        pnlInfo.add(lblMe);
        pnlInfo.add(lblCompetitor);
        pnlInfo.add(lblStatus);
        pnlInfo.add(lblScore);
        pnlInfo.add(lblTimer);
        
        pnlRight.add(pnlInfo, BorderLayout.NORTH);

        // --- B. Chat ---
        JPanel pnlChat = new JPanel(new BorderLayout());
        pnlChat.setBorder(BorderFactory.createTitledBorder("Trò chuyện"));
        
        txtChatArea = new JTextArea();
        txtChatArea.setEditable(false);
        txtChatArea.setLineWrap(true);
        txtChatArea.setWrapStyleWord(true);
        JScrollPane scrollChat = new JScrollPane(txtChatArea);
        pnlChat.add(scrollChat, BorderLayout.CENTER);
        
        // --- C. Input + Đầu Hàng ---
        JPanel pnlSouth = new JPanel(new BorderLayout());
        
        JPanel pnlInput = new JPanel(new BorderLayout());
        txtMessageInput = new JTextField();
        btnSend = new JButton("Gửi");
        ActionListener sendAction = e -> sendMessage();
        btnSend.addActionListener(sendAction);
        txtMessageInput.addActionListener(sendAction); 
        pnlInput.add(txtMessageInput, BorderLayout.CENTER);
        pnlInput.add(btnSend, BorderLayout.EAST);
        
        btnSurrender = new JButton("ĐẦU HÀNG");
        btnSurrender.setBackground(Color.RED);
        btnSurrender.setForeground(Color.WHITE);
        btnSurrender.setFont(new Font("Arial", Font.BOLD, 18));
        btnSurrender.setFocusable(false);
        btnSurrender.setPreferredSize(new Dimension(0, 50)); 
        
        btnSurrender.addActionListener(e -> surrender());
        
        pnlSouth.add(pnlInput, BorderLayout.NORTH);
        pnlSouth.add(Box.createVerticalStrut(10), BorderLayout.CENTER); 
        pnlSouth.add(btnSurrender, BorderLayout.SOUTH);
        pnlSouth.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        pnlChat.add(pnlSouth, BorderLayout.SOUTH);
        pnlRight.add(pnlChat, BorderLayout.CENTER);
        
        this.add(pnlRight, BorderLayout.EAST);
        this.setVisible(true); 
    }
    
    // ========================================================================
    // --- XỬ LÝ ĐẦU HÀNG ---
    // ========================================================================
    private void surrender() {
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Bạn có chắc chắn muốn đầu hàng không?\nBạn sẽ bị xử thua ván này.", 
                "Xác nhận đầu hàng", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Gửi lệnh lên Server và CHỜ KẾT QUẢ (Không tự đóng nữa)
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF("SURRENDER|" + competitorName);
                out.flush();
                
                // Dừng timer để đỡ chạy ngầm
                turnTimer.stop();
                disableBoard();
                
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ========================================================================
    // --- HÀM HIỂN THỊ KẾT QUẢ (Được gọi từ ClientListener) ---
    // ========================================================================
    public void showResultDialog(String winner, int winnerScore, String loser, int loserScore) {
        boolean isWin = myName.equals(winner);
        
        String title = isWin ? "CHIẾN THẮNG!" : "THẤT BẠI!";
        String message;
        int type;
        
        if (isWin) {
            message = "Chúc mừng! Bạn đã giành chiến thắng.\n" +
                      "Điểm cộng: +10\n" +
                      "Tổng điểm hiện tại: " + winnerScore;
            type = JOptionPane.INFORMATION_MESSAGE;
        } else {
            message = "Rất tiếc! Bạn đã để thua ván này.\n" +
                      "Tổng điểm hiện tại: " + loserScore;
            type = JOptionPane.ERROR_MESSAGE;
        }

        Object[] options = {"Chơi lại", "Về màn hình chính"};
        int choice = JOptionPane.showOptionDialog(this,
                message + "\n\nBạn có muốn chơi lại với đối thủ này không?",
                title,
                JOptionPane.YES_NO_OPTION,
                type,
                null,
                options,
                options[0]);

        if (choice == 0) { // Chọn Chơi lại
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF("REMATCH_REQUEST|" + competitorName);
                out.flush();
                JOptionPane.showMessageDialog(this, "Đang chờ đối thủ đồng ý...");
            } catch (Exception e) { e.printStackTrace(); }
        } else { // Chọn Về sảnh
            this.dispose();
            gameFrm.setVisible(true); // Mở lại sảnh chờ
        }
    }

    // ========================================================================
    // --- CÁC HÀM XỬ LÝ CHAT & TIMER (GIỮ NGUYÊN) ---
    // ========================================================================
    private void sendMessage() {
        String msg = txtMessageInput.getText().trim();
        if (msg.isEmpty()) return;
        try {
            addMessage("Bạn: " + msg);
            txtMessageInput.setText("");
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("CHAT|" + msg + "|" + competitorName);
            out.flush();
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public void addMessage(String msg) {
        txtChatArea.append(msg + "\n");
        txtChatArea.setCaretPosition(txtChatArea.getDocument().getLength()); 
    }

    private void initTimer() {
        turnTimer = new Timer(1000, e -> {
            if (!isMyTurn) return; 
            timeLeft--;
            lblTimer.setText("Thời gian: " + timeLeft + "s");
            if (timeLeft <= 10) lblTimer.setForeground(Color.RED);
            else lblTimer.setForeground(Color.BLUE);
            if (timeLeft <= 0) {
                turnTimer.stop();
                JOptionPane.showMessageDialog(this, "Hết giờ! Bạn bị mất lượt.");
                try {
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF("TIMEOUT|" + competitorName);
                    out.flush();
                } catch (Exception ex) {}
                isMyTurn = false;
                updateStatus("Đang chờ đối thủ...");
            }
        });
    }

    public void startTurnTimer() {
        timeLeft = 30; 
        lblTimer.setText("Thời gian: 30s");
        lblTimer.setForeground(Color.BLUE);
        turnTimer.start();
    }
    
    public void stopTurnTimer() {
        turnTimer.stop();
        lblTimer.setText("Thời gian: --");
    }
    
    public void handleCompetitorTimeout() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Đối thủ đã hết giờ! Đến lượt bạn.");
            isMyTurn = true;
            updateStatus("ĐẾN LƯỢT BẠN");
            startTurnTimer(); 
        });
    }

    private void updateStatus(String status) {
        lblStatus.setText(status);
        lblStatus.setForeground(isMyTurn ? Color.RED : Color.BLACK);
    }

    // ========================================================================
    // --- LOGIC GAME & TÍNH ĐIỂM (CẬP NHẬT) ---
    // ========================================================================
    public void addCompetitorMove(int x, int y) {
        SwingUtilities.invokeLater(() -> {
            String competitorSide = mySide.equals("X") ? "O" : "X";
            if(x >= 0 && x < 20 && y >= 0 && y < 20) {
                buttons[x][y].setText(competitorSide);
                buttons[x][y].setForeground(competitorSide.equals("X") ? Color.RED : Color.BLUE);
                
                if (checkWinStrict(x, y, competitorSide)) {
                    increaseCompetitorScore();
                    return; 
                }
                
                isMyTurn = true;
                updateStatus("ĐẾN LƯỢT BẠN");
                startTurnTimer(); 
                this.setTitle("Đến lượt bạn đánh!");
            }
        });
    }

    private boolean checkWinStrict(int x, int y, String value) { 
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}}; 
        for (int[] dir : directions) {
            int dx = dir[0]; int dy = dir[1];
            int count = 1; int i = 1;
            while (true) {
                int nx = x + i * dx; int ny = y + i * dy;
                if (nx < 0 || nx >= 20 || ny < 0 || ny >= 20 || !buttons[nx][ny].getText().equals(value)) break;
                count++; i++;
            }
            int headX = x + i * dx; int headY = y + i * dy;
            int j = 1;
            while (true) {
                int nx = x - j * dx; int ny = y - j * dy;
                if (nx < 0 || nx >= 20 || ny < 0 || ny >= 20 || !buttons[nx][ny].getText().equals(value)) break;
                count++; j++;
            }
            int tailX = x - j * dx; int tailY = y - j * dy;
            if (count == 5) { 
                boolean headBlocked = isBlocked(headX, headY, value);
                boolean tailBlocked = isBlocked(tailX, tailY, value);
                if (!(headBlocked && tailBlocked)) return true;
            }
        }
        return false;
    }

    private boolean isBlocked(int x, int y, String myValue) {
        if (x < 0 || x >= 20 || y < 0 || y >= 20) return true;
        String cellValue = buttons[x][y].getText();
        if (cellValue.equals("")) return false;
        return !cellValue.equals(myValue);
    }
    
    // --- SỬA LOGIC: CHỈ GỬI LỆNH, KHÔNG HIỆN DIALOG ---
    public void increaseMyScore() {
        myScore++;
        updateScoreUI();
        if (myScore >= MAX_SCORE) {
            disableBoard();
            stopTurnTimer();
            try {
                 // Gửi kết quả -> Chờ Server gửi GAME_RESULT về
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 out.writeUTF("GAME_OVER|" + myName + "|" + competitorName);
                 out.flush();
            } catch(Exception e){}
        } else {
            JOptionPane.showMessageDialog(this, "Bạn thắng ván này! Tỉ số: " + myScore + " - " + competitorScore);
            resetBoard();
        }
    }

    // --- SỬA LOGIC: KHÔNG LÀM GÌ, CHỜ GAME_RESULT ---
    private void increaseCompetitorScore() {
        competitorScore++;
        updateScoreUI();
        if (competitorScore >= MAX_SCORE) {
            disableBoard();
            stopTurnTimer();
            // Đợi lệnh GAME_RESULT từ Server
        } else {
            JOptionPane.showMessageDialog(this, "Bạn thua ván này! Tỉ số: " + myScore + " - " + competitorScore);
            resetBoard();
        }
    }
    
    private void updateScoreUI() {
        lblScore.setText("Tỉ số: " + myScore + " - " + competitorScore);
    }
    
    private void resetBoard() {
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
            }
        }
        isMyTurn = mySide.equals("X");
        updateStatus(isMyTurn ? "ĐẾN LƯỢT BẠN" : "Đợi đối thủ...");
        if(isMyTurn) startTurnTimer(); else stopTurnTimer();
    }

    private void disableBoard() {
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                buttons[i][j].setEnabled(false);
            }
        }
    }
}