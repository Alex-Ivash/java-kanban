package model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Subtask")
class SubtaskTest {

    @Test
    @DisplayName("Экземпляры класса равны друг другу, если равен их id")
    void equals_shouldEqualsWithIdenticalIDs() {
        Subtask subtask = new Subtask(TaskStatus.NEW, "name", "description", 0);
        Subtask subtaskExpected = new Subtask(TaskStatus.NEW, "name", "description", 0);

        subtask.setId(1);
        subtaskExpected.setId(1);

        Assertions.assertEquals(subtaskExpected, subtask, "Экземпляры класса не равны друг другу");
    }
}
