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
}