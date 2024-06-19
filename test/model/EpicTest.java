package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.managers.Managers;
import service.managers.task.InMemoryTaskManager;
import service.managers.task.TaskManager;

import java.util.ArrayList;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Epic")
class EpicTest {
    private static Epic epic;
    private static TaskManager taskManager;
    private static int seq;
    private static ArrayList<Task> testObjectsContainer;

    @BeforeEach
    public void init() {
        seq = -1;
        epic = new Epic(++seq, TaskStatus.NEW, "name", "description", Task.DEFAULT_START_TIME, Task.DEFAULT_DURATION);
        taskManager = new InMemoryTaskManager(Managers.getDefaultHistory());
        testObjectsContainer = new ArrayList<>();
    }

    @Test
    @DisplayName("Экземпляры класса равны друг другу, если равен их id")
    void shouldEqualsWithIdenticalIDs() {
        Epic anotherEpic = new Epic(seq, TaskStatus.NEW, "name", "description", Task.DEFAULT_START_TIME, Task.DEFAULT_DURATION);
        assertEquals(anotherEpic, epic, "Экземпляры класса не равны друг другу");
    }

    @Test
    @DisplayName("Можно добавить подзадачи в изначально пустой эпик")
    void addSubtask_shouldAddSubtask_epicIsNotEmpty() {
        IntStream.range(0, 2).forEach(i -> {
            Subtask subtask = new Subtask(++seq, TaskStatus.NEW, "name", "description", epic.getId(), Task.DEFAULT_START_TIME, Task.DEFAULT_DURATION);
            testObjectsContainer.add(subtask);
            epic.addSubtask(subtask.getId());
        });

        assertEquals(testObjectsContainer.size(), epic.getSubtasksIds().size(), "Не добавляется подзадача в непустой эпик");
    }

    @Test
    @DisplayName("При удалении подзадачи из эпика его методами она не удаляется из менеджера")
    void removeSubtask_subtaskRemovedOnlyFromTheEpicButNotFromTheManager() {
        taskManager.createEpic(epic);
        Subtask subtask = new Subtask(++seq, TaskStatus.NEW, "name", "description", epic.getId(), Task.DEFAULT_START_TIME, Task.DEFAULT_DURATION);
        taskManager.createSubtask(subtask);

        epic.removeSubtask(subtask.getId());

        assertAll(
                () -> assertTrue(epic.getSubtasksIds().isEmpty(), "Не удаляется подзадача из эпика"),
                () -> assertFalse(taskManager.getAllSubTasks().isEmpty(), "Подзадача удалена из менеджера")
        );
    }
}
