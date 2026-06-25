package model;

public class MensajeSocket {
    
    private String type; 
    private String roomCode;
    private Integer userId; 
    private String userName;
    private String message;
    private String sentAt;
    private String fullName;

    // Constructor vacío obligatorio para la librería Gson
    public MensajeSocket() {
    }

    // Constructor completo
    public MensajeSocket(String type, String roomCode, Integer userId, String userName, String message, String sentAt) {
        this.type = type;
        this.roomCode = roomCode;
        this.userId = userId;
        this.userName = userName;
        this.message = message;
        this.sentAt = sentAt;
    }

    // Constructor con fullName (para registro)
    public MensajeSocket(String type, String roomCode, Integer userId, String userName, String message, String sentAt, String fullName) {
        this.type = type;
        this.roomCode = roomCode;
        this.userId = userId;
        this.userName = userName;
        this.message = message;
        this.sentAt = sentAt;
        this.fullName = fullName;
    }

    // --- GETTERS Y SETTERS ---
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}