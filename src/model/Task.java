package model;

import service.managers.task.InMemoryTaskManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public class Task {
    protected Integer id;
    protected TaskStatus status;
    protected String name;
    protected String description;
    protected LocalDateTime startTime;
    protected Duration duration;

    protected Task(String name, String description) {
        this.name = name;
        this.description = description;
        this.duration = InMemoryTaskManager.DEFAULT_DURATION;
        this.startTime = InMemoryTaskManager.DEFAULT_START_TIME;
    }

    public Task(TaskStatus status, String name, String description) {
        this(name, description);
        this.status = status;
    }

    public Task(TaskStatus status, String name, String description, LocalDateTime startTime, Duration duration) {
        this(status, name, description);
        this.startTime = startTime;
        this.duration = duration;
    }

    public Task(Integer id, TaskStatus status, String name, String description, LocalDateTime startTime, Duration duration) {
        this(status, name, description, startTime, duration);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskType getType() {
        return TaskType.TASK;
    }

    public LocalDateTime getEndTime() {
        return startTime.plus(duration);
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName() + "{");
        sb.append("id=").append(id);
        sb.append(", status=").append(status);
        sb.append(", name='").append(name).append("'");
        sb.append(", description='").append(description).append("'");
        sb.append(", startTime='").append(startTime).append("'");
        sb.append(", duration='").append(duration).append("'");
        sb.append('}');

        return sb.toString();
    }

    @Override
    public boolean equals(Object otherTask) {
        if (this == otherTask) return true;
        if (otherTask.getClass() != getClass()) return false;
        Task task = (Task) otherTask;

        return id.equals(task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
