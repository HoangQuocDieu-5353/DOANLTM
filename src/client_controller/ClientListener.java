package client_controller;

import client_view.GameFrm;
import client_view.GameBoardFrm; // Import thêm cái này để dùng GameBoardFrm
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Vector;
import javax.swing.JOptionPane;

public class ClientListener extends Thread {
    private Socket socket;
    private DataInputStream in;
    private GameFrm gameFrm;
    
    // --- THÊM BIẾN NÀY ĐỂ QUẢN LÝ BÀN CỜ ---
    private GameBoardFrm gameBoard; 

    // Constructor
    public ClientListener(Socket socket, GameFrm gameFrm) {
        this.socket = socket;
        this.gameFrm = gameFrm;
        try {
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                // 1. Chờ tin nhắn từ Server
                String message = in.readUTF();
                System.out.println("Client nghe thấy: " + message); 
                
                // 2. Phân tích tin nhắn
                String[] data = message.split("\\|");
                String command = data[0];

                // --- 1. CẬP NHẬT DANH SÁCH ONLINE ---
                if (command.equals("ONLINE_LIST")) {
                    Vector<String> onlineUsers = new Vector<>();
                    for (int i = 1; i < data.length; i++) {
                        onlineUsers.add(data[i]);
                    }
                    gameFrm.updateOnlineList(onlineUsers);
                }
                
                // --- 2. XỬ LÝ LỜI MỜI THÁCH ĐẤU ---
                else if (command.equals("INVITE_REQUEST")) {
                    String inviter = data[1];
                    int confirm = JOptionPane.showConfirmDialog(gameFrm, 
                            "Người chơi " + inviter + " muốn thách đấu!\nBạn có đồng ý không?", 
                            "Lời mời thách đấu", JOptionPane.YES_NO_OPTION);
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                            out.writeUTF("ACCEPT_INVITE|" + inviter);
                            out.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                // --- 3. BẮT ĐẦU GAME (Vào bàn cờ) ---
                else if (command.equals("START_GAME")) {
                    // Cấu trúc: START_GAME | Tên_Đối_Thủ | Side(X/O)
                    String rivalName = data[1];
                    String side = data[2]; 
                    
                    System.out.println("Đang vào game với: " + rivalName + " phe: " + side);

                    // Ẩn sảnh chờ
                    gameFrm.setVisible(false); 
                    
                    // Khởi tạo bàn cờ và gán vào biến toàn cục 'gameBoard'
                    // Lưu ý: Đảm bảo GameFrm có hàm getUsername() nhé
                    this.gameBoard = new GameBoardFrm(socket, gameFrm.getUsername(), rivalName, side);
                    this.gameBoard.setVisible(true); // Hiển thị bàn cờ lên
                }
                
                // --- 4. NHẬN NƯỚC ĐI TỪ ĐỐI THỦ ---
                else if (command.equals("CARO")) {
                    // Cấu trúc: CARO | x | y
                    int x = Integer.parseInt(data[1]);
                    int y = Integer.parseInt(data[2]);
                    
                    // Nếu bàn cờ đang mở thì cập nhật nước đi
                    if (this.gameBoard != null) {
                        this.gameBoard.addCompetitorMove(x, y);
                    } else {
                        System.out.println("Lỗi: Nhận được nước đi nhưng bàn cờ chưa mở!");
                    }
                }
                
                // --- 5. ĐỐI THỦ THOÁT GAME (Xử lý thêm cho mượt) ---
                else if (command.equals("EXIT_GAME")) {
                    if (this.gameBoard != null) {
                        JOptionPane.showMessageDialog(this.gameBoard, "Đối thủ đã thoát game!");
                        this.gameBoard.dispose(); // Tắt bàn cờ
                        this.gameBoard = null;    // Reset biến
                        gameFrm.setVisible(true); // Hiện lại sảnh chờ
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Mất kết nối tới Server!");
            // e.printStackTrace(); // Tắt cái này cho đỡ rác console nếu muốn
            JOptionPane.showMessageDialog(gameFrm, "Mất kết nối tới Server. Vui lòng kiểm tra lại.");
        }
    }
}