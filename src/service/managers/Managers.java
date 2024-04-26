package service.managers;

import service.managers.history.HistoryManager;
import service.managers.history.InMemoryHistoryManager;
import service.managers.task.InMemoryTaskManager;
import service.managers.task.TaskManager;

public class Managers {
    public static TaskManager getDefault() {
        return new InMemoryTaskManager(getDefaultHistory());
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}