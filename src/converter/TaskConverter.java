package converter;

import model.*;

import java.time.Duration;
import java.time.LocalDateTime;

public class TaskConverter { // TODO добавить в будущем в тесты
    private static final String DELIMITER = ",";

    public static String toString(Task task) {
        return String.join(DELIMITER,
                String.valueOf(task.getId()),
                String.valueOf(task.getType()),
                task.getName(),
                String.valueOf(task.getStatus()),
                task.getDescription(),
                "null",
                String.valueOf(task.getStartTime()),
                String.valueOf(task.getDuration()));
    }

    public static String toString(Subtask task) {
        return toString((Task) task).replaceFirst("null", String.valueOf(task.getEpicId()));
    }

    public static Task fromString(String taskString) {
        String[] taskFields = taskString.split(DELIMITER);

        TaskType type = TaskType.valueOf(taskFields[1]);

        int id = Integer.parseInt(taskFields[0]);
        TaskStatus status = TaskStatus.valueOf(taskFields[3]);
        String name = taskFields[2];
        String description = taskFields[4];
        LocalDateTime startTime = LocalDateTime.parse(taskFields[6]);
        Duration duration = Duration.parse(taskFields[7]);

        return switch (type) {
            case TASK -> new Task(id, status, name, description, startTime, duration);
            case SUBTASK -> new Subtask(id, status, name, description, Integer.parseInt(taskFields[5]), startTime, duration);
            case EPIC -> new Epic(id, status, name, description, startTime, duration);
        };
    }
}
