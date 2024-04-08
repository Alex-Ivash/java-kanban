package service;

import model.*;

import java.util.Collection;
import java.util.HashMap;

public class TaskManager {
    private int seq;
    private final HashMap<Integer, Task> tasks;

    public TaskManager(HashMap<Integer, Task> tasks) {
        this.tasks = tasks;
    }

    public Task get(int id) {
        return tasks.get(id);
    }

    public Collection<Task> getAll() {
        return tasks.values();
    }

    public void removeAll() {
        tasks.clear();
    }

    public void remove(int id) {
        Task task = tasks.get(id);

        switch (task) {
            case Subtask subtask -> subtask.getEpic().removeSubtask(subtask);
            case Epic epic -> epic.getSubtasks().forEach(subtask -> tasks.remove(subtask.getId()));
            case null, default -> {}
        }

        tasks.remove(id);
    }

    public Task create(Task newTask) {
        if (newTask != null) {
            newTask.setId(getNextId());

            if (newTask instanceof Subtask subtask) {
                subtask.getEpic().addSubtask(subtask);
            }

            tasks.put(newTask.getId(), newTask);
        }

        return newTask;
    }

    public Task update(Task newTask) {
        return switch (newTask) {
            case Epic epic -> tasks.computeIfPresent(newTask.getId(), (k, oldTask) -> {
                    oldTask.setName(epic.getName());
                    oldTask.setDescription(epic.getDescription());

                    return oldTask;
                });
            case Subtask newSubtask -> tasks.computeIfPresent(newTask.getId(), (k, oldTask) -> {
                    Subtask oldSubtask = (Subtask) oldTask;
                    oldSubtask.getEpic().removeSubtask(oldSubtask);
                    newSubtask.getEpic().addSubtask(newSubtask);

                    return newTask;
                });
            case null -> null;
            default -> tasks.computeIfPresent(newTask.getId(), (k, v) -> newTask);
        };
    }

    private int getNextId() {
        return seq++;
    }
}
