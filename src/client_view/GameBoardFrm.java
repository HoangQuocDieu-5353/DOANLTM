package client_view;

import client_controller.AI; 
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.net.Socket;

public class GameBoardFrm extends JFrame {
    private Socket socket;
    private GameFrm gameFrm;
    private String myName;
    private String competitorName;
    
    // Logic game
    private String mySide; 
    private boolean isMyTurn; 
    
    // --- BIẾN PVE (CHƠI VỚI MÁY) ---
    private boolean isPvE = false; 
    private int difficulty = 1;    
    private AI ai;                 
    
    // --- BIẾN HIGHLIGHT ---
    private JButton lastMoveButton = null;
    private final Color LAST_MOVE_COLOR = Color.YELLOW;
    private final Color NORMAL_COLOR = Color.WHITE;

    // Biến tính điểm
    private int myScore = 0;
    private int competitorScore = 0;
    private final int MAX_SCORE = 5; 

    // Biến Timer
    private Timer turnTimer;
    private int timeLeft = 30; 
    
    // UI Components
    private JButton[][] buttons = new JButton[20][20];
    private JLabel lblStatus;
    private JLabel lblScore; 
    private JLabel lblTimer; 
    
    // Chat Components
    private JTextArea txtChatArea;
    private JTextField txtMessageInput;
    private JButton btnSend;
    private JButton btnSurrender;
    
    private JButton btnAddFriend; 
    private JButton btnDraw; // Nút xin hòa

    public GameBoardFrm(Socket socket, GameFrm gameFrm, String myName, String competitorName, String mySide) {
        this.socket = socket;
        this.gameFrm = gameFrm;
        this.myName = myName;
        this.competitorName = competitorName;
        this.mySide = mySide;
        this.isPvE = false; 
        
        this.isMyTurn = mySide.equals("X");
        
        initUI();
        initTimer(); 
        
        // --- CHECK NGAY TỪ ĐẦU: NẾU LÀ BẠN THÌ ẨN NÚT ---
        checkAndHideFriendButton();
        
        if (isMyTurn) startTurnTimer();
    }
    
    // Constructor PvE
    public GameBoardFrm(GameFrm gameFrm, String myName, int difficulty) {
        this.gameFrm = gameFrm;
        this.myName = myName;
        this.difficulty = difficulty;
        this.isPvE = true; 
        
        this.competitorName = "Máy (Cấp " + difficulty + ")";
        this.mySide = "X"; 
        this.isMyTurn = true;
        
        initUI();
        initTimer();
        
        this.ai = new AI(buttons);
        
        txtMessageInput.setEnabled(false);
        btnSend.setEnabled(false);
        btnSurrender.setText("THOÁT TRẬN");
        btnSurrender.setVisible(true); 
        
        // Ẩn nút kết bạn và cầu hòa khi chơi với máy
        if(btnAddFriend != null) btnAddFriend.setVisible(false);
        if(btnDraw != null) btnDraw.setVisible(false);

        startTurnTimer();
    }

    private void initUI() {
        this.setTitle("Caro: " + myName + " vs " + competitorName);
        this.setSize(1100, 750);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout());

        // --- 1. PANEL TRÁI: BÀN CỜ ---
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
                btn.addActionListener(e -> handleButtonClick(btn));
                
                buttons[i][j] = btn;
                pnlBanCo.add(btn);
            }
        }
        this.add(pnlBanCo, BorderLayout.CENTER);

        // --- 2. PANEL PHẢI ---
        JPanel pnlRight = new JPanel();
        pnlRight.setLayout(new BorderLayout());
        pnlRight.setPreferredSize(new Dimension(320, 0)); 
        pnlRight.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, Color.BLACK)); 

        // A. Thông tin
        JPanel pnlInfo = new JPanel(new GridLayout(6, 1, 5, 5)); 
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
        
        // --- Nút Kết Bạn ---
        btnAddFriend = new JButton("Kết bạn");
        btnAddFriend.setBackground(new Color(255, 215, 0));
        btnAddFriend.setFocusable(false);
        btnAddFriend.addActionListener(e -> {
            try {
                if (socket != null) {
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF("MAKE_FRIEND|" + competitorName);
                    out.flush();
                    JOptionPane.showMessageDialog(this, "Đã gửi lời mời kết bạn tới " + competitorName);
                    btnAddFriend.setEnabled(false); // Bấm 1 lần thôi
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        
        pnlInfo.add(lblMe);
        pnlInfo.add(lblCompetitor);
        pnlInfo.add(btnAddFriend); 
        pnlInfo.add(lblStatus);
        pnlInfo.add(lblScore);
        pnlInfo.add(lblTimer);
        pnlRight.add(pnlInfo, BorderLayout.NORTH);

        // B. Chat
        JPanel pnlChat = new JPanel(new BorderLayout());
        pnlChat.setBorder(BorderFactory.createTitledBorder("Trò chuyện"));
        txtChatArea = new JTextArea();
        txtChatArea.setEditable(false);
        txtChatArea.setLineWrap(true);
        txtChatArea.setWrapStyleWord(true);
        JScrollPane scrollChat = new JScrollPane(txtChatArea);
        pnlChat.add(scrollChat, BorderLayout.CENTER);
        
        // C. Input + Nút Hành Động
        JPanel pnlSouth = new JPanel(new BorderLayout());
        JPanel pnlInput = new JPanel(new BorderLayout());
        txtMessageInput = new JTextField();
        btnSend = new JButton("Gửi");
        ActionListener sendAction = e -> sendMessage();
        btnSend.addActionListener(sendAction);
        txtMessageInput.addActionListener(sendAction); 
        pnlInput.add(txtMessageInput, BorderLayout.CENTER);
        pnlInput.add(btnSend, BorderLayout.EAST);
        
        // Panel chứa 2 nút: Xin Hòa + Đầu Hàng
        JPanel pnlActions = new JPanel(new GridLayout(1, 2, 5, 5));
        
        // --- [NEW] NÚT XIN HÒA ---
        btnDraw = new JButton("CẦU HÒA");
        btnDraw.setBackground(new Color(100, 149, 237)); // Cornflower Blue
        btnDraw.setForeground(Color.WHITE);
        btnDraw.setFont(new Font("Arial", Font.BOLD, 14));
        btnDraw.setFocusable(false);
        btnDraw.addActionListener(e -> requestDraw());
        
        btnSurrender = new JButton("ĐẦU HÀNG");
        btnSurrender.setBackground(Color.RED);
        btnSurrender.setForeground(Color.WHITE);
        btnSurrender.setFont(new Font("Arial", Font.BOLD, 14));
        btnSurrender.setFocusable(false);
        btnSurrender.addActionListener(e -> surrender());
        
        pnlActions.add(btnDraw);
        pnlActions.add(btnSurrender);
        
        pnlSouth.add(pnlInput, BorderLayout.NORTH);
        pnlSouth.add(Box.createVerticalStrut(10), BorderLayout.CENTER); 
        pnlSouth.add(pnlActions, BorderLayout.SOUTH);
        pnlSouth.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        pnlChat.add(pnlSouth, BorderLayout.SOUTH);
        pnlRight.add(pnlChat, BorderLayout.CENTER);
        
        this.add(pnlRight, BorderLayout.EAST);
        this.setVisible(true); 
    }
    
    // --- HÀM XỬ LÝ NÚT XIN HÒA ---
    private void requestDraw() {
        if (isPvE) return; // Máy ko hòa
        
        // 1. Tạm dừng Timer của mình
        stopTurnTimer();
        
        // 2. Hỏi xác nhận
        int choice = JOptionPane.showConfirmDialog(this, 
                "Bạn có chắc chắn muốn cầu hòa không?", 
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF("DRAW_REQUEST|" + competitorName);
                out.flush();
                updateStatus("Đã gửi lời cầu hòa...");
                // Timer vẫn dừng để đợi đối thủ trả lời
            } catch (Exception e) {}
        } else {
            // Nếu hủy thì chạy lại Timer (nếu đang là lượt mình)
            if (isMyTurn) startTurnTimer();
        }
    }
    
    // --- HÀM ẨN NÚT KẾT BẠN (GỌI TỪ BÊN NGOÀI) ---
    public void checkAndHideFriendButton() {
        // Hỏi GameFrm xem đối thủ này có phải bạn không
        if (gameFrm.checkIsFriend(competitorName)) {
            btnAddFriend.setVisible(false);
            btnAddFriend.setEnabled(false);
        }
    }
    
    // --- CÁC HÀM QUẢN LÝ TIMER (PUBLIC ĐỂ LISTENER GỌI) ---
    public void stopTimerNow() {
        if (turnTimer != null && turnTimer.isRunning()) turnTimer.stop();
    }
    
    public void resumeTimerNow() {
        // Chỉ chạy lại nếu game chưa kết thúc
        if (!lblScore.getText().contains("thắng") && turnTimer != null) {
            turnTimer.start();
        }
    }
    
    // --- DIALOG KẾT QUẢ HÒA ---
    public void showDrawDialog(int myScore, int opScore) {
        Object[] options = {"Chơi lại", "Về màn hình chính"};
        int choice = JOptionPane.showOptionDialog(this, 
            "Ván đấu HÒA!\nĐiểm bạn: " + myScore + "\nĐiểm đối thủ: " + opScore, 
            "Kết quả", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        
        if (choice == 0) {
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF("REMATCH_REQUEST|" + competitorName);
                out.flush();
                JOptionPane.showMessageDialog(this, "Đang chờ đối thủ đồng ý...");
            } catch (Exception e) {}
        } else {
            this.dispose();
            gameFrm.setVisible(true);
        }
    }

    
    private void highlightLastMove(JButton btn) {
        if (lastMoveButton != null) lastMoveButton.setBackground(NORMAL_COLOR);
        lastMoveButton = btn;
        lastMoveButton.setBackground(LAST_MOVE_COLOR);
    }

    private void handleButtonClick(JButton btn) {
        if (!isMyTurn || !btn.getText().equals("")) return; 
        
        btn.setText(mySide);
        btn.setForeground(mySide.equals("X") ? Color.RED : Color.BLUE);
        highlightLastMove(btn); 
        
        isMyTurn = false;
        stopTurnTimer(); 
        
        int x = (int) btn.getClientProperty("x");
        int y = (int) btn.getClientProperty("y");
        
        if (!isPvE) {
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF("CARO|" + x + "|" + y + "|" + competitorName); 
                out.flush();
            } catch (Exception ex) { ex.printStackTrace(); }
            updateStatus("Đang chờ đối thủ...");
        }

        if (checkWinStrict(x, y, mySide)) {
            if (isPvE) endGamePvE("CHÚC MỪNG! BẠN ĐÃ CHIẾN THẮNG MÁY!");
            else increaseMyScore(); 
            return; 
        }

        if (isPvE) {
            updateStatus("Máy đang nghĩ...");
            new Thread(() -> {
                try { Thread.sleep(500); } catch (Exception ex) {} 
                int[] move = ai.findBestMove(difficulty);
                int aiX = move[0];
                int aiY = move[1];
                SwingUtilities.invokeLater(() -> {
                    if (aiX != -1 && buttons[aiX][aiY].getText().equals("")) {
                        buttons[aiX][aiY].setText("O");
                        buttons[aiX][aiY].setForeground(Color.BLUE);
                        highlightLastMove(buttons[aiX][aiY]); 
                        if (checkWinStrict(aiX, aiY, "O")) {
                            endGamePvE("RẤT TIẾC! MÁY ĐÃ THẮNG BẠN.");
                        } else {
                            isMyTurn = true;
                            updateStatus("ĐẾN LƯỢT BẠN");
                            startTurnTimer();
                        }
                    }
                });
            }).start();
        } 
    }
    
    private void surrender() {
        if (isPvE) {
            String message = "Bạn chấp nhận để thua máy sao?\nThằng gà, định chịu thua 1 con máy à? Nhục lắm!";
            String[] options = {"Đánh tiếp (Sợ gì)", "Thoát (Tôi là gà)"};
            int choice = JOptionPane.showOptionDialog(this, message, "Kích tướng", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (choice == 1) { 
                turnTimer.stop();
                this.dispose();
                gameFrm.setVisible(true); 
            }
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn đầu hàng không?\nBạn sẽ bị xử thua ván này.", "Xác nhận đầu hàng", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF("SURRENDER|" + competitorName);
                out.flush();
                turnTimer.stop();
                disableBoard();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void endGamePvE(String message) {
        JOptionPane.showMessageDialog(this, message);
        this.dispose();
        gameFrm.setVisible(true); 
    }
    
    public void showResultDialog(String winner, int winnerScore, String loser, int loserScore) {
        boolean isWin = myName.equals(winner);
        String title = isWin ? "CHIẾN THẮNG!" : "THẤT BẠI!";
        String message = isWin ? ("Điểm cộng: +10\nTổng điểm: " + winnerScore) : ("Tổng điểm: " + loserScore);
        int type = isWin ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;
        Object[] options = {"Chơi lại", "Về màn hình chính"};
        int choice = JOptionPane.showOptionDialog(this, message + "\n\nChơi lại không?", title, JOptionPane.YES_NO_OPTION, type, null, options, options[0]);
        if (choice == 0) {
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF("REMATCH_REQUEST|" + competitorName);
                out.flush();
                JOptionPane.showMessageDialog(this, "Đang chờ đối thủ đồng ý...");
            } catch (Exception e) {}
        } else {
            this.dispose();
            gameFrm.setVisible(true);
        }
    }

    private void sendMessage() {
        String msg = txtMessageInput.getText().trim();
        if (msg.isEmpty()) return;
        try {
            addMessage("Bạn: " + msg);
            txtMessageInput.setText("");
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("CHAT|" + msg + "|" + competitorName);
            out.flush();
        } catch (Exception e) {}
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
            if (timeLeft <= 10) lblTimer.setForeground(Color.RED); else lblTimer.setForeground(Color.BLUE);
            if (timeLeft <= 0) {
                turnTimer.stop();
                JOptionPane.showMessageDialog(this, "Hết giờ! Bạn bị mất lượt.");
                if (!isPvE) {
                    try {
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        out.writeUTF("TIMEOUT|" + competitorName);
                        out.flush();
                    } catch (Exception ex) {}
                }
                isMyTurn = false;
                updateStatus("Đang chờ đối thủ...");
            }
        });
    }

    public void startTurnTimer() { timeLeft = 30; lblTimer.setText("Thời gian: 30s"); lblTimer.setForeground(Color.BLUE); turnTimer.start(); }
    public void stopTurnTimer() { turnTimer.stop(); lblTimer.setText("Thời gian: --"); }
    
    public void handleCompetitorTimeout() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Đối thủ đã hết giờ! Đến lượt bạn.");
            isMyTurn = true;
            updateStatus("ĐẾN LƯỢT BẠN");
            startTurnTimer(); 
        });
    }

    private void updateStatus(String status) { lblStatus.setText(status); lblStatus.setForeground(isMyTurn ? Color.RED : Color.BLACK); }

    public void addCompetitorMove(int x, int y) {
        SwingUtilities.invokeLater(() -> {
            String competitorSide = mySide.equals("X") ? "O" : "X";
            if(x >= 0 && x < 20 && y >= 0 && y < 20) {
                buttons[x][y].setText(competitorSide);
                buttons[x][y].setForeground(competitorSide.equals("X") ? Color.RED : Color.BLUE);
                highlightLastMove(buttons[x][y]); 
                if (checkWinStrict(x, y, competitorSide)) { 
                    Timer t = new Timer(200, e -> {
                        increaseCompetitorScore();
                        ((Timer)e.getSource()).stop();
                    });
                    t.setRepeats(false);
                    t.start();
                    return; 
                }
                isMyTurn = true; updateStatus("ĐẾN LƯỢT BẠN"); startTurnTimer(); this.setTitle("Đến lượt bạn đánh!");
            }
        });
    }

    private boolean checkWinStrict(int x, int y, String value) {
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] dir : directions) {
            int count = 1; 
            int i = 1;
            while (true) {
                int nx = x + i * dir[0];
                int ny = y + i * dir[1];
                if (nx < 0 || nx >= 20 || ny < 0 || ny >= 20 || !buttons[nx][ny].getText().equals(value)) break;
                count++; i++;
            }
            int j = 1;
            while (true) {
                int nx = x - j * dir[0];
                int ny = y - j * dir[1];
                if (nx < 0 || nx >= 20 || ny < 0 || ny >= 20 || !buttons[nx][ny].getText().equals(value)) break;
                count++; j++;
            }
            if (count >= 5) return true;
        }
        return false;
    }
    
    public void increaseMyScore() {
        myScore++; updateScoreUI();
        if (myScore >= MAX_SCORE) {
            disableBoard(); stopTurnTimer();
            try {
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 out.writeUTF("GAME_OVER|" + myName + "|" + competitorName);
                 out.flush();
            } catch(Exception e){}
        } else {
            JOptionPane.showMessageDialog(this, "Bạn thắng ván này!"); resetBoard();
        }
    }

    private void increaseCompetitorScore() {
        competitorScore++; updateScoreUI();
        if (competitorScore >= MAX_SCORE) { disableBoard(); stopTurnTimer(); } 
        else { JOptionPane.showMessageDialog(this, "Bạn thua ván này!"); resetBoard(); }
    }
    
    private void updateScoreUI() { lblScore.setText("Tỉ số: " + myScore + " - " + competitorScore); }
    
    private void resetBoard() {
        if (lastMoveButton != null) {
            lastMoveButton.setBackground(NORMAL_COLOR);
            lastMoveButton = null;
        }
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) { 
                buttons[i][j].setText(""); 
                buttons[i][j].setEnabled(true); 
                buttons[i][j].setBackground(NORMAL_COLOR); 
            }
        }
        isMyTurn = mySide.equals("X");
        updateStatus(isMyTurn ? "ĐẾN LƯỢT BẠN" : "Đợi đối thủ...");
        if(isMyTurn) startTurnTimer(); else stopTurnTimer();
    }

    private void disableBoard() {
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) { buttons[i][j].setEnabled(false); }
        }
    }
}