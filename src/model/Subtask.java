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
    }

    @Override
    public String toString() {
        return super.toString().replaceFirst("}$", ", epic=" + epicId + "}");
    }
}
