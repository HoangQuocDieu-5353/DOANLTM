package server_controller;

public class Room {
    private String id;
    private ServerThread creator; // Người tạo phòng
    private String password; // Nếu null hoặc rỗng -> Phòng mở

    public Room(String id, ServerThread creator, String password) {
        this.id = id;
        this.creator = creator;
        this.password = password;
    }

    public String getId() { return id; }
    public ServerThread getCreator() { return creator; }
    public String getPassword() { return password; }
}