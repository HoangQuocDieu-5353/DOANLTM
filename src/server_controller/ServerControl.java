/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server_controller;

/**
 *
 * @author Admin
 */
import java.net.ServerSocket;
import java.net.Socket;
import server_dao.DAO;

public class ServerControl {
    private int port = 5555;
    
    public ServerControl() {
        try {
            // 1. Mở kết nối Database 1 lần dùng mãi mãi
            new DAO(); 
            
            // 2. Mở cổng 5555 để lắng nghe
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server dang chay tai cong " + port + "...");
            
            // 3. Vòng lặp vô tận để liên tục nhận khách
            while(true) {
                // Lệnh này sẽ TREO máy ở đây để chờ Client kết nối
                Socket clientSocket = serverSocket.accept();
                
                System.out.println("Co nguoi ket noi: " + clientSocket.getInetAddress());
                
                // 4. Tạo một luồng riêng (ServerThread) để phục vụ người này
                ServerThread serverThread = new ServerThread(clientSocket);
                serverThread.start();
            }
            
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public static void main(String[] args) {
        // Chạy hàm main để khởi động Server
        new ServerControl();
    }
}
