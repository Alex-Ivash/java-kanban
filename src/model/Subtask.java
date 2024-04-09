package model;

import java.util.Objects;

public class Subtask extends Task {
    private Integer epic;

    public Subtask(Status status, String name, String description, Integer epic) {
        super(status, name, description);
        this.epic = Objects.requireNonNull(epic);
    }

    public int getEpic() {
        return epic;
    }

    public void setEpic(int epic) {
        this.epic = epic;
    } // у нас объект сабтаска целиком должен заменяться, так что этот метод не используется, но на всякий оставил.

    // К тому же, реализовал возможность задать эпик для сабтаска в конструкторе

    @Override
    public String toString() {
        return super.toString().replaceFirst("}$", ", epic=" + epic + "}");
    }
}
