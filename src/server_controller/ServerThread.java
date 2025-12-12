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
                        write("LOGIN_OK|" + nickname); 
                        System.out.println("User " + username + " login thanh cong!");
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
                        targetThread.write("INVITE_REQUEST|" + this.clientName);
                    }
                }
                
                // --- ACCEPT_INVITE ---
                else if (command.equals("ACCEPT_INVITE")) {
                    String inviterName = data[1];
                    ServerThread inviterThread = ServerControl.getServerThreadByName(inviterName);
                    if (inviterThread != null) {
                        inviterThread.write("START_GAME|" + this.clientName + "|X");
                        this.write("START_GAME|" + inviterName + "|O");
                    }
                }

                // ================================================================
                // --- THÊM PHẦN NÀY ĐỂ XỬ LÝ NƯỚC ĐI (QUAN TRỌNG) ---
                // ================================================================
                else if (command.equals("CARO")) {
                    // Cấu trúc nhận từ Client: CARO | x | y | Tên_Đối_Thủ
                    int x = Integer.parseInt(data[1]);
                    int y = Integer.parseInt(data[2]);
                    String competitorName = data[3];
                    
                    // Tìm luồng của đối thủ
                    ServerThread competitorThread = ServerControl.getServerThreadByName(competitorName);
                    
                    if (competitorThread != null) {
                        // Gửi tọa độ về cho đối thủ vẽ
                        // Cấu trúc gửi đi: CARO | x | y
                        competitorThread.write("CARO|" + x + "|" + y);
                    } else {
                        System.out.println("Khong tim thay doi thu: " + competitorName);
                    }
                }
                // ================================================================

                // --- Xử lý thoát game (nếu cần) ---
                else if (command.equals("EXIT_GAME")) {
                     String competitorName = data[1];
                     ServerThread competitorThread = ServerControl.getServerThreadByName(competitorName);
                     if (competitorThread != null) {
                         competitorThread.write("EXIT_GAME");
                     }
                }

            } 
        } catch (IOException e) {
            System.out.println(clientName + " da ngat ket noi");
            ServerControl.listServerThreads.remove(this);
            ServerControl.notifyAllPlayers();
        }
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