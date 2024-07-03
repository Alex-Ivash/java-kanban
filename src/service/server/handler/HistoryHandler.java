package service.server.handler;

import com.google.gson.Gson;
import service.managers.task.TaskManager;

public class HistoryHandler extends BaseHttpHandler {
    public HistoryHandler(ExceptionHandler exceptionHandler, Gson gson, TaskManager taskManager) {
        super(exceptionHandler, gson, taskManager);
    }

    @Override
    protected void setupRoutes() {
        registerEndpoint("GET", "/history", (exchange, params) -> {
            respondWithATaskList(exchange, taskManager::getHistory);
        });
    }
}
