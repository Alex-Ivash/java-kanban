package service;

import model.Epic;
import model.Status;
import model.Subtask;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryTaskManager")
class InMemoryTaskManagerTest {
    private static TaskManager taskManager;

    @BeforeEach
    void init() {
        taskManager = Managers.getDefault();

        taskManager.createTask(new Task(Status.NEW, "task1", "task1_descr"));
        taskManager.createTask(new Task(Status.NEW, "task2", "task2_descr"));
        taskManager.createTask(new Task(Status.NEW, "task3", "task3_descr"));

        taskManager.createEpic(new Epic("epic1", "epic1_descr"));
        taskManager.createEpic(new Epic("epic2", "epic2_descr"));

        taskManager.createSubtask(new Subtask(Status.NEW, "subtask1", "subtask1_descr", 3));
        taskManager.createSubtask(new Subtask(Status.NEW, "subtask2", "subtask2_descr", 3));
        taskManager.createSubtask(new Subtask(Status.NEW, "subtask3", "subtask3_descr", 4));
    }

    @Test
    @DisplayName("При удалении всех подзадач очищаются и подзадачи всех эпиков")
    void removeAllSubTasks() {
        taskManager.removeAllSubTasks();

        taskManager.getAllEpics()
                .forEach(epic -> assertTrue(epic.getSubtasksIds().isEmpty(), "Подзадачи не всех эпиков очищаются"));
        assertTrue(taskManager.getAllSubTasks().isEmpty(), "Удаляются не все подзадачи");
    }

    @Test
    @DisplayName("При удалении всех эпиков удаляются и все подзадачи")
    void removeAllEpics() {
        taskManager.removeAllEpics();

        assertTrue(taskManager.getAllSubTasks().isEmpty(), "Удаляются не все подзадачи");
        assertTrue(taskManager.getAllEpics().isEmpty(), "Удаляются не все эпики");
    }

    @Test
    @DisplayName("Задача добавляется и может быть найдена по id")
    void shouldTaskCreatedAndCanBeFoundById() {
        Task task = new Task(Status.NEW, "Test addNewTask", "Test addNewTask description");
        int taskId = taskManager.createTask(task).getId();
        Task savedTask = taskManager.getTask(taskId);

        assertNotNull(savedTask, "Задача не найдена.");
        assertEquals(task, savedTask, "Задачи не совпадают.");

        List<Task> tasks = taskManager.getAllTasks();

        assertNotNull(tasks, "Задачи не возвращаются.");
        assertEquals(4, tasks.size(), "Неверное количество задач.");
        assertEquals(task, tasks.getLast(), "Задачи не совпадают.");
    }

    @Test
    @DisplayName("Подзадача добавляется и может быть найдена по id")
    void shouldSubtaskCreatedAndCanBeFoundById() {
        Subtask subtask = new Subtask(Status.NEW, "Test addNewTask", "Test addNewTask description", 3);
        int subtaskId = taskManager.createSubtask(subtask).getId();
        Subtask savedSubtask = taskManager.getSubtask(subtaskId);

        assertNotNull(savedSubtask, "Подзадача не найдена.");
        assertEquals(subtask, savedSubtask, "Подзадачи не совпадают.");

        List<Subtask> subtasks = taskManager.getAllSubTasks();

        assertNotNull(subtasks, "Подзадачи не возвращаются.");
        assertEquals(4, subtasks.size(), "Неверное количество подзадач.");
        assertEquals(subtask, subtasks.getLast(), "Подзадачи не совпадают.");
    }

    @Test
    @DisplayName("Эпик добавляется и может быть найден по id")
    void shouldEpicCreatedAndCanBeFoundById() {
        Epic epic = new Epic("Test addNewTask", "Test addNewTask description");
        int epicId = taskManager.createEpic(epic).getId();
        Epic savedEpic = taskManager.getEpic(epicId);

        assertNotNull(savedEpic, "Эпик не найден.");
        assertEquals(epic, savedEpic, "Эпики не совпадают.");

        List<Epic> epics = taskManager.getAllEpics();

        assertNotNull(epics, "Эпики не возвращаются.");
        assertEquals(3, epics.size(), "Неверное количество эпиков.");
        assertEquals(epic, epics.getLast(), "Эпики не совпадают.");
    }

    @Test
    @DisplayName("Можно обновить конкретную задачу")
    void updateTask() {
        Task task = new Task(Status.IN_PROGRESS, "Test addNewTask", "Test addNewTask description");
        task.setId(0);
        Task savedTask = taskManager.updateTask(task);

        assertEquals(savedTask.getStatus(), task.getStatus(), "Статусы не обновляются");
        assertEquals(savedTask.getName(), task.getName(), "Имена не обновляются");
        assertEquals(savedTask.getDescription(), task.getDescription(), "Описания не обновляются");
    }

    @Test
    @DisplayName("Можно обновить конкретную подзадачу")
    void updateSubtask() {
        Subtask subtask = new Subtask(Status.DONE, "Test addNewTask", "Test addNewTask description", 4);
        subtask.setId(7);
        Subtask savedSubtask = taskManager.updateSubtask(subtask);

        assertEquals(savedSubtask.getStatus(), subtask.getStatus(), "Статусы не обновляются");
        assertEquals(savedSubtask.getName(), subtask.getName(), "Имена не обновляются");
        assertEquals(savedSubtask.getDescription(), subtask.getDescription(), "Описания не обновляются");
    }

    @Test
    @DisplayName("Можно обновить конкретный эпик")
    void updateEpic() {
        Epic epic = new Epic("Test addNewTask", "Test addNewTask description");
        epic.setId(3);
        Epic savedEpic = taskManager.updateEpic(epic);

        assertEquals(savedEpic.getName(), epic.getName(), "Имена не обновляются");
        assertEquals(savedEpic.getDescription(), epic.getDescription(), "Описания не обновляются");
    }

    @Test
    @DisplayName("Задачу можно удалить")
    void removeTask() {
        taskManager.removeTask(0);

        assertNull(taskManager.getTask(0), "Задача не удалена из менеджера");
    }

    @Test
    @DisplayName("При удалении подзадачи она удаляется также и из своего эпика")
    void removeSubtask() {
        Subtask savedSubtask = taskManager.getSubtask(5);
        int savedSubtaskEpicId = savedSubtask.getEpicId();

        taskManager.removeSubtask(5);

        assertNull(taskManager.getSubtask(5), "Подзадача не удалена из менеджера");
        assertFalse(taskManager.getEpicSubtasks(savedSubtaskEpicId).contains(savedSubtask), "Подзадача не удалена из эпика");
    }

    @Test
    @DisplayName("При удалении эпика удаляются также и все его подзадачи")
    void removeEpic() {
        Set<Integer> savedEpicSubtasksIds = taskManager.getEpic(3).getSubtasksIds();

        taskManager.removeEpic(3);

        savedEpicSubtasksIds.forEach(subtaskId -> {
            assertNull(taskManager.getSubtask(subtaskId),"Не все сабтаски эпика удаляются при удалении эпика");
        });
    }

    @Test
    @DisplayName("История содержит только просмотренные задачи в порядке просмотра")
    void getHistory() {
        assertTrue(taskManager.getHistory().isEmpty(), "История не пуста при отсутствии просмотров");

        Task viewedTask = taskManager.getTask(0);
        Epic viewedEpic = taskManager.getEpic(3);
        Subtask viewedSubtask = taskManager.getSubtask(5);

        List<Task> history = taskManager.getHistory();

        assertEquals(3, history.size(), "Размер истории не совпадает с просмотрами");
        assertEquals(0, history.indexOf(viewedTask), "Порядок просмотра не совпадает с историей");
        assertEquals(1, history.indexOf(viewedEpic), "Порядок просмотра не совпадает с историей");
        assertEquals(2, history.indexOf(viewedSubtask), "Порядок просмотра не совпадает с историей");
    }

    @Test
    @DisplayName("Cтатус эпика рассчитывается на основе статусов его подзадач")
    void epicStatusCalculatedBasedOnSubtasks() {
        Epic epic = taskManager.createEpic(new Epic("name", "desr"));
        assertEquals(Status.NEW, epic.getStatus(), "Пустой эпик имеет статус, отличный от NEW");

        taskManager.createSubtask(new Subtask(Status.NEW, "name", "descr", 8));
        taskManager.createSubtask(new Subtask(Status.NEW, "name", "descr", 8));
        assertEquals(Status.NEW, epic.getStatus(), "Эпик со всеми подзадачами в статусе NEW имеет статус, отличный от NEW");

        taskManager.removeSubtask(9);
        taskManager.removeSubtask(10);

        taskManager.createSubtask(new Subtask(Status.DONE, "name", "descr", 8));
        taskManager.createSubtask(new Subtask(Status.DONE, "name", "descr", 8));
        assertEquals(Status.DONE, epic.getStatus(), "Эпик со всеми подзадачами в статусе DONE имеет статус, отличный от DONE");

        taskManager.removeSubtask(11);
        taskManager.removeSubtask(12);

        taskManager.createSubtask(new Subtask(Status.IN_PROGRESS, "name", "descr", 8));
        taskManager.createSubtask(new Subtask(Status.IN_PROGRESS, "name", "descr", 8));
        assertEquals(Status.IN_PROGRESS, epic.getStatus(), "Эпик со всеми подзадачами в статусе IN_PROGRESS имеет статус, отличный от IN_PROGRESS");

        taskManager.removeSubtask(13);
        taskManager.removeSubtask(14);

        taskManager.createSubtask(new Subtask(Status.DONE, "name", "descr", 8));
        taskManager.createSubtask(new Subtask(Status.NEW, "name", "descr", 8));
        assertEquals(Status.IN_PROGRESS, epic.getStatus(), "Эпик с подзадачей в статусе DONE и NEW имеет статус, отличный от IN_PROGRESS");

        taskManager.removeSubtask(15);
        taskManager.removeSubtask(16);

        taskManager.createSubtask(new Subtask(Status.IN_PROGRESS, "name", "descr", 8));
        taskManager.createSubtask(new Subtask(Status.NEW, "name", "descr", 8));
        assertEquals(Status.IN_PROGRESS, epic.getStatus(), "Эпик с подзадачей в статусе IN_PROGRESS и NEW имеет статус, отличный от IN_PROGRESS");

        taskManager.removeSubtask(17);
        taskManager.removeSubtask(18);

        taskManager.createSubtask(new Subtask(Status.IN_PROGRESS, "name", "descr", 8));
        taskManager.createSubtask(new Subtask(Status.DONE, "name", "descr", 8));
        assertEquals(Status.IN_PROGRESS, epic.getStatus(), "Эпик с подзадачей в статусе IN_PROGRESS и DONE имеет статус, отличный от IN_PROGRESS");
    }

    @Test
    @DisplayName("Состояние добавленной задачи не отличается от состояния добавляемой в менеджер")
    void stateBeforeAndAfterCreateTaskShouldBeSame() {
        Task task = new Task(Status.NEW, "name", "descr");
        Status taskStatus = task.getStatus();
        String taskName = task.getName();
        String taskDescription = task.getDescription();

        Task addedTask = taskManager.createTask(task);

        assertEquals(taskStatus, addedTask.getStatus(), "Статус добавляемой и добавленной задачи изменился при добавлении");
        assertEquals(taskName, addedTask.getName(), "Имя добавляемой и добавленной задачи изменился при добавлении");
        assertEquals(taskDescription, addedTask.getDescription(), "Описание добавляемой и добавленной задачи изменился при добавлении");
    }

    @Test
    @DisplayName("Состояние добавленной подзадачи не отличается от состояния добавляемой в менеджер")
    void stateBeforeAndAfterCreateSubtaskShouldBeSame() {
        Subtask subtask = new Subtask(Status.NEW, "name", "descr", 3);
        Status subtaskStatus = subtask.getStatus();
        String subtaskName = subtask.getName();
        String subtaskDescription = subtask.getDescription();
        int subtaskEpicId = subtask.getEpicId();

        Subtask addedSubtask = taskManager.createSubtask(subtask);

        assertEquals(subtaskStatus, addedSubtask.getStatus(), "Статус добавляемой и добавленной задачи изменился при добавлении");
        assertEquals(subtaskName, addedSubtask.getName(), "Имя добавляемой и добавленной задачи изменился при добавлении");
        assertEquals(subtaskDescription, addedSubtask.getDescription(), "Описание добавляемой и добавленной задачи изменился при добавлении");
        assertEquals(subtaskEpicId, addedSubtask.getEpicId(), "ID эпика добавляемой и добавленной задачи изменился при добавлении");
    }

    @Test
    @DisplayName("Состояние добавленного эпика не отличается от состояния добавляемого в менеджер")
    void stateBeforeAndAfterCreateEpicShouldBeSame() {
        Epic epic = new Epic("name", "descr");
        String epicName = epic.getName();
        String epicDescription = epic.getDescription();

        Epic addedSubtask = taskManager.createEpic(epic);

        assertEquals(epicName, addedSubtask.getName(), "Имя добавляемой и добавленной задачи изменился при добавлении");
        assertEquals(epicDescription, addedSubtask.getDescription(), "Описание добавляемой и добавленной задачи изменился при добавлении");
    }
}