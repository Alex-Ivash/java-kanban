package model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public class Subtask extends Task {
    private Integer epicId;

    public Subtask(TaskStatus status, String name, String description, Integer epicId) {
        super(status, name, description);
        this.epicId = Objects.requireNonNull(epicId);
    }

    public Subtask(TaskStatus status, String name, String description, Integer epicId, LocalDateTime startTime, Duration duration) {
        this(status, name, description, epicId);
        this.epicId = epicId;
        this.startTime = startTime;
        this.duration = duration;
    }

    public Subtask(Integer id, TaskStatus status, String name, String description, Integer epicId, LocalDateTime startTime, Duration duration) {
        super(id, status, name, description, startTime, duration);
        this.epicId = epicId;
    }

    public Integer getEpicId() {
        return epicId;
    }

    @Override
    public TaskType getType() {
        return TaskType.SUBTASK;
    }

    public void setEpicId(int epicId) {
        this.epicId = epicId;
    }

    @Override
    public String toString() {
        return super.toString().replaceFirst("}$", ", epic=" + epicId + "}");
    }
}
