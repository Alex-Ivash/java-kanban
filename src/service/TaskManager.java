package service;

import model.Epic;
import model.Status;
import model.Subtask;
import model.Task;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TaskManager {
    private Integer seq;
    private final HashMap<Integer, Task> tasks;
    private final HashMap<Integer, Subtask> subtasks;
    private final HashMap<Integer, Epic> epics;

    public TaskManager(HashMap<Integer, Task> tasks, HashMap<Integer, Subtask> subtasks, HashMap<Integer, Epic> epics) {
        this.tasks = tasks;
        this.subtasks = subtasks;
        this.epics = epics;
        this.seq = 0;
    }


    public Collection<Task> getAllTasks() {
        return tasks.values();
    }

    public Collection<Subtask> getAllSubTasks() {
        return subtasks.values();
    }

    public Collection<Epic> getAllEpics() {
        return epics.values();
    }


    public void removeAllTasks() {
        tasks.clear();
    }

    public void removeAllSubTasks() {
        subtasks.keySet()
                .stream()
                .toList()
                .forEach(this::removeSubtask);
    }

    public void removeAllEpics() {
        epics.keySet()
                .stream()
                .toList()
                .forEach(this::removeEpic);
    }


    public Task getTask(int id) {
        return tasks.get(id);
    }

    public Subtask getSubtask(int id) {
        return subtasks.get(id);
    }

    public Epic getEpic(int id) {
        return epics.get(id);
    }


    public Task createTask(Task newTask) {
        if (newTask != null) {
            newTask.setId(getNextId());
            tasks.put(newTask.getId(), newTask);
        }

        return newTask;
    }

    public Subtask createSubtask(Subtask newSubtask) {
        if (newSubtask != null) {
            newSubtask.setId(getNextId());

            Epic subtaskEpic = epics.get(newSubtask.getEpic());

            subtaskEpic.addSubtask(newSubtask.getId());
            subtasks.put(newSubtask.getId(), newSubtask);
            calcEpicStatus(subtaskEpic);
        }

        return newSubtask;
    }

    public Epic createEpic(Epic newEpic) {
        if (newEpic != null) {
            newEpic.setId(getNextId());
            epics.put(newEpic.getId(), newEpic);
            calcEpicStatus(newEpic);
        }

        return newEpic;
    }


    public void updateTask(Task newTask) {
        if (newTask != null) {
            tasks.put(newTask.getId(), newTask);
        }
    }

    public void updateSubtask(Subtask newSubtask) {
        if (newSubtask != null) {
            int id = newSubtask.getId();
            Subtask oldSubtask = subtasks.get(id);
            Epic newSubtaskEpic = epics.get(newSubtask.getEpic());
            Epic oldSubtaskEpic = epics.get(oldSubtask.getEpic());

            if (oldSubtaskEpic != newSubtaskEpic) {
                oldSubtaskEpic.removeSubtask(id);
                newSubtaskEpic.addSubtask(id);
            }

            subtasks.put(id, newSubtask);
            calcEpicStatus(newSubtaskEpic);
            calcEpicStatus(oldSubtaskEpic);
        }
    }

    public void updateEpic(Epic newEpic) {
        if (newEpic != null) {
            Epic oldEpic = epics.get(newEpic.getId());

            oldEpic.setName(newEpic.getName());
            oldEpic.setDescription(newEpic.getDescription());
        }
    }


    public void removeTask(int id) {
        tasks.remove(id);
    }

    public void removeSubtask(int id) {
        Subtask subtask = subtasks.get(id);

        if (subtask == null) return;

        Epic subtaskEpic = epics.get(subtask.getEpic());

        subtaskEpic.removeSubtask(id);
        calcEpicStatus(subtaskEpic);
        subtasks.remove(id);
    }

    public void removeEpic(int id) {
        Epic epic = epics.get(id);

        if (epic == null) return;

        epic.getSubtasks().forEach(subtasks::remove);
        epics.remove(id);
    }


    public List<Subtask> getEpicSubtasks(int id) {
        return epics.get(id)
                .getSubtasks()
                .stream()
                .map(subtasks::get)
                .toList();
    }


    private void calcEpicStatus(Epic epic) {
        List<Status> subtaskStatuses = epic.getSubtasks().stream()
                .map(subtaskId -> subtasks.get(subtaskId).getStatus())
                .toList();
        Status newStatus = null;

        if (subtaskStatuses.isEmpty() || subtaskStatuses.stream().allMatch(status -> status == Status.NEW)) {
            newStatus = Status.NEW;
        } else if (subtaskStatuses.stream().allMatch(status -> status == Status.DONE)) {
            newStatus = Status.DONE;
        } else {
            newStatus = Status.IN_PROGRESS;
        }

        epic.setStatus(newStatus);
    }


    private int getNextId() {
        return seq++;
    }
}
