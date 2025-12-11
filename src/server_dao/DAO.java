package server_dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DAO {
    public static Connection con;
    
    public DAO() {
        if(con == null){
            String dbUrl = "jdbc:mysql://localhost:3306/carodb";
            String dbClass = "com.mysql.jdbc.Driver"; // Bản 5
            String user = "root";
            String pass = "";
            try {
                Class.forName(dbClass);
                con = DriverManager.getConnection(dbUrl, user, pass);
                System.out.println("Ket noi DB thanh cong");
            } catch(Exception e) {
                System.out.println("Loi ket noi DB!");
            }
        }
    }
    
    // --- HÀM CHECK LOGIN (Chuyển từ ServerThread sang đây) ---
    public boolean checkLogin(String username, String password) {
        try {
            // Đảm bảo kết nối
            if (con == null || con.isClosed()) new DAO();
            
            String sql = "SELECT * FROM user WHERE Username = ? AND Password = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return true; // Tìm thấy user
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // Không tìm thấy hoặc lỗi
    }
    public boolean register(String username, String password) {
        try {
            if (con == null || con.isClosed()) new DAO();
            
            // 1. Kiểm tra xem user đã tồn tại chưa
            String sqlCheck = "SELECT * FROM user WHERE Username = ?";
            PreparedStatement psCheck = con.prepareStatement(sqlCheck);
            psCheck.setString(1, username);
            
            ResultSet rs = psCheck.executeQuery();
            if (rs.next()) {
                // Đã tồn tại -> Không cho đăng ký
                System.out.println("User " + username + " da ton tai!");
                return false; 
            }
            
            // 2. Nếu chưa tồn tại -> Thêm mới
            // Các cột Score, Win... đã để Default 0 trong SQL nên không cần Insert
            String sqlInsert = "INSERT INTO user (Username, Password) VALUES (?, ?)";
            PreparedStatement psInsert = con.prepareStatement(sqlInsert);
            psInsert.setString(1, username);
            psInsert.setString(2, password);
            
            // executeUpdate trả về số dòng thay đổi (>0 là thành công)
            int rows = psInsert.executeUpdate();
            return rows > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}