package server_controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import server_dao.DAO; // Import DAO

public class ServerThread extends Thread {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    
    // Khai báo đối tượng DAO để dùng
    private DAO dao; 

    public ServerThread(Socket socket) {
        this.socket = socket;
        this.dao = new DAO(); // Khởi tạo DAO
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            
            while(true) {
                String message = in.readUTF(); 
                System.out.println("Nhan tu Client: " + message);
                
                // Cắt chuỗi theo giao thức chuẩn dấu |
                String[] data = message.split("\\|"); 
                String command = data[0];
                
                if(command.equals("LOGIN")) {
                    String username = data[1];
                    String pass = data[2];
                    
                    // --- GỌI SANG DAO ĐỂ KIỂM TRA ---
                    // ServerThread không tự check nữa, mà nhờ DAO check hộ
                    boolean isValid = dao.checkLogin(username, pass);
                    
                    if(isValid) {
                        out.writeUTF("LOGIN_OK|" + username); 
                        System.out.println("User " + username + " login thanh cong!");
                    } else {
                        out.writeUTF("LOGIN_FAIL"); 
                        System.out.println("Login that bai!");
                    }
                    out.flush();
                }
                else if (command.equals("REGISTER")) {
                    // Cấu trúc: REGISTER|username|password
                    String username = data[1];
                    String pass = data[2];
                    
                    // Gọi hàm register bên DAO
                    boolean isSuccess = dao.register(username, pass);
                    
                    if (isSuccess) {
                        out.writeUTF("REGISTER_OK");
                        System.out.println("User " + username + " dang ky thanh cong!");
                    } else {
                        out.writeUTF("REGISTER_FAIL");
                        System.out.println("User " + username + " dang ky that bai (trung ten)!");
                    }
                    out.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Client da ngat ket noi");
        }
    }
}