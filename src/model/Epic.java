package model;

import java.util.HashSet;
import java.util.Set;

public class Epic extends Task {
    private final Set<Integer> subtasksIds = new HashSet<>();

    public Epic(String name, String description) {
        super(name, description);
    }

    public Epic(Integer id, TaskStatus status, String name, String description) {
        super(id, status, name, description);
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
    public TaskType getType() {
        return TaskType.EPIC;
    }

    @Override
    public String toString() {
        return super.toString().replaceFirst("}$", ", subtasks=" + subtasksIds + "}");
    }
}
