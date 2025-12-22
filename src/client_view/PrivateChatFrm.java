package client_view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataOutputStream;
import java.net.Socket;
import javax.swing.*;

public class PrivateChatFrm extends JFrame {
    private String myNick;
    private String friendNick;
    private Socket socket;
    
    private JTextArea txtChatHistory;
    private JTextField txtMessage;
    private JButton btnSend;

    public PrivateChatFrm(String myNick, String friendNick, Socket socket) {
        this.myNick = myNick;
        this.friendNick = friendNick;
        this.socket = socket;
        
        initUI();
    }

    private void initUI() {
        this.setTitle("Chat với: " + friendNick);
        this.setSize(400, 300);
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());

        // 1. Vùng hiển thị tin nhắn
        txtChatHistory = new JTextArea();
        txtChatHistory.setEditable(false);
        txtChatHistory.setFont(new Font("Arial", Font.PLAIN, 14));
        txtChatHistory.setLineWrap(true);
        txtChatHistory.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(txtChatHistory);
        this.add(scrollPane, BorderLayout.CENTER);

        // 2. Vùng nhập liệu
        JPanel pnlBottom = new JPanel(new BorderLayout());
        txtMessage = new JTextField();
        txtMessage.setFont(new Font("Arial", Font.PLAIN, 14));
        
        btnSend = new JButton("Gửi");
        btnSend.setBackground(new Color(30, 144, 255));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFont(new Font("Arial", Font.BOLD, 14));

        // Sự kiện gửi
        ActionListener sendAction = e -> sendMessage();
        btnSend.addActionListener(sendAction);
        txtMessage.addActionListener(sendAction); // Enter cũng gửi

        pnlBottom.add(txtMessage, BorderLayout.CENTER);
        pnlBottom.add(btnSend, BorderLayout.EAST);
        this.add(pnlBottom, BorderLayout.SOUTH);

        this.setVisible(true);
    }

    private void sendMessage() {
        String msg = txtMessage.getText().trim();
        if (msg.isEmpty()) return;

        try {
            // Hiển thị tin nhắn của mình lên vùng chat
            updateChat("Bạn: " + msg);
            
            // Gửi lên Server: CHAT_TO | Người_Nhận | Nội_Dung
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("CHAT_TO|" + friendNick + "|" + msg);
            out.flush();
            
            txtMessage.setText(""); // Xóa ô nhập
            txtMessage.requestFocus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Hàm để bên ngoài gọi vào khi có tin nhắn mới
    public void updateChat(String msg) {
        txtChatHistory.append(msg + "\n");
        txtChatHistory.setCaretPosition(txtChatHistory.getDocument().getLength()); // Cuộn xuống dưới
    }
}