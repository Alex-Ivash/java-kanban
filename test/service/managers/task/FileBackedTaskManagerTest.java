package service.managers.task;

import model.Epic;
import model.Subtask;
import model.Task;
import model.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.managers.Managers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedTaskManagerTest {
    private static TaskManager taskManager;
    private static final String testCSV = "testResources/test.csv";

    @BeforeEach
    void setUp() {
        taskManager = new FileBackedTaskManager(Managers.getDefaultHistory(), testCSV);
        taskManager.createTask(new Task(TaskStatus.NEW, "task1", "task1_descr"));
        taskManager.createTask(new Task(TaskStatus.NEW, "task2", "task2_descr"));
        taskManager.createTask(new Task(TaskStatus.NEW, "task3", "task3_descr"));

        taskManager.createEpic(new Epic("epic1", "epic1_descr"));
        taskManager.createEpic(new Epic("epic2", "epic2_descr"));

        taskManager.createSubtask(new Subtask(TaskStatus.NEW, "subtask1", "subtask1_descr", 3));
        taskManager.createSubtask(new Subtask(TaskStatus.NEW, "subtask2", "subtask2_descr", 3));
        taskManager.createSubtask(new Subtask(TaskStatus.NEW, "subtask3", "subtask3_descr", 4));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of(testCSV));
    }

    @Test
    @DisplayName("Состояние Задач менеджера может быть восстановлено из файла csv")
    void loadFromFile_managerTasksStateCanBeRestoredFromCsv() {
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);
        List<Task> restoredTaskManagerTasks = restoredTaskManager.getAllTasks();

        restoredTaskManagerTasks.forEach(restoredTask -> {
            Task task = taskManager.getTask(restoredTask.getId());

            assertNotNull(task, "Не удалось восстановить Задачу" + restoredTask.getId());
            assertEquals(task.getStatus(), restoredTask.getStatus(), "Статус восстановленной задачи не совпадает с изначальной. id: " + restoredTask.getId());
            assertEquals(task.getDescription(), restoredTask.getDescription(), "Описание восстановленной задачи не совпадает с изначальной. id: " + restoredTask.getId());
            assertEquals(task.getName(), restoredTask.getName(), "Имя восстановленной задачи не совпадает с изначальной. id: " + restoredTask.getId());
        });
    }

    @Test
    @DisplayName("Состояние Подзадач менеджера может быть восстановлено из файла csv")
    void loadFromFile_managerSubtasksStateCanBeRestoredFromCsv() {
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);
        List<Subtask> restoredTaskManagerSubtasks = restoredTaskManager.getAllSubTasks();

        restoredTaskManagerSubtasks.forEach(restoredTask -> {
            Subtask task = taskManager.getSubtask(restoredTask.getId());
            assertNotNull(task, "Не удалось восстановить Подзадачу" + restoredTask.getId());
            assertEquals(task.getStatus(), restoredTask.getStatus(), "Статус восстановленной подзадачи не совпадает с изначальной. id: " + restoredTask.getId());
            assertEquals(task.getDescription(), restoredTask.getDescription(), "Описание восстановленной подзадачи не совпадает с изначальной. id: " + restoredTask.getId());
            assertEquals(task.getName(), restoredTask.getName(), "Имя восстановленной подзадачи не совпадает с изначальной. id: " + restoredTask.getId());
            assertEquals(task.getEpicId(), restoredTask.getEpicId(), "Эпик восстановленной подзадачи не совпадает с изначальной. id: " + restoredTask.getId());
        });
    }

    @Test
    @DisplayName("Состояние Эпиков менеджера может быть восстановлено из файла csv")
    void loadFromFile_managerEpicsStateCanBeRestoredFromCsv() {
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);
        List<Epic> restoredTaskManagerEpics = restoredTaskManager.getAllEpics();

        restoredTaskManagerEpics.forEach(restoredTask -> {
            Epic task = taskManager.getEpic(restoredTask.getId());

            assertNotNull(task, "Не удалось восстановить Эпик" + restoredTask.getId());
            assertEquals(task.getStatus(), restoredTask.getStatus(), "Статус восстановленного эпика не совпадает с изначальной. id: " + restoredTask.getId());
            assertEquals(task.getDescription(), restoredTask.getDescription(), "Описание восстановленного эпика не совпадает с изначальной. id: " + restoredTask.getId());
            assertEquals(task.getName(), restoredTask.getName(), "Имя восстановленного эпика не совпадает с изначальной. id: " + restoredTask.getId());
            assertEquals(task.getSubtasksIds(), restoredTask.getSubtasksIds(), "Подзадачи восстановленного эпика не совпадает с изначальной. id: " + restoredTask.getId());
        });
    }

    @Test
    @DisplayName("После удаления всех задач csv отражает актуальное состояние")
    void removeAllTasks_afterDeletingAllTasksCsvReflectsTheCurrentState() {
        taskManager.removeAllTasks();
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);

        assertEquals(0, restoredTaskManager.getAllTasks().size(), "Не все задачи удалены из csv");
        assertNotEquals(0, restoredTaskManager.getAllSubTasks().size(), "При удалении всех задач из csv удалились подзадачи");
        assertNotEquals(0, restoredTaskManager.getAllEpics().size(), "При удалении всех задач из csv удалились эпики");
    }

    @Test
    @DisplayName("После удаления всех подзадач csv отражает актуальное состояние")
    void removeAllSubtasks_afterDeletingAllSubtasksCsvReflectsTheCurrentState() {
        taskManager.removeAllSubtasks();
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);

        assertEquals(0, restoredTaskManager.getAllSubTasks().size(), "Не все подзадачи удалены из csv");
        assertNotEquals(0, restoredTaskManager.getAllTasks().size(), "При удалении всех подзадач из csv удалились задачи");
        assertNotEquals(0, restoredTaskManager.getAllEpics().size(), "При удалении всех подзадач из csv удалились эпики");

        restoredTaskManager.getAllEpics().forEach(epic -> assertEquals(0, epic.getSubtasksIds().size(), "Подзадачи не удаляются из эпиков в csv"));
    }

    @Test
    @DisplayName("После удаления всех эпиков csv отражает актуальное состояние")
    void removeAllEpics_afterDeletingAllEpicsCsvReflectsTheCurrentState() {
        taskManager.removeAllEpics();
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);

        assertEquals(0, restoredTaskManager.getAllEpics().size(), "Не все эпики удалены из csv");
        assertEquals(0, restoredTaskManager.getAllSubTasks().size(), "При удалении всех эпиков не все подзадачи удалены из csv");
        assertNotEquals(0, restoredTaskManager.getAllTasks().size(), "При удалении всех подзадач из csv удалились задачи");
    }

    @Test
    @DisplayName("Созданная Задача добавляется в csv")
    void createTask_createdTaskIsAddedToTheCsv() {
        int newTaskId = taskManager.createTask(new Task(TaskStatus.NEW, "task3", "task3_descr")).getId();
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);

        assertNotNull(restoredTaskManager.getTask(newTaskId), "Новая задача не добавляется в csv");
    }

    @Test
    @DisplayName("Созданная Подзадача добавляется в csv и в эпик в csv")
    void createSubtask_createdSubtaskIsAddedToTheCsv() {
        Subtask subtask = taskManager.createSubtask(new Subtask(TaskStatus.NEW, "subtask1", "subtask1_descr", 3));
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);

        assertNotNull(restoredTaskManager.getSubtask(subtask.getId()), "Новая подзадача не добавляется в csv");
        assertEquals(3, restoredTaskManager.getEpic(subtask.getEpicId()).getSubtasksIds().size(), "Подзадача не добавилась в эпик в csv");
    }

    @Test
    @DisplayName("Созданный Эпик добавляется в csv")
    void createEpic_createdEpicIsAddedToTheCsv() {
        int newTaskId = taskManager.createEpic(new Epic("epic1", "epic1_descr")).getId();
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);

        assertNotNull(restoredTaskManager.getEpic(newTaskId), "Новый эпик не добавляется в csv");
    }

    @Test
    @DisplayName("Обновленная Задача обновляется в csv")
    void updateTask_updatedTaskIsUpdatedInCsv() {
        Task newTask = taskManager.updateTask(new Task(0, TaskStatus.DONE, "NEW_name", "NEW_description"));
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);

        Task restoredTask = restoredTaskManager.getTask(newTask.getId());

        assertEquals(newTask.getName(), restoredTask.getName(), "Имя не обновилось в csv");
        assertEquals(newTask.getStatus(), restoredTask.getStatus(), "Статус не обновился в csv");
        assertEquals(newTask.getDescription(), restoredTask.getDescription(), "Описание не обновилось в csv");
    }

    @Test
    @DisplayName("Обновленная Подзадача обновляется в csv")
    void updateSubtask_updatedSubtaskIsUpdatedInCsv() {
        Subtask newTask = taskManager.updateSubtask(new Subtask(5, TaskStatus.IN_PROGRESS, "NEW_name", "NEW_description", 4));
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);

        Subtask restoredTask = restoredTaskManager.getSubtask(newTask.getId());

        assertEquals(newTask.getName(), restoredTask.getName(), "Имя не обновилось в csv");
        assertEquals(newTask.getStatus(), restoredTask.getStatus(), "Статус не обновился в csv");
        assertEquals(newTask.getDescription(), restoredTask.getDescription(), "Описание не обновилось в csv");
        assertEquals(newTask.getEpicId(), restoredTask.getEpicId(), "Эпик подзадачи не обновился в csv");
    }

    @Test
    @DisplayName("Обновленный Эпик обновляется в csv")
    void updateEpic_updatedEpicIsUpdatedInCsv() {
        Epic epic = new Epic("NEW_name", "NEW_description");
        epic.setId(3);

        Epic newTask = taskManager.updateEpic(epic);
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);

        Epic restoredTask = restoredTaskManager.getEpic(newTask.getId());

        assertEquals(newTask.getName(), restoredTask.getName(), "Имя не обновилось в csv");
        assertEquals(newTask.getDescription(), restoredTask.getDescription(), "Описание не обновилось в csv");
    }

    @Test
    @DisplayName("Удаленная Задача удаляется из csv")
    void removeTask_deletedTaskIsDeletedFromCsv() {
        taskManager.removeTask(2);
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);

        assertNull(restoredTaskManager.getTask(2), "Задача не удаляется из csv");
    }

    @Test
    @DisplayName("Удаленная Подзадача удаляется из csv и из эпика в csv")
    void removeSubtask_deletedSubtaskIsDeletedFromCsv() {
        taskManager.removeSubtask(5);
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);

        assertNull(restoredTaskManager.getSubtask(5), "Подзадача не удаляется из csv");
        assertEquals(1, restoredTaskManager.getEpic(3).getSubtasksIds().size(), "Удаленная подзадача не удаляется из своего эпика в csv");
    }

    @Test
    @DisplayName("Удаленный Эпик удаляется из csv, как и все его подзадачи")
    void removeEpic_deletedEpicIsDeletedFromCsv() {
        taskManager.removeEpic(3);
        TaskManager restoredTaskManager = FileBackedTaskManager.loadFromFile(Managers.getDefaultHistory(), testCSV);

        assertNull(restoredTaskManager.getEpic(3), "Эпик не удаляется из csv");
        assertNull(restoredTaskManager.getSubtask(5), "Подзадачи эпика не удаляются в csv");
        assertNull(restoredTaskManager.getSubtask(6), "Подзадачи эпика не удаляются в csv");
    }
}
