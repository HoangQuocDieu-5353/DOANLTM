package server_controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;
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
    
    // Biến lưu phòng mình đang tạo (nếu có)
    private Room myRoom = null;

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
                        
                        String friends = dao.getFriendList(this.clientName);
                        write("FRIEND_LIST|" + friends);
                        
                        notifyFriendsStateChange();
                        
                        // --- [FIX QUAN TRỌNG] CẬP NHẬT GIAO DIỆN ADMIN ---
                        ServerControl.updateOnlineBoard();
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

                // ============================================================
                // --- CÁC TÍNH NĂNG KẾT BẠN & CHAT RIÊNG ---
                // ============================================================

                // --- 1. YÊU CẦU KẾT BẠN ---
                else if (command.equals("MAKE_FRIEND")) {
                    String targetName = data[1];
                    ServerThread target = ServerControl.getServerThreadByName(targetName);
                    if (target != null) {
                        target.write("MAKE_FRIEND_REQUEST|" + this.clientName);
                    } else {
                        write("WARN|Người chơi không online hoặc không tồn tại!");
                    }
                }

                // --- 2. XÁC NHẬN KẾT BẠN ---
                else if (command.equals("MAKE_FRIEND_CONFIRM")) {
                    String requester = data[1];
                    dao.addFriendship(this.clientName, requester);
                    
                    String myFriends = dao.getFriendList(this.clientName);
                    this.write("FRIEND_LIST|" + myFriends);
                    
                    ServerThread reqThread = ServerControl.getServerThreadByName(requester);
                    if (reqThread != null) {
                        String reqFriends = dao.getFriendList(requester);
                        reqThread.write("FRIEND_LIST|" + reqFriends);
                        reqThread.write("WARN|" + this.clientName + " đã đồng ý kết bạn!");
                    }
                }
                
                // --- 3. LẤY DANH SÁCH BẠN BÈ ---
                else if (command.equals("GET_FRIEND_LIST")) {
                    String friends = dao.getFriendList(this.clientName);
                    this.write("FRIEND_LIST|" + friends);
                }
                
                // --- 4. CHAT RIÊNG (PRIVATE CHAT) ---
                else if (command.equals("CHAT_TO")) {
                    String targetName = data[1];
                    String msg = data[2];
                    ServerThread target = ServerControl.getServerThreadByName(targetName);
                    
                    if (target != null) {
                        // Chuyển tiếp tin nhắn sang người nhận
                        target.write("CHAT_FROM|" + this.clientName + "|" + msg);
                    }
                }

                // ============================================================
                // --- CÁC TÍNH NĂNG PHÒNG & CHƠI NHANH ---
                // ============================================================

                else if (command.equals("CREATE_ROOM")) {
                    String pass = (data.length > 1) ? data[1] : "";
                    String roomId = "" + (1000 + new Random().nextInt(9000));
                    this.myRoom = new Room(roomId, this, pass);
                    ServerControl.listRooms.add(this.myRoom);
                    this.write("ROOM_CREATED|" + roomId);
                }

                else if (command.equals("GET_ROOM_LIST")) {
                    StringBuilder res = new StringBuilder("ROOM_LIST");
                    for (Room r : ServerControl.listRooms) {
                        String hasPass = (r.getPassword().equals("")) ? "0" : "1";
                        res.append("|").append(r.getId()).append(",").append(r.getCreator().getClientName()).append(",").append(hasPass);
                    }
                    this.write(res.toString());
                }

                else if (command.equals("JOIN_ROOM")) {
                    String idRoom = data[1];
                    String passInput = (data.length > 2) ? data[2] : "";
                    Room room = null;
                    for (Room r : ServerControl.listRooms) {
                        if (r.getId().equals(idRoom)) { room = r; break; }
                    }
                    
                    if (room == null) {
                        this.write("WARN|Phòng không tồn tại hoặc đã bị hủy!");
                    } else {
                        if (!room.getPassword().equals("") && !room.getPassword().equals(passInput)) {
                            this.write("WARN|Mật khẩu phòng không đúng!");
                        } else {
                            ServerThread roomCreator = room.getCreator();
                            ServerControl.listRooms.remove(room);
                            roomCreator.myRoom = null;
                            
                            this.write("START_GAME|" + roomCreator.getClientName() + "|X");
                            roomCreator.write("START_GAME|" + this.clientName + "|O");
                            
                            this.competitor = roomCreator;
                            roomCreator.competitor = this;
                            
                            dao.updatePlaying(this.clientName, true);
                            dao.updatePlaying(roomCreator.getClientName(), true);
                        }
                    }
                }

                else if (command.equals("CANCEL_ROOM")) {
                    if (this.myRoom != null) {
                        ServerControl.listRooms.remove(this.myRoom);
                        this.myRoom = null;
                    }
                }
                
                else if (command.equals("QUICK_PLAY")) {
                    Room foundRoom = null;
                    for (Room r : ServerControl.listRooms) {
                        if (r.getPassword().equals("")) { 
                            foundRoom = r; break; 
                        }
                    }
                    if (foundRoom != null) {
                        this.write("QUICK_PLAY_FOUND|" + foundRoom.getId());
                    } else {
                        String roomId = "" + (1000 + new Random().nextInt(9000));
                        this.myRoom = new Room(roomId, this, "");
                        ServerControl.listRooms.add(this.myRoom);
                        this.write("QUICK_PLAY_WAIT|" + roomId);
                    }
                }

                // ============================================================
                // --- CÁC LỆNH TRONG TRẬN ĐẤU ---
                // ============================================================

                else if (command.equals("CARO")) {
                    int x = Integer.parseInt(data[1]);
                    int y = Integer.parseInt(data[2]);
                    String competitorName = data[3];
                    ServerThread competitorThread = ServerControl.getServerThreadByName(competitorName);
                    if (competitorThread != null) competitorThread.write("CARO|" + x + "|" + y);
                }
                
                else if (command.equals("CHAT")) {
                    String content = data[1];
                    String competitorName = data[2];
                    ServerThread competitor = ServerControl.getServerThreadByName(competitorName);
                    if (competitor != null) competitor.write("CHAT|" + content);
                }

                else if (command.equals("TIMEOUT")) {
                    String competitorName = data[1];
                    ServerThread competitor = ServerControl.getServerThreadByName(competitorName);
                    if (competitor != null) competitor.write("TIMEOUT");
                }
                
                // --- XỬ LÝ XIN HÒA ---
                else if (command.equals("DRAW_REQUEST")) {
                    String competitorName = data[1];
                    ServerThread competitor = ServerControl.getServerThreadByName(competitorName);
                    if (competitor != null) {
                        competitor.write("DRAW_REQUEST|" + this.clientName);
                    }
                }
                
                else if (command.equals("DRAW_CONFIRM")) {
                    String requester = data[1];
                    ServerThread reqThread = ServerControl.getServerThreadByName(requester);
                    
                    dao.updateResult(this.clientName, "DRAW");
                    dao.updateResult(requester, "DRAW");
                    dao.updatePlaying(this.clientName, false);
                    dao.updatePlaying(requester, false);
                    
                    int score1 = getScoreFromDb(this.clientName);
                    int score2 = getScoreFromDb(requester);
                    
                    String msg = "GAME_DRAW|" + this.clientName + "|" + score1 + "|" + requester + "|" + score2;
                    this.write(msg);
                    
                    if (reqThread != null) {
                        reqThread.write(msg);
                        reqThread.competitor = null;
                    }
                    this.competitor = null;
                }
                
                else if (command.equals("DRAW_REFUSE")) {
                    String requester = data[1];
                    ServerThread reqThread = ServerControl.getServerThreadByName(requester);
                    if (reqThread != null) {
                        reqThread.write("DRAW_REFUSE|" + this.clientName);
                    }
                }
                
                // --- GAME_OVER ---
                else if (command.equals("GAME_OVER")) {
                    String winner = data[1];
                    String loser = data[2];
                    
                    dao.updateResult(winner, "WIN");
                    dao.updateResult(loser, "LOSE");
                    dao.updatePlaying(winner, false);
                    dao.updatePlaying(loser, false);
                    
                    int winnerScore = getScoreFromDb(winner);
                    int loserScore = getScoreFromDb(loser);
                    
                    String msg = "GAME_RESULT|" + winner + "|" + winnerScore + "|" + loser + "|" + loserScore;
                    this.write(msg); 
                    
                    ServerThread loserThread = ServerControl.getServerThreadByName(loser);
                    if (loserThread != null) {
                        loserThread.write(msg);
                        loserThread.competitor = null; 
                    }
                    this.competitor = null; 
                }
                
                // --- SURRENDER ---
                else if (command.equals("SURRENDER")) {
                    String winner = data[1]; 
                    String loser = this.clientName;
                    
                    dao.updateResult(winner, "WIN");        
                    dao.updateResult(loser, "SURRENDER");  
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
                
                // --- REMATCH ---
                else if (command.equals("REMATCH_REQUEST")) {
                    String competitorName = data[1];
                    ServerThread competitor = ServerControl.getServerThreadByName(competitorName);
                    if (competitor != null) competitor.write("REMATCH_REQUEST|" + this.clientName);
                    else write("WARN|Đối thủ đã thoát, không thể chơi lại!");
                }
                else if (command.equals("REMATCH_ACCEPT")) {
                    String inviterName = data[1];
                    ServerThread inviter = ServerControl.getServerThreadByName(inviterName);
                    if (inviter != null) {
                        inviter.write("START_GAME|" + this.clientName + "|X");
                        this.write("START_GAME|" + inviterName + "|O");
                        dao.updatePlaying(this.clientName, true);
                        dao.updatePlaying(inviterName, true);
                        this.competitor = inviter;
                        inviter.competitor = this;
                    }
                }
                else if (command.equals("REMATCH_REFUSE")) {
                    String inviterName = data[1];
                    ServerThread inviter = ServerControl.getServerThreadByName(inviterName);
                    if (inviter != null) inviter.write("REMATCH_REFUSE");
                }
                
                // --- GET_INFO ---
                else if (command.equals("GET_INFO")) {
                    String targetUser = data[1];
                    String stats = dao.getUserStats(targetUser); 
                    this.write("RETURN_INFO|" + stats + "|" + targetUser);
                }
                
                // --- GET_RANK ---
                else if (command.equals("GET_RANK")) {
                    String rankData = dao.getLeaderboard();
                    this.write("RETURN_RANK|" + rankData);
                }

            } 
        } catch (IOException e) {
            // --- NGẮT KẾT NỐI ---
            System.out.println(clientName + " da ngat ket noi");
            
            if (this.myRoom != null) {
                ServerControl.listRooms.remove(this.myRoom);
            }

            if (this.competitor != null) {
                String winner = competitor.getClientName();
                String loser = this.clientName;
                
                dao.updateResult(winner, "WIN");        
                dao.updateResult(loser, "SURRENDER"); 
                dao.updatePlaying(winner, false);
                dao.updatePlaying(loser, false);
                
                int winnerScore = getScoreFromDb(winner);
                int loserScore = getScoreFromDb(loser);
                
                competitor.write("GAME_RESULT|" + winner + "|" + winnerScore + "|" + loser + "|" + loserScore); 
                competitor.competitor = null;
                this.competitor = null;
            }

            if (clientName != null) {
                dao.updateStatus(clientName, false);  
                dao.updatePlaying(clientName, false);
                
                notifyFriendsStateChange();
            }
            ServerControl.listServerThreads.remove(this);
            
            // --- [FIX QUAN TRỌNG] CẬP NHẬT GIAO DIỆN ADMIN KHI THOÁT ---
            ServerControl.updateOnlineBoard();
        }
    }
    
    private void notifyFriendsStateChange() {
        for (ServerThread st : ServerControl.listServerThreads) {
            if (st != this && st.getClientName() != null) {
                String friendList = dao.getFriendList(st.getClientName());
                if (friendList.contains(this.clientName)) { 
                    st.write("FRIEND_LIST|" + friendList);
                }
            }
        }
    }
    
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