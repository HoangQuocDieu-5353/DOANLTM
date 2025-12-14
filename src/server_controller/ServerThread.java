package server_controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import server_dao.DAO; 

public class ServerThread extends Thread {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private DAO dao; 
    
    // Biến lưu Nickname
    private String clientName = null;
    
    // Lưu luồng của đối thủ đang đánh cùng
    private ServerThread competitor = null; 

    public ServerThread(Socket socket) {
        this.socket = socket;
        this.dao = new DAO(); 
    }
    
    public String getClientName() {
        return clientName;
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            
            while(true) {
                String message = in.readUTF(); 
                System.out.println("Nhan tu Client: " + message);
                
                String[] data = message.split("\\|"); 
                String command = data[0];
                
                // --- LOGIN ---
                if(command.equals("LOGIN")) {
                    String username = data[1];
                    String pass = data[2];
                    String nickname = dao.checkLogin(username, pass);
                    
                    if(nickname != null) {
                        this.clientName = nickname;
                        dao.updateStatus(nickname, true); 
                        write("LOGIN_OK|" + nickname); 
                        System.out.println("User " + nickname + " login thanh cong!");
                        ServerControl.notifyAllPlayers();
                    } else {
                        write("LOGIN_FAIL"); 
                    }
                }
                
                // --- REGISTER ---
                else if (command.equals("REGISTER")) {
                    String username = data[1];
                    String pass = data[2];
                    String nick = data[3];
                    boolean isSuccess = dao.register(username, pass, nick);
                    if (isSuccess) write("REGISTER_OK");
                    else write("REGISTER_FAIL");
                }

                // --- INVITE ---
                else if (command.equals("INVITE")) {
                    String targetName = data[1];
                    ServerThread targetThread = ServerControl.getServerThreadByName(targetName);
                    
                    if (targetThread != null) {
                        if (dao.checkIsPlaying(targetName)) {
                            write("WARN|Người chơi " + targetName + " đang bận chơi với người khác!");
                        } else {
                            targetThread.write("INVITE_REQUEST|" + this.clientName);
                        }
                    } else {
                        write("WARN|Người chơi " + targetName + " hiện không online!");
                    }
                }
                
                // --- ACCEPT_INVITE ---
                else if (command.equals("ACCEPT_INVITE")) {
                    String inviterName = data[1];
                    ServerThread inviterThread = ServerControl.getServerThreadByName(inviterName);
                    
                    if (inviterThread != null) {
                        inviterThread.write("START_GAME|" + this.clientName + "|X");
                        this.write("START_GAME|" + inviterName + "|O");
                        
                        this.competitor = inviterThread;
                        inviterThread.competitor = this;
                        
                        dao.updatePlaying(this.clientName, true);
                        dao.updatePlaying(inviterName, true);
                    }
                }

                // --- CARO ---
                else if (command.equals("CARO")) {
                    int x = Integer.parseInt(data[1]);
                    int y = Integer.parseInt(data[2]);
                    String competitorName = data[3];
                    ServerThread competitorThread = ServerControl.getServerThreadByName(competitorName);
                    if (competitorThread != null) {
                        competitorThread.write("CARO|" + x + "|" + y);
                    }
                }
                
                // --- CHAT ---
                else if (command.equals("CHAT")) {
                    String content = data[1];
                    String competitorName = data[2];
                    ServerThread competitor = ServerControl.getServerThreadByName(competitorName);
                    if (competitor != null) {
                        competitor.write("CHAT|" + content);
                    }
                }

                // --- TIMEOUT ---
                else if (command.equals("TIMEOUT")) {
                    String competitorName = data[1];
                    ServerThread competitor = ServerControl.getServerThreadByName(competitorName);
                    if (competitor != null) {
                        competitor.write("TIMEOUT");
                    }
                }
                
                // ================================================================
                // --- CÁC TÍNH NĂNG KẾT QUẢ & REMATCH (ĐÃ CẬP NHẬT) ---
                // ================================================================

                // --- GAME_OVER (Kết thúc trận đấu) ---
                else if (command.equals("GAME_OVER")) {
                    String winner = data[1];
                    String loser = data[2];
                    
                    // 1. Update DB
                    dao.updateResult(winner, "WIN");
                    dao.updateResult(loser, "LOSE");
                    dao.updatePlaying(winner, false);
                    dao.updatePlaying(loser, false);
                    
                    // 2. Lấy điểm số mới nhất để gửi về Client
                    int winnerScore = getScoreFromDb(winner);
                    int loserScore = getScoreFromDb(loser);
                    
                    // 3. Gửi GAME_RESULT cho cả 2
                    String msg = "GAME_RESULT|" + winner + "|" + winnerScore + "|" + loser + "|" + loserScore;
                    this.write(msg); 
                    
                    // Reset đối thủ
                    ServerThread loserThread = ServerControl.getServerThreadByName(loser);
                    if (loserThread != null) {
                        loserThread.write(msg);
                        loserThread.competitor = null; 
                    }
                    this.competitor = null; 
                    
                    System.out.println("Tran dau ket thuc: " + winner + " thang " + loser);
                }
                
                // --- SURRENDER (Đầu hàng chủ động) ---
                else if (command.equals("SURRENDER")) {
                    String winner = data[1]; 
                    String loser = this.clientName;
                    
                    dao.updateResult(winner, "WIN");
                    dao.updateResult(loser, "LOSE");
                    dao.updatePlaying(winner, false);
                    dao.updatePlaying(loser, false);
                    
                    int winnerScore = getScoreFromDb(winner);
                    int loserScore = getScoreFromDb(loser);
                    
                    String msg = "GAME_RESULT|" + winner + "|" + winnerScore + "|" + loser + "|" + loserScore;
                    this.write(msg); 
                    
                    if (this.competitor != null) {
                        this.competitor.write(msg); 
                        this.competitor.competitor = null;
                    }
                    this.competitor = null;
                }
                
                // --- REMATCH_REQUEST (Yêu cầu đấu lại) ---
                else if (command.equals("REMATCH_REQUEST")) {
                    String competitorName = data[1];
                    ServerThread competitor = ServerControl.getServerThreadByName(competitorName);
                    if (competitor != null) {
                        competitor.write("REMATCH_REQUEST|" + this.clientName);
                    } else {
                        write("WARN|Đối thủ đã thoát, không thể chơi lại!");
                    }
                }

                // --- REMATCH_ACCEPT (Đồng ý đấu lại) ---
                else if (command.equals("REMATCH_ACCEPT")) {
                    String inviterName = data[1];
                    ServerThread inviter = ServerControl.getServerThreadByName(inviterName);
                    
                    if (inviter != null) {
                        // Bắt đầu game mới: Người mời (thua trước đó) đi trước
                        inviter.write("START_GAME|" + this.clientName + "|X");
                        this.write("START_GAME|" + inviterName + "|O");
                        
                        dao.updatePlaying(this.clientName, true);
                        dao.updatePlaying(inviterName, true);
                        
                        this.competitor = inviter;
                        inviter.competitor = this;
                    }
                }

                // --- REMATCH_REFUSE (Từ chối đấu lại) ---
                else if (command.equals("REMATCH_REFUSE")) {
                    String inviterName = data[1];
                    ServerThread inviter = ServerControl.getServerThreadByName(inviterName);
                    if (inviter != null) {
                        inviter.write("REMATCH_REFUSE");
                    }
                }
                
                // --- GET_INFO ---
                else if (command.equals("GET_INFO")) {
                    String targetUser = data[1];
                    String stats = dao.getUserStats(targetUser); 
                    this.write("RETURN_INFO|" + stats + "|" + targetUser);
                }

            } 
        } catch (IOException e) {
            // --- XỬ LÝ NGẮT KẾT NỐI ---
            System.out.println(clientName + " da ngat ket noi");
            
            if (this.competitor != null) {
                String winner = competitor.getClientName();
                String loser = this.clientName;
                
                dao.updateResult(winner, "WIN");
                dao.updateResult(loser, "LOSE");
                dao.updatePlaying(winner, false);
                dao.updatePlaying(loser, false);
                
                // Lấy điểm để hiển thị đúng cho người thắng
                int winnerScore = getScoreFromDb(winner);
                int loserScore = getScoreFromDb(loser);
                
                // Gửi GAME_RESULT thay vì COMPETITOR_QUIT để đồng bộ giao diện
                competitor.write("GAME_RESULT|" + winner + "|" + winnerScore + "|" + loser + "|" + loserScore); 
                
                competitor.competitor = null;
                this.competitor = null;
            }

            if (clientName != null) {
                dao.updateStatus(clientName, false);  
                dao.updatePlaying(clientName, false); 
            }
            ServerControl.listServerThreads.remove(this);
            ServerControl.notifyAllPlayers();
        }
    }
    
    // Hàm phụ lấy điểm nhanh
    private int getScoreFromDb(String nickname) {
        String stats = dao.getUserStats(nickname); 
        String[] s = stats.split("\\|");
        return Integer.parseInt(s[3]); 
    }
    
    public void write(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}