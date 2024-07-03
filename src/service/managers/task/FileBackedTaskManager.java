package service.managers.task;

import converter.TaskConverter;
import exception.ManagerLoadException;
import exception.ManagerSaveException;
import model.Epic;
import model.Subtask;
import model.Task;
import service.managers.history.HistoryManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private Path storageCSV;
    public static final String DEFAULT_CSV_FILE = "resources/tasks.csv";

    public FileBackedTaskManager(HistoryManager historyManager, String file) {
        super(historyManager);
        this.storageCSV = Path.of(file);
    }

    public FileBackedTaskManager(HistoryManager historyManager) {
        this(historyManager, DEFAULT_CSV_FILE);
    }

    public static FileBackedTaskManager loadFromFile(HistoryManager historyManager, String file) {
        FileBackedTaskManager fileBackedTaskManager = new FileBackedTaskManager(historyManager, file);
        fileBackedTaskManager.loadFromFile();

        return fileBackedTaskManager;
    }

    private void loadFromFile() {
        try {
            List<String> taskStrings = Files.readAllLines(storageCSV);

            taskStrings.stream()
                    .skip(1)
                    .forEach(tsk -> {
                        Task loadedTask = TaskConverter.fromString(tsk);

                        switch (loadedTask) {
                            case Subtask subtask -> {
                                subtasks.put(subtask.getId(), subtask);
                                prioritizedTasks.add(subtask);
                            }
                            case Epic epic -> epics.put(epic.getId(), epic);
                            case Task task -> {
                                tasks.put(task.getId(), task);
                                prioritizedTasks.add(task);
                            }
                        }

                        seq = Math.max(seq, loadedTask.getId());
                    });

            subtasks.forEach((subtaskId, subtask) -> epics.get(subtask.getEpicId()).addSubtask(subtaskId));
            epics.forEach((epicId, epic) -> calculateEpicState(epic)); // Оказалось, что у меня не восстанавливалось время окончания эпика. И это все ради восстановления времени окончания эпика. Наверное стоит просто хранить его в csv
        } catch (IOException e) {
            throw new ManagerLoadException(e.getMessage());
        }
    }

    private void save() {
        Path parentPathToStorage = storageCSV.getParent();

        if (parentPathToStorage != null) {
            try {
                Files.createDirectories(parentPathToStorage);
            } catch (IOException e) {
                throw new ManagerSaveException(e.getMessage());
            }
        }

        try (PrintWriter printer = new PrintWriter(storageCSV.toFile(), StandardCharsets.UTF_8)) {
            printer.println("id,type,name,status,description,epic,startTime,duration");
            getAllTasks().forEach(task -> printer.println(TaskConverter.toString(task)));
            getAllSubTasks().forEach(subtask -> printer.println(TaskConverter.toString(subtask)));
            getAllEpics().forEach(epic -> printer.println(TaskConverter.toString(epic)));

            if (printer.checkError()) {
                System.err.println("An error occurred during writing tasks in storage.");
            }
        } catch (IOException e) {
            throw new ManagerSaveException(e.getMessage());
        }
    }

    @Override
    public void removeAllTasks() {
        super.removeAllTasks();
        save();
    }

    @Override
    public void removeAllSubtasks() {
        super.removeAllSubtasks();
        save();
    }

    @Override
    public void removeAllEpics() {
        super.removeAllEpics();
        save();
    }

    @Override
    public Task createTask(Task newTask) {
        Task task = super.createTask(newTask);
        save();

        return task;
    }

    @Override
    public Subtask createSubtask(Subtask newSubtask) {
        Subtask task = super.createSubtask(newSubtask);
        save();

        return task;
    }

    @Override
    public Epic createEpic(Epic newEpic) {
        Epic task = super.createEpic(newEpic);
        save();

        return task;
    }

    @Override
    public Task updateTask(Task newTask) {
        Task task = super.updateTask(newTask);
        save();

        return task;
    }

    @Override
    public Subtask updateSubtask(Subtask newSubtask) {
        Subtask task = super.updateSubtask(newSubtask);
        save();

        return task;
    }

    @Override
    public Epic updateEpic(Epic newEpic) {
        Epic task = super.updateEpic(newEpic);
        save();

        return task;
    }

    @Override
    public void removeTask(int id) {
        super.removeTask(id);
        save();
    }

    @Override
    public void removeSubtask(int id) {
        super.removeSubtask(id);
        save();
    }

    @Override
    public void removeEpic(int id) {
        super.removeEpic(id);
        save();
    }
}
