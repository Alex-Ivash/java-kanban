package service.managers.task;

import model.Epic;
import model.Subtask;
import model.Task;
import model.TaskStatus;
import service.managers.history.HistoryManager;

import java.util.HashMap;
import java.util.List;

public class InMemoryTaskManager implements TaskManager {
    protected int seq;
    protected final HashMap<Integer, Task> tasks = new HashMap<>();
    protected final HashMap<Integer, Subtask> subtasks = new HashMap<>();
    protected final HashMap<Integer, Epic> epics = new HashMap<>();
    protected final HistoryManager historyManager;

    public InMemoryTaskManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
        this.seq = -1;
    }

    @Override
    public List<Task> getAllTasks() {
        return tasks.values().stream().toList();
    }

    @Override
    public List<Subtask> getAllSubTasks() {
        return subtasks.values().stream().toList();
    }

    @Override
    public List<Epic> getAllEpics() {
        return epics.values().stream().toList();
    }


    @Override
    public void removeAllTasks() {
        tasks.forEach((id, task) -> historyManager.remove(id));
        tasks.clear();
    }

    @Override
    public void removeAllSubtasks() {
        subtasks.forEach((id, task) -> historyManager.remove(id));
        subtasks.clear();
        epics.forEach((id, epic) -> {
            epic.getSubtasksIds().clear();
            calculateEpicStatus(epic);
        });
    }

    @Override
    public void removeAllEpics() {
        subtasks.forEach((id, task) -> historyManager.remove(id));
        epics.forEach((id, task) -> historyManager.remove(id));
        subtasks.clear();
        epics.clear();
    }


    @Override
    public Task getTask(int id) {
        Task task = tasks.get(id);
        historyManager.add(task);

        return task;
    }

    @Override
    public Subtask getSubtask(int id) {
        Subtask subtask = subtasks.get(id);
        historyManager.add(subtask);

        return subtask;
    }

    @Override
    public Epic getEpic(int id) {
        Epic epic = epics.get(id);
        historyManager.add(epic);

        return epic;
    }


    @Override
    public Task createTask(Task newTask) {
        if (newTask == null) {
            return null;
        }

        newTask.setId(getNextId());
        tasks.put(newTask.getId(), newTask);

        return newTask;
    }

    @Override
    public Subtask createSubtask(Subtask newSubtask) {
        if (newSubtask == null) {
            return null;
        }

        newSubtask.setId(getNextId());

        Epic subtaskEpic = epics.get(newSubtask.getEpicId());

        subtaskEpic.addSubtask(newSubtask.getId());
        subtasks.put(newSubtask.getId(), newSubtask);
        calculateEpicStatus(subtaskEpic);

        return newSubtask;
    }

    @Override
    public Epic createEpic(Epic newEpic) {
        if (newEpic == null) {
            return null;
        }

        newEpic.setId(getNextId());
        epics.put(newEpic.getId(), newEpic);
        calculateEpicStatus(newEpic);

        return newEpic;
    }


    @Override
    public Task updateTask(Task newTask) {
        if (newTask == null) {
            return null;
        }

        tasks.put(newTask.getId(), newTask);

        return newTask;
    }

    @Override
    public Subtask updateSubtask(Subtask newSubtask) {
        if (newSubtask == null) {
            return null;
        }

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

    @Override
    public Epic updateEpic(Epic newEpic) {
        if (newEpic == null) {
            return null;
        }

        Epic oldEpic = epics.get(newEpic.getId());

        oldEpic.setName(newEpic.getName());
        oldEpic.setDescription(newEpic.getDescription());

        return oldEpic;
    }


    @Override
    public void removeTask(int id) {
        tasks.remove(id);
        historyManager.remove(id);
    }

    @Override
    public void removeSubtask(int id) {
        Subtask subtask = subtasks.get(id);

        if (subtask == null) {
            return;
        }

        Epic subtaskEpic = epics.get(subtask.getEpicId());

        subtaskEpic.removeSubtask(id);
        calculateEpicStatus(subtaskEpic);
        subtasks.remove(id);
        historyManager.remove(id);
    }

    @Override
    public void removeEpic(int id) {
        Epic epic = epics.get(id);

        if (epic == null) {
            return;
        }

        epic.getSubtasksIds().forEach(historyManager::remove);
        epic.getSubtasksIds().forEach(subtasks::remove);

        epics.remove(id);
        historyManager.remove(id);
    }


    @Override
    public List<Subtask> getEpicSubtasks(int id) {
        return epics.get(id)
                .getSubtasksIds()
                .stream()
                .map(subtasks::get)
                .toList();
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }


    private void calculateEpicStatus(Epic epic) {
        List<TaskStatus> subtaskStatuses = getEpicSubtasks(epic.getId())
                .stream()
                .map(Subtask::getStatus)
                .toList();
        TaskStatus newStatus = null;

        if (subtaskStatuses.isEmpty() || subtaskStatuses.stream().allMatch(status -> status == TaskStatus.NEW)) {
            newStatus = TaskStatus.NEW;
        } else if (subtaskStatuses.stream().allMatch(status -> status == TaskStatus.DONE)) {
            newStatus = TaskStatus.DONE;
        } else {
            newStatus = TaskStatus.IN_PROGRESS;
        }

        epic.setStatus(newStatus);
    }


    private int getNextId() {
        return ++seq;
    }
}
