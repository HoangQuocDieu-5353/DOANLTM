package client_controller;

import client_view.GameFrm;
import client_view.GameBoardFrm;
import client_view.PlayerProfileFrm;
import client_view.RoomListFrm;
import client_view.WaitingRoomFrm;
import client_view.RankFrm; 
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
    
    private GameBoardFrm gameBoard; 
    private RoomListFrm roomListFrm;
    private WaitingRoomFrm waitingRoomFrm;
    private RankFrm rankFrm; 

    public ClientListener(Socket socket, GameFrm gameFrm) {
        this.socket = socket;
        this.gameFrm = gameFrm;
        try {
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRoomListFrm(RoomListFrm roomListFrm) {
        this.roomListFrm = roomListFrm;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = in.readUTF();
                System.out.println("Client nghe thấy: " + message); 
                
                String[] data = message.split("\\|");
                String command = data[0];

                // --- 1. NHẬN DANH SÁCH BẠN BÈ ---
                if (command.equals("FRIEND_LIST")) {
                    Vector<String> friends = new Vector<>();
                    if (data.length > 1) {
                        String[] list = data[1].split(";");
                        for (String s : list) {
                            if (!s.isEmpty()) {
                                String[] info = s.split(","); 
                                String status = info[1].equals("1") ? "(Online)" : "(Offline)";
                                friends.add(info[0] + " " + status);
                            }
                        }
                    }
                    SwingUtilities.invokeLater(() -> {
                        gameFrm.updateOnlineList(friends);
                        if (gameBoard != null) gameBoard.checkAndHideFriendButton();
                    });
                }
                
                // --- 2. NHẬN LỜI MỜI KẾT BẠN ---
                else if (command.equals("MAKE_FRIEND_REQUEST")) {
                    String requester = data[1];
                    SwingUtilities.invokeLater(() -> {
                        int confirm = JOptionPane.showConfirmDialog(gameFrm, 
                            requester + " muốn kết bạn với bạn!", "Lời mời kết bạn", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                             try {
                                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                                out.writeUTF("MAKE_FRIEND_CONFIRM|" + requester);
                                out.flush();
                             } catch(Exception e) {}
                        }
                    });
                }
                
                // --- NHẬN TIN NHẮN RIÊNG (PRIVATE CHAT) ---
                else if (command.equals("CHAT_FROM")) {
                    String sender = data[1];
                    String msg = data[2];
                    SwingUtilities.invokeLater(() -> {
                        gameFrm.incomingMessage(sender, msg);
                    });
                }

                // --- 3. XỬ LÝ PHÒNG ---
                else if (command.equals("ROOM_LIST")) {
                    Vector<String> rooms = new Vector<>();
                    for (int i = 1; i < data.length; i++) rooms.add(data[i]);
                    if (roomListFrm != null && roomListFrm.isVisible()) {
                        roomListFrm.updateData(rooms);
                    }
                }
                else if (command.equals("ROOM_CREATED")) {
                    String roomId = data[1];
                    SwingUtilities.invokeLater(() -> {
                        waitingRoomFrm = new WaitingRoomFrm(gameFrm, socket, roomId, false);
                        waitingRoomFrm.setVisible(true); 
                    });
                }
                else if (command.equals("QUICK_PLAY_FOUND")) {
                    String foundRoomId = data[1];
                    SwingUtilities.invokeLater(() -> {
                        int confirm = JOptionPane.showConfirmDialog(gameFrm, 
                            "Tìm thấy phòng trống (" + foundRoomId + ")! Vào ngay?", "Tìm thấy phòng", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            try {
                                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                                out.writeUTF("JOIN_ROOM|" + foundRoomId + "|"); 
                                out.flush();
                            } catch (Exception e) {}
                        }
                    });
                }
                else if (command.equals("QUICK_PLAY_WAIT")) {
                    String createdRoomId = data[1];
                    SwingUtilities.invokeLater(() -> {
                        waitingRoomFrm = new WaitingRoomFrm(gameFrm, socket, createdRoomId, true);
                        waitingRoomFrm.setVisible(true);
                    });
                }
                
                // --- 4. GAME PLAY ---
                else if (command.equals("START_GAME")) {
                    if (rankFrm != null && rankFrm.isVisible()) {
                        rankFrm.dispose();
                        rankFrm = null;
                    }
                    if (waitingRoomFrm != null) waitingRoomFrm.dispose();
                    if (roomListFrm != null) roomListFrm.dispose();
                    
                    String rivalName = data[1];
                    String side = data[2]; 
                    gameFrm.setVisible(false); 
                    if (this.gameBoard != null) this.gameBoard.dispose();
                    
                    SwingUtilities.invokeLater(() -> {
                        this.gameBoard = new GameBoardFrm(socket, gameFrm, gameFrm.getUsername(), rivalName, side);
                        this.gameBoard.setVisible(true); 
                    });
                }
                else if (command.equals("CARO")) {
                    int x = Integer.parseInt(data[1]);
                    int y = Integer.parseInt(data[2]);
                    if (this.gameBoard != null) this.gameBoard.addCompetitorMove(x, y);
                }
                else if (command.equals("CHAT")) {
                    String content = data[1];
                    if (gameBoard != null) gameBoard.addMessage("Đối thủ: " + content);
                }
                else if (command.equals("TIMEOUT")) {
                    if (gameBoard != null) gameBoard.handleCompetitorTimeout();
                }
                
                // --- 5. KẾT THÚC / XỬ LÝ ---
                else if (command.equals("EXIT_GAME")) {
                    if (this.gameBoard != null) {
                        JOptionPane.showMessageDialog(this.gameBoard, "Đối thủ đã thoát game!");
                        this.gameBoard.dispose(); 
                        this.gameBoard = null;    
                        gameFrm.setVisible(true); 
                    }
                }
                else if (command.equals("WARN")) {
                    String msg = data[1];
                    JOptionPane.showMessageDialog(gameFrm, msg, "Thông báo", JOptionPane.WARNING_MESSAGE);
                }
                
                else if (command.equals("BROADCAST")) {
                    String msg = data[1];
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(gameFrm, "THÔNG BÁO TỪ SERVER:\n" + msg, "Thông báo Admin", JOptionPane.INFORMATION_MESSAGE);
                    });
                }
                
                else if (command.equals("GAME_RESULT")) {
                    String winner = data[1];
                    int winnerScore = Integer.parseInt(data[2]);
                    String loser = data[3];
                    int loserScore = Integer.parseInt(data[4]);
                    SwingUtilities.invokeLater(() -> {
                        if (gameBoard != null) gameBoard.showResultDialog(winner, winnerScore, loser, loserScore);
                    });
                }
                
                // --- 6. HÒA (DRAW) ---
                else if (command.equals("DRAW_REQUEST")) {
                    String requester = data[1];
                    SwingUtilities.invokeLater(() -> {
                        if (gameBoard != null) {
                            gameBoard.stopTimerNow();
                            int confirm = JOptionPane.showConfirmDialog(gameBoard, 
                                "Đối thủ " + requester + " muốn cầu hòa.\nBạn đồng ý không?", "Lời cầu hòa", JOptionPane.YES_NO_OPTION);
                            try {
                                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                                if (confirm == JOptionPane.YES_OPTION) out.writeUTF("DRAW_CONFIRM|" + requester);
                                else {
                                    out.writeUTF("DRAW_REFUSE|" + requester);
                                    gameBoard.resumeTimerNow();
                                }
                                out.flush();
                            } catch (Exception e) {}
                        }
                    });
                }
                else if (command.equals("GAME_DRAW")) {
                    SwingUtilities.invokeLater(() -> {
                        if (gameBoard != null) {
                            gameBoard.stopTimerNow();
                            gameBoard.showDrawDialog(0, 0); 
                        }
                    });
                }
                else if (command.equals("DRAW_REFUSE")) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(gameBoard, "Đối thủ không đồng ý hòa! Đánh tiếp.");
                        if (gameBoard != null) gameBoard.resumeTimerNow();
                    });
                }

                // --- 7. MỜI / CHƠI LẠI ---
                else if (command.equals("INVITE_REQUEST")) {
                    String inviter = data[1];
                    SwingUtilities.invokeLater(() -> {
                        int confirm = JOptionPane.showConfirmDialog(gameFrm, 
                                "Người chơi " + inviter + " muốn thách đấu!", "Lời mời", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            try {
                                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                                out.writeUTF("ACCEPT_INVITE|" + inviter);
                                out.flush();
                            } catch (IOException e) {}
                        }
                    });
                }
                else if (command.equals("REMATCH_REQUEST")) {
                    String inviter = data[1];
                    SwingUtilities.invokeLater(() -> {
                        int confirm = JOptionPane.showConfirmDialog(gameBoard, 
                            "Đối thủ " + inviter + " muốn chơi lại!", "Đấu lại", JOptionPane.YES_NO_OPTION);
                        try {
                            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                            if (confirm == JOptionPane.YES_OPTION) out.writeUTF("REMATCH_ACCEPT|" + inviter);
                            else {
                                out.writeUTF("REMATCH_REFUSE|" + inviter);
                                if (gameBoard != null) { gameBoard.dispose(); gameBoard = null; }
                                gameFrm.setVisible(true);
                            }
                            out.flush();
                        } catch (Exception e) {}
                    });
                }
                else if (command.equals("REMATCH_REFUSE")) {
                    JOptionPane.showMessageDialog(gameBoard, "Đối thủ từ chối chơi lại!");
                    if (gameBoard != null) { gameBoard.dispose(); gameBoard = null; }
                    gameFrm.setVisible(true);
                }
                
                // --- 8. THÔNG TIN & RANK ---
                else if (command.equals("RETURN_INFO")) {
                    int win = Integer.parseInt(data[1]);
                    int lose = Integer.parseInt(data[2]);
                    int draw = Integer.parseInt(data[3]);
                    int score = Integer.parseInt(data[4]);
                    String username = data[5];
                    new PlayerProfileFrm(username, win, lose, draw, score);
                }
                else if (command.equals("RETURN_RANK")) {
                    String rankData = "";
                    if (data.length > 1) rankData = data[1];
                    String finalRankData = rankData;
                    SwingUtilities.invokeLater(() -> {
                        Vector<String> myFriends = gameFrm.getFriendListNames();
                        rankFrm = new RankFrm(socket, gameFrm.getUsername(), finalRankData, myFriends);
                    });
                }
                
            } 
        } catch (IOException e) {
            System.out.println("Mất kết nối tới Server!");
        }
    }
}