package service;

import model.Status;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("InMemoryHistoryManager")
class InMemoryHistoryManagerTest {
    private static Task[] tasks;
    private static InMemoryHistoryManager inMemoryHistoryManager;

    @BeforeEach
    void init() {
        inMemoryHistoryManager = new InMemoryHistoryManager();
        tasks = new Task[InMemoryHistoryManager.MAXIMUM_CAPACITY - 1];

        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = new Task(Status.NEW, "name", "description");
            tasks[i].setId(i);
            inMemoryHistoryManager.add(tasks[i]);
        }
    }

    @Test
    @DisplayName("В историю можно добавить "+ InMemoryHistoryManager.MAXIMUM_CAPACITY +" задач")
    void addMaxCapacityTasks() {
        inMemoryHistoryManager.add(new Task(Status.NEW, "name", "description"));

        assertEquals(InMemoryHistoryManager.MAXIMUM_CAPACITY, inMemoryHistoryManager.getHistory().size());
    }

    @Test
    @DisplayName("При добавлении "+ (InMemoryHistoryManager.MAXIMUM_CAPACITY + 1) +"-й задачи удаляется самая первая задачи из истории")
    void addMoreMaxCapacityTasks() {
        Task task1 = new Task(Status.NEW, "name", "description");
        Task task2 = new Task(Status.NEW, "name", "description");

        task1.setId(9);
        task2.setId(10);

        inMemoryHistoryManager.add(task1);
        inMemoryHistoryManager.add(task2);

        assertEquals(
                1,
                inMemoryHistoryManager.getHistory().getFirst().getId(),
                "При добавлении "+ (InMemoryHistoryManager.MAXIMUM_CAPACITY + 1) +"-й задачи самая первая задача не удаляется"
        );
    }
}
