package UI.memento;

import java.util.ArrayList;
import java.util.List;

public class ChatHistoryCaretaker {
    private final List<ChatInputMemento> mementos = new ArrayList<>();
    private int currentIndex = -1;

    public void addMemento(ChatInputMemento memento) {
        // Evitar que el historial crezca ilimitadamente
        if (mementos.size() > 50) {
            mementos.remove(0);
        }
        mementos.add(memento);
        currentIndex = mementos.size(); // Apuntar al final (nueva redacción)
    }

    public ChatInputMemento getPrevious() {
        if (mementos.isEmpty()) return null;
        if (currentIndex > 0) {
            currentIndex--;
        }
        return mementos.get(currentIndex);
    }

    public ChatInputMemento getNext() {
        if (mementos.isEmpty()) return null;
        if (currentIndex < mementos.size() - 1) {
            currentIndex++;
            return mementos.get(currentIndex);
        }
        // Retornar vacío si llegamos al final del historial
        currentIndex = mementos.size();
        return new ChatInputMemento("");
    }

    public void resetIndex() {
        currentIndex = mementos.size();
    }
}
