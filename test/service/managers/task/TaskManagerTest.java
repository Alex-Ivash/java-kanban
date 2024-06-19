package service.managers.task;

import exception.CollisionException;
import exception.NotFoundException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaskManagerTest")
abstract class TaskManagerTest<T extends TaskManager> {
    protected T taskManager;
    static final int TASKS_COUNT = 3;
    static final int SUBTASKS_COUNT = 3;
    static final int EPICS_COUNT = 3;

    abstract void initTaskManager();

    @BeforeEach
    void setUp() {
        initTaskManager();

        IntStream.range(0, TASKS_COUNT).forEach(i -> taskManager.createTask(new Task(TaskStatus.NEW, "task" + i, "task_descr" + i)));
        IntStream.range(0, EPICS_COUNT).forEach(i -> {
            int id = taskManager.createEpic(new Epic("epic" + i, "epic_descr" + i)).getId();
            IntStream.range(0, SUBTASKS_COUNT).forEach(k -> taskManager.createSubtask(new Subtask(TaskStatus.NEW, "subtask" + i + k, "subtask_descr" + i + k, id)));
        });
    }

    static Stream<Arguments> tasksTypesProvider() {
        return Stream.of(
                Arguments.of(TaskType.TASK),
                Arguments.of(TaskType.SUBTASK),
                Arguments.of(TaskType.EPIC)
        );
    }

    static Stream<Arguments> startTimeAndTaskProviderForCollisionTest() {
        List<String[][]> intersectionCases = List.of(
                new String[][]{
                        {"2024-06-19T07:00:00", "PT24H"},
                        {"2024-06-19T06:00:00", "PT2H"}},
                new String[][]{
                        {"2024-05-19T10:00:00", "PT24H"},
                        {"2024-05-19T10:00:00", "PT24H"}},
                new String[][]{
                        {"2024-04-19T05:00:00", "PT24H"},
                        {"2024-04-19T07:00:00", "PT5H"}},
                new String[][]{
                        {"2024-03-19T05:00:00", "PT2H"},
                        {"2024-03-19T06:00:00", "PT2H"}}
        );

        List<String> descriptions = List.of(
                "пересечение с началом интервала существующей задачи",
                "полное совпадение с интервалом существующей задачи",
                "вхождение в интервал существующей задачи с несовпадением по границам",
                "пересечение с концом интервала существующей задачи"
        );

        return Stream.of(TaskType.TASK, TaskType.SUBTASK)
                .flatMap(taskType -> IntStream.range(0, intersectionCases.size())
                        .mapToObj(i -> Arguments.of(taskType, descriptions.get(i), intersectionCases.get(i)))
                );
    }

    static Stream<Arguments> taskProvider() {
        return Stream.of(
                Arguments.of("Task", new Task(TaskStatus.DONE, "Test addNewTask", "Test addNewTask description")),
                Arguments.of("Epic", new Epic("Test addNewTask", "Test addNewTask description")),
                Arguments.of("Subtask", new Subtask(TaskStatus.IN_PROGRESS, "Test addNewTask", "Test addNewTask description", TASKS_COUNT))
        );
    }

    static Stream<Arguments> taskProviderForPriory() {
        return Stream.of(
                Arguments.of("Task", new Task(TaskStatus.DONE, "Test addNewTask", "Test addNewTask description")),
                Arguments.of("Subtask", new Subtask(TaskStatus.IN_PROGRESS, "Test addNewTask", "Test addNewTask description", TASKS_COUNT))
        );
    }

    @ParameterizedTest(name = "Состояние добавленной задачи типа {0} не отличается от состояния добавляемой в менеджер")
    @MethodSource("taskProvider")
    @DisplayName("Состояние добавленной задачи любого типа не отличается от состояния добавляемой в менеджер")
    void create_stateBeforeAndAfterCreateTaskShouldBeSame(String label, Task tsk) {
        TaskStatus taskStatus = tsk.getStatus();
        String taskName = tsk.getName();
        String taskDescription = tsk.getDescription();
        LocalDateTime taskStartTime = tsk.getStartTime();
        LocalDateTime taskEndTime = tsk.getEndTime();
        Duration taskDuration = tsk.getDuration();

        Task addedTask = switch (tsk) {
            case Epic epic -> taskManager.createEpic(epic);
            case Subtask subtask -> taskManager.createSubtask(subtask);
            case Task task -> taskManager.createTask(task);
        };

        assertAll(
                () -> assertEquals(taskName, addedTask.getName(), "Имя добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(taskDescription, addedTask.getDescription(), "Описание добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(taskStartTime, addedTask.getStartTime(), "Время старта добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(taskEndTime, addedTask.getEndTime(), "Время окончания добавляемой и добавленной задачи изменился при добавлении"),
                () -> assertEquals(taskDuration, addedTask.getDuration(), "Длительность добавляемой и добавленной задачи изменился при добавлении"),
                () -> {
                    switch (addedTask) {
                        case Epic epic -> {
                        }

                        case Subtask subtask -> assertAll(
                                () -> assertEquals(taskStatus, subtask.getStatus(), "Статус добавляемой и добавленной задачи изменился при добавлении"),
                                () -> assertEquals(((Subtask) tsk).getEpicId(), subtask.getEpicId(), "ID эпика добавляемой и добавленной задачи изменился при добавлении")
                        );

                        case Task task ->
                                assertEquals(taskStatus, task.getStatus(), "Статус добавляемой и добавленной задачи изменился при добавлении");
                    }
                }
        );
    }

    @Test
    @DisplayName("Список по приоритетам сортируется на основе времени начала задачи")
    void getPrioritizedTasks_priorityListShouldBeSortedByTaskStartTime() {
        Task task = taskManager.createTask(new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.now().plusDays(1), Duration.ofHours(1)));
        Subtask subtask = taskManager.createSubtask(new Subtask(TaskStatus.NEW, "name", "descr", taskManager.getAllEpics().getFirst().getId(), LocalDateTime.now(), Duration.ofHours(1)));

        List<Task> prioritizedTasks = taskManager.getPrioritizedTasks();

        assertAll(
                () -> assertNotEquals(0, prioritizedTasks.size(), "Список по приоритетам не формируется вообще"),
                () -> assertEquals(prioritizedTasks.get(0), subtask, "Задачи не сортируются по времени начала от ранней к поздней"),
                () -> assertEquals(prioritizedTasks.get(1), task, "Задачи не сортируются по времени начала от ранней к поздней")
        );
    }

    @ParameterizedTest(name = "При создании {0} она добавляется в список по приоритетам при наличии врмени начала")
    @MethodSource("taskProviderForPriory")
    @DisplayName("При создании задач и подзадач они добавляются в список по приоритетам при наличии заданного времени начала")
    void create_shouldAddTasksAndSubtasksToListByPriorityWithSpecifiedStartTime(String label, Task task) {
        int initialPriorityTasksSize = taskManager.getPrioritizedTasks().size();

        switch (task) {
            case Epic epic -> {
            }
            case Subtask subtask -> {
                subtask.setStartTime(LocalDateTime.now());
                taskManager.createSubtask(subtask);
            }
            case Task tazk -> {
                tazk.setStartTime(LocalDateTime.now());
                taskManager.createTask(task);
            }
        }

        assertEquals(initialPriorityTasksSize + 1, taskManager.getPrioritizedTasks().size(), label + " не добавляется в список по приоритетам при заданном времени начала");
    }

    @ParameterizedTest(name = "При создании {0} она не добавляется в список по приоритетам если не задано время начала")
    @MethodSource("taskProviderForPriory")
    @DisplayName("При создании задач и подзадач они не добавляются в список по приоритетам при отсутствии заданного времени начала")
    void create_shouldNotAddTasksAndSubtasksToListByPriorityWithoutSpecifiedStartTime(String label, Task task) {
        int initialPriorityTasksSize = taskManager.getPrioritizedTasks().size();

        switch (task) {
            case Epic epic -> {
            }
            case Subtask subtask -> taskManager.createSubtask(subtask);
            case Task tazk -> taskManager.createTask(task);
        }

        assertEquals(initialPriorityTasksSize, taskManager.getPrioritizedTasks().size(), label + " не добавляется в список по приоритетам при заданном времени начала");
    }

    @Test
    @DisplayName("При создании эпика он не должен добавляться в список по приоритетам")
    void createEpic_epicShouldNotBeAddedToPriorityListUponCreation() {
        int initialPriorityTasksSize = taskManager.getPrioritizedTasks().size();

        taskManager.createEpic(new Epic("name", "descr"));
        assertEquals(initialPriorityTasksSize, taskManager.getPrioritizedTasks().size(), "При создании эпика он добавляется в список по приоритетам");
    }

    @ParameterizedTest(name = "Тип задачи: {0}, Кейс для интервала создаваемой задачи: {1}")
    @MethodSource("startTimeAndTaskProviderForCollisionTest")
    @DisplayName("При создании задач и подзадач c пересекающимся сроком исполнения с уже существующими задачами выбрасывается CollisionException")
    void create_shouldThrowCollisionExceptionWhenCreatingTasksAndSubtasksWithOverlappingDeadlines(TaskType taskType, String intersectionCaseLabel, String[][] testIntervals) {
        assertThrows(
                CollisionException.class,
                () -> {
                    switch (taskType) {
                        case EPIC -> {
                        }
                        case SUBTASK -> {
                            taskManager.createSubtask(
                                    new Subtask(TaskStatus.NEW,
                                            "", "",
                                            taskManager.getAllEpics().getFirst().getId(),
                                            LocalDateTime.parse(testIntervals[0][0]),
                                            Duration.parse(testIntervals[0][1]))
                            );
                            taskManager.createSubtask(
                                    new Subtask(TaskStatus.NEW,
                                            "", "",
                                            taskManager.getAllEpics().getFirst().getId(),
                                            LocalDateTime.parse(testIntervals[1][0]),
                                            Duration.parse(testIntervals[1][1])
                                    )
                            );
                        }
                        case TASK -> {
                            taskManager.createTask(
                                    new Task(TaskStatus.NEW,
                                            "", "",
                                            LocalDateTime.parse(testIntervals[0][0]),
                                            Duration.parse(testIntervals[0][1]))
                            );
                            taskManager.createTask(
                                    new Task(TaskStatus.NEW,
                                            "", "",
                                            LocalDateTime.parse(testIntervals[1][0]),
                                            Duration.parse(testIntervals[1][1])
                                    )
                            );
                        }
                    }
                }, "При условии: " + intersectionCaseLabel + "не выбрасывается исключение для " + taskType
        );
    }

    @ParameterizedTest(name = "Обновление не срока {0}")
    @MethodSource("taskProviderForPriory")
    @DisplayName("При обновлении любых полей, кроме времени начала и продолжительности не будет пересечения сроков исполнения задачи с собственными")
    void update_shouldNotCauseDeadlineOverlapWithSelfWhenUpdatingFieldsExceptStartTimeAndDuration(String label, Task task) {
        task.setStartTime(LocalDateTime.now());
        task.setDuration(Duration.ofDays(1));

        assertDoesNotThrow(
                () -> {
                    switch (task) {
                        case Epic epic -> {
                        }
                        case Subtask subtask -> {
                            taskManager.createSubtask(subtask);
                            taskManager.updateSubtask(new Subtask(subtask.getId(), TaskStatus.DONE, "new", "new", subtask.getEpicId(), subtask.getStartTime(), subtask.getDuration()));
                        }
                        case Task tazk -> {
                            taskManager.createTask(task);
                            taskManager.updateTask(new Task(tazk.getId(), TaskStatus.IN_PROGRESS, "new", "new", tazk.getStartTime(), tazk.getDuration()));
                        }
                    }
                }, "При обновлении любых полей задачи, кроме страта и продолжительности проваливается валидация на пересечение с самим собой для " + label
        );
    }

    @Test
    @DisplayName("При обновлении срока исполнения Task и пересечении нового срока с существующими задачами выбрасывается CollisionException")
    void update_shouldThrowCollisionExceptionWhenUpdatingTaskDeadlineAndNewDeadlineOverlapsWithExistingTasks() {
        Task task1 = taskManager.createTask(new Task(TaskStatus.NEW, "", "", LocalDateTime.now(), Duration.ofDays(1)));
        Task task2 = taskManager.createTask(new Task(TaskStatus.NEW, "", "", task1.getEndTime(), Duration.ofDays(1)));

        Task updatedTask = new Task(task1.getId(), TaskStatus.DONE, "new", "new", task1.getEndTime(), task1.getDuration());
        Task controlTask = taskManager.getTask(task1.getId());

        assertAll(
                () -> assertThrows(CollisionException.class, () -> taskManager.updateTask(updatedTask)),
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
    @DisplayName("При обновлении срока исполнения Subtask и пересечении нового срока с существующими задачами выбрасывается CollisionException")
    void update_shouldThrowCollisionExceptionWhenUpdatingSubtaskDeadlineAndNewDeadlineOverlapsWithExistingTasks() {
        Subtask subtask1 = taskManager.createSubtask(new Subtask(TaskStatus.NEW, "", "", taskManager.getAllEpics().getFirst().getId(), LocalDateTime.now(), Duration.ofDays(1)));
        Subtask subtask2 = taskManager.createSubtask(new Subtask(TaskStatus.NEW, "", "", taskManager.getAllEpics().getLast().getId(), subtask1.getEndTime(), Duration.ofDays(1)));

        Subtask updatedSubtask = new Subtask(subtask1.getId(), TaskStatus.DONE, "new", "new", subtask2.getEpicId(), subtask1.getEndTime(), subtask1.getDuration());
        Subtask controlSubtask = taskManager.getSubtask(subtask1.getId());

        assertAll(
                () -> assertThrows(CollisionException.class, () -> taskManager.updateSubtask(updatedSubtask)),
                () -> assertAll("Subtask обновился",
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
    @DisplayName("Обновив время начала и/или продолжительности Task при отсутствии коллизий она изменяет свое положение в списке приоритетов")
    void update_updatingTaskStartTimeOrDurationWithoutCollisionsShouldChangeItsPositionInPriorityList() {
        Task task1 = taskManager.createTask(new Task(TaskStatus.NEW, "name", "descr", LocalDateTime.now(), Duration.ofDays(1)));
        Task task2 = taskManager.createTask(new Task(TaskStatus.NEW, "name", "descr", task1.getEndTime(), Duration.ofDays(1)));
        Task task3 = taskManager.createTask(new Task(TaskStatus.NEW, "name", "descr", task2.getEndTime(), Duration.ofDays(1)));

        List<Task> oldPrioritizedTasks = List.of(task1, task2, task3);

        taskManager.updateTask(new Task(task2.getId(), TaskStatus.NEW, "", "", task3.getEndTime().plusDays(10), task3.getDuration()));

        List<Task> prioritizedTasks = taskManager.getPrioritizedTasks();

        assertAll("Положение в списке приоритетов не изменилось",
                () -> assertNotEquals(oldPrioritizedTasks.get(1), prioritizedTasks.get(1)),
                () -> assertEquals(oldPrioritizedTasks.get(1), prioritizedTasks.get(2))
        );
    }

    @Test
    @DisplayName("Обновив время начала и/или продолжительности Subtask при отсутствии коллизий она изменяет свое положение в списке приоритетов")
    void update_updatingSubtaskStartTimeOrDurationWithoutCollisionsShouldChangeItsPositionInPriorityList() {
        Subtask subtask1 = taskManager.createSubtask(new Subtask(TaskStatus.NEW, "name", "descr", taskManager.getAllEpics().getFirst().getId(), LocalDateTime.now(), Duration.ofDays(1)));
        Subtask subtask2 = taskManager.createSubtask(new Subtask(TaskStatus.NEW, "name", "descr", taskManager.getAllEpics().getFirst().getId(), subtask1.getEndTime(), Duration.ofDays(1)));
        Subtask subtask3 = taskManager.createSubtask(new Subtask(TaskStatus.NEW, "name", "descr", taskManager.getAllEpics().getFirst().getId(), subtask2.getEndTime(), Duration.ofDays(1)));

        List<Subtask> oldPrioritizedTasks = List.of(subtask1, subtask2, subtask3);

        taskManager.updateSubtask(new Subtask(subtask2.getId(), TaskStatus.NEW, "", "", taskManager.getAllEpics().getFirst().getId(), subtask3.getEndTime().plusDays(10), subtask3.getDuration()));

        List<Task> prioritizedTasks = taskManager.getPrioritizedTasks();

        assertAll("Положение в списке приоритетов не изменилось",
                () -> assertNotEquals(oldPrioritizedTasks.get(1), prioritizedTasks.get(1)),
                () -> assertEquals(oldPrioritizedTasks.get(1), prioritizedTasks.get(2))
        );
    }

    @ParameterizedTest(name = "Получение не существующей {0}")
    @MethodSource("tasksTypesProvider")
    @DisplayName("При попытке получения не существующей задачи выбрасывается NotFoundException")
    void get_shouldThrowNotFoundExceptionWhenFetchingNonExistentTask(TaskType taskType) {
        assertThrows(NotFoundException.class, () -> {
            switch (taskType) {
                case EPIC -> taskManager.getEpic(Integer.MAX_VALUE);
                case SUBTASK -> taskManager.getSubtask(Integer.MAX_VALUE);
                case TASK -> taskManager.getTask(Integer.MAX_VALUE);
            }
        }, "При попытке получить несуществующую задачу не падает исключение");
    }

    @ParameterizedTest(name = "Обновление не существующей {0}")
    @MethodSource("tasksTypesProvider")
    @DisplayName("При попытке обновления не существующей задачи выбрасывается NotFoundException")
    void update_shouldThrowNotFoundExceptionWhenUpdatingNonExistentTask(TaskType taskType) {
        assertThrows(NotFoundException.class, () -> {
            switch (taskType) {
                case EPIC -> {
                    Epic epic = new Epic(Integer.MAX_VALUE, TaskStatus.NEW, "", "", Task.DEFAULT_START_TIME, Task.DEFAULT_DURATION);
                    taskManager.updateEpic(epic);
                }
                case SUBTASK -> {
                    Subtask subtask = new Subtask(Integer.MAX_VALUE, TaskStatus.NEW, "", "", taskManager.getAllEpics().getFirst().getId(), Task.DEFAULT_START_TIME, Task.DEFAULT_DURATION);
                    taskManager.updateSubtask(subtask);
                }
                case TASK -> {
                    Task task = new Task(Integer.MAX_VALUE, TaskStatus.NEW, "", "", Task.DEFAULT_START_TIME, Task.DEFAULT_DURATION);
                    taskManager.updateTask(task);
                }
            }
        }, "При попытке получить несуществующую задачу не падает исключение");
    }

    @Test
    @DisplayName("При удалении Task из менеджера она так же удаляется из списка по приоритетам")
    void removeTask_deletingTaskFromManagerShouldAlsoRemoveItFromPriorityList() {
        Task task = taskManager.getTask(taskManager.getAllTasks().getFirst().getId());
        taskManager.updateTask(new Task(task.getId(), TaskStatus.NEW, "", "", LocalDateTime.now(), Duration.ofDays(1)));

        int initialPrioritizedTasksSize = taskManager.getPrioritizedTasks().size();

        taskManager.removeTask(task.getId());

        assertEquals(initialPrioritizedTasksSize - 1, taskManager.getPrioritizedTasks().size(), "Удаление Task из менеджера не привело к удалению Task из списка по приоритетам");
    }

    @Test
    @DisplayName("При удалении Subtask из менеджера она так же удаляется из списка по приоритетам")
    void removeSubtask_deletingSubtaskFromManagerShouldAlsoRemoveItFromPriorityList() {
        Subtask subtask = taskManager.getSubtask(taskManager.getAllSubTasks().getFirst().getId());
        taskManager.updateSubtask(new Subtask(subtask.getId(), TaskStatus.NEW, "", "", taskManager.getAllEpics().getFirst().getId(), LocalDateTime.now(), Duration.ofDays(1)));

        int initialPrioritizedTasksSize = taskManager.getPrioritizedTasks().size();

        taskManager.removeSubtask(subtask.getId());

        assertEquals(initialPrioritizedTasksSize - 1, taskManager.getPrioritizedTasks().size(), "Удаление Subtask из менеджера не привело к удалению Subtask из списка по приоритетам");
    }

    @Test
    @DisplayName("При удалении всех Task из менеджера они так же удаляется из списка по приоритетам")
    void removeAllTasks_deletingAllTasksFromManagerShouldAlsoRemoveThemFromPriorityList() {
        Task task = taskManager.getTask(taskManager.getAllTasks().getFirst().getId());
        taskManager.updateTask(new Task(task.getId(), TaskStatus.NEW, "", "", LocalDateTime.now(), Duration.ofDays(1)));

        int initialPrioritizedTasksSize = taskManager.getPrioritizedTasks().size();

        taskManager.removeAllTasks();

        assertEquals(initialPrioritizedTasksSize - 1, taskManager.getPrioritizedTasks().size(), "Удаление всех Task из менеджера не привело к удалению всех Task из списка по приоритетам");
    }

    @Test
    @DisplayName("При удалении всех Subtask из менеджера они так же удаляется из списка по приоритетам")
    void removeAllSubtasks_deletingAllSubtasksFromManagerShouldAlsoRemoveThemFromPriorityList() {
        Subtask subtask = taskManager.getSubtask(taskManager.getAllSubTasks().getFirst().getId());
        taskManager.updateSubtask(new Subtask(subtask.getId(), TaskStatus.NEW, "", "", taskManager.getAllEpics().getFirst().getId(), LocalDateTime.now(), Duration.ofDays(1)));

        int initialPrioritizedTasksSize = taskManager.getPrioritizedTasks().size();

        taskManager.removeAllSubtasks();

        assertEquals(initialPrioritizedTasksSize - 1, taskManager.getPrioritizedTasks().size(), "Удаление всех Subtask из менеджера не привело к удалению всех Subtask из списка по приоритетам");
    }

    @Test
    @DisplayName("При удалении всех Epic из менеджера их подзадачи удаляются из списка по приоритетам")
    void removeAllEpics_deletingAllEpicsFromManagerShouldAlsoRemoveTheirSubtasksFromPriorityList() {
        Subtask subtask = taskManager.getSubtask(taskManager.getAllSubTasks().getFirst().getId());
        taskManager.updateSubtask(new Subtask(subtask.getId(), TaskStatus.NEW, "", "", taskManager.getAllEpics().getFirst().getId(), LocalDateTime.now(), Duration.ofDays(1)));

        int initialPrioritizedTasksSize = taskManager.getPrioritizedTasks().size();

        taskManager.removeAllEpics();

        assertEquals(initialPrioritizedTasksSize - 1, taskManager.getPrioritizedTasks().size(), "Удаление всех Epic из менеджера не привело к удалению всех Epic из списка по приоритетам");
    }

    @Test
    @DisplayName("При удалении подзадачи обновляется состояние её эпика")
    void removeSubtask_whenRemovingSubtaskStateOfItsEpicIsUpdated() {
        Epic epic = taskManager.createEpic(new Epic("epic_name", "epic_descr"));
        Subtask subtask1 = taskManager.createSubtask(new Subtask(TaskStatus.DONE, "name", "descr", epic.getId(), LocalDateTime.now(), Duration.ofHours(1)));
        Subtask subtask2 = taskManager.createSubtask(new Subtask(TaskStatus.NEW, "name", "descr", epic.getId(), LocalDateTime.now().plusDays(1), Duration.ofDays(2)));

        TaskStatus newEpicTaskStatus = TaskStatus.DONE;
        LocalDateTime newEpicStartTime = subtask1.getStartTime();
        LocalDateTime newEpicEndTime = subtask1.getEndTime();
        Duration newEpicDuration = subtask1.getDuration();

        taskManager.removeSubtask(subtask2.getId());

        assertAll(
                () -> assertEquals(epic.getStatus(), newEpicTaskStatus, "Статус эпика не обновился"),
                () -> assertEquals(epic.getStartTime(), newEpicStartTime, "Время начала эпика не обновилось"),
                () -> assertEquals(epic.getEndTime(), newEpicEndTime, "Время окончания эпика не обновилось"),
                () -> assertEquals(epic.getDuration(), newEpicDuration, "Продолжительность эпика не обновилось")
        );
    }

    @Test
    @DisplayName("При удалении всех подзадач все эпики меняют статус на NEW")
    void removeAllSubtasks_shouldSetEpicStatusToNewWhenAllSubtasksAreDeleted() {
        taskManager.removeAllSubtasks();
        assertTrue(
                taskManager.getAllEpics()
                        .stream()
                        .map(Epic::getStatus)
                        .allMatch(taskStatus -> taskStatus == TaskStatus.NEW)
        );
    }

    @Test
    @DisplayName("История содержит только просмотренные задачи в порядке просмотра")
    void getHistory_historyContainsOnlyViewedTasksAreInTheOrderOfViewing() {
        assertTrue(taskManager.getHistory().isEmpty(), "История не пуста при отсутствии просмотров");

        List<Task> testObjectsContainer = new ArrayList<>() {{
            add(taskManager.getTask(taskManager.getAllTasks().getFirst().getId()));
            add(taskManager.getEpic(taskManager.getAllEpics().getFirst().getId()));
            add(taskManager.getSubtask(taskManager.getAllSubTasks().getFirst().getId()));
        }};

        List<Task> history = taskManager.getHistory();

        assertAll(
                () -> assertEquals(testObjectsContainer.size(), history.size(), "Размер истории не совпадает с кол-вом просмотров"),
                () -> testObjectsContainer.forEach(
                        viewedTask -> assertEquals(testObjectsContainer.indexOf(viewedTask), history.indexOf(viewedTask), "Порядок просмотра не совпадает с историей")
                )
        );
    }

    @Test
    @DisplayName("При удалении всех подзадач очищаются и подзадачи всех эпиков")
    void removeAllSubTasks_allSubtasksFromManagerAndEpicsAreDeleted() {
        taskManager.removeAllSubtasks();

        taskManager.getAllEpics()
                .forEach(epic -> assertTrue(epic.getSubtasksIds().isEmpty(), "Подзадачи не всех эпиков очищаются"));

        assertTrue(taskManager.getAllSubTasks().isEmpty(), "Удаляются не все подзадачи");
    }

    @Test
    @DisplayName("При удалении всех эпиков удаляются и все подзадачи")
    void removeAllEpics_allEpicsAndAllTheirSubtasksAreDeleted() {
        taskManager.removeAllEpics();

        assertAll(
                () -> assertTrue(taskManager.getAllSubTasks().isEmpty(), "Удаляются не все подзадачи"),
                () -> assertTrue(taskManager.getAllEpics().isEmpty(), "Удаляются не все эпики")
        );
    }

    @ParameterizedTest(name = "{0} добавляется в менеджер и может быть найден по id")
    @MethodSource("taskProvider")
    @DisplayName("Все типы задач добавляются в менеджер и могут быть найдены по id")
    void create_get_shouldTaskCreatedAndCanBeFoundById(String label, Task tsk) {
        int taskId = -1;
        Task savedTask;
        int initialTasksCount;

        var tasks = switch (tsk) {
            case Epic epic -> {
                initialTasksCount = taskManager.getAllEpics().size();
                taskId = taskManager.createEpic(epic).getId();
                savedTask = taskManager.getEpic(taskId);
                yield taskManager.getAllEpics();
            }

            case Subtask subtask -> {
                initialTasksCount = taskManager.getAllSubTasks().size();
                taskId = taskManager.createSubtask(subtask).getId();
                savedTask = taskManager.getSubtask(taskId);
                yield taskManager.getAllSubTasks();
            }

            case Task task -> {
                initialTasksCount = taskManager.getAllTasks().size();
                taskId = taskManager.createTask(task).getId();
                savedTask = taskManager.getTask(taskId);
                yield taskManager.getAllTasks();
            }
        };

        assertAll(
                () -> assertNotNull(savedTask, "Задача не найдена."),
                () -> assertEquals(tsk, savedTask, "Задачи не совпадают."),
                () -> assertNotNull(tasks, "Задачи не возвращаются."),
                () -> assertEquals(initialTasksCount + 1, tasks.size(), "Неверное количество задач."),
                () -> assertEquals(tsk, tasks.getLast(), "Задачи не совпадают.")
        );
    }

    @Test
    @DisplayName("При создании подзадачи обновляется состояние её эпика")
    void createSubtask_shouldUpdateEpicStatusWhenSubtaskIsCreated() {
        Epic epic = taskManager.createEpic(new Epic("epic_name", "epic_descr"));
        Subtask subtask1 = taskManager.createSubtask(new Subtask(TaskStatus.DONE, "name", "descr", epic.getId(), LocalDateTime.now(), Duration.ofHours(1)));

        TaskStatus newEpicTaskStatus = TaskStatus.DONE;
        LocalDateTime newEpicStartTime = subtask1.getStartTime();
        LocalDateTime newEpicEndTime = subtask1.getEndTime();
        Duration newEpicDuration = subtask1.getDuration();

        assertAll(
                () -> assertEquals(epic.getStatus(), newEpicTaskStatus, "Статус эпика не обновился"),
                () -> assertEquals(epic.getStartTime(), newEpicStartTime, "Время начала эпика не обновилось"),
                () -> assertEquals(epic.getEndTime(), newEpicEndTime, "Время окончания эпика не обновилось"),
                () -> assertEquals(epic.getDuration(), newEpicDuration, "Продолжительность эпика не обновилось")
        );
    }

    @Test
    @DisplayName("При отсутствующем эпике, указанном в создаваемой подзадаче, выбрасывается NotFoundException")
    void createSubtask_shouldUpdateEpicStatusUponSubtaskCreation() {
        assertThrows(NotFoundException.class, () -> taskManager.createSubtask(new Subtask(TaskStatus.NEW, "", "", Integer.MAX_VALUE)));
    }

    @Test
    @DisplayName("Можно обновить существующий Task")
    void updateTask_shouldUpdateExistingTask() {
        Task oldTask = taskManager.createTask(new Task(TaskStatus.NEW, "", "", LocalDateTime.now(), Duration.ofDays(1)));
        Task updatedTask = new Task(oldTask.getId(), TaskStatus.DONE, "new", "new", Task.DEFAULT_START_TIME, Task.DEFAULT_DURATION);

        taskManager.updateTask(updatedTask);

        Task controlTask = taskManager.getTask(oldTask.getId());

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
    void updateSubtask_shouldUpdateExistingSubtask() {
        Subtask oldSubtask = taskManager.createSubtask(new Subtask(TaskStatus.NEW, "", "", taskManager.getAllEpics().getFirst().getId(), LocalDateTime.now(), Duration.ofDays(1)));
        Subtask updatedSubtask = new Subtask(oldSubtask.getId(), TaskStatus.DONE, "new", "new", taskManager.getAllEpics().getLast().getId(), Task.DEFAULT_START_TIME, Task.DEFAULT_DURATION);

        taskManager.updateSubtask(updatedSubtask);

        Subtask controlSubtask = taskManager.getSubtask(oldSubtask.getId());

        assertAll(
                () -> assertEquals(controlSubtask.getStatus(), updatedSubtask.getStatus(), "Статус подзадачи не обновился."),
                () -> assertEquals(controlSubtask.getName(), updatedSubtask.getName(), "Имя подзадачи не обновилось."),
                () -> assertEquals(controlSubtask.getDescription(), updatedSubtask.getDescription(), "Описание подзадачи не обновилось."),
                () -> assertEquals(controlSubtask.getStartTime(), updatedSubtask.getStartTime(), "Время начала подзадачи не обновилось."),
                () -> assertEquals(controlSubtask.getDuration(), updatedSubtask.getDuration(), "Длительность подзадачи не обновилась."),
                () -> assertEquals(controlSubtask.getEpicId(), updatedSubtask.getEpicId(), "ID эпика подзадачи не обновился."),
                () -> assertTrue(taskManager.getEpic(controlSubtask.getEpicId()).getSubtasksIds().contains(controlSubtask.getId()), "Подзадача не перемещена в новый эпик."),
                () -> assertFalse(taskManager.getEpic(oldSubtask.getEpicId()).getSubtasksIds().contains(controlSubtask.getId()), "Подзадача не удалена из старого эпика при изменении эпика.")
        );
    }

    @Test
    @DisplayName("Можно обновить только имя и описание существующего Epic")
    void updateEpic_shouldOnlyUpdateNameAndDescriptionOfExistingEpic() {
        Epic oldEpic = taskManager.createEpic(new Epic("", ""));
        Epic updatedEpic = new Epic(oldEpic.getId(), TaskStatus.DONE, "new", "new", LocalDateTime.now(), Duration.ofDays(1));

        taskManager.updateEpic(updatedEpic);

        Epic controlEpic = taskManager.getEpic(oldEpic.getId());

        assertAll(
                () -> assertEquals(controlEpic.getName(), updatedEpic.getName(), "Имя эпика не обновилось."),
                () -> assertEquals(controlEpic.getDescription(), updatedEpic.getDescription(), "Описание эпика не обновилось."),
                () -> assertNotEquals(controlEpic.getStatus(), updatedEpic.getStatus(), "Статус эпика не обновился."),
                () -> assertNotEquals(controlEpic.getStartTime(), updatedEpic.getStartTime(), "Время начала эпика не обновилось."),
                () -> assertNotEquals(controlEpic.getDuration(), updatedEpic.getDuration(), "Длительность эпика не обновилась.")
        );
    }

    @Test
    @DisplayName("При обновлении подзадачи обновляется состояние её эпика")
    void updateSubtask_whenUpdatingSubtaskStateOfItsEpicIsUpdated() {
        Epic epic = taskManager.createEpic(new Epic("epic_name", "epic_descr"));
        Subtask subtask1 = taskManager.createSubtask(new Subtask(TaskStatus.IN_PROGRESS, "name", "descr", epic.getId()));
        Subtask subtask2 = taskManager.createSubtask(new Subtask(TaskStatus.NEW, "name", "descr", epic.getId()));

        Subtask newSubtask1 = new Subtask(subtask1.getId(), TaskStatus.DONE, "name", "descr", epic.getId(), LocalDateTime.now(), Duration.ofHours(1));
        Subtask newSubtask2 = new Subtask(subtask2.getId(), TaskStatus.DONE, "name", "descr", epic.getId(), LocalDateTime.now().plusMonths(1), Duration.ofMinutes(12867));

        TaskStatus newEpicTaskStatus = TaskStatus.DONE;
        LocalDateTime newEpicStartTime = newSubtask1.getStartTime();
        LocalDateTime newEpicEndTime = newSubtask2.getEndTime();
        Duration newEpicDuration = newSubtask1.getDuration().plus(newSubtask2.getDuration());

        taskManager.updateSubtask(newSubtask1);
        taskManager.updateSubtask(newSubtask2);

        assertAll(
                () -> assertEquals(epic.getStatus(), newEpicTaskStatus, "Статус эпика не обновился"),
                () -> assertEquals(epic.getStartTime(), newEpicStartTime, "Время начала эпика не обновилось"),
                () -> assertEquals(epic.getEndTime(), newEpicEndTime, "Время окончания эпика не обновилось"),
                () -> assertEquals(epic.getDuration(), newEpicDuration, "Продолжительность эпика не обновилось")
        );
    }

    @Test
    @DisplayName("Задачу можно удалить")
    void removeTask_taskRemovedFromManager() {
        int id = taskManager.getAllTasks().getFirst().getId();

        taskManager.removeTask(id);

        assertThrows(RuntimeException.class,
                () -> taskManager.getTask(id),
                "Задача не удалена из менеджера");
    }

    @Test
    @DisplayName("При удалении подзадачи она удаляется также и из своего эпика")
    void removeSubtask_subtaskRemovedFromManagerAndFromItsEpic() {
        int id = taskManager.getAllSubTasks().getFirst().getId();
        Subtask savedSubtask = taskManager.getSubtask(id);
        int savedSubtaskEpicId = savedSubtask.getEpicId();

        taskManager.removeSubtask(id);

        assertAll(
                () -> assertThrows(RuntimeException.class,
                        () -> taskManager.getSubtask(id),
                        "Подзадача не удалена из менеджера"),
                () -> assertFalse(taskManager.getEpicSubtasks(savedSubtaskEpicId).contains(savedSubtask), "Подзадача не удалена из эпика")
        );
    }

    @Test
    @DisplayName("При удалении эпика удаляются также и все его подзадачи")
    void removeEpic_epicRemovedFromManagerAndAllEpicSubtasksAreRemovedFromManager() {
        Epic epic = taskManager.getAllEpics().getFirst();
        Set<Integer> savedEpicSubtasksIds = epic.getSubtasksIds();

        taskManager.removeEpic(epic.getId());

        savedEpicSubtasksIds.forEach(subtaskId ->
                assertThrows(RuntimeException.class,
                        () -> taskManager.getSubtask(subtaskId),
                        "Не все сабтаски эпика удаляются при удалении эпика")
        );
    }
}
