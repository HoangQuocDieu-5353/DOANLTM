package server_dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.MessageDigest; 
import java.nio.charset.StandardCharsets;

public class DAO {
    public static Connection con;
    
    public DAO() {
        if(con == null){
            String dbUrl = "jdbc:mysql://localhost:3306/carodb";
            String dbClass = "com.mysql.jdbc.Driver";
            String user = "root";
            String pass = ""; // Nhớ điền pass MySQL của bro nếu có
            try {
                Class.forName(dbClass);
                con = DriverManager.getConnection(dbUrl, user, pass);
                System.out.println("Ket noi DB thanh cong");
            } catch(Exception e) {
                System.out.println("Loi ket noi DB!");
            }
        }
    }

    private String encrypt(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashInBytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return text; 
        }
    }

    public boolean register(String username, String password, String nickname) {
        try {
            if (con == null || con.isClosed()) new DAO();
            
            String sqlCheck = "SELECT * FROM user WHERE Username = ?";
            PreparedStatement psCheck = con.prepareStatement(sqlCheck);
            psCheck.setString(1, username);
            if (psCheck.executeQuery().next()) {
                return false;
            }
            
            // Mặc định khi tạo mới IsBanned = 0 (trong DB đã set default, nhưng code này ko cần sửa)
            String sqlInsert = "INSERT INTO user (Username, Password, Nickname) VALUES (?, ?, ?)";
            PreparedStatement psInsert = con.prepareStatement(sqlInsert);
            psInsert.setString(1, username);
            psInsert.setString(2, encrypt(password)); 
            psInsert.setString(3, nickname);
            return psInsert.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // --- [UPDATE] CHECK THÊM TRẠNG THÁI BANNED ---
    public String checkLogin(String username, String password) {
        try {
            if (con == null || con.isClosed()) new DAO();
            
            String hashedPass = encrypt(password);
            
            String sql = "SELECT * FROM user WHERE Username = ? AND Password = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, hashedPass); 
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Kiểm tra xem có bị khóa không
                int isBanned = rs.getInt("IsBanned");
                if (isBanned == 1) {
                    return "BANNED"; // Trả về cờ hiệu này để ServerThread xử lý
                }
                return rs.getString("Nickname");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // --- [NEW] HÀM CẬP NHẬT TRẠNG THÁI KHÓA NICK (CHO ADMIN) ---
    public void updateBanStatus(String nickname, boolean ban) {
        String sql = "UPDATE user SET IsBanned = ? WHERE Nickname = ?";
        try {
            if (con == null || con.isClosed()) new DAO();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, ban ? 1 : 0);
            ps.setString(2, nickname);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void updateStatus(String nickname, boolean isOnline) {
        String sql = "UPDATE user SET IsOnline = ? WHERE Nickname = ?";
        try {
            if (con == null || con.isClosed()) new DAO();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, isOnline ? 1 : 0);
            ps.setString(2, nickname);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void updatePlaying(String nickname, boolean isPlaying) {
        String sql = "UPDATE user SET IsPlaying = ? WHERE Nickname = ?";
        try {
            if (con == null || con.isClosed()) new DAO();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, isPlaying ? 1 : 0);
            ps.setString(2, nickname);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- CẬP NHẬT KẾT QUẢ VỚI LOGIC TÍNH ĐIỂM MỚI ---
    public void updateResult(String nickname, String type) {
        String sql = "";
        try {
            if (con == null || con.isClosed()) new DAO();
            
            // 1. Thắng: +10 điểm, +1 WinCount
            if (type.equals("WIN")) {
                sql = "UPDATE user SET WinCount = WinCount + 1, Score = Score + 10 WHERE Nickname = ?";
            } 
            // 2. Thua: -5 điểm (Không âm), +1 LoseCount
            else if (type.equals("LOSE")) {
                sql = "UPDATE user SET LoseCount = LoseCount + 1, Score = GREATEST(0, Score - 5) WHERE Nickname = ?";
            } 
            // 3. Hòa: +2 điểm, +1 DrawCount
            else if (type.equals("DRAW")) {
                sql = "UPDATE user SET DrawCount = DrawCount + 1, Score = Score + 2 WHERE Nickname = ?";
            } 
            // 4. Đầu hàng/Thoát: -10 điểm, +1 LoseCount
            else if (type.equals("SURRENDER")) {
                sql = "UPDATE user SET LoseCount = LoseCount + 1, Score = GREATEST(0, Score - 10) WHERE Nickname = ?";
            }

            if (!sql.isEmpty()) {
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, nickname);
                ps.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public String getUserStats(String nickname) {
        String sql = "SELECT WinCount, LoseCount, DrawCount, Score FROM user WHERE Nickname = ?";
        try {
            if (con == null || con.isClosed()) new DAO();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nickname);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("WinCount") + "|" +
                       rs.getInt("LoseCount") + "|" +
                       rs.getInt("DrawCount") + "|" +
                       rs.getInt("Score");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return "0|0|0|0";
    }

    public void resetAllStatus() {
        String sql = "UPDATE user SET IsOnline = 0, IsPlaying = 0";
        try {
            if (con == null || con.isClosed()) new DAO();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

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
    
    // --- [UPDATE] LẤY BẢNG XẾP HẠNG (KÈM TRẠNG THÁI ONLINE) ---
    public String getLeaderboard() {
        String result = "";
        // Thêm IsOnline vào câu Query
        String sql = "SELECT Nickname, Score, WinCount, IsOnline FROM user ORDER BY Score DESC LIMIT 10";
        try {
            if (con == null || con.isClosed()) new DAO();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result += rs.getString("Nickname") + "," + 
                          rs.getInt("Score") + "," + 
                          rs.getInt("WinCount") + "," +
                          rs.getInt("IsOnline") + ";"; 
            }
        } catch (Exception e) { e.printStackTrace(); }
        return result;
    }

    // ========================================================================
    // --- CÁC HÀM HỖ TRỢ KẾT BẠN ---
    // ========================================================================

    public int getUserId(String nickname) {
        try {
            if (con == null || con.isClosed()) new DAO();
            PreparedStatement ps = con.prepareStatement("SELECT ID FROM user WHERE Nickname = ?");
            ps.setString(1, nickname);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("ID");
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    public void addFriendship(String nick1, String nick2) {
        int id1 = getUserId(nick1);
        int id2 = getUserId(nick2);
        if (id1 == -1 || id2 == -1) return;
        
        try {
            if (con == null || con.isClosed()) new DAO();
            String check = "SELECT * FROM friend WHERE (User1 = ? AND User2 = ?) OR (User1 = ? AND User2 = ?)";
            PreparedStatement psCheck = con.prepareStatement(check);
            psCheck.setInt(1, id1); psCheck.setInt(2, id2);
            psCheck.setInt(3, id2); psCheck.setInt(4, id1);
            if (psCheck.executeQuery().next()) return; 

            String sql = "INSERT INTO friend(User1, User2) VALUES (?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id1);
            ps.setInt(2, id2);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public String getFriendList(String nickname) {
        int id = getUserId(nickname);
        StringBuilder res = new StringBuilder();
        String sql = "SELECT u.Nickname, u.IsOnline FROM user u " +
                     "JOIN friend f ON (u.ID = f.User1 OR u.ID = f.User2) " +
                     "WHERE (f.User1 = ? OR f.User2 = ?) AND u.ID != ?";
        try {
            if (con == null || con.isClosed()) new DAO();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ps.setInt(2, id);
            ps.setInt(3, id); 
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String name = rs.getString("Nickname");
                int isOnline = rs.getInt("IsOnline");
                res.append(name).append(",").append(isOnline).append(";");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return res.toString();
    }
}