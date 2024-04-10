package model;

import java.util.HashSet;
import java.util.Set;

public class Epic extends Task {
    private final Set<Integer> subtasksIds;

    public Epic(String name, String description) {
        super(name, description);
        this.subtasksIds = new HashSet<>();
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
    public String toString() {
        return super.toString().replaceFirst("}$", ", subtasks=" + subtasksIds + "}");
    }
}
