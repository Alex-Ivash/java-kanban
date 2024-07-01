package service.managers.task;

import exception.NotFoundException;
import model.Epic;
import model.Subtask;
import model.Task;
import model.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.managers.Managers;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

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
//        Files.deleteIfExists(Path.of(TEST_CSV));
    }

    @Test
    @DisplayName("Состояние Task может быть восстановлено из файла csv")
    void loadFromFile_TaskStateRestoredCorrectly() {
        //given
        Task newTask = new Task(TaskStatus.NEW, "name", "description");
        taskManager.createTask(newTask);
        Task task = taskManager.getTask(0);

        //when
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        Task restoredTask = restoredTaskManager.getTask(0);

        //then
        assertAll(
                () -> assertNotNull(task, String.format("Не удалось восстановить %s. id: %d", "Task", restoredTask.getId())),
                () -> assertEquals(task.getStatus(), restoredTask.getStatus(), String.format("Статус восстановленной %s не совпадает с изначальной. id: %d", "Task", restoredTask.getId())),
                () -> assertEquals(task.getDescription(), restoredTask.getDescription(), String.format("Описание восстановленной %s не совпадает с изначальной. id: %d", "Task", restoredTask.getId())),
                () -> assertEquals(task.getName(), restoredTask.getName(), String.format("Имя восстановленной %s не совпадает с изначальной. id: %d", "Task", restoredTask.getId())),
                () -> assertEquals(task.getStartTime(), restoredTask.getStartTime(), String.format("Время начала восстановленной %s не совпадает с изначальной. id: %d", "Task", restoredTask.getId())),
                () -> assertEquals(task.getDuration(), restoredTask.getDuration(), String.format("Продолжительность восстановленной %s не совпадает с изначальной. id: %d", "Task", restoredTask.getId()))
        );
    }

    @Test
    @DisplayName("Состояние Subtask может быть восстановлено из файла csv")
    void loadFromFile_SubtaskStateRestoredCorrectly() {
        //given
        Epic epic = new Epic("", "");
        Subtask newSubtask = new Subtask(TaskStatus.NEW, "name", "description", 0);
        taskManager.createEpic(epic);
        taskManager.createSubtask(newSubtask);
        Subtask subtask = taskManager.getSubtask(1);

        //when
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        Subtask restoredSubtask = restoredTaskManager.getSubtask(1);

        //then
        assertAll(
                () -> assertNotNull(subtask, String.format("Не удалось восстановить %s. id: %d", "Subtask", restoredSubtask.getId())),
                () -> assertEquals(subtask.getStatus(), restoredSubtask.getStatus(), String.format("Статус восстановленной %s не совпадает с изначальной. id: %d", "Subtask", restoredSubtask.getId())),
                () -> assertEquals(subtask.getDescription(), restoredSubtask.getDescription(), String.format("Описание восстановленной %s не совпадает с изначальной. id: %d", "Subtask", restoredSubtask.getId())),
                () -> assertEquals(subtask.getName(), restoredSubtask.getName(), String.format("Имя восстановленной %s не совпадает с изначальной. id: %d", "Subtask", restoredSubtask.getId())),
                () -> assertEquals(subtask.getStartTime(), restoredSubtask.getStartTime(), String.format("Время начала восстановленной %s не совпадает с изначальной. id: %d", "Subtask", restoredSubtask.getId())),
                () -> assertEquals(subtask.getDuration(), restoredSubtask.getDuration(), String.format("Продолжительность восстановленной %s не совпадает с изначальной. id: %d", "Subtask", restoredSubtask.getId())),
                () -> assertEquals(subtask.getEpicId(), restoredSubtask.getEpicId(), "Эпик восстановленной Subtask не совпадает с изначальным. id: " + restoredSubtask.getId())
        );
    }

    @Test
    @DisplayName("Состояние Epic может быть восстановлено из файла csv")
    void loadFromFile_EpicStateRestoredCorrectly() {
        //given
        Epic newEpic = new Epic("name", "description");
        Subtask newSubtask = new Subtask(TaskStatus.NEW, "name", "description", 0);
        taskManager.createEpic(newEpic);
        taskManager.createSubtask(newSubtask);
        Epic epic = taskManager.getEpic(0);

        //when
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        Epic restoredEpic = restoredTaskManager.getEpic(0);

        //then
        assertAll(
                () -> assertNotNull(epic, String.format("Не удалось восстановить %s. id: %d", "Epic", restoredEpic.getId())),
                () -> assertEquals(epic.getStatus(), restoredEpic.getStatus(), String.format("Статус восстановленной %s не совпадает с изначальной. id: %d", "Epic", restoredEpic.getId())),
                () -> assertEquals(epic.getDescription(), restoredEpic.getDescription(), String.format("Описание восстановленной %s не совпадает с изначальной. id: %d", "Epic", restoredEpic.getId())),
                () -> assertEquals(epic.getName(), restoredEpic.getName(), String.format("Имя восстановленной %s не совпадает с изначальной. id: %d", "Epic", restoredEpic.getId())),
                () -> assertEquals(epic.getStartTime(), restoredEpic.getStartTime(), String.format("Время начала восстановленной %s не совпадает с изначальной. id: %d", "Epic", restoredEpic.getId())),
                () -> assertEquals(epic.getDuration(), restoredEpic.getDuration(), String.format("Продолжительность восстановленной %s не совпадает с изначальной. id: %d", "Epic", restoredEpic.getId())),
                () -> assertEquals(epic.getSubtasksIds(), restoredEpic.getSubtasksIds(), "Подзадачи восстановленного EPIC не совпадают с изначальными. id: " + restoredEpic.getId()),
                () -> assertEquals(epic.getEndTime(), restoredEpic.getEndTime(), "Время окончания восстановленного EPIC не совпадает с изначальным. id: " + restoredEpic.getId())
        );
    }

    @Test
    @DisplayName("Состояние списка по приоритетам может быть восстановлено из файла")
    void loadFromFile_RestorePrioritizedListStateFromFile() {
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

        List<Task> prioritizedTasks = taskManager.getPrioritizedTasks();

        //when
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);

        //then
        assertEquals(prioritizedTasks, restoredTaskManager.getPrioritizedTasks(), "Список по приоритетам восстанавливается из файла не корректно");
    }

    @Test
    @DisplayName("После удаления всех Task csv отражает актуальное состояние")
    void removeAllTasks_CsvFileIsUpdated() {
        //given
        Epic newEpic = new Epic("name", "description");
        Subtask newSubtask = new Subtask(TaskStatus.NEW, "name", "description", 0);
        Task newTask = new Task(TaskStatus.NEW, "name", "description");
        taskManager.createEpic(newEpic);
        taskManager.createSubtask(newSubtask);
        taskManager.createTask(newTask);

        //when
        taskManager.removeAllTasks();

        //then
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        assertAll(
                () -> assertEquals(0, restoredTaskManager.getAllTasks().size(), "Не все задачи удалены из csv"),
                () -> assertNotEquals(0, restoredTaskManager.getAllSubTasks().size(), "При удалении всех задач из csv удалились подзадачи"),
                () -> assertNotEquals(0, restoredTaskManager.getAllEpics().size(), "При удалении всех задач из csv удалились эпики")
        );
    }

    @Test
    @DisplayName("После удаления всех Subtask csv отражает актуальное состояние")
    void removeAllSubtasks_CsvFileIsUpdated() {
        //given
        Epic newEpic = new Epic("name", "description");
        Subtask newSubtask = new Subtask(TaskStatus.NEW, "name", "description", 0);
        Task newTask = new Task(TaskStatus.NEW, "name", "description");
        taskManager.createEpic(newEpic);
        taskManager.createSubtask(newSubtask);
        taskManager.createTask(newTask);

        //when
        taskManager.removeAllSubtasks();

        //then
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        assertAll(
                () -> assertEquals(0, restoredTaskManager.getAllSubTasks().size(), "Не все подзадачи удалены из csv"),
                () -> assertNotEquals(0, restoredTaskManager.getAllTasks().size(), "При удалении всех подзадач из csv удалились задачи"),
                () -> assertNotEquals(0, restoredTaskManager.getAllEpics().size(), "При удалении всех подзадач из csv удалились эпики"),
                () -> assertTrue(
                        restoredTaskManager
                                .getEpic(0)
                                .getSubtasksIds()
                                .stream()
                                .allMatch(size -> size == 0), "Подзадачи не удаляются из эпиков в csv"
                )
        );
    }

    @Test
    @DisplayName("После удаления всех Epic csv отражает актуальное состояние")
    void removeAllEpics_CsvFileIsUpdated() {
        //given
        Epic newEpic = new Epic("name", "description");
        Subtask newSubtask = new Subtask(TaskStatus.NEW, "name", "description", 0);
        Task newTask = new Task(TaskStatus.NEW, "name", "description");
        taskManager.createEpic(newEpic);
        taskManager.createSubtask(newSubtask);
        taskManager.createTask(newTask);

        //when
        taskManager.removeAllEpics();

        //then
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        assertAll(
                () -> assertEquals(0, restoredTaskManager.getAllEpics().size(), "Не все эпики удалены из csv"),
                () -> assertEquals(0, restoredTaskManager.getAllSubTasks().size(), "При удалении всех эпиков не все подзадачи удалены из csv"),
                () -> assertNotEquals(0, restoredTaskManager.getAllTasks().size(), "При удалении всех подзадач из csv удалились задачи")
        );
    }

    @Test
    @DisplayName("Созданная Задача добавляется в csv")
    void createTask_CsvFileIsUpdated() {
        //given
        Task newTask = new Task(TaskStatus.NEW, "task", "task_descr");

        //when
        taskManager.createTask(newTask);

        //then
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        assertDoesNotThrow(() -> restoredTaskManager.getTask(0), "Новая задача не добавляется в csv");
    }

    @Test
    @DisplayName("Созданная Подзадача добавляется в csv")
    void createSubtask_CsvFileIsUpdated() {
        //given
        Epic epic = new Epic("", "");
        Subtask newSubtask = new Subtask(TaskStatus.NEW, "subtask1", "subtask1_descr", 0);
        taskManager.createEpic(epic);

        //when
        taskManager.createSubtask(newSubtask);

        //then
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        assertDoesNotThrow(() -> restoredTaskManager.getSubtask(1), "Новая подзадача не добавляется в csv");
    }

    @Test
    @DisplayName("Созданный Эпик добавляется в csv")
    void createEpic_CsvFileIsUpdated() {
        //given
        Epic newEpic = new Epic("epic1", "epic1_descr");

        //when
        taskManager.createEpic(newEpic);

        //then
        Epic epic = taskManager.getEpic(0);
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        assertDoesNotThrow(() -> restoredTaskManager.getEpic(0), "Новый эпик не добавляется в csv");
    }

    @Test
    @DisplayName("Обновленная Задача обновляется в csv")
    void updateTask_CsvFileIsUpdated() {
        //given
        Task newTask = new Task(TaskStatus.DONE, "", "");
        taskManager.createTask(newTask);
        Task taskUpdate = new Task(0, TaskStatus.IN_PROGRESS, "NEW_name", "NEW_description", LocalDateTime.now(), Duration.ofDays(1));

        //when
        taskManager.updateTask(taskUpdate);

        //then
        Task updatesTask = taskManager.getTask(0);
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        Task restoredTask = restoredTaskManager.getTask(0);
        assertAll(
                () -> assertEquals(updatesTask.getName(), restoredTask.getName(), "Имя не обновилось в csv"),
                () -> assertEquals(updatesTask.getStatus(), restoredTask.getStatus(), "Статус не обновился в csv"),
                () -> assertEquals(updatesTask.getDescription(), restoredTask.getDescription(), "Описание не обновилось в csv")
        );
    }

    @Test
    @DisplayName("Обновленная Подзадача обновляется в csv")
    void updateSubtask_CsvFileIsUpdated() {
        //given
        Epic epic = new Epic("", "");
        Subtask newSubtask = new Subtask(TaskStatus.NEW, "", "", 0);
        taskManager.createEpic(epic);
        taskManager.createSubtask(newSubtask);
        Subtask subtaskUpdate = new Subtask(1, TaskStatus.IN_PROGRESS, "NEW_name", "NEW_description", 0, LocalDateTime.now(), Duration.ofDays(1));

        //when
        taskManager.updateSubtask(subtaskUpdate);

        //then
        Subtask updatedSubtask = taskManager.getSubtask(1);
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        Subtask restoredSubtask = restoredTaskManager.getSubtask(1);
        assertAll(
                () -> assertEquals(updatedSubtask.getName(), restoredSubtask.getName(), "Имя не обновилось в csv"),
                () -> assertEquals(updatedSubtask.getStatus(), restoredSubtask.getStatus(), "Статус не обновился в csv"),
                () -> assertEquals(updatedSubtask.getDescription(), restoredSubtask.getDescription(), "Описание не обновилось в csv"),
                () -> assertEquals(updatedSubtask.getEpicId(), restoredSubtask.getEpicId(), "Эпик подзадачи не обновился в csv")
        );
    }

    @Test
    @DisplayName("Обновленный Эпик обновляется в csv")
    void updateEpic_CsvFileIsUpdated() {
        //given
        Epic newEpic = new Epic("", "");
        Subtask subtask = new Subtask(TaskStatus.NEW, "", "", 0);
        taskManager.createEpic(newEpic);
        taskManager.createSubtask(subtask);
        Epic epicUpdate = new Epic("NEW_name", "NEW_description");
        epicUpdate.setId(0);

        //when
        taskManager.updateEpic(epicUpdate);

        //then
        Epic updatedEpic = taskManager.getEpic(0);
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        Epic restoredEpic = restoredTaskManager.getEpic(0);
        assertAll(
                () -> assertEquals(updatedEpic.getName(), restoredEpic.getName(), "Имя не обновилось в csv"),
                () -> assertEquals(updatedEpic.getDescription(), restoredEpic.getDescription(), "Описание не обновилось в csv"),
                () -> assertEquals(updatedEpic.getSubtasksIds(), restoredEpic.getSubtasksIds(), "Сабтаски не обновились в csv")
        );
    }

    @Test
    @DisplayName("Удаленная Задача удаляется из csv")
    void removeTask_CsvFileIsUpdated() {
        //given
        Task newTask = new Task(TaskStatus.NEW, "", "");
        taskManager.createTask(newTask);

        //when
        taskManager.removeTask(0);

        //then
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        assertThrows(NotFoundException.class,
                () -> restoredTaskManager.getTask(0),
                "Задача не удаляется из csv");
    }

    @Test
    @DisplayName("Удаленная Подзадача удаляется из csv")
    void removeSubtask_CsvFileIsUpdated() {
        //given
        Epic epic = new Epic("", "");
        Subtask subtask = new Subtask(TaskStatus.NEW, "", "", 0);
        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask);

        //when
        taskManager.removeSubtask(1);

        //then
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        assertAll(
                () -> assertThrows(NotFoundException.class,
                        () -> restoredTaskManager.getSubtask(1),
                        "Подзадача не удаляется из csv"),
                () -> assertEquals(0, restoredTaskManager.getEpic(0).getSubtasksIds().size(), "Удаленная подзадача не удаляется из своего эпика в csv")
        );
    }

    @Test
    @DisplayName("Удаленный Эпик удаляется из csv, как и все его подзадачи")
    void removeEpic_CsvFileIsUpdated() {
        //given
        Epic epic = new Epic("", "");
        Subtask subtask = new Subtask(TaskStatus.NEW, "", "", 0);
        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask);

        //when
        taskManager.removeEpic(0);

        //then
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), TEST_CSV);
        assertAll(
                () -> assertThrows(NotFoundException.class,
                        () -> restoredTaskManager.getEpic(0),
                        "Эпик не удаляется из csv"),
                () -> assertThrows(NotFoundException.class,
                        () -> restoredTaskManager.getSubtask(1),
                        "Не все подзадачи удаленного эпика удаляются из csv")
        );
    }
}
