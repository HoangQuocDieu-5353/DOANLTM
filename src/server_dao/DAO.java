/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server_dao;

/**
 *
 * @author Admin
 */
import java.sql.Connection;
import java.sql.DriverManager;

public class DAO {
    // Biến con (connection) dùng chung cho toàn bộ server
    // static để đảm bảo chỉ có duy nhất 1 kết nối được tạo ra
    public static Connection con;
    
    public DAO() {
        if(con == null){
            // Thông số kết nối XAMPP mặc định
            String dbUrl = "jdbc:mysql://localhost:3306/carodb";
            String dbClass = "com.mysql.cj.jdbc.Driver";
            String user = "root";     // Mặc định XAMPP là root
            String pass = "";         // Mặc định XAMPP không có mật khẩu

            try {
                // 1. Nạp Driver
                Class.forName(dbClass);
                
                // 2. Mở kết nối
                con = DriverManager.getConnection(dbUrl, user, pass);
                
                System.out.println("Kết nối Database thành công!");
                
            } catch(Exception e) {
                // In lỗi ra nếu kết nối thất bại (để debug)
                System.out.println("Lỗi kết nối Database!");
                e.printStackTrace();
            }
        }
    }
}