package service.managers.task;

import model.Epic;
import model.Subtask;
import model.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import service.managers.Managers;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("InMemoryTaskManager")
class InMemoryTaskManagerTest extends TaskManagerTest<InMemoryTaskManager> {

    @Override
    void initTaskManager() {
        taskManager = new InMemoryTaskManager(Managers.getDefaultHistory());
    }

    static Stream<Arguments> taskStatusProvider() {
        return Stream.of(
                Arguments.of(List.of(), TaskStatus.NEW),
                Arguments.of(List.of(TaskStatus.DONE, TaskStatus.DONE), TaskStatus.DONE),
                Arguments.of(List.of(TaskStatus.IN_PROGRESS, TaskStatus.IN_PROGRESS), TaskStatus.IN_PROGRESS),
                Arguments.of(List.of(TaskStatus.IN_PROGRESS, TaskStatus.DONE), TaskStatus.IN_PROGRESS),
                Arguments.of(List.of(TaskStatus.NEW, TaskStatus.NEW), TaskStatus.NEW),
                Arguments.of(List.of(TaskStatus.NEW, TaskStatus.DONE), TaskStatus.IN_PROGRESS),
                Arguments.of(List.of(TaskStatus.NEW, TaskStatus.IN_PROGRESS), TaskStatus.IN_PROGRESS),
                Arguments.of(List.of(TaskStatus.NEW, TaskStatus.DONE, TaskStatus.IN_PROGRESS), TaskStatus.IN_PROGRESS)
        );
    }

    @ParameterizedTest(name = "Эпик с подзадачами в статусах {0} имеет статус {1}")
    @MethodSource("taskStatusProvider")
    @DisplayName("Cтатус эпика рассчитывается на основе статусов его подзадач")
    void calculateEpicStatus_epicStatusCalculatedBasedOnTheStatusesOfItsSubtasks(List<TaskStatus> subtaskStatuses, TaskStatus expectedStatus) {
        Epic epic = taskManager.createEpic(new Epic("name", "desr"));

        subtaskStatuses.forEach(taskStatus -> taskManager.createSubtask(new Subtask(taskStatus, "name", "descr", epic.getId())));

        assertEquals(
                expectedStatus, epic.getStatus(),
                String.format("Эпик с подзадачами в статусах %s имеет статус, отличный от %s", subtaskStatuses, expectedStatus)
        );
    }
}
