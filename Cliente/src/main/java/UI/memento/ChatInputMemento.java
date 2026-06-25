package UI.memento;

public class ChatInputMemento {
    private final String text;

    public ChatInputMemento(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
