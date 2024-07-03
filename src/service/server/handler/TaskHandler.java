package service.server.handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.Task;
import service.managers.task.TaskManager;

public class TaskHandler extends BaseHttpHandler {
    public TaskHandler(ExceptionHandler exceptionHandler, Gson gson, TaskManager taskManager) {
        super(exceptionHandler, gson, taskManager);
    }

    @Override
    protected void setupRoutes() {
        registerEndpoint("GET", "/tasks/{id}", (exchange, params) -> {
            respondWithATask(exchange, Integer.parseInt(params.get("id")), taskManager::getTask);
        });

        registerEndpoint("GET", "/tasks", (exchange, params) -> {
            respondWithATaskList(exchange, taskManager::getAllTasks);
        });

        registerEndpoint("POST", "/tasks", (exchange, params) -> {
            TypeToken<Task> typeToken = new TypeToken<>() {
            };
            updateOrAddTask(exchange, typeToken, taskManager::updateTask, taskManager::createTask);
        });

        registerEndpoint("DELETE", "/tasks/{id}", (exchange, params) -> {
            deleteTask(exchange, Integer.parseInt(params.get("id")), taskManager::removeTask);
        });
    }
}
