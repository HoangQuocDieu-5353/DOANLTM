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
import java.util.Vector; 
import server_dao.DAO;

public class ServerControl {
    private int port = 5555;
    
    // Danh sách chứa tất cả các luồng nhân viên đang chạy
    public static Vector<ServerThread> listServerThreads = new Vector<>();
    
    public ServerControl() {
        try {
            // --- CẬP NHẬT MỚI Ở ĐÂY ---
            // 1. Reset trạng thái DB trước khi nhận khách
            // Để đảm bảo không ai bị kẹt Online khi Server vừa khởi động lại
            new DAO().resetAllStatus();
            System.out.println("Da reset toan bo trang thai User trong Database ve Offline!");
            // ---------------------------
            
            // 2. Mở cổng 5555
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server dang chay tai cong " + port + "...");
            
            // 3. Vòng lặp nhận khách
            while(true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Co nguoi ket noi: " + clientSocket.getInetAddress());
                
                // Tạo luồng phục vụ
                ServerThread serverThread = new ServerThread(clientSocket);
                
                // Thêm nhân viên này vào danh sách quản lý
                listServerThreads.add(serverThread);
                
                serverThread.start();
            }
            
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    // Hàm thông báo danh sách Online cho tất cả mọi người
    public static void notifyAllPlayers() {
        String msg = "ONLINE_LIST";
        
        // Bước 1: Gom tên của những người đã đăng nhập
        for (ServerThread th : listServerThreads) {
            if (th.getClientName() != null) {
                msg += "|" + th.getClientName();
            }
        }
        
        // Bước 2: Gửi danh sách này cho tất cả mọi người
        for (ServerThread th : listServerThreads) {
            if (th.getClientName() != null) {
                th.write(msg); 
            }
        }
        System.out.println("Server broadcast: " + msg);
    }
    
    public static void main(String[] args) {
        new ServerControl();
    }
    
    public static ServerThread getServerThreadByName(String name) {
        for (ServerThread th : listServerThreads) {
            if (th.getClientName() != null && th.getClientName().equals(name)) {
                return th;
            }
        }
        return null; 
    }
}