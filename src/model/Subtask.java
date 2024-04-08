package model;

import java.util.Objects;

public class Subtask extends Task {
    private Epic epic;

    public Subtask(Status status, String name, String description, Epic epic) {
        super(status, name, description);
        this.epic = Objects.requireNonNull(epic);
    }

    public Epic getEpic() {
        return epic;
    }

    public void setEpic(Epic epic) {
        this.epic = epic;
    } // у нас объект сабтаска целиком должен заменяться, так что этот метод не используется, но на всякий оставил.
    // К тому же, реализовал возможность задать эпик для сабтаска в конструкторе
}
