package service.server.handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.Subtask;
import service.managers.task.TaskManager;

public class SubtaskHandler extends BaseHttpHandler {
    public SubtaskHandler(ExceptionHandler exceptionHandler, Gson gson, TaskManager taskManager) {
        super(exceptionHandler, gson, taskManager);
    }

    @Override
    protected void setupRoutes() {
        registerEndpoint("GET", "/subtasks/{id}", (exchange, params) -> {
            respondWithATask(exchange, Integer.parseInt(params.get("id")), taskManager::getSubtask);
        });

        registerEndpoint("GET", "/subtasks", (exchange, params) -> {
            respondWithATaskList(exchange, taskManager::getAllSubTasks);
        });

        registerEndpoint("POST", "/subtasks", (exchange, params) -> {
            TypeToken<Subtask> typeToken = new TypeToken<>() {
            };
            updateOrAddTask(exchange, typeToken, taskManager::updateSubtask, taskManager::createSubtask);
        });

        registerEndpoint("DELETE", "/subtasks/{id}", (exchange, params) -> {
            deleteTask(exchange, Integer.parseInt(params.get("id")), taskManager::removeSubtask);
        });
    }
}
