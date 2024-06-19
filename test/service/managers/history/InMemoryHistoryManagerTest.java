package service.managers.history;

import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import service.managers.Managers;
import service.managers.task.InMemoryTaskManager;
import service.managers.task.TaskManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryHistoryManager")
class InMemoryHistoryManagerTest {
    private static TaskManager taskManager;
    private static ArrayList<Task> testObjectsContainer;
    private static final int TASKS_COUNT = 3;
    private static final int SUBTASKS_COUNT = 3;
    private static final int EPICS_COUNT = 3;

    @BeforeEach
    void setUp() {
        testObjectsContainer = new ArrayList<>();
        taskManager = new InMemoryTaskManager(Managers.getDefaultHistory());

        IntStream.range(0, TASKS_COUNT).forEach(i -> taskManager.createTask(new Task(TaskStatus.NEW, "task" + i, "task_descr" + i)));
        IntStream.range(0, EPICS_COUNT).forEach(i -> {
            int id = taskManager.createEpic(new Epic("epic" + i, "epic_descr" + i)).getId();
            IntStream.range(0, SUBTASKS_COUNT).forEach(k -> taskManager.createSubtask(new Subtask(TaskStatus.NEW, "subtask" + i + k, "subtask_descr" + i + k, id)));
        });
    }

    static Stream<Arguments> viewedTasksTypesProvider() {
        return Stream.of(
                Arguments.of(Collections.nCopies(3, TaskType.TASK)),
                Arguments.of(Collections.nCopies(3, TaskType.SUBTASK)),
                Arguments.of(Collections.nCopies(3, TaskType.EPIC)),
                Arguments.of(List.of(TaskType.TASK, TaskType.SUBTASK, TaskType.EPIC))
        );
    }

    static Stream<Arguments> tasksTypesProvider() {
        return Stream.of(
                Arguments.of(TaskType.TASK),
                Arguments.of(TaskType.SUBTASK),
                Arguments.of(TaskType.EPIC)
        );
    }

    @ParameterizedTest(name = "После последовательного просмотра задач типов {0} они добавляются в историю в порядке просмотра")
    @MethodSource("viewedTasksTypesProvider")
    @DisplayName("Задачи всех типов могут быть добавлены в историю")
    void add_taskCanBeAddedToTheHistory(List<TaskType> taskTypes) {
        IntStream.range(0, taskTypes.size()).forEach(i ->
                testObjectsContainer.add(
                        switch (taskTypes.get(i)) {
                            case EPIC -> taskManager.getEpic(taskManager.getAllEpics().get(i).getId());
                            case TASK -> taskManager.getTask(taskManager.getAllTasks().get(i).getId());
                            case SUBTASK -> taskManager.getSubtask(taskManager.getAllSubTasks().get(i).getId());
                        }
                )
        );

        assertAll(
                () -> assertEquals(testObjectsContainer.size(), taskManager.getHistory().size(), "Длина истории не соответствует кол-ву уникальных просмотров"),
                () -> IntStream.range(0, testObjectsContainer.size())
                        .forEach(i -> assertEquals(testObjectsContainer.get(i), taskManager.getHistory().get(i), i + "-я задача в истории не соответствует действительно просмотренной"))
        );
    }

    @Test
    @DisplayName("При повторном просмотре удаляется существующий в истории просмотр")
    void add_whenViewAgainExistingViewInTheHistoryIsDeleted() {
        List<Integer> taskIds = taskManager.getAllTasks().stream().map(Task::getId).toList();

        taskManager.getTask(taskIds.get(0));
        taskManager.getTask(taskIds.get(0));
        taskManager.getTask(taskIds.get(1));
        assertEquals(2, taskManager.getHistory().size(), "При повторном просмотре старая запись в истории не удаляется с головы");

        taskManager.getTask(taskIds.get(1));
        assertEquals(2, taskManager.getHistory().size(), "При повторном просмотре старая запись в истории не удаляется с хвоста");

        taskManager.getTask(taskIds.get(2));
        taskManager.getTask(taskIds.get(1));
        assertEquals(3, taskManager.getHistory().size(), "При повторном просмотре старая запись в истории не удаляется из середины");
    }

    @Test
    @DisplayName("История пуста при отсутствии просмотров")
    void getHistory_historyIsEmptyIfThereAreNoViews() {
        assertTrue(taskManager.getHistory().isEmpty(), "История не пуста при отсутствии просмотров");
    }

    @Test
    @DisplayName("Порядок задач в выводе истории соответствует порядку просмотров")
    void getHistory_orderOfTasksInTheHistoryOutputCorrespondsToTheOrderOfViews() {
        int taskId = taskManager.getAllTasks().getFirst().getId();
        int subTaskId = taskManager.getAllSubTasks().getFirst().getId();
        int epicId = taskManager.getAllEpics().getFirst().getId();

        taskManager.getTask(taskId);
        taskManager.getSubtask(subTaskId);
        taskManager.getSubtask(subTaskId);
        taskManager.getEpic(epicId);
        taskManager.getEpic(epicId);

        var history = taskManager.getHistory();

        assertEquals(Task.class, history.get(0).getClass(), "Ожидался Task");
        assertEquals(Subtask.class, history.get(1).getClass(), "Ожидался Subtask");
        assertEquals(Epic.class, history.get(2).getClass(), "Ожидался Epic");
    }

    @ParameterizedTest(name = "При удалении задачи типа {0} из TaskManager она так же удаляется и из истории")
    @MethodSource("tasksTypesProvider")
    @DisplayName("При удалении задач всех типов из TaskManager они так же удаляются и из истории")
    void remove_deletingTaskFromTaskManagerDeletesTaskInTheHistoryAsWell(TaskType taskType) {
        List<Integer> taskIds = switch (taskType) {
            case EPIC -> taskManager.getAllEpics().stream().map(Task::getId).toList();
            case SUBTASK -> taskManager.getAllSubTasks().stream().map(Task::getId).toList();
            case TASK -> taskManager.getAllTasks().stream().map(Task::getId).toList();
        };

        assertAll(
                () -> {
                    switch (taskType) {
                        case EPIC -> {
                            taskManager.getEpic(taskIds.get(0));
                            taskManager.removeEpic(taskIds.get(0));
                            assertEquals(0, taskManager.getHistory().size(), "После удаления единственной задачи, попавшей в историю история не пустеет");

                            taskManager.getEpic(taskIds.get(1));
                            taskManager.getEpic(taskIds.get(2));
                            taskManager.removeEpic(taskIds.get(1));
                        }
                        case SUBTASK -> {
                            taskManager.getSubtask(taskIds.get(0));
                            taskManager.removeSubtask(taskIds.get(0));
                            assertEquals(0, taskManager.getHistory().size(), "После удаления единственной задачи, попавшей в историю история не пустеет");

                            taskManager.getSubtask(taskIds.get(1));
                            taskManager.getSubtask(taskIds.get(2));
                            taskManager.removeSubtask(taskIds.get(1));
                        }
                        case TASK -> {
                            taskManager.getTask(taskIds.get(0));
                            taskManager.removeTask(taskIds.get(0));
                            assertEquals(0, taskManager.getHistory().size(), "После удаления единственной задачи, попавшей в историю история не пустеет");

                            taskManager.getTask(taskIds.get(1));
                            taskManager.getTask(taskIds.get(2));
                            taskManager.removeTask(taskIds.get(1));
                        }
                    }
                },
                () -> assertEquals(1, taskManager.getHistory().size(), "После удаления последней задачи из двух попавших в историю длина истории больше 1")
        );
    }

    @Test
    @DisplayName("При удалении задачи типа Epic из TaskManager он удаляется из истории вместе со своими подзадачами")
    void remove_deletingEpicFromTaskManagerDeletesTaskInTheHistoryAsWellAlongWithTheSubtasks() {
        Epic epic = taskManager.getEpic(taskManager.getAllEpics().getFirst().getId());

        Set<Integer> subtaskIds = epic.getSubtasksIds();
        subtaskIds.forEach(subtaskId -> taskManager.getSubtask(subtaskId));
        taskManager.getSubtask(taskManager.getAllSubTasks().stream().filter(subtask -> !subtaskIds.contains(subtask.getId())).findAny().get().getId());
        taskManager.removeEpic(epic.getId());

        assertAll(
                () -> assertNotEquals(0, taskManager.getHistory().size(), "Удаление эпика приводит к удалению не только его собственных подзадач"),
                () -> assertNotEquals(2, taskManager.getHistory().size(), "Удаление эпика приводит только к удалению его подзадач, но не самого эпика"),
                () -> assertNotEquals(subtaskIds.size() + 1, taskManager.getHistory().size(), "После удаления эпика его подзадачи не удалились из истории")
        );
    }

    @Test
    @DisplayName("При удалении всех эпиков из TaskManager так же удаляются и все их подзадачи из истории")
    void remove_deletingAllEpicsRemovesAllEpicsAndSubtasksFromTheHistory() {
        List<Integer> epicsIds = taskManager.getAllEpics().stream().map(Epic::getId).toList();
        testObjectsContainer.add(taskManager.getEpic(epicsIds.get(0)));
        testObjectsContainer.add(taskManager.getEpic(epicsIds.get(1)));

        List<Subtask> viewedSubtasks = ((Epic) testObjectsContainer.get(0)).getSubtasksIds().stream().map(taskManager::getSubtask).toList();

        taskManager.removeAllEpics();

        assertAll(
                () -> testObjectsContainer.forEach(viewedEpic -> assertFalse(taskManager.getHistory().contains(viewedEpic), "Эпики не удалены из истории")),
                () -> viewedSubtasks.forEach(viewedSubtask -> assertFalse(taskManager.getHistory().contains(viewedSubtask), "Подзадачи не удалены из истории"))
        );
    }

    @ParameterizedTest(name = "При удалении всех задач типа {0} из TaskManager они так же удаляются и из истории")
    @MethodSource("tasksTypesProvider")
    @DisplayName("При удалении всех задач определенного типа из TaskManager они так же удаляются и из истории")
    void remove_whenDeleteAllTasksOfTheTaskTypeFromTheTaskManagerTheyAreAlsoDeletedFromTheHistory(TaskType taskType) {
        switch (taskType) {
            case EPIC -> {
                List<Integer> epicsIds = taskManager.getAllEpics().stream().map(Epic::getId).toList();
                taskManager.getEpic(epicsIds.get(0));
                taskManager.getEpic(epicsIds.get(1));
                taskManager.getTask(taskManager.getAllTasks().getFirst().getId());
                taskManager.getTask(taskManager.getAllTasks().getLast().getId());

                taskManager.removeAllEpics();
            }
            case SUBTASK -> {
                List<Integer> subtasksIds = taskManager.getAllSubTasks().stream().map(Subtask::getId).toList();
                taskManager.getSubtask(subtasksIds.get(0));
                taskManager.getSubtask(subtasksIds.get(1));
                taskManager.getTask(taskManager.getAllTasks().getFirst().getId());
                taskManager.getTask(taskManager.getAllTasks().getLast().getId());

                taskManager.removeAllSubtasks();
            }
            case TASK -> {
                List<Integer> tasksIds = taskManager.getAllTasks().stream().map(Task::getId).toList();
                taskManager.getTask(tasksIds.get(0));
                taskManager.getTask(tasksIds.get(0));
                taskManager.getTask(tasksIds.get(1));
                taskManager.getSubtask(taskManager.getAllSubTasks().getFirst().getId());
                taskManager.getEpic(taskManager.getAllEpics().getLast().getId());

                taskManager.removeAllTasks();
            }
        }

        assertNotEquals(0, taskManager.getHistory().size(), "Удаление всех задач типа " + taskType + " приводит к удалению задач всех типов из истории");
        assertEquals(2, taskManager.getHistory().size(), "Удаление всех задач типа " + taskType + " не приводит к удалению всех задач этого типа из истории");
    }
}
