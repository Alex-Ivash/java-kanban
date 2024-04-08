package model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Epic extends Task {
    private final Set<Subtask> subtasks;

    public Epic(String name, String description) {
        super(name, description);
        this.subtasks = new HashSet<>();
        calcStatus();
    }

    public void addSubtask(Subtask subtask) {
        subtasks.add(subtask);
        calcStatus();
    }

    public void removeSubtask(Subtask subtask) {
        subtasks.remove(subtask);
        calcStatus();
    }

    public Set<Subtask> getSubtasks() {
        return subtasks;
    }

    private void calcStatus() {
        List<Status> subtaskStatuses = subtasks.stream()
                .map(Task::getStatus)
                .toList();

        if (subtaskStatuses.isEmpty() || subtaskStatuses.stream().allMatch(status -> status == Status.NEW)){
            status = Status.NEW;
        } else if (subtaskStatuses.stream().allMatch(status -> status == Status.DONE)) {
            status = Status.DONE;
        } else {
            status = Status.IN_PROGRESS;
        }
    }

    @Override
    public String toString() {
        return super.toString().replaceFirst("}$", ", subtasks=" + subtasks + "}");
    }
}
