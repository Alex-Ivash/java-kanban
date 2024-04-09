package model;

import java.util.HashSet;
import java.util.Set;

public class Epic extends Task {
    private final Set<Integer> subtasks;

    public Epic(String name, String description) {
        super(name, description);
        this.subtasks = new HashSet<>();
    }

    public void addSubtask(int subtaskId) {
        subtasks.add(subtaskId);
    }

    public void removeSubtask(int subtaskId) {
        subtasks.remove(subtaskId);
    }

    public Set<Integer> getSubtasks() {
        return subtasks;
    }

    @Override
    public String toString() {
        return super.toString().replaceFirst("}$", ", subtasks=" + subtasks + "}");
    }
}
