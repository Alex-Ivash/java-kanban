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
    public static final LocalDateTime NULL_START_TIME_INDICATOR = LocalDateTime.MAX;
    public static final LocalDateTime NULL_END_TIME_INDICATOR = LocalDateTime.MIN;
    public static final Duration NULL_DURATION_INDICATOR = Duration.ZERO;
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
        if (newTask == null) {
            throw new IllegalArgumentException("Task should not be null");
        }

        newTask.setId(getNextId());
        applyNullIndicatorStartTimeAndDurationIfMissing(newTask);
        validateCollision(newTask);
        tasks.put(newTask.getId(), newTask);
        prioritizedTasks.add(newTask);

        return newTask;
    }


    @Override
    public Subtask createSubtask(Subtask newSubtask) {
        if (newSubtask == null) {
            throw new IllegalArgumentException("Subtask should not be null");
        }

        newSubtask.setId(getNextId());

        applyNullIndicatorStartTimeAndDurationIfMissing(newSubtask);
        validateCollision(newSubtask);

        int subtaskEpicId = newSubtask.getEpicId(); // тут бы еще влепить проверку на null в id эпика, наверное, хоть и нельзя его не задать, так как он есть в конструкторе
        Epic subtaskEpic = (Epic) getNotNullValue(epics.get(subtaskEpicId), subtaskEpicId);
        subtaskEpic.addSubtask(newSubtask.getId());

        subtasks.put(newSubtask.getId(), newSubtask);
        prioritizedTasks.add(newSubtask);
        calculateEpicState(subtaskEpic);

        return newSubtask;
    }

    @Override
    public Epic createEpic(Epic newEpic) {
        if (newEpic == null) {
            throw new IllegalArgumentException("Epic should not be null");
        }

        newEpic.setId(getNextId());

        applyNullIndicatorStartTimeAndDurationIfMissing(newEpic);

        epics.put(newEpic.getId(), newEpic);
        calculateEpicState(newEpic);

        return newEpic;
    }


    @Override
    public Task updateTask(Task newTask) {
        if (newTask == null) {
            throw new IllegalArgumentException("Task should not be null");
        }

        int id = newTask.getId();
        Task oldTask = getNotNullValue(tasks.get(id), id);

        applyNullIndicatorStartTimeAndDurationIfMissing(newTask);

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
        if (newSubtask == null) {
            throw new IllegalArgumentException("Subtask should not be null");
        }

        int id = newSubtask.getId();
        Subtask oldSubtask = (Subtask) getNotNullValue(subtasks.get(id), id);

        applyNullIndicatorStartTimeAndDurationIfMissing(newSubtask);

        if (isStartTimeOrDurationChanged(oldSubtask, newSubtask)) {
            validateCollision(newSubtask);
        }

        int newSubtaskEpicId = newSubtask.getEpicId();
        int oldSubtaskEpicId = oldSubtask.getEpicId();

        Epic newSubtaskEpic = (Epic) getNotNullValue(epics.get(newSubtaskEpicId), newSubtaskEpicId);
        Epic oldSubtaskEpic = epics.get(oldSubtaskEpicId);

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
        if (newEpic == null) {
            throw new IllegalArgumentException("Epic should not be null");
        }

        int id = newEpic.getId();
        Epic oldEpic = (Epic) getNotNullValue(epics.get(id), id);

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
        Duration newDuration = NULL_DURATION_INDICATOR;
        LocalDateTime newStartTime = NULL_START_TIME_INDICATOR;
        LocalDateTime newEndTime = NULL_END_TIME_INDICATOR;
        TaskStatus newStatus = epicsSubtasks.isEmpty() ? TaskStatus.NEW : epicsSubtasks.getFirst().getStatus();

        for (Subtask epicSubtask : epicsSubtasks) {
            if (newStatus != epicSubtask.getStatus()) {
                newStatus = TaskStatus.IN_PROGRESS;
            }

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
        epic.setStatus(newStatus);
    }

    private LocalDateTime getUpdatedEpicStartTime(Subtask subtask, LocalDateTime startTime) {
        LocalDateTime newStartTime = subtask.getStartTime();

        return newStartTime.isBefore(startTime) ? newStartTime : startTime;
    }

    private LocalDateTime getUpdatedEpicEndTime(Subtask subtask, LocalDateTime endTime) {
        LocalDateTime newEndTime = subtask.getEndTime();

        return newEndTime.isAfter(endTime) ? newEndTime : endTime;
    }

    private int getNextId() {
        return ++seq;
    }

    private boolean isDefaultStartTime(Task task) {
        return task.getStartTime().equals(NULL_START_TIME_INDICATOR);
    }

    private boolean isStartTimeOrDurationChanged(Task originalTask, Task updatedTask) {
        return !originalTask.getStartTime().equals(updatedTask.getStartTime())
                || !originalTask.getDuration().equals(updatedTask.getDuration());

    }

    private void applyNullIndicatorStartTimeAndDurationIfMissing(Task task) {
        if (task.getStartTime() == null || task.getDuration() == null) {
            task.setStartTime(NULL_START_TIME_INDICATOR);
            task.setDuration(NULL_DURATION_INDICATOR);
        }
    }

    private void applyNullIndicatorStartTimeAndDurationIfMissing(Epic epic) {
        if (epic.getStartTime() == null || epic.getDuration() == null) {
            epic.setStartTime(NULL_START_TIME_INDICATOR);
            epic.setDuration(NULL_DURATION_INDICATOR);
            epic.setEndTime(NULL_END_TIME_INDICATOR);
        }
    }

    private Task getNotNullValue(Task task, int id) {
        if (task == null) {
            throw new NotFoundException(String.format("Task with id %d not found", id));
        }

        return task;
    }
}
