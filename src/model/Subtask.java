package model;

import java.util.Objects;

public class Subtask extends Task {
    private Integer epicId;

    public Subtask(Status status, String name, String description, Integer epicId) {
        super(status, name, description);
        this.epicId = Objects.requireNonNull(epicId);
    }

    public Subtask(Integer id, Status status, String name, String description, Integer epicId) {
        super(id, status, name, description);
        this.epicId = epicId;
    }

    @Override
    public Integer getEpicId() {
        return epicId;
    }

    @Override
    public Type getType() {
        return Type.SUBTASK;
    }

    public void setEpicId(int epicId) {
        this.epicId = epicId;
    }

    @Override
    public String toString() {
        return super.toString().replaceFirst("}$", ", epic=" + epicId + "}");
    }
}
