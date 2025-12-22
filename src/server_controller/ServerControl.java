/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server_controller;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.Socket;
import java.util.Vector;
import server_dao.DAO;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 *
 * @author Admin
 */
public class ServerControl extends JFrame implements Runnable {
    private int port = 5555;
    
    public static Vector<ServerThread> listServerThreads = new Vector<>();
    public static Vector<Room> listRooms = new Vector<>();
    
    // --- LINH KIỆN GIAO DIỆN ---
    private JTextArea txtLog;
    private JButton btnSendBroadcast;
    private JTextField txtBroadcast;
    private JTable tblOnline;
    private DefaultTableModel modelOnline;
    private JLabel lblUserCount;
    
    // Tên admin đăng nhập
    private String adminName;

    // Constructor nhận thêm tham số adminName
    public ServerControl(String adminName) {
        this.adminName = adminName;
        initUI();
        new Thread(this).start();
    }
    
    private void initUI() {
        this.setTitle("Hệ Thống Quản Trị Caro Server - Xin chào: " + adminName);
        this.setSize(800, 550);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout(10, 10)); // Gap 10px
        
        // --- PANEL TRÁI: DANH SÁCH ONLINE ---
        JPanel pnlLeft = new JPanel(new BorderLayout());
        pnlLeft.setBorder(BorderFactory.createTitledBorder("Danh sách Online"));
        pnlLeft.setPreferredSize(new Dimension(220, 0));
        
        String[] headers = {"Tên người chơi", "Trạng thái"};
        modelOnline = new DefaultTableModel(headers, 0);
        tblOnline = new JTable(modelOnline);
        tblOnline.setRowHeight(25);
        tblOnline.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tblOnline.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        pnlLeft.add(new JScrollPane(tblOnline), BorderLayout.CENTER);
        
        lblUserCount = new JLabel("Online: 0");
        lblUserCount.setBorder(new EmptyBorder(5, 5, 5, 5));
        lblUserCount.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblUserCount.setForeground(new Color(0, 100, 0)); // Xanh lá đậm
        pnlLeft.add(lblUserCount, BorderLayout.SOUTH);
        
        this.add(pnlLeft, BorderLayout.WEST);

        // --- PANEL GIỮA: LOG HỆ THỐNG ---
        JPanel pnlCenter = new JPanel(new BorderLayout());
        pnlCenter.setBorder(BorderFactory.createTitledBorder("Nhật ký hệ thống (System Log)"));
        
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setBackground(Color.WHITE); 
        txtLog.setForeground(new Color(50, 50, 50)); 
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtLog.setMargin(new Insets(5, 5, 5, 5));
        
        JScrollPane scrollLog = new JScrollPane(txtLog);
        pnlCenter.add(scrollLog, BorderLayout.CENTER);
        this.add(pnlCenter, BorderLayout.CENTER);
        
        // --- PANEL DƯỚI: CHỨC NĂNG ADMIN ---
        JPanel pnlBottom = new JPanel(new BorderLayout(10, 0));
        pnlBottom.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JLabel lblMsg = new JLabel("Thông báo toàn Server: ");
        lblMsg.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        txtBroadcast = new JTextField();
        txtBroadcast.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        btnSendBroadcast = new JButton("Gửi Thông Báo");
        btnSendBroadcast.setBackground(new Color(0, 120, 215)); // Màu xanh Windows
        btnSendBroadcast.setForeground(Color.BLACK);
        btnSendBroadcast.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSendBroadcast.setFocusPainted(false);
        
        // Sự kiện nút Gửi
        btnSendBroadcast.addActionListener(e -> sendBroadcast());
        
        pnlBottom.add(lblMsg, BorderLayout.WEST);
        pnlBottom.add(txtBroadcast, BorderLayout.CENTER);
        pnlBottom.add(btnSendBroadcast, BorderLayout.EAST);
        
        this.add(pnlBottom, BorderLayout.SOUTH);
        
        this.setVisible(true);
    }
    
    // --- LOGIC CHẠY SERVER ---
    @Override
    public void run() {
        try {
            new DAO().resetAllStatus();
            log("System: Đã reset toàn bộ trạng thái User về Offline.");
            
            // Cấu hình SSL (Nhớ kiểm tra pass keystore của bro là 123456 hay 123456789 nhé)
            System.setProperty("javax.net.ssl.keyStore", "carokeystore.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", "123456789"); 

            SSLServerSocketFactory sslFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket serverSocket = (SSLServerSocket) sslFactory.createServerSocket(port);
            
            log("System: Server SSL đang chạy tại port " + port + "...");
            log("System: Đang chờ kết nối từ Client...");
            
            while(true) {
                Socket clientSocket = serverSocket.accept();
                log("Connection: Có kết nối mới từ " + clientSocket.getInetAddress());
                
                ServerThread serverThread = new ServerThread(clientSocket);
                listServerThreads.add(serverThread);
                serverThread.start();
            }
            
        } catch (Exception e) {
            log("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void sendBroadcast() {
        String msg = txtBroadcast.getText().trim();
        if(msg.isEmpty()) return;
        
        for(ServerThread th : listServerThreads) {
            th.write("BROADCAST|" + msg);
        }
        log("[ADMIN " + adminName + "]: Đã gửi thông báo: " + msg);
        txtBroadcast.setText(""); 
    }
    
    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    // --- [UPDATE] HÀM CẬP NHẬT BẢNG ONLINE (ĐÃ VIẾT CODE) ---
    public static void updateOnlineBoard() {
        // 1. Lấy danh sách những người đang có tên (đã đăng nhập)
        Vector<String> onlineNames = new Vector<>();
        for (ServerThread th : listServerThreads) {
            if (th.getClientName() != null) {
                onlineNames.add(th.getClientName());
            }
        }

        // 2. Tìm cái JFrame ServerControl đang mở để cập nhật
        for (Frame f : Frame.getFrames()) {
            if (f instanceof ServerControl) {
                ServerControl sc = (ServerControl) f;
                
                SwingUtilities.invokeLater(() -> {
                    sc.modelOnline.setRowCount(0); // Xóa hết dữ liệu cũ
                    
                    for (String name : onlineNames) {
                        sc.modelOnline.addRow(new Object[]{name, "Đang online"});
                    }
                    
                    sc.lblUserCount.setText("Online: " + onlineNames.size());
                });
                return; 
            }
        }
    }

    // --- [UPDATE] HÀM THÔNG BÁO CHO CLIENT VÀ GỌI UPDATE GUI ---
    public static void notifyAllPlayers() {
        String msg = "ONLINE_LIST";
        
        // 1. Tạo chuỗi danh sách để gửi cho Client
        for (ServerThread th : listServerThreads) {
            if (th.getClientName() != null) {
                msg += "|" + th.getClientName();
            }
        }
        
        // 2. Gửi broadcast cho tất cả Client
        for (ServerThread th : listServerThreads) {
            if (th.getClientName() != null) {
                th.write(msg); 
            }
        }
        
        // 3. Cập nhật giao diện bảng Admin
        updateOnlineBoard(); 
    }
    
    public static ServerThread getServerThreadByName(String name) {
        for (ServerThread th : listServerThreads) {
            if (th.getClientName() != null && th.getClientName().equals(name)) {
                return th;
            }
        }
        return null; 
    }
    
    // --- MAIN ---
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {}

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JLabel lblUser = new JLabel("Tài khoản Admin:");
        JTextField txtUser = new JTextField("admin");
        JLabel lblPass = new JLabel("Mật khẩu:");
        JPasswordField txtPass = new JPasswordField(); 
        
        panel.add(lblUser);
        panel.add(txtUser);
        panel.add(lblPass);
        panel.add(txtPass);
        
        while (true) {
            int option = JOptionPane.showConfirmDialog(null, panel, "Đăng nhập quản trị Server",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            
            if (option == JOptionPane.OK_OPTION) {
                String user = txtUser.getText();
                String pass = new String(txtPass.getPassword());
                
                // Check cứng (Bro có thể đổi pass ở đây)
                if (user.equals("admin") && pass.equals("123456")) {
                    new ServerControl(user); 
                    break;
                } else {
                    JOptionPane.showMessageDialog(null, "Sai tài khoản hoặc mật khẩu Admin!");
                }
            } else {
                System.exit(0);
            }
        }
    }
}