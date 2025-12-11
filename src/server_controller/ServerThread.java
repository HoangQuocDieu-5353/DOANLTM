/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server_controller;

/**
 *
 * @author Admin
 */
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import server_dao.DAO;

public class ServerThread extends Thread {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private int clientID;
    
    // Constructor nhận socket từ ServerControl
    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Tạo luồng đọc/ghi dữ liệu (Dạng chuỗi cho đơn giản)
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            
            // Vòng lặp liên tục lắng nghe Client nói gì
            while(true) {
                // Đọc chuỗi tin nhắn từ Client gửi lên
                String message = in.readUTF(); 
                System.out.println("Nhận từ Client: " + message);
                
                // Phân tích tin nhắn (Giao thức: "Lệnh;ThamSo1;ThamSo2...")
                String[] data = message.split(";");
                String command = data[0];
                
                if(command.equals("login")) {
                    // Cấu trúc: login;email;password
                    String email = data[1];
                    String pass = data[2];
                    
                    // Gọi hàm kiểm tra DB
                    if(checkLogin(email, pass)) {
                        out.writeUTF("login-success"); // Trả lời thành công
                        System.out.println("User " + email + " đã đăng nhập!");
                    } else {
                        out.writeUTF("login-fail"); // Trả lời thất bại
                    }
                    out.flush(); // Đẩy dữ liệu đi ngay lập tức
                }
                
                // Sau này thêm các lệnh khác: "move", "chat", "logout"... ở đây
            }
            
        } catch (IOException e) {
            System.out.println("Client đã ngắt kết nối!");
        }
    }
    
    // Hàm kiểm tra tài khoản trong Database
    private boolean checkLogin(String email, String pass) {
        try {
            Connection con = DAO.con; // Lấy kết nối từ class DAO
            String sql = "SELECT * FROM user WHERE Username = ? AND Password = ?";
            
            // Dùng PreparedStatement để chống hack SQL Injection
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, pass);
            
            ResultSet rs = ps.executeQuery();
            
            // Nếu rs.next() trả về true nghĩa là tìm thấy dòng dữ liệu => Đúng
            if (rs.next()) {
                this.clientID = rs.getInt("ID"); // Lưu lại ID người chơi
                return true;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
