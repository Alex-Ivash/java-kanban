package service.managers.task;

import model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import service.managers.Managers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileBackedTaskManager")
class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> {
    private static final String TEST_CSV = "testResources/test.csv";

    @Override
    void initTaskManager() {
        taskManager = new FileBackedTaskManager(Managers.getDefaultHistory(), TEST_CSV);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of(TEST_CSV));
    }

    @ParameterizedTest(name = "Состояние задачи менеджера типа {0} может быть восстановлено из файла csv")
    @MethodSource("tasksTypesProvider")
    @DisplayName("Состояние Задач менеджера всех типов может быть восстановлено из файла csv")
    void loadFromFile_managerTasksStateCanBeRestoredFromCsv(TaskType taskType) {
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        var restoredTaskManagerTasks = switch (taskType) {
            case EPIC -> restoredTaskManager.getAllEpics();
            case SUBTASK -> restoredTaskManager.getAllSubTasks();
            case TASK -> restoredTaskManager.getAllTasks();
        };

        restoredTaskManagerTasks.forEach(restoredTask -> {
            var task = switch (taskType) {
                case EPIC -> taskManager.getEpic(restoredTask.getId());
                case SUBTASK -> taskManager.getSubtask(restoredTask.getId());
                case TASK -> taskManager.getTask(restoredTask.getId());
            };

            assertAll(
                    () -> assertNotNull(task, String.format("Не удалось восстановить %s. id: %d", taskType, restoredTask.getId())),
                    () -> assertEquals(task.getStatus(), restoredTask.getStatus(), String.format("Статус восстановленной %s не совпадает с изначальной. id: %d", taskType, restoredTask.getId())),
                    () -> assertEquals(task.getDescription(), restoredTask.getDescription(), String.format("Описание восстановленной %s не совпадает с изначальной. id: %d", taskType, restoredTask.getId())),
                    () -> assertEquals(task.getName(), restoredTask.getName(), String.format("Имя восстановленной %s не совпадает с изначальной. id: %d", taskType, restoredTask.getId())),
                    () -> assertEquals(task.getStartTime(), restoredTask.getStartTime(), String.format("Время начала восстановленной %s не совпадает с изначальной. id: %d", taskType, restoredTask.getId())),
                    () -> assertEquals(task.getDuration(), restoredTask.getDuration(), String.format("Продолжительность восстановленной %s не совпадает с изначальной. id: %d", taskType, restoredTask.getId())),
                    () -> {
                        switch (task) {
                            case Epic epic ->
                                    assertEquals(epic.getSubtasksIds(), ((Epic) restoredTask).getSubtasksIds(), "Подзадачи восстановленного EPIC не совпадают с изначальными. id: " + restoredTask.getId());
                            case Subtask subtask ->
                                    assertEquals(subtask.getEpicId(), ((Subtask) restoredTask).getEpicId(), "Эпик восстановленной SUBTASK не совпадает с изначальным. id: " + restoredTask.getId());
                            case Task tazk -> {
                            }
                        }
                    }
            );
        });
    }

    @Test
    @DisplayName("После удаления всех задач csv отражает актуальное состояние")
    void removeAllTasks_afterDeletingAllTasksCsvReflectsTheCurrentState() {
        taskManager.removeAllTasks();
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        assertAll(
                () -> assertEquals(0, restoredTaskManager.getAllTasks().size(), "Не все задачи удалены из csv"),
                () -> assertNotEquals(0, restoredTaskManager.getAllSubTasks().size(), "При удалении всех задач из csv удалились подзадачи"),
                () -> assertNotEquals(0, restoredTaskManager.getAllEpics().size(), "При удалении всех задач из csv удалились эпики")
        );
    }

    @Test
    @DisplayName("После удаления всех подзадач csv отражает актуальное состояние")
    void removeAllSubtasks_afterDeletingAllSubtasksCsvReflectsTheCurrentState() {
        taskManager.removeAllSubtasks();
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        assertAll(
                () -> assertEquals(0, restoredTaskManager.getAllSubTasks().size(), "Не все подзадачи удалены из csv"),
                () -> assertNotEquals(0, restoredTaskManager.getAllTasks().size(), "При удалении всех подзадач из csv удалились задачи"),
                () -> assertNotEquals(0, restoredTaskManager.getAllEpics().size(), "При удалении всех подзадач из csv удалились эпики")
        );

        assertTrue(
                restoredTaskManager
                        .getAllEpics()
                        .stream()
                        .map(epic -> epic.getSubtasksIds().size())
                        .allMatch(size -> size == 0), "Подзадачи не удаляются из эпиков в csv"
        );
    }

    @Test
    @DisplayName("После удаления всех эпиков csv отражает актуальное состояние")
    void removeAllEpics_afterDeletingAllEpicsCsvReflectsTheCurrentState() {
        taskManager.removeAllEpics();
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        assertAll(
                () -> assertEquals(0, restoredTaskManager.getAllEpics().size(), "Не все эпики удалены из csv"),
                () -> assertEquals(0, restoredTaskManager.getAllSubTasks().size(), "При удалении всех эпиков не все подзадачи удалены из csv"),
                () -> assertNotEquals(0, restoredTaskManager.getAllTasks().size(), "При удалении всех подзадач из csv удалились задачи")
        );
    }

    @Test
    @DisplayName("Созданная Задача добавляется в csv")
    void createTask_createdTaskIsAddedToTheCsv() {
        int newTaskId = taskManager.createTask(new Task(TaskStatus.NEW, "task3", "task3_descr")).getId();
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        assertDoesNotThrow(() -> restoredTaskManager.getTask(newTaskId), "Новая задача не добавляется в csv");
    }

    @Test
    @DisplayName("Созданная Подзадача добавляется в csv и в эпик в csv")
    void createSubtask_createdSubtaskIsAddedToTheCsv() {
        Epic epic = taskManager.getAllEpics().getFirst();
        Subtask subtask = taskManager.createSubtask(new Subtask(TaskStatus.NEW, "subtask1", "subtask1_descr", epic.getId()));

        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        assertAll(
                () -> assertDoesNotThrow(() -> restoredTaskManager.getSubtask(subtask.getId()), "Новая подзадача не добавляется в csv"),
                () -> assertEquals(epic.getSubtasksIds().size(), restoredTaskManager.getEpic(subtask.getEpicId()).getSubtasksIds().size(), "Подзадача не добавилась в эпик в csv")
        );
    }

    @Test
    @DisplayName("Созданный Эпик добавляется в csv")
    void createEpic_createdEpicIsAddedToTheCsv() {
        int newTaskId = taskManager.createEpic(new Epic("epic1", "epic1_descr")).getId();
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        assertDoesNotThrow(() -> restoredTaskManager.getEpic(newTaskId), "Новый эпик не добавляется в csv");
    }

    @Test
    @DisplayName("Обновленная Задача обновляется в csv")
    void updateTask_updatedTaskIsUpdatedInCsv() {
        Task newTask = taskManager.updateTask(new Task(taskManager.getAllTasks().getFirst().getId(), TaskStatus.DONE, "NEW_name", "NEW_description", Task.DEFAULT_START_TIME, Task.DEFAULT_DURATION));
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        Task restoredTask = restoredTaskManager.getTask(newTask.getId());

        assertAll(
                () -> assertEquals(newTask.getName(), restoredTask.getName(), "Имя не обновилось в csv"),
                () -> assertEquals(newTask.getStatus(), restoredTask.getStatus(), "Статус не обновился в csv"),
                () -> assertEquals(newTask.getDescription(), restoredTask.getDescription(), "Описание не обновилось в csv")
        );
    }

    @Test
    @DisplayName("Обновленная Подзадача обновляется в csv")
    void updateSubtask_updatedSubtaskIsUpdatedInCsv() {
        int epicId = taskManager.getAllEpics().getFirst().getId();
        Subtask newTask = taskManager.updateSubtask(
                new Subtask(taskManager.getAllSubTasks().getFirst().getId(), TaskStatus.IN_PROGRESS, "NEW_name", "NEW_description", epicId, Task.DEFAULT_START_TIME, Task.DEFAULT_DURATION)
        );
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        Subtask restoredTask = restoredTaskManager.getSubtask(newTask.getId());

        assertAll(
                () -> assertEquals(newTask.getName(), restoredTask.getName(), "Имя не обновилось в csv"),
                () -> assertEquals(newTask.getStatus(), restoredTask.getStatus(), "Статус не обновился в csv"),
                () -> assertEquals(newTask.getDescription(), restoredTask.getDescription(), "Описание не обновилось в csv"),
                () -> assertEquals(newTask.getEpicId(), restoredTask.getEpicId(), "Эпик подзадачи не обновился в csv")
        );
    }

    @Test
    @DisplayName("Обновленный Эпик обновляется в csv")
    void updateEpic_updatedEpicIsUpdatedInCsv() {
        Epic epic = new Epic(taskManager.getAllEpics().getFirst().getId(), TaskStatus.NEW, "NEW_name", "NEW_description", Task.DEFAULT_START_TIME, Task.DEFAULT_DURATION);

        Epic newTask = taskManager.updateEpic(epic);
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        Epic restoredTask = restoredTaskManager.getEpic(newTask.getId());

        assertAll(
                () -> assertEquals(newTask.getName(), restoredTask.getName(), "Имя не обновилось в csv"),
                () -> assertEquals(newTask.getDescription(), restoredTask.getDescription(), "Описание не обновилось в csv")
        );
    }

    @Test
    @DisplayName("Удаленная Задача удаляется из csv")
    void removeTask_deletedTaskIsDeletedFromCsv() {
        int taskId = taskManager.getAllTasks().getFirst().getId();

        taskManager.removeTask(taskId);
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        assertThrows(RuntimeException.class,
                () -> restoredTaskManager.getTask(taskId),
                "Задача не удаляется из csv");
    }

    @Test
    @DisplayName("Удаленная Подзадача удаляется из csv и из эпика в csv")
    void removeSubtask_deletedSubtaskIsDeletedFromCsv() {
        Subtask subtask = taskManager.getAllSubTasks().getFirst();
        int initialSubtasksCount = taskManager.getEpic(subtask.getEpicId()).getSubtasksIds().size();

        taskManager.removeSubtask(subtask.getId());
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        assertAll(
                () -> assertThrows(RuntimeException.class,
                        () -> restoredTaskManager.getSubtask(subtask.getId()),
                        "Подзадача не удаляется из csv"),
                () -> assertEquals(initialSubtasksCount - 1, restoredTaskManager.getEpic(subtask.getEpicId()).getSubtasksIds().size(), "Удаленная подзадача не удаляется из своего эпика в csv")
        );
    }

    @Test
    @DisplayName("Удаленный Эпик удаляется из csv, как и все его подзадачи")
    void removeEpic_deletedEpicIsDeletedFromCsv() {
        Epic epic = taskManager.getAllEpics().getFirst();
        Set<Integer> epicsSubtasks = epic.getSubtasksIds();

        taskManager.removeEpic(epic.getId());

        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        assertAll(
                () -> assertThrows(RuntimeException.class,
                        () -> restoredTaskManager.getEpic(epic.getId()),
                        "Эпик не удаляется из csv"),
                () -> epicsSubtasks.forEach(subtaskId -> assertThrows(RuntimeException.class,
                        () -> restoredTaskManager.getSubtask(subtaskId),
                        "Не все подзадачи удаленного эпика удаляются из csv"))
        );
    }
}
