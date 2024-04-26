package service.managers.history;

import model.Task;

import java.util.LinkedList;
import java.util.List;

public class InMemoryHistoryManager implements HistoryManager {
    private final List<Task> history;
    static final int MAXIMUM_CAPACITY = 10;

    public InMemoryHistoryManager() {
        this.history = new LinkedList<>();
    }

    @Override
    public void add(Task task) {
        if (task == null) {
            return;
        }

        if (history.size() == MAXIMUM_CAPACITY) {
            history.removeFirst();
        }

        history.add(task);
    }

    @Override
    public List<Task> getHistory() {
        return history;
    }
}
