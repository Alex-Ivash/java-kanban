package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("Epic")
class EpicTest {
    @BeforeEach
    public void init() {

    }

    @Test
    @DisplayName("Экземпляры класса равны друг другу, если равен их id")
    void shouldEqualsWithIdenticalIDs() {
        //given
        Epic epic = new Epic("", "");
        Epic epicExpected = new Epic("", "");

        //when
        epic.setId(1);
        epicExpected.setId(1);

        //then
        assertEquals(epicExpected, epic, "Экземпляры класса не равны друг другу");
    }

    @Test
    @DisplayName("В сабтасках эпика не может быть двух одинаковых id сабтасков")
    void addSubtask_() {
        //given
        Epic epic = new Epic("", "");

        //when
        epic.addSubtask(0);
        epic.addSubtask(0);
        epic.addSubtask(1);

        //then
        assertNotEquals(3, epic.getSubtasksIds().size(), "В подзадачи эпика может быть добавлено несколько одинаковых id");
    }
}
