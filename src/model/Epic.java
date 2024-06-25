package model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class Epic extends Task {
    private final Set<Integer> subtasksIds = new HashSet<>();
    private LocalDateTime endTime;

    public Epic(String name, String description) {
        super(name, description);
    }

    public Epic(Integer id, TaskStatus status, String name, String description, LocalDateTime startTime, Duration duration) {
        super(id, status, name, description, startTime, duration);
    }

    public void addSubtask(int subtaskId) {
        subtasksIds.add(subtaskId);
    }

    public void removeSubtask(int subtaskId) {
        subtasksIds.remove(subtaskId);
    }

    public Set<Integer> getSubtasksIds() {
        return subtasksIds;
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public TaskType getType() {
        return TaskType.EPIC;
    }

    @Override
    public String toString() {
        return super.toString().replaceFirst("}$", ", subtasks=" + subtasksIds + "}");
    }
}
