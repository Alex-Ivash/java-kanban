package service;

import model.Epic;
import model.Status;
import model.Subtask;
import model.Task;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TaskManager {
    private int seq;
    private final HashMap<Integer, Task> tasks;
    private final HashMap<Integer, Subtask> subtasks;
    private final HashMap<Integer, Epic> epics;

    public TaskManager() {
        this.tasks = new HashMap<>();
        this.subtasks = new HashMap<>();
        this.epics = new HashMap<>();
        this.seq = 0; // вроде бы, значение по умолчанию лучше не оставлять просто так и инициализировать явно, чтобы избежать подводных?
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
        subtasks.clear();
        epics.forEach((id, epic) -> {
            epic.getSubtasksIds().clear();
            calculateEpicStatus(epic);
        });
    }

    public void removeAllEpics() {
        subtasks.clear();
        epics.clear();
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
        if (newTask == null) return null;

        newTask.setId(getNextId());
        tasks.put(newTask.getId(), newTask);

        return newTask;
    }

    public Subtask createSubtask(Subtask newSubtask) {
        if (newSubtask == null) return null;

        newSubtask.setId(getNextId());

        Epic subtaskEpic = epics.get(newSubtask.getEpicId());

        subtaskEpic.addSubtask(newSubtask.getId());
        subtasks.put(newSubtask.getId(), newSubtask);
        calculateEpicStatus(subtaskEpic);

        return newSubtask;
    }

    public Epic createEpic(Epic newEpic) {
        if (newEpic == null) return null;

        newEpic.setId(getNextId());
        epics.put(newEpic.getId(), newEpic);
        calculateEpicStatus(newEpic);

        return newEpic;
    }


    public Task updateTask(Task newTask) {
        if (newTask == null) return null;

        tasks.put(newTask.getId(), newTask);

        return newTask; // на случай, если таски с нужным id не будет в мапе и put вернёт null
    }

    public Subtask updateSubtask(Subtask newSubtask) {
        if (newSubtask == null) return null;

        int id = newSubtask.getId();
        Subtask oldSubtask = subtasks.get(id);
        Epic newSubtaskEpic = epics.get(newSubtask.getEpicId());
        Epic oldSubtaskEpic = epics.get(oldSubtask.getEpicId());

        if (oldSubtaskEpic != newSubtaskEpic) {
            oldSubtaskEpic.removeSubtask(id);
            newSubtaskEpic.addSubtask(id);
        }

        subtasks.put(id, newSubtask);
        calculateEpicStatus(newSubtaskEpic);
        calculateEpicStatus(oldSubtaskEpic);

        return newSubtask;
    }

    public Epic updateEpic(Epic newEpic) {
        if (newEpic == null) return null;

        Epic oldEpic = epics.get(newEpic.getId());

        oldEpic.setName(newEpic.getName());
        oldEpic.setDescription(newEpic.getDescription());

        return newEpic;
    }


    public void removeTask(int id) {
        tasks.remove(id);
    }

    public void removeSubtask(int id) {
        Subtask subtask = subtasks.get(id);

        if (subtask == null) return;

        Epic subtaskEpic = epics.get(subtask.getEpicId());

        subtaskEpic.removeSubtask(id);
        calculateEpicStatus(subtaskEpic);
        subtasks.remove(id);
    }

    public void removeEpic(int id) {
        Epic epic = epics.get(id);

        if (epic == null) return;

        epic.getSubtasksIds().forEach(subtasks::remove);
        epics.remove(id);
    }


    public List<Subtask> getEpicSubtasks(int id) {
        return epics.get(id)
                .getSubtasksIds()
                .stream()
                .map(subtasks::get)
                .toList();
    }


    private void calculateEpicStatus(Epic epic) {
        List<Status> subtaskStatuses = getEpicSubtasks(epic.getId())
                .stream()
                .map(Subtask::getStatus)
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
