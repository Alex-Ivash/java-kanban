package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Epic")
class EpicTest {
    private static Epic epic;
    private static Epic anotherEpic;

    @BeforeEach
    public void init() {
        epic = new Epic("name", "description");
        anotherEpic = new Epic("name", "description");
        epic.setId(0);
    }

    @Test
    @DisplayName("Экземпляры класса равны друг другу, если равен их id")
    void shouldEqualsWithIdenticalIDs() {
        anotherEpic.setId(0);

        assertEquals(anotherEpic, epic, "Экземпляры класса не равны друг другу");
    }

    @Test
    @DisplayName("Можно добавить подзадачу в пустой эпик(без подзадач)")
    void shouldAddSubtaskWhereEpicIsEmpty() {
        Subtask subtask = new Subtask(Status.NEW, "name", "description", 0);
        subtask.setId(1);

        epic.addSubtask(subtask.getId());

        assertEquals(1, epic.getSubtasksIds().size(), "Не добавляется подзадача в пустой эпик");
    }

    @Test
    @DisplayName("Можно добавить подзадачу в непустой эпик(уже есть подзадачи)")
    void shouldAddSubtaskWhereEpicIsNotEmpty() {
        Subtask subtask = new Subtask(Status.NEW, "name", "description", 0);
        subtask.setId(1);
        Subtask subtask2 = new Subtask(Status.NEW, "name", "description", 0);
        subtask2.setId(2);

        epic.addSubtask(subtask.getId());
        epic.addSubtask(subtask2.getId());

        assertEquals(2, epic.getSubtasksIds().size(), "Не добавляется подзадача в непустой эпик");
    }

    @Test
    @DisplayName("Можно удалить подзадачу из эпика")
    void removeSubtask() {
        Subtask subtask = new Subtask(Status.NEW, "name", "description", 0);
        subtask.setId(1);

        epic.addSubtask(subtask.getId());
        epic.removeSubtask(subtask.getId());

        assertTrue(epic.getSubtasksIds().isEmpty(), "Не удаляется подзадача из эпика");
    }

    @Test
    @DisplayName("Эпик содержит все подзадачи, ранее добавленные в него")
    void getSubtasksIds() {
        Subtask subtask = new Subtask(Status.NEW, "name", "description", 0);
        subtask.setId(1);
        Subtask subtask2 = new Subtask(Status.NEW, "name", "description", 0);
        subtask2.setId(2);
        Subtask subtask3 = new Subtask(Status.NEW, "name", "description", 0);
        subtask3.setId(3);

        epic.addSubtask(subtask.getId());
        epic.addSubtask(subtask2.getId());
        epic.addSubtask(subtask3.getId());

        assertEquals(3, epic.getSubtasksIds().size());
    }
}
