package service.managers.memory;

import converter.TaskConverter;
import model.Epic;
import model.Subtask;
import model.Task;
import service.managers.Managers;
import service.managers.history.HistoryManager;
import service.managers.task.InMemoryTaskManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private Path storageCSV;
    private static final String DEFAULT_CSV_FILE = "resources/tasks.csv";
    public FileBackedTaskManager(HistoryManager historyManager, String file) {
        super(historyManager);
        this.storageCSV = Path.of(file);
    }

    private FileBackedTaskManager(String file) {
        this(Managers.getDefaultHistory(), file);
    }

    public FileBackedTaskManager(HistoryManager historyManager) {
        this(historyManager, DEFAULT_CSV_FILE);
    }

    public static FileBackedTaskManager loadFromFile(String file) {
        FileBackedTaskManager fileBackedTaskManager = new FileBackedTaskManager(file);
        fileBackedTaskManager.loadFromFile();

        return fileBackedTaskManager;
    }

    private void loadFromFile() {
        try {
            List<String> taskStrings = Files.readAllLines(storageCSV);

            for (int i = 1; i < taskStrings.size(); i++) {
                String tsk = taskStrings.get(i);
                Task loadedTask = TaskConverter.fromString(tsk);

                switch (loadedTask) {
                    case Subtask subtask -> subtasks.put(subtask.getId(), subtask);
                    case Epic epic -> epics.put(epic.getId(), epic);
                    case Task task -> tasks.put(task.getId(), task);
                }

                seq = Math.max(seq, loadedTask.getId());
            }

            subtasks.forEach((subtaskId, subtask) -> epics.get(subtask.getEpicId()).addSubtask(subtaskId));
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
                throw new RuntimeException(e);
            }
        }

        try (PrintWriter printer = new PrintWriter(storageCSV.toFile(), StandardCharsets.UTF_8)) {
            printer.println("id,type,name,status,description,epic");
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
