package service.server.handler;

import com.google.gson.Gson;
import service.managers.task.TaskManager;

public class PrioritizedHandler extends BaseHttpHandler {
    public PrioritizedHandler(ExceptionHandler exceptionHandler, Gson gson, TaskManager taskManager) {
        super(exceptionHandler, gson, taskManager);
    }

    @Override
    protected void setupRoutes() {
        registerEndpoint("GET", "/prioritized", (exchange, params) -> {
            respondWithATaskList(exchange, taskManager::getPrioritizedTasks);
        });
    }
}
