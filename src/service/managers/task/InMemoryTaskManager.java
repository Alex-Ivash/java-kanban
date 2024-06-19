package service.managers.task;

import exception.CollisionException;
import exception.NotFoundException;
import model.Epic;
import model.Subtask;
import model.Task;
import model.TaskStatus;
import service.managers.history.HistoryManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

public class InMemoryTaskManager implements TaskManager {
    protected int seq = -1;
    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Subtask> subtasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected TreeSet<Task> prioritizedTasks = new TreeSet<>(Comparator.comparing(Task::getStartTime));
    protected final HistoryManager historyManager;

    public InMemoryTaskManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
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
        tasks.forEach((id, task) -> {
            historyManager.remove(id);
            prioritizedTasks.remove(task);
        });

        tasks.clear();
    }

    @Override
    public void removeAllSubtasks() {
        subtasks.forEach((id, task) -> {
            historyManager.remove(id);
            prioritizedTasks.remove(task);
        });

        subtasks.clear();

        epics.forEach((id, epic) -> {
            epic.getSubtasksIds().clear();
            calculateEpicState(epic);
        });
    }

    @Override
    public void removeAllEpics() {
        subtasks.forEach((id, task) -> {
            historyManager.remove(id);
            prioritizedTasks.remove(task);
        });

        epics.forEach((id, task) -> historyManager.remove(id));
        subtasks.clear();
        epics.clear();
    }

    @Override
    public Task getTask(int id) {
        Task task = tasks.get(id);
        historyManager.add(findTask(task, id));

        return task;
    }

    @Override
    public Subtask getSubtask(int id) {
        Subtask subtask = subtasks.get(id);
        historyManager.add(findTask(subtask, id));

        return subtask;
    }

    @Override
    public Epic getEpic(int id) {
        Epic epic = epics.get(id);
        historyManager.add(findTask(epic, id));

        return epic;
    }

    @Override
    public Task createTask(Task newTask) {
        validateCollision(newTask);
        newTask.setId(getNextId());
        tasks.put(newTask.getId(), newTask);

        if (!newTask.getStartTime().equals(Task.DEFAULT_START_TIME)) {
            prioritizedTasks.add(newTask);
        }

        return newTask;
    }

    @Override
    public Subtask createSubtask(Subtask newSubtask) {
        validateCollision(newSubtask);
        newSubtask.setId(getNextId());

        Epic subtaskEpic = (Epic) findTask(epics.get(newSubtask.getEpicId()), newSubtask.getEpicId());

        subtaskEpic.addSubtask(newSubtask.getId());
        subtasks.put(newSubtask.getId(), newSubtask);

        if (!newSubtask.getStartTime().equals(Task.DEFAULT_START_TIME)) {
            prioritizedTasks.add(newSubtask);
        }

        calculateEpicState(subtaskEpic);

        return newSubtask;
    }

    @Override
    public Epic createEpic(Epic newEpic) {
        newEpic.setId(getNextId());
        epics.put(newEpic.getId(), newEpic);
        calculateEpicState(newEpic);

        return newEpic;
    }


    @Override
    public Task updateTask(Task newTask) {
        int id = newTask.getId();
        Task oldTask = findTask(tasks.get(id), id);

        validateCollision(newTask, oldTask);

        tasks.put(newTask.getId(), newTask);

        if (!oldTask.getStartTime().equals(newTask.getStartTime())
                || (!oldTask.getDuration().equals(newTask.getDuration()))) {
            prioritizedTasks.remove(oldTask);
            prioritizedTasks.add(newTask);
        }

        return newTask;
    }

    @Override
    public Subtask updateSubtask(Subtask newSubtask) {
        int id = newSubtask.getId();
        Subtask oldSubtask = (Subtask) findTask(subtasks.get(id), id);
        validateCollision(newSubtask, oldSubtask);

        Epic newSubtaskEpic = (Epic) findTask(epics.get(newSubtask.getEpicId()), newSubtask.getEpicId());
        Epic oldSubtaskEpic = epics.get(oldSubtask.getEpicId());

        if (oldSubtaskEpic != newSubtaskEpic) {
            oldSubtaskEpic.removeSubtask(id);
            newSubtaskEpic.addSubtask(id);
            calculateEpicState(oldSubtaskEpic);
        }

        subtasks.put(id, newSubtask);

        if (!oldSubtask.getStartTime().equals(newSubtask.getStartTime())
                || (!oldSubtask.getDuration().equals(newSubtask.getDuration()))) {
            prioritizedTasks.remove(oldSubtask);
            prioritizedTasks.add(newSubtask);
        }

        calculateEpicState(newSubtaskEpic);

        return newSubtask;
    }

    @Override
    public Epic updateEpic(Epic newEpic) {
        Epic oldEpic = (Epic) findTask(epics.get(newEpic.getId()), newEpic.getId());

        oldEpic.setName(newEpic.getName());
        oldEpic.setDescription(newEpic.getDescription());

        return oldEpic;
    }


    @Override
    public void removeTask(int id) {
        Task task = tasks.get(id);

        if (task == null) {
            return;
        }

        tasks.remove(id);
        historyManager.remove(id);
        prioritizedTasks.remove(task);
    }

    @Override
    public void removeSubtask(int id) {
        Subtask subtask = subtasks.get(id);

        if (subtask == null) {
            return;
        }

        Epic subtaskEpic = epics.get(subtask.getEpicId());

        subtaskEpic.removeSubtask(id);
        calculateEpicState(subtaskEpic);

        subtasks.remove(id);
        historyManager.remove(id);
        prioritizedTasks.remove(subtask);
    }

    @Override
    public void removeEpic(int id) {
        Epic epic = epics.get(id);

        if (epic == null) {
            return;
        }

        epic.getSubtasksIds().forEach(historyManager::remove);
        epic.getSubtasksIds().forEach(subtaskId -> {
            prioritizedTasks.remove(subtasks.get(subtaskId));
            subtasks.remove(subtaskId);
        });

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

    public List<Task> getPrioritizedTasks() {
        return new ArrayList<>(prioritizedTasks);
    }

    private void validateCollision(final Task checkedTask) {
        if (checkedTask.getStartTime().equals(Task.DEFAULT_START_TIME)) {
            return;
        }

        Stream.of(prioritizedTasks.floor(checkedTask), prioritizedTasks.ceiling(checkedTask))
                .filter(Objects::nonNull)
                .filter(neighbour -> (neighbour.getStartTime().isBefore(checkedTask.getEndTime())))
                .filter(neighbour -> (checkedTask.getStartTime().isBefore(neighbour.getEndTime())))
                .findAny()
                .ifPresent(conflictingNeighbor -> {
                    throw new CollisionException(String.format("The execution interval of the added task collisions with the existing tasks with id %d", conflictingNeighbor.getId()));
                });
    }

    private void validateCollision(Task checkedNewTask, Task checkedOldTask) {
        if (!checkedOldTask.getStartTime().equals(checkedNewTask.getStartTime())
                || (!checkedOldTask.getDuration().equals(checkedNewTask.getDuration()))) {
            validateCollision(checkedNewTask);
        }
    }

    private void calculateEpicState(Epic epic) {
        List<Subtask> epicsSubtasks = getEpicSubtasks(epic.getId());
        int statusesBitSet = 0;

        if (epicsSubtasks.isEmpty()) {
            calculateEpicStatus(epic, statusesBitSet);
            return;
        }

        Subtask initialSubtask = epicsSubtasks.getFirst();
        Duration newDuration = Duration.ZERO;
        LocalDateTime newStartTime = initialSubtask.getStartTime();
        LocalDateTime newEndTime = initialSubtask.getEndTime();

        for (Subtask epicSubtask : epicsSubtasks) {
            statusesBitSet |= 1 << epicSubtask.getStatus().bitSetIndex;
            LocalDateTime startTime = epicSubtask.getStartTime();

            if (startTime.equals(Task.DEFAULT_START_TIME)) {
                continue;
            }

            newStartTime = startTime.isBefore(newStartTime) ? startTime : newStartTime;
            LocalDateTime endTime = epicSubtask.getEndTime();
            newEndTime = endTime.isAfter(newEndTime) ? endTime : newEndTime;
            newDuration = newDuration.plus(epicSubtask.getDuration());
        }

        epic.setStartTime(newStartTime);
        epic.setEndTime(newEndTime);
        epic.setDuration(newDuration);

        calculateEpicStatus(epic, statusesBitSet);
    }

    private void calculateEpicStatus(Epic epic, int statusesBitSet) {
        epic.setStatus(
                switch (statusesBitSet) {
                    case 0, 1 -> TaskStatus.NEW;
                    case 4 -> TaskStatus.DONE;
                    default -> TaskStatus.IN_PROGRESS;
                }
        );
    }

    private Task findTask(Task task, int id) {
        if (task == null) {
            throw new NotFoundException(String.format("Task with id %d not found", id));
        }

        return task;
    }

    private int getNextId() {
        return ++seq;
    }
}
