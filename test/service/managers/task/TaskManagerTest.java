package service.managers.task;

import exception.CollisionException;
import exception.NotFoundException;
import model.Epic;
import model.Subtask;
import model.Task;
import model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaskManagerTest")
abstract class TaskManagerTest<T extends TaskManager> {
    protected T taskManager;

    abstract void initTaskManager();

    @BeforeEach
    void setUp() {
        initTaskManager();
    }

    @Test
    @DisplayName("Список по приоритетам сортируется на основе времени начала задачи")
    void getPrioritizedTasks_SortedByTaskStartTime() {
        //given
        Task task = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.now().plusDays(1), Duration.ofHours(1));
        taskManager.createTask(task);
        Task addedTask = taskManager.getTask(0);

        Epic epic = new Epic("name", "descr");
        taskManager.createEpic(epic);
        Epic adedEpic = taskManager.getEpic(1);

        Subtask subtask = new Subtask(TaskStatus.NEW, "name", "descr", 1, LocalDateTime.now(), Duration.ofHours(1));
        taskManager.createSubtask(subtask);
        Subtask addedSubtask = taskManager.getSubtask(2);

        //when
        List<Task> prioritizedTasks = taskManager.getPrioritizedTasks();

        //then
        assertAll(
                () -> assertNotEquals(0, prioritizedTasks.size(), "Список по приоритетам не формируется вообще"),
                () -> assertAll("Добавляемые с список по приоритетам задачи не сортируются по времени начала",
                        () -> assertEquals(prioritizedTasks.getFirst(), addedSubtask),
                        () -> assertEquals(prioritizedTasks.getLast(), addedTask)
                )
        );
    }

    @Test
    @DisplayName("При попытке получения не существующей Task выбрасывается NotFoundException")
    void getTask_ThrownNotFoundException_TaskDoesNotExist() {
        assertThrows(NotFoundException.class, () -> taskManager.getTask(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("При попытке получения не существующей Subtask выбрасывается NotFoundException")
    void getSubtask_ThrownNotFoundException_TaskDoesNotExist() {
        assertThrows(NotFoundException.class, () -> taskManager.getSubtask(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("При попытке получения не существующей Epic выбрасывается NotFoundException")
    void getEpic_ThrownNotFoundException_TaskDoesNotExist() {
        assertThrows(NotFoundException.class, () -> taskManager.getEpic(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("При получении Task она добавляется в историю")
    void getTask_AddedToHistory() {
        //given
        Task task = new Task(TaskStatus.NEW, "", "");
        taskManager.createTask(task);

        //when
        Task controlTask = taskManager.getTask(0);

        //then
        assertEquals(controlTask, taskManager.getHistory().getFirst(), "Получение Task не добавляет её в историю");
    }

    @Test
    @DisplayName("При получении Subtask она добавляется в историю")
    void getSubtask_AddedToHistory() {
        //given
        Epic epic = new Epic("", "");
        Subtask task = new Subtask(TaskStatus.NEW, "", "", 0);
        taskManager.createEpic(epic);
        taskManager.createSubtask(task);

        //when
        Subtask controlSubtask = taskManager.getSubtask(1);

        //then
        assertEquals(controlSubtask, taskManager.getHistory().getFirst(), "Получение Subtask не добавляет её в историю");
    }

    @Test
    @DisplayName("При получении Epic она добавляется в историю")
    void getEpic_AddedToHistory() {
        //given
        Epic epic = new Epic("", "");
        Subtask task = new Subtask(TaskStatus.NEW, "", "", 0);
        taskManager.createEpic(epic);
        taskManager.createSubtask(task);

        //when
        Epic controlEpic = taskManager.getEpic(0);

        //then
        assertEquals(controlEpic, taskManager.getHistory().getFirst(), "Получение Epic не добавляет её в историю");
    }

    @Test
    @DisplayName("При повторном получении Task из истории удаляется старый Task и добавляется только что возвращенный")
    void getTask_RemovesPreviousFromHistoryAndAddsFresh_ReGet() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "", "");
        Task task2 = new Task(TaskStatus.NEW, "", "");
        taskManager.createTask(task1);
        taskManager.createTask(task2);

        //when
        Task controlTask1 = taskManager.getTask(0);
        taskManager.getTask(0);
        Task controlTask2 = taskManager.getTask(1);

        //then
        List<Task> history = taskManager.getHistory();
        assertAll("Повторный просмотр задачи с тем же id не удаляет прошлый просмотр из истории",
                () -> assertEquals(2, history.size()),
                () -> assertEquals(controlTask1, history.getFirst()),
                () -> assertEquals(controlTask2, history.getLast())
        );
    }

    @Test
    @DisplayName("При повторном получении Subtask из истории удаляется старый Subtask и добавляется только что возвращенный")
    void getSubtask_RemovesPreviousFromHistoryAndAddsFresh_ReGet() {
        //given
        Epic epic = new Epic("", "");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "", "", 0);
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "", "", 0);
        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        //when
        Subtask controlSubtask1 = taskManager.getSubtask(1);
        taskManager.getSubtask(1);
        Subtask controlSubtask2 = taskManager.getSubtask(2);

        //then
        List<Task> history = taskManager.getHistory();
        assertAll("Повторный просмотр подзадачи с тем же id не удаляет прошлый просмотр из истории",
                () -> assertEquals(2, history.size()),
                () -> assertEquals(controlSubtask1, history.getFirst()),
                () -> assertEquals(controlSubtask2, history.getLast())
        );
    }

    @Test
    @DisplayName("При повторном получении Epic из истории удаляется старый Epic и добавляется только что возвращенный")
    void getEpic_RemovesPreviousFromHistoryAndAddsFresh_ReGet() {
        //given
        Epic epic1 = new Epic("", "");
        Epic epic2 = new Epic("", "");

        taskManager.createEpic(epic1);
        taskManager.createEpic(epic2);

        //when
        Epic controlEpic1 = taskManager.getEpic(0);
        taskManager.getEpic(0);
        Epic controlEpic2 = taskManager.getEpic(1);

        //then
        List<Task> history = taskManager.getHistory();
        assertAll("Повторный просмотр подзадачи с тем же id не удаляет прошлый просмотр из истории",
                () -> assertEquals(2, history.size()),
                () -> assertEquals(controlEpic1, history.getFirst()),
                () -> assertEquals(controlEpic2, history.getLast())
        );
    }

    @Test
    @DisplayName("При получении Task после обновления удаляется ранее существующая в истории задача и добавляется свежая")
    void getTask_AddsMostActual_AfterUpdate() {
        //given
        Task task = new Task(TaskStatus.NEW, "", "");
        Task newTask = new Task(0, TaskStatus.DONE, "new", "new_description", LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createTask(task);
        taskManager.updateTask(newTask);

        //when
        Task updatedTask = taskManager.getTask(0);

        //then
        assertEquals(1, taskManager.getHistory().size(), "Старая задача не удалилась / новая не добавилась");
        assertEquals(updatedTask, taskManager.getHistory().getFirst(), "Задача была обновлена, но в истории старая");
    }

    @Test
    @DisplayName("При получении Subtask после обновления удаляется ранее существующая в истории задача и добавляется свежая")
    void getSubtask_AddsMostActual_AfterUpdate() {
        //given
        Epic epic = new Epic("", "");
        Subtask subtask = new Subtask(TaskStatus.DONE, "", "", 0);
        Subtask newSubtask = new Subtask(1, TaskStatus.IN_PROGRESS, "new", "new_description", 0, LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask);
        taskManager.updateSubtask(newSubtask);

        //when
        Subtask updatedSubtask = taskManager.getSubtask(1);

        //then
        assertEquals(1, taskManager.getHistory().size(), "Старая задача не удалилась / новая не добавилась");
        assertEquals(updatedSubtask, taskManager.getHistory().getFirst(), "Задача была обновлена, но в истории старая");
    }

    @Test
    @DisplayName("При получении Epic после обновления удаляется ранее существующая в истории задача и добавляется свежая")
    void getEpic_AddsMostActual_AfterUpdate() {
        //given
        Epic epic = new Epic("", "");
        Epic newEpic = new Epic(0, TaskStatus.NEW, "new", "new_description", LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createEpic(epic);
        taskManager.updateEpic(newEpic);

        //when
        Epic updatedEpic = taskManager.getEpic(0);

        //then
        assertEquals(1, taskManager.getHistory().size(), "Старая задача не удалилась / новая не добавилась");
        assertEquals(updatedEpic, taskManager.getHistory().getFirst(), "Задача была обновлена, но в истории старая");
    }

    @Test
    @DisplayName("При передаче null в аргумент метода создания Task будет выброшено IllegalArgumentException")
    void createTask_1() {
        assertThrows(IllegalArgumentException.class, () -> taskManager.createTask(null), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При передаче null в аргумент метода создания Subtask будет выброшено IllegalArgumentException")
    void createSubtask_1() {
        assertThrows(IllegalArgumentException.class, () -> taskManager.createSubtask(null), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При передаче null в аргумент метода создания Epic будет выброшено IllegalArgumentException")
    void createEpic_1() {
        assertThrows(IllegalArgumentException.class, () -> taskManager.createEpic(null), "Исключение не выброшено");
    }

    @Test
    @DisplayName("Состояние добавленной задачи типа Task не отличается от состояния добавляемой в менеджер")
    void createTask_StateEqualsToAddedInManager_NoDifference() {
        //given
        Task task = new Task(TaskStatus.NEW, "name", "description", LocalDateTime.now(), Duration.ofDays(1));

        TaskStatus taskStatus = task.getStatus();
        String taskName = task.getName();
        String taskDescription = task.getDescription();
        LocalDateTime taskStartTime = task.getStartTime();
        Duration taskDuration = task.getDuration();

        //when
        taskManager.createTask(task);

        //then
        Task addedTask = taskManager.getTask(0);
        assertAll(
                () -> assertEquals(taskName, addedTask.getName(), "Имя добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(taskDescription, addedTask.getDescription(), "Описание добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(taskStartTime, addedTask.getStartTime(), "Время старта добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(taskDuration, addedTask.getDuration(), "Длительность добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(taskStatus, task.getStatus(), "Статус добавляемой и добавленной задачи изменился при добавлении")
        );
    }

    @Test
    @DisplayName("Состояние добавленной задачи типа Subtask не отличается от состояния добавляемой в менеджер")
    void createSubtask_StateEqualsToAddedInManager_NoDifference() {
        //given
        Epic epic = new Epic("epicName", "epicDescription");
        taskManager.createEpic(epic);
        Subtask subtask = new Subtask(TaskStatus.NEW, "name", "description", 0, LocalDateTime.now(), Duration.ofDays(1));

        TaskStatus taskStatus = subtask.getStatus();
        String taskName = subtask.getName();
        String taskDescription = subtask.getDescription();
        LocalDateTime taskStartTime = subtask.getStartTime();
        Duration taskDuration = subtask.getDuration();
        int epicId = subtask.getEpicId();

        //when
        taskManager.createSubtask(subtask);

        //then
        Subtask addedSubtask = taskManager.getSubtask(1);
        assertAll(
                () -> assertEquals(taskName, addedSubtask.getName(), "Имя добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(taskDescription, addedSubtask.getDescription(), "Описание добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(taskStartTime, addedSubtask.getStartTime(), "Время старта добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(taskDuration, addedSubtask.getDuration(), "Длительность добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(taskStatus, subtask.getStatus(), "Статус добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(epicId, addedSubtask.getEpicId(), "ID эпика добавляемой и добавленной задачи изменился при добавлении")
        );
    }

    @Test
    @DisplayName("Состояние добавленной задачи типа Epic не отличается от состояния добавляемой в менеджер")
    void createEpic_StateEqualsToAddedInManager_NoDifference() {
        //given
        Epic epic = new Epic("name", "description");

        String epicName = epic.getName();
        String epicDescription = epic.getDescription();

        //when
        taskManager.createEpic(epic);

        //then
        Epic addedEpic = taskManager.getEpic(0);
        assertAll(
                () -> assertEquals(epicName, addedEpic.getName(), "Имя добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(epicDescription, addedEpic.getDescription(), "Описание добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertTrue(addedEpic.getSubtasksIds().isEmpty(), "Список подзадач у добавляемого и добавленного эпика изменился при добавлении"),
                () -> assertEquals(InMemoryTaskManager.NULL_START_TIME_INDICATOR, addedEpic.getStartTime(), "Время старта добавляемого и добавленного эпика изменилось при добавлении"),
                () -> assertEquals(InMemoryTaskManager.NULL_END_TIME_INDICATOR, addedEpic.getEndTime(), "Время окончания добавляемого и добавленного эпика изменилось при добавлении"),
                () -> assertEquals(InMemoryTaskManager.NULL_DURATION_INDICATOR, addedEpic.getDuration(), "Длительность добавляемого и добавленного эпика изменилось при добавлении"),
                () -> assertEquals(TaskStatus.NEW, addedEpic.getStatus(), "Статус добавляемого и добавленного эпика изменился при добавлении")
        );
    }

    @Test
    @DisplayName("При создании Task c заданным временем начала она добавляется в список по приоритетам")
    void createTask_AddsToPrioritizedList_WithStartTime() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.now().plusDays(1), Duration.ofHours(1));

        //when
        taskManager.createTask(task1);

        //then
        Task addedTask1 = taskManager.getTask(0);
        assertEquals(addedTask1, taskManager.getPrioritizedTasks().getFirst(), "Task не добавляется в список по приоритетам при заданном времени начала");
    }

    @Test
    @DisplayName("При создании Task c не заданным временем начала она добавляется в конец списка по приоритетам")
    void createTask_AddsToEndOfPrioritizedList_WithoutStartTime() {
        //given
        Task taskWithStartTime = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.now().plusDays(1), Duration.ofHours(1));
        Task taskWithoutStartTime = new Task(TaskStatus.NEW, "name", "descr");

        //when
        taskManager.createTask(taskWithoutStartTime);
        taskManager.createTask(taskWithStartTime);

        //then
        Task addedTaskWithoutStartTime = taskManager.getTask(0);
        assertEquals(addedTaskWithoutStartTime, taskManager.getPrioritizedTasks().getLast(), "Task не добавляется в конец списка по приоритетам при не заданном времени начала");
    }

    @Test
    @DisplayName("При создании Subtask с заданным временем начала она добавляется в список по приоритетам")
    void createSubtask_AddsToPrioritizedList_WithStartTime() {
        //given
        Epic epic = new Epic("epicName", "epicDescription");
        taskManager.createEpic(epic);
        Subtask subtask = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.now().plusDays(1), Duration.ofHours(1));

        //when
        taskManager.createSubtask(subtask);

        //then
        Subtask addedSubtask = taskManager.getSubtask(1);
        assertEquals(addedSubtask, taskManager.getPrioritizedTasks().getFirst(), "Subtask не добавляется в список по приоритетам при заданном времени начала");
    }

    @Test
    @DisplayName("При создании Subtask с не заданным временем начала она добавляется в конец списка по приоритетам")
    void createSubtask_AddsToEndOfPrioritizedList_WithoutStartTime() {
        //given
        Epic epic = new Epic("epicName", "epicDescription");
        taskManager.createEpic(epic);
        Subtask subtaskWithStartTime = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.now().plusDays(1), Duration.ofHours(1));
        Subtask subtaskWithoutStartTime = new Subtask(TaskStatus.NEW, "name", "descr", epic.getId());

        //when
        taskManager.createSubtask(subtaskWithoutStartTime);
        taskManager.createSubtask(subtaskWithStartTime);

        //then
        Subtask addedSubtaskWithoutStartTime = taskManager.getSubtask(1);
        assertEquals(addedSubtaskWithoutStartTime, taskManager.getPrioritizedTasks().getLast(), "Subtask не добавляется в конец списка по приоритетам при не заданном времени начала");
    }

    @Test
    @DisplayName("При создании эпика он не должен добавляться в список по приоритетам")
    void createEpic_NotAddsToPrioritizedList() {
        //given
        Epic epic = new Epic("epicName", "epicDescription");

        //when
        taskManager.createEpic(epic);

        //then
        assertEquals(0, taskManager.getPrioritizedTasks().size(), "Epic добавился в список по приоритетам");
    }

    @Test
    @DisplayName("При создании подзадачи обновляется состояние её эпика")
    void createSubtask_EpicStateUpdatedUponCreation() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        taskManager.createEpic(epic);
        Subtask subtask1 = new Subtask(TaskStatus.DONE, "name", "descr", 0, LocalDateTime.now(), Duration.ofHours(1));
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "name", "descr", 0, subtask1.getEndTime(), Duration.ofHours(1));
        TaskStatus newEpicTaskStatus = TaskStatus.IN_PROGRESS;
        LocalDateTime newEpicStartTime = subtask1.getStartTime();
        LocalDateTime newEpicEndTime = subtask2.getEndTime();
        Duration newEpicDuration = subtask1.getDuration().plus(subtask2.getDuration());

        //when
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        //then
        assertAll(
                () -> assertEquals(epic.getStatus(), newEpicTaskStatus, "Статус эпика не обновился"),
                () -> assertEquals(epic.getStartTime(), newEpicStartTime, "Время начала эпика не обновилось"),
                () -> assertEquals(epic.getEndTime(), newEpicEndTime, "Время окончания эпика не обновилось"),
                () -> assertEquals(epic.getDuration(), newEpicDuration, "Продолжительность эпика не обновилось")
        );
    }

    @Test
    @DisplayName("При создании Subtask, эпик которой не существует выбрасывается NotFoundException")
    void createSubtask_() {
        //given
        Epic epic = new Epic("", "");
        taskManager.createEpic(epic);
        Subtask subtask = new Subtask(TaskStatus.NEW, "", "", Integer.MAX_VALUE);

        //when
        //then
        assertThrows(NotFoundException.class, () -> taskManager.createSubtask(subtask), "Исключение не выброшено");
    }

    @Test
    @DisplayName("Task добавляется в менеджер и может быть найден по id")
    void createTask_AddedAndFoundById() {
        //given
        Task task = new Task(TaskStatus.NEW, "name", "descr");

        //when
        taskManager.createTask(task);

        //then
        Task savedTask = taskManager.getTask(0);
        assertAll(
                () -> assertNotNull(savedTask, "Задача не найдена."),
                () -> assertEquals(task, savedTask, "Задачи не совпадают.")
        );
    }

    @Test
    @DisplayName("Subtask добавляется в менеджер и может быть найден по id")
    void createSubtask_AddedAndFoundById() {
        //given
        Epic epic = new Epic("name", "descr");
        taskManager.createEpic(epic);
        Subtask subtask = new Subtask(TaskStatus.DONE, "name", "descr", 0, LocalDateTime.now(), Duration.ofHours(1));

        //when
        taskManager.createSubtask(subtask);

        //then
        Subtask savedSubtask = taskManager.getSubtask(1);
        assertAll(
                () -> assertNotNull(savedSubtask, "Задача не найдена."),
                () -> assertEquals(subtask, savedSubtask, "Задачи не совпадают.")
        );
    }

    @Test
    @DisplayName("Epic добавляется в менеджер и может быть найден по id")
    void createEpic_AddedAndFoundById() {
        //given
        Epic epic = new Epic("name", "descr");

        //when
        taskManager.createEpic(epic);

        //then
        Epic savedEpic = taskManager.getEpic(0);
        assertAll(
                () -> assertNotNull(savedEpic, "Задача не найдена."),
                () -> assertEquals(epic, savedEpic, "Задачи не совпадают.")
        );
    }

    @Test
    @DisplayName("При создании Task со сроком исполнения пересекающимся с началом срока исполнения существующей задачи выбрасывается CollisionException")
    void createTask_ThrownCollisionException_TimeCollisionWithExistingStartTime() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.parse("2024-06-19T07:00:00"), Duration.ofHours(24));
        Task task2 = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.parse("2024-06-19T06:00:00"), Duration.ofHours(2));

        //when
        taskManager.createTask(task1);

        //then
        assertThrows(CollisionException.class, () -> taskManager.createTask(task2), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При создании Subtask со сроком исполнения пересекающимся с началом срока исполнения существующей задачи выбрасывается CollisionException")
    void createSubtask_ThrownCollisionException_TimeCollisionWithExistingStartTime() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        taskManager.createEpic(epic);
        Subtask task1 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.parse("2024-06-19T07:00:00"), Duration.ofHours(24));
        Subtask task2 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.parse("2024-06-19T06:00:00"), Duration.ofHours(2));

        //when
        taskManager.createSubtask(task1);

        //then
        assertThrows(CollisionException.class, () -> taskManager.createSubtask(task2), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При создании Task со сроком исполнения с полным совпадением со сроком исполнения существующей задачи выбрасывается CollisionException")
    void createTask_ThrownCollisionException_ExactDurationMatch() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.parse("2024-05-19T10:00:00"), Duration.ofHours(24));
        Task task2 = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.parse("2024-05-19T10:00:00"), Duration.ofHours(24));

        //when
        taskManager.createTask(task1);

        //then
        assertThrows(CollisionException.class, () -> taskManager.createTask(task2), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При создании Subtask со сроком исполнения с полным совпадением со сроком исполнения существующей задачи выбрасывается CollisionException")
    void createSubtask_ThrownCollisionException_ExactDurationMatch() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        taskManager.createEpic(epic);
        Subtask task1 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.parse("2024-05-19T10:00:00"), Duration.ofHours(24));
        Subtask task2 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.parse("2024-05-19T10:00:00"), Duration.ofHours(24));

        //when
        taskManager.createSubtask(task1);

        //then
        assertThrows(CollisionException.class, () -> taskManager.createSubtask(task2), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При создании Task со сроком исполнения, входящем в интервал существующей задачи с несовпадением по границам выбрасывается CollisionException")
    void createTask_ThrownCollisionException_DurationWithinIntervalWithMismatchedBoundaries() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.parse("2024-04-19T05:00:00"), Duration.ofHours(24));
        Task task2 = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.parse("2024-04-19T07:00:00"), Duration.ofHours(5));

        //when
        taskManager.createTask(task1);

        //then
        assertThrows(CollisionException.class, () -> taskManager.createTask(task2), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При создании Subtask со сроком исполнения, входящем в интервал существующей задачи с несовпадением по границам выбрасывается CollisionException")
    void createSubtask_ThrownCollisionException_DurationWithinIntervalWithMismatchedBoundaries() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        taskManager.createEpic(epic);
        Subtask task1 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.parse("2024-04-19T05:00:00"), Duration.ofHours(24));
        Subtask task2 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.parse("2024-04-19T07:00:00"), Duration.ofHours(5));

        //when
        taskManager.createSubtask(task1);

        //then
        assertThrows(CollisionException.class, () -> taskManager.createSubtask(task2), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При создании Task со сроком исполнения пересекающимся с концом срока исполнения существующей задачи выбрасывается CollisionException")
    void createTask_ThrownCollisionException_TimeCollisionWithExistingEndTime() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.parse("2024-03-19T05:00:00"), Duration.ofHours(2));
        Task task2 = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.parse("2024-03-19T06:00:00"), Duration.ofHours(2));

        //when
        taskManager.createTask(task1);

        //then
        assertThrows(CollisionException.class, () -> taskManager.createTask(task2), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При создании Subtask со сроком исполнения пересекающимся с концом срока исполнения существующей задачи выбрасывается CollisionException")
    void createSubtask_ThrownCollisionException_TimeCollisionWithExistingEndTime() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        taskManager.createEpic(epic);
        Subtask task1 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.parse("2024-03-19T05:00:00"), Duration.ofHours(2));
        Subtask task2 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.parse("2024-03-19T06:00:00"), Duration.ofHours(2));

        //when
        taskManager.createSubtask(task1);

        //then
        assertThrows(CollisionException.class, () -> taskManager.createSubtask(task2), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При обновлении времени начала Task и пересечении нового срока с существующими задачами выбрасывается CollisionException")
    void updateTask_ThrownCollisionException_TimeCollisionWithExistingTasks() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.now(), Duration.ofHours(1));
        Task task2 = new Task(TaskStatus.NEW, "name", "descr", task1.getEndTime(), Duration.ofHours(1));

        taskManager.createTask(task1);
        taskManager.createTask(task2);

        Task newTask2 = new Task(1, TaskStatus.DONE, "new", "new", task1.getStartTime(), Duration.ofHours(1));
        Task controlTask = taskManager.getTask(1);

        //when
        //then
        assertAll(
                () -> assertThrows(CollisionException.class, () -> taskManager.updateTask(newTask2), "Исключение не выброшено"),
                () -> assertAll("Task обновился",
                        () -> assertEquals(task2.getName(), controlTask.getName()),
                        () -> assertEquals(task2.getDescription(), controlTask.getDescription()),
                        () -> assertEquals(task2.getStatus(), controlTask.getStatus()),
                        () -> assertEquals(task2.getStartTime(), controlTask.getStartTime()),
                        () -> assertEquals(task2.getDuration(), controlTask.getDuration())
                )
        );
    }

    @Test
    @DisplayName("При обновлении длительности Task и пересечении нового срока с существующими задачами выбрасывается CollisionException")
    void updateTask_ThrownCollisionException_TimeCollisionWithExistingTasksWithDurationChange() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.now(), Duration.ofHours(1));
        Task task2 = new Task(TaskStatus.NEW, "name", "descr", task1.getEndTime(), Duration.ofHours(1));

        taskManager.createTask(task1);
        taskManager.createTask(task2);

        Task newTask1 = new Task(0, TaskStatus.DONE, "new", "new", task1.getStartTime(), task1.getDuration().plusHours(1));
        Task controlTask = taskManager.getTask(0);

        //when
        //then
        assertAll(
                () -> assertThrows(CollisionException.class, () -> taskManager.updateTask(newTask1), "Исключение не выброшено"),
                () -> assertAll("Task обновился",
                        () -> assertEquals(task1.getName(), controlTask.getName()),
                        () -> assertEquals(task1.getDescription(), controlTask.getDescription()),
                        () -> assertEquals(task1.getStatus(), controlTask.getStatus()),
                        () -> assertEquals(task1.getStartTime(), controlTask.getStartTime()),
                        () -> assertEquals(task1.getDuration(), controlTask.getDuration())
                )
        );
    }

    @Test
    @DisplayName("При обновлении времени начала исполнения Subtask и пересечении нового срока с существующими задачами выбрасывается CollisionException")
    void updateSubtask_ThrownCollisionException_TimeCollisionWithExistingTasks() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        taskManager.createEpic(epic);
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.now(), Duration.ofHours(1));
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "name", "descr", 0, subtask1.getEndTime(), Duration.ofHours(1));

        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        Subtask newSubtask2 = new Subtask(2, TaskStatus.DONE, "new", "new", 0, subtask1.getStartTime(), Duration.ofHours(1));
        Subtask controlSubtask = taskManager.getSubtask(2);

        //when
        //then
        assertAll(
                () -> assertThrows(CollisionException.class, () -> taskManager.updateSubtask(newSubtask2), "Исключение не выброшено"),
                () -> assertAll("Task обновился",
                        () -> assertEquals(subtask2.getName(), controlSubtask.getName()),
                        () -> assertEquals(subtask2.getDescription(), controlSubtask.getDescription()),
                        () -> assertEquals(subtask2.getStatus(), controlSubtask.getStatus()),
                        () -> assertEquals(subtask2.getStartTime(), controlSubtask.getStartTime()),
                        () -> assertEquals(subtask2.getDuration(), controlSubtask.getDuration()),
                        () -> assertEquals(subtask2.getEpicId(), controlSubtask.getEpicId())
                )
        );
    }

    @Test
    @DisplayName("При обновлении длительности Subtask и пересечении нового срока с существующими задачами выбрасывается CollisionException")
    void updateSubtask_ThrownCollisionException_TimeCollisionWithExistingTasksWithDurationChange() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        taskManager.createEpic(epic);
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.now(), Duration.ofHours(1));
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "name", "descr", 0, subtask1.getEndTime(), Duration.ofHours(1));

        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        Subtask newSubtask1 = new Subtask(1, TaskStatus.DONE, "new", "new", 0, subtask1.getStartTime(), subtask1.getDuration().plusHours(1));
        Subtask controlSubtask = taskManager.getSubtask(1);

        //when
        //then
        assertAll(
                () -> assertThrows(CollisionException.class, () -> taskManager.updateSubtask(newSubtask1), "Исключение не выброшено"),
                () -> assertAll("Task обновился",
                        () -> assertEquals(subtask1.getName(), controlSubtask.getName()),
                        () -> assertEquals(subtask1.getDescription(), controlSubtask.getDescription()),
                        () -> assertEquals(subtask1.getStatus(), controlSubtask.getStatus()),
                        () -> assertEquals(subtask1.getStartTime(), controlSubtask.getStartTime()),
                        () -> assertEquals(subtask1.getDuration(), controlSubtask.getDuration()),
                        () -> assertEquals(subtask1.getEpicId(), controlSubtask.getEpicId())
                )
        );
    }

    @Test
    @DisplayName("При обновлении любых полей Task, кроме времени начала и продолжительности не будет пересечения сроков исполнения задачи с собственными")
    void updateTask_NotThrownCollisionException_StartTimeOrDurationHasNotBeenChanged() {
        //given
        Task task = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.now(), Duration.ofHours(1));
        taskManager.createTask(task);
        Task updateTask = new Task(0, TaskStatus.DONE, "new", "new", task.getStartTime(), task.getDuration());

        //when
        //then
        assertDoesNotThrow(() -> taskManager.updateTask(updateTask), "Исключение выброшено");
    }

    @Test
    @DisplayName("При обновлении любых полей Subtask, кроме времени начала и продолжительности не будет пересечения сроков исполнения задачи с собственными")
    void updateSubtask_NotThrownCollisionException_StartTimeOrDurationHasNotBeenChanged() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        taskManager.createEpic(epic);
        Subtask task = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.now(), Duration.ofHours(1));
        taskManager.createSubtask(task);
        Subtask updateSubtask = new Subtask(1, TaskStatus.DONE, "new", "new", 0, task.getStartTime(), task.getDuration());

        //when
        //then
        assertDoesNotThrow(() -> taskManager.updateSubtask(updateSubtask), "Исключение выброшено");
    }

    @Test
    @DisplayName("Обновление времени начала Task изменяет его положение с списке по приоритетам")
    void updateTask_AffectsPriorityListPosition_StartTimeChange() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.now(), Duration.ofDays(1));
        Task task2 = new Task(TaskStatus.NEW, "name", "descr", task1.getEndTime(), Duration.ofDays(1));
        Task task3 = new Task(TaskStatus.NEW, "name", "descr", task2.getEndTime(), Duration.ofDays(1));

        taskManager.createTask(task1);
        taskManager.createTask(task2);
        taskManager.createTask(task3);

        List<Task> oldPrioritizedTasks = List.of(task1, task2, task3);
        Task newTask2 = new Task(1, TaskStatus.NEW, "", "", task3.getEndTime().plusDays(10), task3.getDuration());

        //when
        taskManager.updateTask(newTask2);

        //then
        List<Task> prioritizedTasks = taskManager.getPrioritizedTasks();
        assertAll(
                () -> assertEquals(oldPrioritizedTasks.size(), prioritizedTasks.size(), "Кол-во элементов в списке по приоритетам изменилось"),
                () -> assertAll("Положение в списке приоритетов не изменилось",
                        () -> assertEquals(oldPrioritizedTasks.get(0), prioritizedTasks.get(0)),
                        () -> assertEquals(oldPrioritizedTasks.get(1), prioritizedTasks.get(2)),
                        () -> assertEquals(oldPrioritizedTasks.get(2), prioritizedTasks.get(1))
                )
        );
    }

    @Test
    @DisplayName("Обновление времени начала Subtask изменяет его положение с списке по приоритетам")
    void updateSubtask_AffectsPriorityListPosition_StartTimeChange() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        taskManager.createEpic(epic);

        Subtask task1 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.now(), Duration.ofDays(1));
        Subtask task2 = new Subtask(TaskStatus.NEW, "name", "descr", 0, task1.getEndTime(), Duration.ofDays(1));
        Subtask task3 = new Subtask(TaskStatus.NEW, "name", "descr", 0, task2.getEndTime(), Duration.ofDays(1));

        taskManager.createSubtask(task1);
        taskManager.createSubtask(task2);
        taskManager.createSubtask(task3);

        List<Task> oldPrioritizedTasks = List.of(task1, task2, task3);
        Subtask newSubtask2 = new Subtask(2, TaskStatus.NEW, "", "", 0, task3.getEndTime().plusDays(10), task3.getDuration());

        //when
        taskManager.updateSubtask(newSubtask2);

        //then
        List<Task> prioritizedTasks = taskManager.getPrioritizedTasks();
        assertAll(
                () -> assertEquals(oldPrioritizedTasks.size(), prioritizedTasks.size(), "Кол-во элементов в списке по приоритетам изменилось"),
                () -> assertAll("Положение в списке приоритетов не изменилось",
                        () -> assertEquals(oldPrioritizedTasks.get(0), prioritizedTasks.get(0)),
                        () -> assertEquals(oldPrioritizedTasks.get(1), prioritizedTasks.get(2)),
                        () -> assertEquals(oldPrioritizedTasks.get(2), prioritizedTasks.get(1))
                )
        );
    }

    @Test
    @DisplayName("Можно обновить существующий Task")
    void updateTask_StateUpdated() {
        //given
        Task oldTask = new Task(TaskStatus.NEW, "", "", LocalDateTime.now(), Duration.ofDays(1));
        Task updatedTask = new Task(TaskStatus.DONE, "new", "new");
        updatedTask.setId(0);
        taskManager.createTask(oldTask);

        //when
        taskManager.updateTask(updatedTask);

        //then
        Task controlTask = taskManager.getTask(0);
        assertAll(
                () -> assertEquals(controlTask.getStatus(), updatedTask.getStatus(), "Статус задачи не обновился."),
                () -> assertEquals(controlTask.getName(), updatedTask.getName(), "Имя задачи не обновилось."),
                () -> assertEquals(controlTask.getDescription(), updatedTask.getDescription(), "Описание задачи не обновилось."),
                () -> assertEquals(controlTask.getStartTime(), updatedTask.getStartTime(), "Время начала задачи не обновилось."),
                () -> assertEquals(controlTask.getDuration(), updatedTask.getDuration(), "Длительность задачи не обновилась.")
        );
    }

    @Test
    @DisplayName("Можно обновить существующий Subtask")
    void updateSubtask_StateUpdated() {
        //given
        Epic epic1 = new Epic("epic_name", "epic_descr");
        taskManager.createEpic(epic1);

        Epic epic2 = new Epic("epic_name", "epic_descr");
        taskManager.createEpic(epic2);

        Subtask oldSubtask = new Subtask(TaskStatus.NEW, "", "", 0, LocalDateTime.now(), Duration.ofDays(1));
        Subtask updatedSubtask = new Subtask(TaskStatus.DONE, "new", "new", 1);
        updatedSubtask.setId(2);

        taskManager.createSubtask(oldSubtask);

        //when
        taskManager.updateSubtask(updatedSubtask);

        //then
        Subtask controlSubtask = taskManager.getSubtask(2);
        assertAll(
                () -> assertEquals(controlSubtask.getStatus(), updatedSubtask.getStatus(), "Статус подзадачи не обновился."),
                () -> assertEquals(controlSubtask.getName(), updatedSubtask.getName(), "Имя подзадачи не обновилось."),
                () -> assertEquals(controlSubtask.getDescription(), updatedSubtask.getDescription(), "Описание подзадачи не обновилось."),
                () -> assertEquals(controlSubtask.getStartTime(), updatedSubtask.getStartTime(), "Время начала подзадачи не обновилось."),
                () -> assertEquals(controlSubtask.getDuration(), updatedSubtask.getDuration(), "Длительность подзадачи не обновилась."),
                () -> assertEquals(controlSubtask.getEpicId(), updatedSubtask.getEpicId(), "ID эпика подзадачи не обновился."),
                () -> assertTrue(epic2.getSubtasksIds().contains(controlSubtask.getId()), "Подзадача не перемещена в новый эпик."),
                () -> assertFalse(epic1.getSubtasksIds().contains(controlSubtask.getId()), "Подзадача не удалена из старого эпика при изменении эпика.")
        );
    }

    @Test
    @DisplayName("Можно обновить только имя и описание существующего Epic")
    void updateEpic_OnlyNameAndDescriptionUpdated() {
        //given
        Epic oldEpic = new Epic("", "");
        taskManager.createEpic(oldEpic);
        Epic updatedEpic = new Epic(0, TaskStatus.DONE, "new", "new", LocalDateTime.now(), Duration.ofDays(1));

        //when
        taskManager.updateEpic(updatedEpic);

        //then
        Epic controlEpic = taskManager.getEpic(0);
        assertAll(
                () -> assertEquals(controlEpic.getName(), updatedEpic.getName(), "Имя эпика не обновилось."),
                () -> assertEquals(controlEpic.getDescription(), updatedEpic.getDescription(), "Описание эпика не обновилось."),
                () -> assertNotEquals(controlEpic.getStatus(), updatedEpic.getStatus(), "Статус эпика обновился."),
                () -> assertNotEquals(controlEpic.getStartTime(), updatedEpic.getStartTime(), "Время начала эпика обновилось."),
                () -> assertNotEquals(controlEpic.getDuration(), updatedEpic.getDuration(), "Длительность эпика обновилась.")
        );
    }

    @Test
    @DisplayName("При попытке обновления несуществующего Task выбрасывается NotFoundException")
    void updateTask_() {
        //given
        Task task = new Task(TaskStatus.NEW, "", "");
        task.setId(0);

        //when
        //then
        assertThrows(NotFoundException.class, () -> taskManager.updateTask(task), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При попытке обновления несуществующего Subtask выбрасывается NotFoundException")
    void updateSubtask_() {
        //given
        Epic epic = new Epic("", "");
        taskManager.createEpic(epic);
        Subtask subtask = new Subtask(TaskStatus.NEW, "", "", 0);
        subtask.setId(1);

        //when
        //then
        assertThrows(NotFoundException.class, () -> taskManager.updateSubtask(subtask), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При попытке обновления несуществующего Epic выбрасывается NotFoundException")
    void updateEpic_() {
        //given
        Epic epic = new Epic("", "");
        epic.setId(0);

        //when
        //then
        assertThrows(NotFoundException.class, () -> taskManager.updateEpic(epic), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При передаче null в аргумент метода обновления Task будет выброшено IllegalArgumentException")
    void updateTask_1() {
        assertThrows(IllegalArgumentException.class, () -> taskManager.updateTask(null), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При передаче null в аргумент метода обновления Subtask будет выброшено IllegalArgumentException")
    void updateSubtask_1() {
        assertThrows(IllegalArgumentException.class, () -> taskManager.updateSubtask(null), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При передаче null в аргумент метода обновления Epic будет выброшено IllegalArgumentException")
    void updateEpic_1() {
        assertThrows(IllegalArgumentException.class, () -> taskManager.updateEpic(null), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При обновлении эпика, которому принадлежит Subtask на не существующий выбрасывается NotFoundException")
    void updateSubtask_2() {
        //given
        Epic epic = new Epic("", "");
        Subtask subtask = new Subtask(TaskStatus.NEW, "", "", 0);

        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask);

        Subtask newSubtask = new Subtask(TaskStatus.NEW, "", "", Integer.MAX_VALUE);
        newSubtask.setId(1);

        //when
        //then
        assertThrows(NotFoundException.class, () -> taskManager.updateSubtask(newSubtask), "Исключение не выброшено");
    }

    @Test
    @DisplayName("При обновлении подзадачи обновляется состояние её эпика")
    void updateSubtask_EpicStateUpdated_SubtaskUpdate() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        taskManager.createEpic(epic);

        Subtask subtask1 = new Subtask(TaskStatus.IN_PROGRESS, "name", "descr", epic.getId());
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "name", "descr", epic.getId());

        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        Subtask updatedSubtask1 = new Subtask(1, TaskStatus.DONE, "name", "descr", epic.getId(), LocalDateTime.now(), Duration.ofHours(1));
        Subtask updatedSubtask2 = new Subtask(2, TaskStatus.DONE, "name", "descr", epic.getId(), LocalDateTime.now().plusMonths(1), Duration.ofMinutes(12867));

        TaskStatus newEpicTaskStatus = TaskStatus.DONE;
        LocalDateTime newEpicStartTime = updatedSubtask1.getStartTime();
        LocalDateTime newEpicEndTime = updatedSubtask2.getEndTime();
        Duration newEpicDuration = updatedSubtask1.getDuration().plus(updatedSubtask2.getDuration());

        //when
        taskManager.updateSubtask(updatedSubtask1);
        taskManager.updateSubtask(updatedSubtask2);

        //then
        assertAll(
                () -> assertEquals(epic.getStatus(), newEpicTaskStatus, "Статус эпика не обновился"),
                () -> assertEquals(epic.getStartTime(), newEpicStartTime, "Время начала эпика не обновилось"),
                () -> assertEquals(epic.getEndTime(), newEpicEndTime, "Время окончания эпика не обновилось"),
                () -> assertEquals(epic.getDuration(), newEpicDuration, "Продолжительность эпика не обновилось")
        );
    }

    @Test
    @DisplayName("Задачу можно удалить")
    void removeTask_TaskRemovedFromManager() {
        //given
        Task task = new Task(TaskStatus.NEW, "", "", LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createTask(task);

        //when
        taskManager.removeTask(0);

        //then
        assertThrows(NotFoundException.class,
                () -> taskManager.getTask(0),
                "Задача не удалена из менеджера");
    }

    @Test
    @DisplayName("При удалении Subtask подзадача так же удаляется из своего эпика")
    void removeSubtask_RemovesFromManagerAndEpic() {
        //given
        Epic epic = new Epic("", "");
        Subtask subtask = new Subtask(TaskStatus.NEW, "", "", 0, LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask);

        //when
        taskManager.removeSubtask(1);

        //then
        assertAll(
                () -> assertThrows(NotFoundException.class,
                        () -> taskManager.getSubtask(1),
                        "Подзадача не удалена из менеджера"),
                () -> assertTrue(taskManager.getEpic(0).getSubtasksIds().isEmpty(), "Подзадача не удалена из эпика")
        );
    }

    @Test
    @DisplayName("При удалении Epic удаляется эпик и все его подзадачи")
    void removeEpic_RemovesEpicAndAllItsSubtasks() {
        //given
        Epic epic = new Epic("", "");
        Subtask subtask = new Subtask(TaskStatus.NEW, "", "", 0, LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask);

        //when
        taskManager.removeEpic(epic.getId());

        //then
        assertAll(
                () -> assertThrows(NotFoundException.class,
                        () -> taskManager.getEpic(0),
                        "Подзадача не удалена из менеджера"),
                () -> assertEquals(0, taskManager.getAllSubTasks().size(), "Не удаляются сабтаски")
        );

    }

    @Test
    @DisplayName("При удалении Epic удаляется эпик и все его подзадачи из истории")
    void removeEpic_RemovesEpicAndAllItsSubtasksFromHistory() {
        //given
        Epic epic = new Epic("", "");
        Subtask subtask = new Subtask(TaskStatus.NEW, "", "", 0, LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask);

        //when
        taskManager.getEpic(0);
        taskManager.getSubtask(1);
        taskManager.removeEpic(epic.getId());

        //then
        assertEquals(0, taskManager.getHistory().size(), "Не удаляются эпик и сабтаски из истории");
    }

    @Test
    @DisplayName("При удалении Subtask обновляется состояние её эпика")
    void removeSubtask_UpdatesEpicState() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        Subtask subtask1 = new Subtask(TaskStatus.DONE, "name", "descr", 0, LocalDateTime.now(), Duration.ofHours(1));
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.now().plusDays(1), Duration.ofDays(2));

        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        TaskStatus newEpicTaskStatus = TaskStatus.DONE;
        LocalDateTime newEpicStartTime = subtask1.getStartTime();
        LocalDateTime newEpicEndTime = subtask1.getEndTime();
        Duration newEpicDuration = subtask1.getDuration();

        //when
        taskManager.removeSubtask(subtask2.getId());

        //then
        assertAll(
                () -> assertEquals(epic.getStatus(), newEpicTaskStatus, "Статус эпика не обновился"),
                () -> assertEquals(epic.getStartTime(), newEpicStartTime, "Время начала эпика не обновилось"),
                () -> assertEquals(epic.getEndTime(), newEpicEndTime, "Время окончания эпика не обновилось"),
                () -> assertEquals(epic.getDuration(), newEpicDuration, "Продолжительность эпика не обновилось")
        );
    }

    @Test
    @DisplayName("При удалении Task она так же удаляется из списка по приоритетам")
    void removeTask_RemovesTaskFromManagerAndPriorityList() {
        //given
        Task task = new Task(TaskStatus.NEW, "", "", LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createTask(task);

        //when
        taskManager.removeTask(0);

        //then
        assertTrue(taskManager.getPrioritizedTasks().isEmpty(), "Удаление Task из менеджера не привело к удалению Task из списка по приоритетам");
    }

    @Test
    @DisplayName("При удалении Task она так же удаляется из истории")
    void removeTask_RemovesTaskFromManagerAndHistory() {
        //given
        Task task = new Task(TaskStatus.NEW, "", "", LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createTask(task);

        //when
        taskManager.getTask(0);
        taskManager.removeTask(0);

        //then
        assertTrue(taskManager.getHistory().isEmpty(), "Удаление Task из менеджера не привело к удалению Task из списка по приоритетам");
    }

    @Test
    @DisplayName("При удалении Subtask она так же удаляется из списка по приоритетам")
    void removeSubtask_AlsoRemovingFromPriorityList() {
        //given
        Epic epic = new Epic("", "");
        Subtask subtask = new Subtask(TaskStatus.NEW, "", "", 0, LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask);

        //when
        taskManager.removeSubtask(1);

        //then
        assertTrue(taskManager.getPrioritizedTasks().isEmpty(), "Удаление Subtask из менеджера не привело к удалению Subtask из списка по приоритетам");
    }

    @Test
    @DisplayName("При удалении Subtask она так же удаляется из истории")
    void removeSubtask_AlsoRemovingFromHistory() {
        //given
        Epic epic = new Epic("", "");
        Subtask subtask = new Subtask(TaskStatus.NEW, "", "", 0, LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask);

        //when
        taskManager.getEpic(0);
        taskManager.getSubtask(1);
        taskManager.removeSubtask(1);

        //then
        assertEquals(1, taskManager.getHistory().size(), "Удаление Subtask из менеджера не привело к удалению её эпика из истории");
    }

    @Test
    @DisplayName("При удалении всех Subtask очищаются и подзадачи всех эпиков")
    void removeAllSubTasks_RemovesAllSubtasksAndClearsAllEpicsSubtasks() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        Subtask subtask1 = new Subtask(TaskStatus.DONE, "name", "descr", 0, LocalDateTime.now(), Duration.ofHours(1));
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.now().plusDays(1), Duration.ofDays(2));

        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        //when
        taskManager.removeAllSubtasks();

        //then
        assertAll(
                () -> assertTrue(taskManager.getEpic(0).getSubtasksIds().isEmpty(), "Подзадачи эпиков не очищаются"),
                () -> assertTrue(taskManager.getAllSubTasks().isEmpty(), "Удаляются не все подзадачи")
        );
    }

    @Test
    @DisplayName("При удалении всех Epic удаляются и все подзадачи")
    void removeAllEpics_RemovesAllEpicsAndSubtasks() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        Subtask subtask1 = new Subtask(TaskStatus.DONE, "name", "descr", 0, LocalDateTime.now(), Duration.ofHours(1));
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.now().plusDays(1), Duration.ofDays(2));

        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        //when
        taskManager.removeAllEpics();

        //then
        assertAll(
                () -> assertTrue(taskManager.getAllSubTasks().isEmpty(), "Удаляются не все подзадачи"),
                () -> assertTrue(taskManager.getAllEpics().isEmpty(), "Удаляются не все эпики")
        );
    }

    @Test
    @DisplayName("При удалении всех Subtask состояние всех эпиков cбрасывается на дефолтное")
    void removeAllSubtasks_ResetsAllEpicsToDefaultState() {
        //given
        Epic epic = new Epic("epic_name", "epic_descr");
        Subtask subtask1 = new Subtask(TaskStatus.DONE, "name", "descr", 0, LocalDateTime.now(), Duration.ofHours(1));
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "name", "descr", 0, LocalDateTime.now().plusDays(1), Duration.ofDays(2));

        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        TaskStatus newEpicTaskStatus = TaskStatus.NEW;
        LocalDateTime newEpicStartTime = InMemoryTaskManager.NULL_START_TIME_INDICATOR;
        LocalDateTime newEpicEndTime = InMemoryTaskManager.NULL_END_TIME_INDICATOR;
        Duration newEpicDuration = InMemoryTaskManager.NULL_DURATION_INDICATOR;

        //when
        taskManager.removeAllSubtasks();

        //then
        Epic controlEpic = taskManager.getEpic(0);
        assertAll(
                () -> assertEquals(controlEpic.getStatus(), newEpicTaskStatus, "Статус эпика не обновился"),
                () -> assertEquals(controlEpic.getStartTime(), newEpicStartTime, "Время начала эпика не обновилось"),
                () -> assertEquals(controlEpic.getEndTime(), newEpicEndTime, "Время окончания эпика не обновилось"),
                () -> assertEquals(controlEpic.getDuration(), newEpicDuration, "Продолжительность эпика не обновилось")
        );
    }

    @Test
    @DisplayName("При удалении всех Task они так же удаляется из списка по приоритетам")
    void removeAllTasks_RemovesTasksFromPriorityList() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "", "");
        Task task2 = new Task(TaskStatus.NEW, "", "");
        taskManager.createTask(task1);
        taskManager.createTask(task2);

        //when
        taskManager.removeAllTasks();

        //then
        assertTrue(taskManager.getPrioritizedTasks().isEmpty(), "Удалены не все Task");
    }

    @Test
    @DisplayName("При удалении всех Task они так же удаляется из истории")
    void removeAllTasks_RemovesTasksFromHistory() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "", "");
        Task task2 = new Task(TaskStatus.NEW, "", "");
        taskManager.createTask(task1);
        taskManager.createTask(task2);

        //when
        taskManager.getTask(0);
        taskManager.getTask(1);
        taskManager.removeAllTasks();

        //then
        assertTrue(taskManager.getHistory().isEmpty(), "Удалены не все Task");
    }

    @Test
    @DisplayName("При удалении всех Subtask они так же удаляется из списка по приоритетам")
    void removeAllSubtasks_RemovesSubtasksFromPriorityList() {
        //given
        Epic epic = new Epic("", "");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "", "", 0);
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "", "", 0);
        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        //when
        taskManager.removeAllSubtasks();

        //then
        assertTrue(taskManager.getPrioritizedTasks().isEmpty(), "Удалены не все Subtask");
    }

    @Test
    @DisplayName("При удалении всех Subtask они так же удаляется из истории")
    void removeAllSubtasks_RemovesSubtasksFromHistory() {
        //given
        Epic epic = new Epic("", "");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "", "", 0);
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "", "", 0);
        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);
        taskManager.getSubtask(1);
        taskManager.getSubtask(2);

        //when
        taskManager.removeAllSubtasks();

        //then
        assertTrue(taskManager.getHistory().isEmpty(), "Удалены не все Subtask");
    }

    @Test
    @DisplayName("При удалении всех Epic их подзадачи в том числе удаляются из списка по приоритетам")
    void removeAllEpics_RemovesEpicsAndSubtasksFromPriorityList() {
        //given
        Epic epic1 = new Epic("", "");
        Epic epic2 = new Epic("", "");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "", "", 0);
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "", "", 1);
        taskManager.createEpic(epic1);
        taskManager.createEpic(epic2);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        //when
        taskManager.removeAllEpics();

        //then
        assertTrue(taskManager.getPrioritizedTasks().isEmpty(), "Удалены не все Epic и их подзадачи");
    }

    @Test
    @DisplayName("При удалении всех Epic их подзадачи в том числе удаляются из истории")
    void removeAllEpics_RemovesEpicsAndSubtasksFromHistory() {
        //given
        Epic epic1 = new Epic("", "");
        Epic epic2 = new Epic("", "");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "", "", 0);
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "", "", 1);
        taskManager.createEpic(epic1);
        taskManager.createEpic(epic2);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);
        taskManager.getEpic(0);
        taskManager.getEpic(1);
        taskManager.getSubtask(2);
        taskManager.getSubtask(3);

        //when
        taskManager.removeAllEpics();

        //then
        assertTrue(taskManager.getHistory().isEmpty(), "Удалены не все Epic и их подзадачи");
    }

    @Test
    @DisplayName("Можно получить все Task")
    void getAllTasks_ReturnsAllTasks() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "", "");
        Task task2 = new Task(TaskStatus.NEW, "", "");
        Epic epic = new Epic("", "");

        taskManager.createTask(task1);
        taskManager.createTask(task2);
        taskManager.createEpic(epic);

        Task savedTask1 = taskManager.getTask(0);
        Task savedTask2 = taskManager.getTask(1);

        //when
        List<Task> allTasks = taskManager.getAllTasks();

        //then
        assertAll(
                () -> assertEquals(2, allTasks.size(), "Длина списка не верная"),
                () -> assertAll("Задачи в списке не совпадают с ранее добавленными",
                        () -> assertEquals(savedTask1, allTasks.get(0)),
                        () -> assertEquals(savedTask2, allTasks.get(1))
                )
        );
    }

    @Test
    @DisplayName("Можно получить все Subtask")
    void getAllSubtasks_ReturnsAllSubtasks() {
        //given
        Epic epic = new Epic("", "");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "", "", 0);
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "", "", 0);

        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        Task savedSubtask1 = taskManager.getSubtask(1);
        Task savedSubtask2 = taskManager.getSubtask(2);

        //when
        List<Subtask> allSubtasks = taskManager.getAllSubTasks();

        //then
        assertAll(
                () -> assertEquals(2, allSubtasks.size(), "Длина списка не верная"),
                () -> assertAll("Задачи в списке не совпадают с ранее добавленными",
                        () -> assertEquals(savedSubtask1, allSubtasks.get(0)),
                        () -> assertEquals(savedSubtask2, allSubtasks.get(1))
                )
        );
    }

    @Test
    @DisplayName("Можно получить все Epic")
    void getAllEpics_ReturnsAllEpics() {
        //given
        Epic epic1 = new Epic("", "");
        Epic epic2 = new Epic("", "");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "", "", 0);

        taskManager.createEpic(epic1);
        taskManager.createEpic(epic2);
        taskManager.createSubtask(subtask1);

        Task savedEpic1 = taskManager.getEpic(0);
        Task savedEpic2 = taskManager.getEpic(1);

        //when
        List<Epic> allEpics = taskManager.getAllEpics();

        //then
        assertAll(
                () -> assertEquals(2, allEpics.size(), "Длина списка не верная"),
                () -> assertAll("Задачи в списке не совпадают с ранее добавленными",
                        () -> assertEquals(savedEpic1, allEpics.get(0)),
                        () -> assertEquals(savedEpic2, allEpics.get(1))
                )
        );
    }

    @Test
    @DisplayName("История содержит только полученные задачи в порядке получения")
    void getHistory_HistoryContainsGetTasksInOrderOfGet() {
        //given
        Task task = new Task(TaskStatus.NEW, "", "");
        Epic epic = new Epic("", "");
        Subtask subtask = new Subtask(TaskStatus.NEW, "", "", 1);

        taskManager.createTask(task);
        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask);

        Task task1 = taskManager.getTask(0);
        Task task2 = taskManager.getEpic(1);
        Task task3 = taskManager.getSubtask(2);

        //when
        List<Task> history = taskManager.getHistory();

        //then
        assertAll(
                () -> assertEquals(3, history.size(), "Размер истории не совпадает с кол-вом просмотров"),
                () -> assertAll("Порядок задач в истории не соответствует порядку просмотра задач",
                        () -> assertEquals(history.get(0), task1),
                        () -> assertEquals(history.get(1), task2),
                        () -> assertEquals(history.get(2), task3)
                )
        );
    }


}
