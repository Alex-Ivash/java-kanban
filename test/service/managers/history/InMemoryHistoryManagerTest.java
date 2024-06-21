package service.managers.history;

import model.Epic;
import model.Subtask;
import model.Task;
import model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.managers.Managers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryHistoryManager")
class InMemoryHistoryManagerTest {
    private static HistoryManager historyManager;

    @BeforeEach
    void setUp() {
        historyManager = Managers.getDefaultHistory();
    }

    @Test
    @DisplayName("Задачу можно добавить в историю")
    void add_AddedToHistory() {
        //given
        Task task1 = new Task(0, TaskStatus.NEW, "", "", LocalDateTime.now(), Duration.ofDays(1));

        //when
        historyManager.add(task1);

        //then
        assertEquals(task1, historyManager.getHistory().getFirst(), "Задача в историю не добавляется");
    }

    @Test
    @DisplayName("Задачу можно удалить из истории")
    void remove_RemovedFromHistory() {
        //given
        Epic task1 = new Epic(0, TaskStatus.NEW, "", "", LocalDateTime.now(), Duration.ofDays(1));

        //when
        historyManager.remove(0);

        //then
        assertTrue(historyManager.getHistory().isEmpty(), "Задача из истории не удаляется");
    }

    @Test
    @DisplayName("Порядок задач в выводе истории соответствует порядку добавления")
    void getHistory_HistoryOrderMatchesInsertionOrder() {
        //given
        Task task1 = new Task(0, TaskStatus.NEW, "", "", LocalDateTime.now(), Duration.ofDays(1));
        Epic task2 = new Epic(1, TaskStatus.NEW, "", "", task1.getStartTime(), Duration.ofDays(1));
        Subtask task3 = new Subtask(2, TaskStatus.NEW, "", "", 1, task2.getStartTime(), Duration.ofDays(1));

        //when
        historyManager.add(task1);
        historyManager.add(task3);
        historyManager.add(task2);
        List<Task> history = historyManager.getHistory();

        //then
        assertAll("Порядок задач в выводе не соответствует порядку введения задач в историю",
                () -> assertEquals(task1, history.get(0)),
                () -> assertEquals(task3, history.get(1)),
                () -> assertEquals(task2, history.get(2))
        );
    }

    @Test
    @DisplayName("При повторном просмотре удаляется существующий в истории просмотр")
    void add_RemoveExistingFromHistory_Revisit() {
        //given
        Task task1 = new Task(0, TaskStatus.NEW, "", "", LocalDateTime.now(), Duration.ofDays(1));
        Epic task2 = new Epic(1, TaskStatus.NEW, "", "", task1.getStartTime(), Duration.ofDays(1));
        Subtask task3 = new Subtask(2, TaskStatus.NEW, "", "", 1, task2.getStartTime(), Duration.ofDays(1));

        //when
        //then
        historyManager.add(task1);
        historyManager.add(task1);
        historyManager.add(task2);
        assertEquals(2, historyManager.getHistory().size(), "При повторном просмотре старая запись в истории не удаляется с головы");

        historyManager.add(task2);
        assertEquals(2, historyManager.getHistory().size(), "При повторном просмотре старая запись в истории не удаляется с хвоста");

        historyManager.add(task3);
        historyManager.add(task2);
        assertEquals(3, historyManager.getHistory().size(), "При повторном просмотре старая запись в истории не удаляется из середины");
    }
}
