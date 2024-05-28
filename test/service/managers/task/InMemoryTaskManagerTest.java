package service.managers.task;

import model.Epic;
import model.Subtask;
import model.Task;
import model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.managers.Managers;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryTaskManager")
class InMemoryTaskManagerTest {
    private static TaskManager taskManager;

    @BeforeEach
    void init() {
        taskManager = new InMemoryTaskManager(Managers.getDefaultHistory());

        taskManager.createTask(new Task(TaskStatus.NEW, "task1", "task1_descr"));
        taskManager.createTask(new Task(TaskStatus.NEW, "task2", "task2_descr"));
        taskManager.createTask(new Task(TaskStatus.NEW, "task3", "task3_descr"));

        taskManager.createEpic(new Epic("epic1", "epic1_descr"));
        taskManager.createEpic(new Epic("epic2", "epic2_descr"));

        taskManager.createSubtask(new Subtask(TaskStatus.NEW, "subtask1", "subtask1_descr", 3));
        taskManager.createSubtask(new Subtask(TaskStatus.NEW, "subtask2", "subtask2_descr", 3));
        taskManager.createSubtask(new Subtask(TaskStatus.NEW, "subtask3", "subtask3_descr", 4));
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

        assertTrue(taskManager.getAllSubTasks().isEmpty(), "Удаляются не все подзадачи");
        assertTrue(taskManager.getAllEpics().isEmpty(), "Удаляются не все эпики");
    }

    @Test
    @DisplayName("Задача добавляется и может быть найдена по id")
    void createTask_getTask_shouldTaskCreatedAndCanBeFoundById() {
        Task task = new Task(TaskStatus.NEW, "Test addNewTask", "Test addNewTask description");
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
    void createSubtask_getSubtask_shouldSubtaskCreatedAndCanBeFoundById() {
        Subtask subtask = new Subtask(TaskStatus.NEW, "Test addNewTask", "Test addNewTask description", 3);
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
    void createEpic_getEpic_shouldEpicCreatedAndCanBeFoundById() {
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
    void updateTask_taskUpdatedBasedOnTheObjectFieldsInTheArgument() {
        Task task = new Task(TaskStatus.IN_PROGRESS, "Test addNewTask", "Test addNewTask description");
        task.setId(0);
        Task savedTask = taskManager.updateTask(task);

        assertEquals(savedTask.getStatus(), task.getStatus(), "Статус не обновляется");
        assertEquals(savedTask.getName(), task.getName(), "Имя не обновляется");
        assertEquals(savedTask.getDescription(), task.getDescription(), "Описание не обновляется");
    }

    @Test
    @DisplayName("Можно обновить конкретную подзадачу")
    void updateSubtask_subtaskUpdatedBasedOnTheObjectFieldsInTheArgument() {
        Subtask subtask = new Subtask(TaskStatus.DONE, "Test addNewTask", "Test addNewTask description", 3);
        subtask.setId(7);
        Subtask savedSubtask = taskManager.updateSubtask(subtask);

        assertEquals(savedSubtask.getStatus(), subtask.getStatus(), "Статус не обновляется");
        assertEquals(savedSubtask.getName(), subtask.getName(), "Имя не обновляется");
        assertEquals(savedSubtask.getDescription(), subtask.getDescription(), "Описание не обновляется");
        assertEquals(savedSubtask.getEpicId(), subtask.getEpicId(), "Принадлежность к эпику не обновляется");
    }

    @Test
    @DisplayName("При обновлении эпика обновляются только имя и описание")
    void updateEpic_onlyNameAndDescriptionAreUpdated() {
        Epic epic = new Epic("Test addNewTask", "Test addNewTask description");
        epic.setId(3);
        Epic savedEpic = taskManager.updateEpic(epic);

        assertNotEquals(savedEpic.getStatus(), epic.getStatus(), "Статус обновился, не должен!");
        assertEquals(savedEpic.getName(), epic.getName(), "Имя не обновляется");
        assertEquals(savedEpic.getDescription(), epic.getDescription(), "Описание не обновляется");
        assertNotEquals(savedEpic.getSubtasksIds().size(), epic.getSubtasksIds().size(), "Коллекция подзадач обновилась, не должна!");
    }

    @Test
    @DisplayName("Задачу можно удалить")
    void removeTask_taskRemovedFromManager() {
        taskManager.removeTask(0);

        assertNull(taskManager.getTask(0), "Задача не удалена из менеджера");
    }

    @Test
    @DisplayName("При удалении подзадачи она удаляется также и из своего эпика")
    void removeSubtask_subtaskRemovedFromManagerAndFromItsEpic() {
        Subtask savedSubtask = taskManager.getSubtask(5);
        int savedSubtaskEpicId = savedSubtask.getEpicId();

        taskManager.removeSubtask(5);

        assertNull(taskManager.getSubtask(5), "Подзадача не удалена из менеджера");
        assertFalse(taskManager.getEpicSubtasks(savedSubtaskEpicId).contains(savedSubtask), "Подзадача не удалена из эпика");
    }

    @Test
    @DisplayName("При удалении эпика удаляются также и все его подзадачи")
    void removeEpic_epicRemovedFromManagerAndAllEpicSubtasksAreRemovedFromManager() {
        Set<Integer> savedEpicSubtasksIds = taskManager.getEpic(3).getSubtasksIds();

        taskManager.removeEpic(3);

        savedEpicSubtasksIds.forEach(subtaskId -> {
            assertNull(taskManager.getSubtask(subtaskId),"Не все сабтаски эпика удаляются при удалении эпика");
        });
    }

    @Test
    @DisplayName("История содержит только просмотренные задачи в порядке просмотра")
    void getHistory_historyContainsOnlyViewedTasksAreInTheOrderOfViewing() {
        assertTrue(taskManager.getHistory().isEmpty(), "История не пуста при отсутствии просмотров");

        Task viewedTask = taskManager.getTask(0);
        Epic viewedEpic = taskManager.getEpic(3);
        Subtask viewedSubtask = taskManager.getSubtask(5);

        List<Task> history = taskManager.getHistory();

        assertEquals(3, history.size(), "Размер истории не совпадает с кол-вом просмотров");
        assertEquals(0, history.indexOf(viewedTask), "Порядок просмотра не совпадает с историей");
        assertEquals(1, history.indexOf(viewedEpic), "Порядок просмотра не совпадает с историей");
        assertEquals(2, history.indexOf(viewedSubtask), "Порядок просмотра не совпадает с историей");
    }

    @Test
    @DisplayName("Cтатус эпика NEW рассчитывается на основе статусов его подзадач")
    void calculateEpicStatus_shouldBeNEW_emptyEpicOrAllSubtasksInStatusNEW() {
        Epic epic = taskManager.createEpic(new Epic("name", "desr"));
        assertEquals(TaskStatus.NEW, epic.getStatus(), "Пустой эпик имеет статус, отличный от NEW");

        taskManager.createSubtask(new Subtask(TaskStatus.NEW, "name", "descr", 8));
        taskManager.createSubtask(new Subtask(TaskStatus.NEW, "name", "descr", 8));
        assertEquals(TaskStatus.NEW, epic.getStatus(), "Эпик со всеми подзадачами в статусе NEW имеет статус, отличный от NEW");
    }

    @Test
    @DisplayName("Cтатус эпика DONE рассчитывается на основе статусов его подзадач")
    void calculateEpicStatus_shouldBeDONE_allSubtasksInStatusDONE() {
        Epic epic = taskManager.createEpic(new Epic("name", "desr"));

        taskManager.createSubtask(new Subtask(TaskStatus.DONE, "name", "descr", 8));
        taskManager.createSubtask(new Subtask(TaskStatus.DONE, "name", "descr", 8));
        assertEquals(TaskStatus.DONE, epic.getStatus(), "Эпик со всеми подзадачами в статусе DONE имеет статус, отличный от DONE");
    }

    @Test
    @DisplayName("Cтатус эпика IN_PROGRESS рассчитывается на основе статусов его подзадач")
    void calculateEpicStatus_shouldBeIN_PROGRESS_allSubtasksInStatusIN_PROGRESSOrDONEandNEWorIN_PROGRESSandNEWorIN_PROGRESSandDONE() {
        Epic epic = taskManager.createEpic(new Epic("name", "desr"));

        taskManager.createSubtask(new Subtask(TaskStatus.IN_PROGRESS, "name", "descr", 8));
        taskManager.createSubtask(new Subtask(TaskStatus.IN_PROGRESS, "name", "descr", 8));
        assertEquals(TaskStatus.IN_PROGRESS, epic.getStatus(), "Эпик со всеми подзадачами в статусе IN_PROGRESS имеет статус, отличный от IN_PROGRESS");

        taskManager.removeSubtask(13);
        taskManager.removeSubtask(14);

        taskManager.createSubtask(new Subtask(TaskStatus.DONE, "name", "descr", 8));
        taskManager.createSubtask(new Subtask(TaskStatus.NEW, "name", "descr", 8));
        assertEquals(TaskStatus.IN_PROGRESS, epic.getStatus(), "Эпик с подзадачей в статусе DONE и NEW имеет статус, отличный от IN_PROGRESS");

        taskManager.removeSubtask(15);
        taskManager.removeSubtask(16);

        taskManager.createSubtask(new Subtask(TaskStatus.IN_PROGRESS, "name", "descr", 8));
        taskManager.createSubtask(new Subtask(TaskStatus.NEW, "name", "descr", 8));
        assertEquals(TaskStatus.IN_PROGRESS, epic.getStatus(), "Эпик с подзадачей в статусе IN_PROGRESS и NEW имеет статус, отличный от IN_PROGRESS");

        taskManager.removeSubtask(17);
        taskManager.removeSubtask(18);

        taskManager.createSubtask(new Subtask(TaskStatus.IN_PROGRESS, "name", "descr", 8));
        taskManager.createSubtask(new Subtask(TaskStatus.DONE, "name", "descr", 8));
        assertEquals(TaskStatus.IN_PROGRESS, epic.getStatus(), "Эпик с подзадачей в статусе IN_PROGRESS и DONE имеет статус, отличный от IN_PROGRESS");
    }

    @Test
    @DisplayName("Состояние добавленной задачи не отличается от состояния добавляемой в менеджер")
    void createTask_stateBeforeAndAfterCreateTaskShouldBeSame() {
        Task task = new Task(TaskStatus.NEW, "name", "descr");
        TaskStatus taskStatus = task.getStatus();
        String taskName = task.getName();
        String taskDescription = task.getDescription();

        Task addedTask = taskManager.createTask(task);

        assertEquals(taskStatus, addedTask.getStatus(), "Статус добавляемой и добавленной задачи изменился при добавлении");
        assertEquals(taskName, addedTask.getName(), "Имя добавляемой и добавленной задачи изменился при добавлении");
        assertEquals(taskDescription, addedTask.getDescription(), "Описание добавляемой и добавленной задачи изменился при добавлении");
    }

    @Test
    @DisplayName("Состояние добавленной подзадачи не отличается от состояния добавляемой в менеджер")
    void createSubtask_stateBeforeAndAfterCreateSubtaskShouldBeSame() {
        Subtask subtask = new Subtask(TaskStatus.NEW, "name", "descr", 3);
        TaskStatus subtaskStatus = subtask.getStatus();
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
    void createEpic_stateBeforeAndAfterCreateEpicShouldBeSame() {
        Epic epic = new Epic("name", "descr");
        String epicName = epic.getName();
        String epicDescription = epic.getDescription();

        Epic addedSubtask = taskManager.createEpic(epic);

        assertEquals(epicName, addedSubtask.getName(), "Имя добавляемой и добавленной задачи изменился при добавлении");
        assertEquals(epicDescription, addedSubtask.getDescription(), "Описание добавляемой и добавленной задачи изменился при добавлении");
    }
}
