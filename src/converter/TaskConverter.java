package converter;

import model.*;

public class TaskConverter {
    private static final String DELIMITER = ",";
    public static String toString(Task task) {
        return String.join(DELIMITER,
                String.valueOf(task.getId()),
                String.valueOf(task.getType()),
                task.getName(),
                String.valueOf(task.getStatus()),
                task.getDescription(),
                String.valueOf(task.getEpicId()));
    }

    public static Task fromString(String taskString) {
        String[] taskFields = taskString.split(DELIMITER);

        Type type = Type.valueOf(taskFields[1]);

        int id = Integer.parseInt(taskFields[0]);
        Status status = Status.valueOf(taskFields[3]);
        String name = taskFields[2];
        String description = taskFields[4];

        return switch (type) {
            case TASK -> new Task(id, status, name, description);
            case SUBTASK -> new Subtask(id, status, name, description, Integer.parseInt(taskFields[5]));
            case EPIC -> new Epic(id, status, name, description);
        };
    }
}
