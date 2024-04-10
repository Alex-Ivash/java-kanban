package model;

import java.util.Objects;

public class Subtask extends Task {
    private Integer epicId;

    public Subtask(Status status, String name, String description, Integer epicId) {
        super(status, name, description);
        this.epicId = Objects.requireNonNull(epicId);
    }

    public int getEpicId() {
        return epicId;
    }

    public void setEpicId(int epicId) {
        this.epicId = epicId;
    } // у нас объект сабтаска целиком должен заменяться, так что этот метод не используется, но на всякий оставил.

    // К тому же, реализовал возможность задать эпик для сабтаска в конструкторе

    @Override
    public String toString() {
        return super.toString().replaceFirst("}$", ", epic=" + epicId + "}");
    }
}
