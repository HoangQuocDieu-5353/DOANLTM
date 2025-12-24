package client_view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Vector;

public class RoomListFrm extends JFrame {
    private Socket socket;
    private String username;
    private JTable table;
    private DefaultTableModel model;

    public RoomListFrm(Socket socket, String username) {
        this.socket = socket;
        this.username = username;
        initUI();
        requestUpdate();
    }

    private void initUI() {
        this.setTitle("Danh Sách Phòng - " + username);
        this.setSize(600, 400);
        this.setLayout(new BorderLayout());
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // --- BẢNG DANH SÁCH ---
        String[] headers = {"Mã Phòng", "Chủ Phòng", "Trạng Thái"};
        model = new DefaultTableModel(headers, 0);
        table = new JTable(model);
        // Double click để vào phòng nhanh
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    joinSelectedRoom();
                }
            }
        });
        this.add(new JScrollPane(table), BorderLayout.CENTER);

        // --- PANEL CHỨC NĂNG (DƯỚI) ---
        JPanel pnlBottom = new JPanel(new FlowLayout());
        
        JButton btnCreate = new JButton("Tạo Phòng");
        btnCreate.addActionListener(e -> createRoom());
        
        JButton btnJoin = new JButton("Vào Phòng");
        btnJoin.addActionListener(e -> joinSelectedRoom());
        
        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.addActionListener(e -> requestUpdate());

        pnlBottom.add(btnCreate);
        pnlBottom.add(btnJoin);
        pnlBottom.add(btnRefresh);
        this.add(pnlBottom, BorderLayout.SOUTH);
        
        // --- PANEL TÌM KIẾM (TRÊN) ---
        JPanel pnlTop = new JPanel(new FlowLayout());
        JTextField txtSearchId = new JTextField(10);
        JButton btnSearch = new JButton("Tìm theo ID");
        btnSearch.addActionListener(e -> {
             // Logic tìm phòng: Gửi lệnh Join luôn với ID nhập tay
             String id = txtSearchId.getText().trim();
             if(!id.isEmpty()) joinRoomById(id);
        });
        pnlTop.add(new JLabel("Nhập mã phòng:"));
        pnlTop.add(txtSearchId);
        pnlTop.add(btnSearch);
        this.add(pnlTop, BorderLayout.NORTH);

        this.setVisible(true);
    }

    // Gửi yêu cầu lấy danh sách mới nhất
    private void requestUpdate() {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("GET_ROOM_LIST");
            out.flush();
        } catch (Exception e) {}
    }

    // Xử lý nút Tạo Phòng
    private void createRoom() {
        String pass = JOptionPane.showInputDialog(this, "Nhập mật khẩu (Để trống nếu muốn phòng mở):");
        if (pass == null) return; // Bấm Cancel
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("CREATE_ROOM|" + pass);
            out.flush();
        } catch (Exception e) {}
    }

    // Xử lý nút Vào Phòng (từ bảng)
    private void joinSelectedRoom() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Chọn một phòng để vào!");
            return;
        }
        String roomId = model.getValueAt(row, 0).toString();
        String status = model.getValueAt(row, 2).toString();
        
        joinRoomLogic(roomId, status);
    }
    
    // Xử lý tìm theo ID
    private void joinRoomById(String roomId) {
        // Mặc định cứ thử vào, server check sau
        joinRoomLogic(roomId, "Không rõ");
    }

    private void joinRoomLogic(String roomId, String status) {
        String pass = "";
        // Nếu status ghi là "Có mật khẩu" thì hỏi pass
        if (status.equals("Có mật khẩu")) {
            pass = JOptionPane.showInputDialog(this, "Nhập mật khẩu phòng " + roomId + ":");
            if (pass == null) return;
        }
        
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("JOIN_ROOM|" + roomId + "|" + pass);
            out.flush();
        } catch (Exception e) {}
    }

    // --- HÀM CẬP NHẬT DỮ LIỆU TỪ SERVER GỬI VỀ ---
    public void updateData(Vector<String> roomsData) {
        model.setRowCount(0);
        for (String s : roomsData) {
            String[] arr = s.split(","); // ID,Creator,HasPass
            String status = arr[2].equals("0") ? "Công khai" : "Có mật khẩu";
            model.addRow(new Object[]{arr[0], arr[1], status});
        }
    }
}