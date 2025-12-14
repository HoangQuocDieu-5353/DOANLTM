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
            String dbClass = "com.mysql.jdbc.Driver"; 
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
    
    // --- CÁC HÀM CŨ (GIỮ NGUYÊN) ---
    public boolean register(String username, String password, String nickname) {
        try {
            if (con == null || con.isClosed()) new DAO();

            String sqlCheck = "SELECT * FROM user WHERE Username = ?";
            PreparedStatement psCheck = con.prepareStatement(sqlCheck);
            psCheck.setString(1, username);
            if (psCheck.executeQuery().next()) {
                return false; 
            }

            String sqlInsert = "INSERT INTO user (Username, Password, Nickname) VALUES (?, ?, ?)";
            PreparedStatement psInsert = con.prepareStatement(sqlInsert);
            psInsert.setString(1, username);
            psInsert.setString(2, password);
            psInsert.setString(3, nickname); 

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
                return rs.getString("Nickname"); 
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // ========================================================================
    // --- CÁC HÀM MỚI CẬP NHẬT (DÙNG NICKNAME LÀM KHÓA) ---
    // ========================================================================

    // 1. Cập nhật trạng thái Online (1) / Offline (0)
    public void updateStatus(String nickname, boolean isOnline) {
        // ĐỔI WHERE Username -> WHERE Nickname
        String sql = "UPDATE user SET IsOnline = ? WHERE Nickname = ?"; 
        try {
            if (con == null || con.isClosed()) new DAO();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, isOnline ? 1 : 0);
            ps.setString(2, nickname); 
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // 2. Cập nhật trạng thái Đang chơi (1) / Rảnh (0)
    public void updatePlaying(String nickname, boolean isPlaying) {
        // ĐỔI WHERE Username -> WHERE Nickname
        String sql = "UPDATE user SET IsPlaying = ? WHERE Nickname = ?";
        try {
            if (con == null || con.isClosed()) new DAO();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, isPlaying ? 1 : 0);
            ps.setString(2, nickname);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // 3. Cập nhật Kết quả trận đấu (Thắng/Thua/Hòa)
    public void updateResult(String nickname, String type) {
        String sql = "";
        // ĐỔI WHERE Username -> WHERE Nickname cho tất cả các câu lệnh
        if (type.equals("WIN")) {
            // Thắng: +1 WinCount, +10 Score
            sql = "UPDATE user SET WinCount = WinCount + 1, Score = Score + 10 WHERE Nickname = ?";
        } else if (type.equals("LOSE")) {
            // Thua: +1 LoseCount
            sql = "UPDATE user SET LoseCount = LoseCount + 1 WHERE Nickname = ?";
        } else if (type.equals("DRAW")) {
            // Hòa: +1 DrawCount
            sql = "UPDATE user SET DrawCount = DrawCount + 1 WHERE Nickname = ?";
        }
        
        try {
            if (con == null || con.isClosed()) new DAO();
            if (!sql.isEmpty()) {
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, nickname);
                ps.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // 4. Lấy thông tin User (Cho chức năng xem Profile)
    public String getUserStats(String nickname) {
        // ĐỔI WHERE Username -> WHERE Nickname
        String sql = "SELECT WinCount, LoseCount, DrawCount, Score FROM user WHERE Nickname = ?";
        try {
            if (con == null || con.isClosed()) new DAO();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nickname);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Trả về chuỗi: Win|Lose|Draw|Score
                return rs.getInt("WinCount") + "|" + 
                       rs.getInt("LoseCount") + "|" + 
                       rs.getInt("DrawCount") + "|" + 
                       rs.getInt("Score");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return "0|0|0|0"; // Mặc định nếu lỗi
    }

    // 5. Reset toàn bộ trạng thái khi khởi động Server (Tránh kẹt acc)
    // Hàm này không cần tham số nickname vì nó reset TOÀN BỘ bảng
    public void resetAllStatus() {
        String sql = "UPDATE user SET IsOnline = 0, IsPlaying = 0";
        try {
            if (con == null || con.isClosed()) new DAO();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
    // 6. Kiểm tra xem User có đang bận không (IsPlaying = 1)
    public boolean checkIsPlaying(String nickname) {
        String sql = "SELECT IsPlaying FROM user WHERE Nickname = ?";
        try {
            if (con == null || con.isClosed()) new DAO();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nickname);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("IsPlaying") == 1;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }
}