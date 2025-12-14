package client_controller;

import client_view.GameFrm;
import client_view.GameBoardFrm;
import client_view.PlayerProfileFrm; 
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ClientListener extends Thread {
    private Socket socket;
    private DataInputStream in;
    private GameFrm gameFrm;
    
    // Quản lý bàn cờ
    private GameBoardFrm gameBoard; 

    // Constructor
    public ClientListener(Socket socket, GameFrm gameFrm) {
        this.socket = socket;
        this.gameFrm = gameFrm;
        try {
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                // 1. Chờ tin nhắn từ Server
                String message = in.readUTF();
                System.out.println("Client nghe thấy: " + message); 
                
                // 2. Phân tích tin nhắn
                String[] data = message.split("\\|");
                String command = data[0];

                // --- CẬP NHẬT DANH SÁCH ONLINE ---
                if (command.equals("ONLINE_LIST")) {
                    Vector<String> onlineUsers = new Vector<>();
                    for (int i = 1; i < data.length; i++) {
                        onlineUsers.add(data[i]);
                    }
                    gameFrm.updateOnlineList(onlineUsers);
                }
                
                // --- XỬ LÝ LỜI MỜI THÁCH ĐẤU ---
                else if (command.equals("INVITE_REQUEST")) {
                    String inviter = data[1];
                    int confirm = JOptionPane.showConfirmDialog(gameFrm, 
                            "Người chơi " + inviter + " muốn thách đấu!\nBạn có đồng ý không?", 
                            "Lời mời thách đấu", JOptionPane.YES_NO_OPTION);
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                            out.writeUTF("ACCEPT_INVITE|" + inviter);
                            out.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                // --- BẮT ĐẦU GAME ---
                else if (command.equals("START_GAME")) {
                    String rivalName = data[1];
                    String side = data[2]; 
                    
                    System.out.println("Đang vào game với: " + rivalName + " phe: " + side);

                    // Ẩn sảnh chờ
                    gameFrm.setVisible(false); 
                    
                    // Nếu đang mở bàn cờ cũ (do chơi lại) thì đóng đi
                    if (this.gameBoard != null) {
                        this.gameBoard.dispose();
                    }

                    // Mở bàn cờ mới
                    this.gameBoard = new GameBoardFrm(socket, gameFrm, gameFrm.getUsername(), rivalName, side);
                    this.gameBoard.setVisible(true); 
                }
                
                // --- NHẬN NƯỚC ĐI TỪ ĐỐI THỦ ---
                else if (command.equals("CARO")) {
                    int x = Integer.parseInt(data[1]);
                    int y = Integer.parseInt(data[2]);
                    
                    if (this.gameBoard != null) {
                        this.gameBoard.addCompetitorMove(x, y);
                    }
                }
                
                // --- CHAT TRONG GAME ---
                else if (command.equals("CHAT")) {
                    String content = data[1];
                    if (gameBoard != null) {
                        gameBoard.addMessage("Đối thủ: " + content);
                    }
                }

                // --- ĐỐI THỦ HẾT GIỜ (TIMEOUT) ---
                else if (command.equals("TIMEOUT")) {
                    if (gameBoard != null) {
                        gameBoard.handleCompetitorTimeout();
                    }
                }
                
                // --- ĐỐI THỦ THOÁT GAME ĐỘT NGỘT (EXIT_GAME) ---
                else if (command.equals("EXIT_GAME")) {
                    if (this.gameBoard != null) {
                        JOptionPane.showMessageDialog(this.gameBoard, "Đối thủ đã thoát game!");
                        this.gameBoard.dispose(); 
                        this.gameBoard = null;    
                        gameFrm.setVisible(true); 
                    }
                }
                
                // ============================================================
                // --- CÁC TÍNH NĂNG MỚI CẬP NHẬT ---
                // ============================================================
                
                // --- 1. NHẬN CẢNH BÁO (WARN) ---
                else if (command.equals("WARN")) {
                    String msg = data[1];
                    JOptionPane.showMessageDialog(gameFrm, msg, "Thông báo", JOptionPane.WARNING_MESSAGE);
                }

                // --- 2. NHẬN KẾT QUẢ TRẬN ĐẤU (GAME_RESULT) ---
                // Thay thế cho COMPETITOR_QUIT để hiển thị điểm số
                else if (command.equals("GAME_RESULT")) {
                    String winner = data[1];
                    int winnerScore = Integer.parseInt(data[2]);
                    String loser = data[3];
                    int loserScore = Integer.parseInt(data[4]);
                    
                    SwingUtilities.invokeLater(() -> {
                        if (gameBoard != null) {
                            // Gọi hàm hiển thị Dialog kết quả xịn xò bên GameBoard
                            gameBoard.showResultDialog(winner, winnerScore, loser, loserScore);
                        }
                    });
                }
                
                // --- 3. XỬ LÝ YÊU CẦU CHƠI LẠI (REMATCH) ---
                else if (command.equals("REMATCH_REQUEST")) {
                    String inviter = data[1];
                    SwingUtilities.invokeLater(() -> {
                        int confirm = JOptionPane.showConfirmDialog(gameBoard, 
                            "Đối thủ " + inviter + " muốn chơi lại ván nữa!\nBạn có đồng ý không?", 
                            "Yêu cầu đấu lại", JOptionPane.YES_NO_OPTION);
                        
                        try {
                            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                            if (confirm == JOptionPane.YES_OPTION) {
                                out.writeUTF("REMATCH_ACCEPT|" + inviter);
                            } else {
                                out.writeUTF("REMATCH_REFUSE|" + inviter);
                                // Từ chối xong thì mình về sảnh
                                if (gameBoard != null) {
                                    gameBoard.dispose();
                                    gameBoard = null;
                                }
                                gameFrm.setVisible(true);
                            }
                            out.flush();
                        } catch (Exception e) {}
                    });
                }
                
                // --- 4. KHI ĐỐI THỦ TỪ CHỐI CHƠI LẠI ---
                else if (command.equals("REMATCH_REFUSE")) {
                    JOptionPane.showMessageDialog(gameBoard, "Đối thủ đã từ chối chơi lại!");
                    if (gameBoard != null) {
                        gameBoard.dispose();
                        gameBoard = null;
                    }
                    gameFrm.setVisible(true);
                }
                
                // --- 5. XEM THÔNG TIN PROFILE (RETURN_INFO) ---
                else if (command.equals("RETURN_INFO")) {
                    int win = Integer.parseInt(data[1]);
                    int lose = Integer.parseInt(data[2]);
                    int draw = Integer.parseInt(data[3]);
                    int score = Integer.parseInt(data[4]);
                    String username = data[5];
                    
                    new PlayerProfileFrm(username, win, lose, draw, score);
                }
                
            }
        } catch (IOException e) {
            System.out.println("Mất kết nối tới Server!");
            // Tránh spam dialog khi tắt client
            // JOptionPane.showMessageDialog(gameFrm, "Mất kết nối tới Server."); 
        }
    }
}