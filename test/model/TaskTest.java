package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Task")
class TaskTest {

    @Test
    @DisplayName("Экземпляры класса равны друг другу, если равен их id")
    void equals_InstancesAreEqual_IdsAreEqual() {
        //given
        Task task = new Task(TaskStatus.NEW, "name", "description");
        Task taskExpected = new Task(TaskStatus.NEW, "name", "description");

        //when
        task.setId(1);
        taskExpected.setId(1);

        //then
        assertEquals(taskExpected, task, "Экземпляры класса не равны друг другу");
    }
}
