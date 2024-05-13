package service.managers.task;

import model.Epic;
import model.Subtask;
import model.Task;

import java.util.List;

public interface TaskManager {
    List<Task> getAllTasks();

    List<Subtask> getAllSubTasks();

    List<Epic> getAllEpics();

    void removeAllTasks();

    void removeAllSubtasks();

    void removeAllEpics();

    Task getTask(int id);

    Subtask getSubtask(int id);

    Epic getEpic(int id);

    Task createTask(Task newTask);

    Subtask createSubtask(Subtask newSubtask);

    Epic createEpic(Epic newEpic);

    Task updateTask(Task newTask);

    Subtask updateSubtask(Subtask newSubtask);

    Epic updateEpic(Epic newEpic);

    void removeTask(int id);

    void removeSubtask(int id);

    void removeEpic(int id);

    List<Subtask> getEpicSubtasks(int id);

    List<Task> getHistory();
}
