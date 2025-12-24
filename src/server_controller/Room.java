package server_controller;

public class Room {
    private String id;
    private ServerThread creator; 
    private String password; 

    public Room(String id, ServerThread creator, String password) {
        this.id = id;
        this.creator = creator;
        this.password = password;
    }

    public String getId() { return id; }
    public ServerThread getCreator() { return creator; }
    public String getPassword() { return password; }
}