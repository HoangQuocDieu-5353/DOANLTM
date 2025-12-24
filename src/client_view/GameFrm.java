package client_view;

import client_controller.ClientListener;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class GameFrm extends JFrame {
    private Socket socket;
    private String username;
    private ClientListener listener; 

    private JList<String> listOnline;
    private DefaultListModel<String> listModel;
    
    // Quản lý các cửa sổ chat (Giữ lại trong Map để lưu lịch sử)
    private Map<String, PrivateChatFrm> chatWindows = new HashMap<>();

    public GameFrm(Socket socket, String username) {
        this.socket = socket;
        this.username = username;
        initUI();
        listener = new ClientListener(socket, this);
        listener.start();
    }

    private void initUI() {
        this.setTitle("Sảnh Chờ Game Caro - " + username);
        this.setSize(600, 600); 
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(null);
        this.setResizable(false);
        this.getContentPane().setBackground(new Color(240, 248, 255)); 

        JLabel lblList = new JLabel("Danh sách bạn bè:");
        lblList.setFont(new Font("Arial", Font.BOLD, 14));
        lblList.setBounds(20, 20, 200, 20);
        this.add(lblList);

        listModel = new DefaultListModel<>();
        listOnline = new JList<>(listModel);
        listOnline.setFont(new Font("Arial", Font.PLAIN, 14));
        listOnline.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Sự kiện chuột
        listOnline.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Double click -> Thách đấu
                if (e.getClickCount() == 2) {
                    guiLoiMoi();
                }
                // Chuột phải -> Hiện Menu
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = listOnline.locationToIndex(e.getPoint());
                    listOnline.setSelectedIndex(row);
                    if (row != -1) {
                        showFriendMenu(e);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(listOnline);
        scrollPane.setBounds(20, 50, 230, 440); 
        this.add(scrollPane);
        
        // --- NÚT KẾT BẠN ---
        JButton btnAddFriend = new JButton("Kết Bạn (+)");
        btnAddFriend.setBounds(20, 500, 230, 35);
        btnAddFriend.setFont(new Font("Arial", Font.BOLD, 13));
        btnAddFriend.setBackground(new Color(255, 215, 0));
        btnAddFriend.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Nhập Nickname người muốn kết bạn:");
            if (name != null && !name.trim().isEmpty()) {
                if (name.equals(username)) {
                    JOptionPane.showMessageDialog(this, "Không thể tự kết bạn với chính mình!");
                    return;
                }
                try {
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF("MAKE_FRIEND|" + name);
                    out.flush();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
        this.add(btnAddFriend);

        
        JLabel lblWelcome = new JLabel("Xin chào, " + username + "!");
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 18));
        lblWelcome.setForeground(new Color(220, 20, 60)); 
        lblWelcome.setBounds(280, 20, 300, 30);
        this.add(lblWelcome);

        // 1. CHƠI NHANH
        JButton btnQuickPlay = new JButton("CHƠI NHANH");
        btnQuickPlay.setBounds(280, 60, 280, 45);
        btnQuickPlay.setFont(new Font("Arial", Font.BOLD, 16));
        btnQuickPlay.setBackground(new Color(30, 144, 255)); 
        btnQuickPlay.setForeground(Color.WHITE);
        btnQuickPlay.setFocusable(false);
        btnQuickPlay.addActionListener(e -> {
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF("QUICK_PLAY");
                out.flush();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi kết nối Server!");
            }
        });
        this.add(btnQuickPlay);

        // 2. DANH SÁCH PHÒNG
        JButton btnRoomList = new JButton("Danh Sách Phòng");
        btnRoomList.setBounds(280, 115, 135, 40);
        btnRoomList.setFont(new Font("Arial", Font.PLAIN, 12));
        btnRoomList.addActionListener(e -> {
            RoomListFrm frm = new RoomListFrm(socket, username);
            listener.setRoomListFrm(frm); 
            frm.setVisible(true);
        });
        this.add(btnRoomList);

        // 3. TẠO PHÒNG
        JButton btnCreateRoom = new JButton("Tạo Phòng");
        btnCreateRoom.setBounds(425, 115, 135, 40);
        btnCreateRoom.setFont(new Font("Arial", Font.PLAIN, 12));
        btnCreateRoom.addActionListener(e -> {
            String pass = JOptionPane.showInputDialog(this, "Mật khẩu phòng (Để trống nếu công khai):");
            if (pass != null) {
                try {
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF("CREATE_ROOM|" + pass);
                    out.flush();
                } catch (IOException ex) { ex.printStackTrace(); }
            }
        });
        this.add(btnCreateRoom);

        // 4. ĐẤU VỚI MÁY
        JButton btnPvE = new JButton("Luyện Tập Với Máy (AI)");
        btnPvE.setBounds(280, 170, 280, 40);
        btnPvE.setFont(new Font("Arial", Font.PLAIN, 14));
        btnPvE.setBackground(new Color(144, 238, 144)); 
        btnPvE.addActionListener(e -> {
            String[] options = {"Dễ (Gà)", "Bình thường", "Khó (Pro)"};
            int choice = JOptionPane.showOptionDialog(this, "Chọn mức độ khó:", "Cấu hình AI",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
            if (choice != JOptionPane.CLOSED_OPTION) {
                this.setVisible(false);
                new GameBoardFrm(this, username, choice + 1).setVisible(true);
            }
        });
        this.add(btnPvE);

        // 5. THÁCH ĐẤU
        JButton btnChallenge = new JButton("Thách Đấu Bạn Bè");
        btnChallenge.setBounds(280, 220, 280, 40);
        btnChallenge.setFont(new Font("Arial", Font.PLAIN, 14));
        btnChallenge.setBackground(new Color(255, 228, 181));
        btnChallenge.addActionListener(e -> guiLoiMoi());
        this.add(btnChallenge);
        
        // 6. BẢNG XẾP HẠNG
        JButton btnRank = new JButton("Bảng Xếp Hạng");
        btnRank.setBounds(280, 270, 280, 40);
        btnRank.setFont(new Font("Arial", Font.BOLD, 14));
        btnRank.setBackground(new Color(255, 165, 0)); // Màu cam
        btnRank.setForeground(Color.WHITE);
        btnRank.addActionListener(e -> {
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF("GET_RANK");
                out.flush();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi kết nối Server!");
            }
        });
        this.add(btnRank);

        // 7. XEM HỒ SƠ
        JButton btnProfile = new JButton("Hồ Sơ Cá Nhân");
        btnProfile.setBounds(280, 320, 280, 40); 
        btnProfile.setFont(new Font("Arial", Font.PLAIN, 14));
        btnProfile.addActionListener(e -> xemHoSo()); 
        this.add(btnProfile);

        // 8. ĐĂNG XUẤT
        JButton btnLogout = new JButton("Đăng Xuất");
        btnLogout.setBounds(280, 500, 280, 40); 
        btnLogout.setFont(new Font("Arial", Font.BOLD, 14));
        btnLogout.setBackground(new Color(255, 182, 193)); 
        btnLogout.addActionListener(e -> {
            try {
                socket.close();
                this.dispose();
                new LoginUI();
            } catch (IOException ex) {}
        });
        this.add(btnLogout);

        this.setVisible(true);
    }
    
    // --- MENU CHUỘT PHẢI ---
    private void showFriendMenu(MouseEvent e) {
        String selected = listOnline.getSelectedValue();
        if (selected == null) return;
        
        String[] parts = selected.split(" ");
        String friendName = parts[0];
        
        JPopupMenu popup = new JPopupMenu();
        
        JMenuItem itemChat = new JMenuItem("Chat riêng");
        itemChat.addActionListener(evt -> openPrivateChat(friendName));
        
        JMenuItem itemChallenge = new JMenuItem("Thách đấu");
        itemChallenge.addActionListener(evt -> guiLoiMoi());
        
        popup.add(itemChat);
        popup.add(itemChallenge);
        popup.show(e.getComponent(), e.getX(), e.getY());
    }
    
    // --- [UPDATE] MỞ CỬA SỔ CHAT ---
    private void openPrivateChat(String friendName) {
        if (!chatWindows.containsKey(friendName)) {
            // Nếu chưa có thì tạo mới
            PrivateChatFrm chatFrm = new PrivateChatFrm(username, friendName, socket);
            
            // [QUAN TRỌNG] Chỉ ẩn đi khi đóng, để giữ lịch sử
            chatFrm.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); 
            
            chatWindows.put(friendName, chatFrm);
        }
        
        // Lấy ra và hiển thị lên
        PrivateChatFrm chatFrm = chatWindows.get(friendName);
        chatFrm.setVisible(true);
        chatFrm.toFront();
    }
    
    public void incomingMessage(String sender, String msg) {
        // 1. Nếu chưa có cửa sổ chat -> Tạo mới (nhưng KHÔNG hiện lên)
        if (!chatWindows.containsKey(sender)) {
            PrivateChatFrm chatFrm = new PrivateChatFrm(username, sender, socket);
            chatFrm.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            chatWindows.put(sender, chatFrm);
        }
        
        // 2. Cập nhật nội dung vào cửa sổ đó
        PrivateChatFrm chatFrm = chatWindows.get(sender);
        chatFrm.updateChat(sender + ": " + msg);
        
        // 3. TUYỆT ĐỐI KHÔNG GỌI setVisible(true) Ở ĐÂY
        // Để nó tự âm thầm nhận tin. 
        // Khi người dùng bấm "Chat riêng", nó sẽ hiện ra cùng toàn bộ tin nhắn.
    }

    private void xemHoSo() {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("GET_INFO|" + username);
            out.flush();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối tới Server!");
        }
    }

    private void guiLoiMoi() {
        String userSelected = listOnline.getSelectedValue();
        if(userSelected == null) {
            JOptionPane.showMessageDialog(this, "Bạn chưa chọn người bạn nào!");
            return;
        }
        
        String[] parts = userSelected.split(" ");
        String realName = parts[0];
        String status = parts[1];

        if (status.equals("(Offline)")) {
            JOptionPane.showMessageDialog(this, "Người chơi này đang Offline, không thể thách đấu!");
            return;
        }

        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("INVITE|" + realName);
            out.flush();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi lời mời!");
        }
    }

    public void updateOnlineList(Vector<String> users) {
        listModel.clear();
        for (String u : users) {
            listModel.addElement(u);
        }
    }
    
    public boolean checkIsFriend(String nickname) {
        for (int i = 0; i < listModel.size(); i++) {
            String item = listModel.get(i); 
            String realName = item.split(" ")[0]; 
            if (realName.equals(nickname)) return true;
        }
        return false;
    }
    
    public Vector<String> getFriendListNames() {
        Vector<String> friends = new Vector<>();
        for (int i = 0; i < listModel.size(); i++) {
            String item = listModel.get(i);
            String realName = item.split(" ")[0];
            friends.add(realName);
        }
        return friends;
    }
    
    public String getUsername() {
        return this.username;
    }
}