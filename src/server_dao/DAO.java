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
    
    public boolean register(String username, String password, String nickname) {
        try {
            if (con == null || con.isClosed()) new DAO();

            // Check trùng username
            String sqlCheck = "SELECT * FROM user WHERE Username = ?";
            PreparedStatement psCheck = con.prepareStatement(sqlCheck);
            psCheck.setString(1, username);
            if (psCheck.executeQuery().next()) {
                return false; // Đã tồn tại user
            }

            // INSERT thêm cột Nickname
            String sqlInsert = "INSERT INTO user (Username, Password, Nickname) VALUES (?, ?, ?)";
            PreparedStatement psInsert = con.prepareStatement(sqlInsert);
            psInsert.setString(1, username);
            psInsert.setString(2, password);
            psInsert.setString(3, nickname); // <--- THÊM DÒNG NÀY

            return psInsert.executeUpdate() > 0;

        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public String checkLogin(String username, String password) {
        try {
            if (con == null || con.isClosed()) new DAO();
            String sql = "SELECT * FROM user WHERE Username = ? AND Password = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Trả về Nickname để hiển thị
                return rs.getString("Nickname"); 
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }
}