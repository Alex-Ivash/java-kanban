package service.managers.history;

import model.Epic;
import model.Status;
import model.Subtask;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.managers.Managers;
import service.managers.task.TaskManager;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryHistoryManager")
class InMemoryHistoryManagerTest {
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
    @DisplayName("Задачи типа Task могут быть добавлены в историю")
    void add_taskCanBeAddedToTheHistory() {
        var task1 = taskManager.getTask(0);
        assertEquals(1, taskManager.getHistory().size(), "При попытке добавления одной задачи в историю список не вырос");

        var task2 = taskManager.getTask(1);
        var task3 = taskManager.getTask(2);
        assertEquals(3, taskManager.getHistory().size(), "Длина истории не соответствует кол-ву уникальных просмотров");

        assertEquals(task1, taskManager.getHistory().get(0), "Первая задача в истории не соответствует действительно просмотренной");
        assertEquals(task2, taskManager.getHistory().get(1), "Вторая задача в истории не соответствует действительно просмотренной");
        assertEquals(task3, taskManager.getHistory().get(2), "Третья задача в истории не соответствует действительно просмотренной");
    }

    @Test
    @DisplayName("Задачи типа Subtask могут быть добавлены в историю")
    void add_subtaskCanBeAddedToTheHistory() {
        var task1 = taskManager.getSubtask(5);
        assertEquals(1, taskManager.getHistory().size(), "При попытке добавления одной задачи в историю список не вырос");

        var task2 = taskManager.getSubtask(6);
        var task3 = taskManager.getSubtask(7);
        assertEquals(3, taskManager.getHistory().size(), "Длина истории не соответствует кол-ву уникальных просмотров");

        assertEquals(task1, taskManager.getHistory().get(0), "Первая задача в истории не соответствует действительно просмотренной");
        assertEquals(task2, taskManager.getHistory().get(1), "Вторая задача в истории не соответствует действительно просмотренной");
        assertEquals(task3, taskManager.getHistory().get(2), "Третья задача в истории не соответствует действительно просмотренной");
    }

    @Test
    @DisplayName("Задачи типа Epic могут быть добавлены в историю")
    void add_epicCanBeAddedToTheHistory() {
        var task1 = taskManager.getEpic(3);
        assertEquals(1, taskManager.getHistory().size(), "При попытке добавления одной задачи в историю список не вырос");

        var task2 = taskManager.getEpic(4);
        assertEquals(2, taskManager.getHistory().size(), "Длина истории не соответствует кол-ву уникальных просмотров");

        assertEquals(task1, taskManager.getHistory().get(0), "Первая задача в истории не соответствует действительно просмотренной");
        assertEquals(task2, taskManager.getHistory().get(1), "Вторая задача в истории не соответствует действительно просмотренной");
    }

    @Test
    @DisplayName("Задачи типа Task, Subtask и Epic могут быть совместно добавлены в историю")
    void add_taskAndSubtaskAndEpicCanBeAddedToTheHistory() {
        var task1 = taskManager.getTask(0);
        assertEquals(1, taskManager.getHistory().size(), "При попытке добавления одной задачи в историю список не вырос");

        var task2 = taskManager.getSubtask(5);
        var task3 = taskManager.getEpic(3);
        assertEquals(3, taskManager.getHistory().size(), "Длина истории не соответствует кол-ву уникальных просмотров");

        assertEquals(task1, taskManager.getHistory().get(0), "Первая задача в истории не соответствует действительно просмотренной");
        assertEquals(task2, taskManager.getHistory().get(1), "Вторая задача в истории не соответствует действительно просмотренной");
        assertEquals(task3, taskManager.getHistory().get(2), "Третья задача в истории не соответствует действительно просмотренной");
    }

    @Test
    @DisplayName("При повторном просмотре удаляется существующий в истории просмотр")
    void add_whenViewAgainExistingViewInTheHistoryIsDeleted() {
        taskManager.getTask(0);
        taskManager.getTask(0);
        taskManager.getTask(1);
        assertEquals(2, taskManager.getHistory().size(), "При повторном просмотре старая запись в истории не удаляется с головы");

        taskManager.getTask(1);
        assertEquals(2, taskManager.getHistory().size(), "При повторном просмотре старая запись в истории не удаляется с хвоста");

        taskManager.getTask(2);
        taskManager.getTask(1);
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
        taskManager.getTask(0);
        taskManager.getSubtask(5);
        taskManager.getSubtask(5);
        taskManager.getEpic(3);
        taskManager.getEpic(3);

        var history = taskManager.getHistory();

        assertEquals(Task.class, history.get(0).getClass(), "Ожидался Task");
        assertEquals(Subtask.class, history.get(1).getClass(), "Ожидался Subtask");
        assertEquals(Epic.class, history.get(2).getClass(), "Ожидался Epic");
    }

    @Test
    @DisplayName("При удалении задачи типа Task из TaskManager она так же удаляется и из истории")
    void remove_deletingTaskFromTaskManagerDeletesTaskInTheHistoryAsWell() {
        taskManager.getTask(0);
        taskManager.removeTask(0);
        assertEquals(0, taskManager.getHistory().size(), "После удаления единственной задачи, попавшей в историю история не пустеет");

        taskManager.getTask(1);
        taskManager.getTask(2);
        taskManager.removeTask(1);
        assertEquals(1, taskManager.getHistory().size(), "После удаления последней задачи из двух попавших в историю длина истории больше 1");
    }

    @Test
    @DisplayName("При удалении задачи типа Subtask из TaskManager она так же удаляется и из истории")
    void remove_deletingSubtaskFromTaskManagerDeletesTaskInTheHistoryAsWell() {
        taskManager.getSubtask(5);
        taskManager.removeSubtask(5);
        assertEquals(0, taskManager.getHistory().size(), "После удаления единственной задачи, попавшей в историю история не пустеет");

        taskManager.getSubtask(6);
        taskManager.getSubtask(7);
        taskManager.removeSubtask(7);
        assertEquals(1, taskManager.getHistory().size(), "После удаления последней задачи из двух попавших в историю длина истории больше 1");
    }

    @Test
    @DisplayName("При удалении задачи типа Epic из TaskManager он так же удаляется и из истории")
    void remove_deletingEpicFromTaskManagerDeletesTaskInTheHistoryAsWell() {
        taskManager.getEpic(3);
        taskManager.removeEpic(3);
        assertEquals(0, taskManager.getHistory().size(), "После удаления единственной задачи, попавшей в историю история не пустеет");
    }

    @Test
    @DisplayName("При удалении задачи типа Epic из TaskManager он так же удаляется и из истории вместе со своими подзадачами")
    void remove_deletingEpicFromTaskManagerDeletesTaskInTheHistoryAsWellAlongWithTheSubtasks() {
        taskManager.getEpic(3);
        taskManager.getSubtask(5);
        taskManager.getSubtask(6);
        taskManager.getSubtask(7);
        taskManager.removeEpic(3);
        assertNotEquals(0, taskManager.getHistory().size(), "Удаление эпика приводит к удалению не только его собственных подзадач");
        assertEquals(1, taskManager.getHistory().size(), "После удаления эпика его подзадачи не удалились из истории");
    }

    @Test
    @DisplayName("При удалении всех задач типа Task из TaskManager они так же удаляются и из истории")
    void remove_whenDeleteAllTasksOfTheTaskTypeFromTheTaskManagerTheyAreAlsoDeletedFromTheHistory() {
        taskManager.getTask(0);
        taskManager.getTask(0);
        taskManager.getTask(1);
        taskManager.getSubtask(5);
        taskManager.getEpic(3);

        taskManager.removeAllTasks();
        assertNotEquals(0, taskManager.getHistory().size(), "Удаление всех задач типа Task приводит к удалению задач всех типов из истории");
        assertEquals(2, taskManager.getHistory().size(), "Удаление всех задач типа Task не приводит к удалению всех Task из истории");
    }

    @Test
    @DisplayName("При удалении всех задач типа Subtask из TaskManager они так же удаляются и из истории")
    void remove_whenDeleteAllTasksOfTheSubtaskTypeFromTheTaskManagerTheyAreAlsoDeletedFromTheHistory() {
        taskManager.getSubtask(5);
        taskManager.getSubtask(6);
        taskManager.getSubtask(7);
        taskManager.getTask(1);
        taskManager.getTask(2);

        taskManager.removeAllSubtasks();
        assertNotEquals(0, taskManager.getHistory().size(), "Удаление всех задач типа Subtask приводит к удалению задач всех типов из истории");
        assertEquals(2, taskManager.getHistory().size(), "Удаление всех задач типа Subtask не приводит к удалению всех Subtask из истории");
    }

    @Test
    @DisplayName("При удалении всех задач типа Epic из TaskManager они так же удаляются и из истории")
    void remove_whenDeleteAllTasksOfTheEpicTypeFromTheTaskManagerTheyAreAlsoDeletedFromTheHistory() {
        taskManager.getEpic(3);
        taskManager.getEpic(4);
        taskManager.getTask(1);
        taskManager.getTask(2);

        taskManager.removeAllEpics();
        assertNotEquals(0, taskManager.getHistory().size(), "Удаление всех задач типа Subtask приводит к удалению задач всех типов из истории");
        assertEquals(2, taskManager.getHistory().size(), "Удаление всех задач типа Subtask не приводит к удалению всех Subtask из истории");
    }
}
