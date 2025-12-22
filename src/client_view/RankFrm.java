package client_view;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Vector;
import java.util.List; // Import List

public class RankFrm extends JFrame {
    private String rankData;
    private JTable table;
    private DefaultTableModel tableModel;
    
    private Socket socket;
    private String myUsername;
    
    // --- [NEW] DANH SÁCH BẠN BÈ ---
    private List<String> friendList;

    // Cập nhật Constructor: Thêm tham số friendList
    public RankFrm(Socket socket, String myUsername, String rankData, List<String> friendList) {
        this.socket = socket;
        this.myUsername = myUsername;
        this.rankData = rankData;
        this.friendList = friendList; // Lưu lại
        initUI();
    }

    private void initUI() {
        this.setTitle("Bảng Xếp Hạng Cao Thủ");
        this.setSize(600, 450); 
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(new Color(245, 245, 245));

        // 1. Tiêu đề
        JLabel lblTitle = new JLabel("TOP 10 CAO THỦ", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitle.setForeground(new Color(255, 69, 0));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        this.add(lblTitle, BorderLayout.NORTH);

        // 2. Bảng xếp hạng
        String[] columnNames = {"Hạng", "Nickname", "Điểm", "Thắng", "Trạng thái"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(tableModel);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(30, 144, 255));
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < columnNames.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        loadData();

        // --- XỬ LÝ CHUỘT PHẢI ---
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < table.getRowCount()) {
                        table.setRowSelectionInterval(row, row); 
                        showPopupMenu(e, row);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);
        this.add(scrollPane, BorderLayout.CENTER);
        
        JLabel lblNote = new JLabel("(*) Chuột phải vào người chơi để Thách đấu hoặc Kết bạn", SwingConstants.CENTER);
        lblNote.setFont(new Font("Arial", Font.ITALIC, 12));
        lblNote.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
        this.add(lblNote, BorderLayout.SOUTH);

        this.setVisible(true);
    }
    
    // --- [UPDATE] MENU CHUỘT PHẢI ---
    private void showPopupMenu(MouseEvent e, int row) {
        String targetName = (String) table.getValueAt(row, 1); 
        String status = (String) table.getValueAt(row, 4);     
        
        if (targetName.equals(myUsername)) return;

        JPopupMenu popup = new JPopupMenu();
        
        // 1. Thách đấu
        JMenuItem itemChallenge = new JMenuItem("Thách Đấu");
        if (status.equals("Online")) {
            itemChallenge.addActionListener(evt -> sendRequest("INVITE", targetName));
        } else {
            itemChallenge.setEnabled(false); 
            itemChallenge.setText("Thách Đấu (Offline)");
        }
        popup.add(itemChallenge);
        
        // 2. Kết bạn (KIỂM TRA XEM ĐÃ LÀ BẠN CHƯA)
        if (!friendList.contains(targetName)) {
            JMenuItem itemAddFriend = new JMenuItem("Kết Bạn");
            itemAddFriend.addActionListener(evt -> sendRequest("MAKE_FRIEND", targetName));
            popup.add(itemAddFriend);
        } else {
            // Nếu là bạn rồi thì hiện dòng này nhưng không ấn được (hoặc không hiện gì cả tùy bro)
            JMenuItem itemFriend = new JMenuItem("Đã là bạn bè");
            itemFriend.setEnabled(false);
            popup.add(itemFriend);
        }
        
        popup.show(e.getComponent(), e.getX(), e.getY());
    }
    
    private void sendRequest(String cmd, String targetUser) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(cmd + "|" + targetUser);
            out.flush();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối!");
        }
    }

    private void loadData() {
        if (rankData == null || rankData.isEmpty()) return;
        String[] rows = rankData.split(";");
        int rank = 1;
        for (String row : rows) {
            if (!row.trim().isEmpty()) {
                String[] cols = row.split(",");
                if (cols.length >= 4) {
                    Vector<Object> v = new Vector<>();
                    v.add(rank++);       
                    v.add(cols[0]);      
                    v.add(cols[1]);      
                    v.add(cols[2]);
                    String status = cols[3].equals("1") ? "Online" : "Offline";
                    v.add(status);
                    tableModel.addRow(v);
                }
            }
        }
    }
}