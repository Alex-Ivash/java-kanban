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
    public static final LocalDateTime DEFAULT_START_TIME = LocalDateTime.MAX;
    public static final LocalDateTime DEFAULT_END_TIME = LocalDateTime.MIN;
    public static final Duration DEFAULT_DURATION = Duration.ZERO;
    protected int seq = -1;
    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Subtask> subtasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected TreeSet<Task> prioritizedTasks = new TreeSet<>(Comparator.comparing(Task::getStartTime).thenComparing(Task::getId));
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
        Task task = getNotNullValue(tasks.get(id), id);
        historyManager.add(task);

        return task;
    }

    @Override
    public Subtask getSubtask(int id) {
        Subtask subtask = (Subtask) getNotNullValue(subtasks.get(id), id);
        historyManager.add(subtask);

        return subtask;
    }

    @Override
    public Epic getEpic(int id) {
        Epic epic = (Epic) getNotNullValue(epics.get(id), id);
        historyManager.add(epic);

        return epic;
    }

    @Override
    public Task createTask(Task newTask) {
        newTask.setId(getNextId());
        validateCollision(newTask);
        tasks.put(newTask.getId(), newTask);

        prioritizedTasks.add(newTask);

        return newTask;
    }

    @Override
    public Subtask createSubtask(Subtask newSubtask) {
        newSubtask.setId(getNextId());
        validateCollision(newSubtask);

        Epic subtaskEpic = epics.get(newSubtask.getEpicId());

        subtaskEpic.addSubtask(newSubtask.getId());
        subtasks.put(newSubtask.getId(), newSubtask);
        prioritizedTasks.add(newSubtask);
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
        Task oldTask = tasks.get(id);

        if (isStartTimeOrDurationChanged(oldTask, newTask)) {
            validateCollision(newTask);
        }

        tasks.put(newTask.getId(), newTask);
        prioritizedTasks.remove(oldTask);
        prioritizedTasks.add(newTask);

        return newTask;
    }

    @Override
    public Subtask updateSubtask(Subtask newSubtask) {
        int id = newSubtask.getId();
        Subtask oldSubtask = subtasks.get(id);

        if (isStartTimeOrDurationChanged(oldSubtask, newSubtask)) {
            validateCollision(newSubtask);
        }

        Epic newSubtaskEpic = epics.get(newSubtask.getEpicId());
        Epic oldSubtaskEpic = epics.get(oldSubtask.getEpicId());

        if (oldSubtaskEpic != newSubtaskEpic) {
            oldSubtaskEpic.removeSubtask(id);
            newSubtaskEpic.addSubtask(id);
            calculateEpicState(oldSubtaskEpic);
        }

        subtasks.put(id, newSubtask);
        prioritizedTasks.remove(oldSubtask);
        prioritizedTasks.add(newSubtask);
        calculateEpicState(newSubtaskEpic);

        return newSubtask;
    }

    @Override
    public Epic updateEpic(Epic newEpic) {
        Epic oldEpic = epics.get(newEpic.getId());

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
        if (isDefaultStartTime(checkedTask)) {
            return;
        }

        Task lower = prioritizedTasks.floor(checkedTask);
        Task upper = prioritizedTasks.ceiling(checkedTask);

        Stream.of(lower, upper)
                .filter(Objects::nonNull)
                .filter(neighbour -> isCollision(neighbour, checkedTask))
                .findAny()
                .ifPresent(overlappingNeighbor -> {
                    throw new CollisionException(String.format("The execution interval of the added task collisions with the existing tasks with id %d", overlappingNeighbor.getId()));
                });
    }

    private boolean isCollision(Task lower, Task upper) {
        return lower.getStartTime().isBefore(upper.getEndTime())
                && upper.getStartTime().isBefore(lower.getEndTime());
    }

    private void calculateEpicState(Epic epic) {
        List<Subtask> epicsSubtasks = getEpicSubtasks(epic.getId());
        int statusesBitSet = 0;
        Duration newDuration = Duration.ZERO;
        LocalDateTime newStartTime = LocalDateTime.MAX;
        LocalDateTime newEndTime = LocalDateTime.MIN;

        for (Subtask epicSubtask : epicsSubtasks) {
            statusesBitSet |= 1 << epicSubtask.getStatus().bitSetIndex;

            if (isDefaultStartTime(epicSubtask)) {
                continue;
            }

            newStartTime = getUpdatedEpicStartTime(epicSubtask, newStartTime);
            newEndTime = getUpdatedEpicEndTime(epicSubtask, newEndTime);
            newDuration = newDuration.plus(epicSubtask.getDuration());
        }

        epic.setStartTime(newStartTime);
        epic.setEndTime(newEndTime);
        epic.setDuration(newDuration);
        epic.setStatus(getUpdatedEpicStatus(statusesBitSet));
    }

    private LocalDateTime getUpdatedEpicStartTime(Subtask subtask, LocalDateTime startTime) {
        LocalDateTime newStartTime = subtask.getStartTime();

        return newStartTime.isBefore(startTime) ? newStartTime : startTime;
    }

    private LocalDateTime getUpdatedEpicEndTime(Subtask subtask, LocalDateTime endTime) {
        LocalDateTime newEndTime = subtask.getEndTime();

        return newEndTime.isAfter(endTime) ? newEndTime : endTime;
    }

    private TaskStatus getUpdatedEpicStatus(int statusesBitSet) {
        return switch (statusesBitSet) {
            case 0, 1 -> TaskStatus.NEW;
            case 4 -> TaskStatus.DONE;
            default -> TaskStatus.IN_PROGRESS;
        };
    }

    private int getNextId() {
        return ++seq;
    }

    private boolean isDefaultStartTime(Task task) {
        return task.getStartTime().equals(DEFAULT_START_TIME);
    }

    private boolean isStartTimeOrDurationChanged(Task originalTask, Task updatedTask) {
        return !originalTask.getStartTime().equals(updatedTask.getStartTime())
                || !originalTask.getDuration().equals(updatedTask.getDuration());

    }

    private Task getNotNullValue(Task task, int id) {
        if (task == null) {
            throw new NotFoundException(String.format("Task with id %d not found", id));
        }

        return task;
    }
}
