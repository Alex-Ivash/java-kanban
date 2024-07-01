package service.server.handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.Epic;
import service.managers.task.TaskManager;

public class EpicHandler extends BaseHttpHandler {
    public EpicHandler(ExceptionHandler exceptionHandler, Gson gson, TaskManager taskManager) {
        super(exceptionHandler, gson, taskManager);
    }

    @Override
    protected void setupRoutes() {
        registerEndpoint("GET", "/epics/{id}/subtasks", (exchange, params) -> {
            respondWithATaskList(exchange, Integer.parseInt(params.get("id")), taskManager::getEpicSubtasks);
        });

        registerEndpoint("GET", "/epics/{id}", (exchange, params) -> {
            respondWithATask(exchange, Integer.parseInt(params.get("id")), taskManager::getEpic);
        });

        registerEndpoint("GET", "/epics", (exchange, params) -> {
            respondWithATaskList(exchange, taskManager::getAllEpics);
        });

        registerEndpoint("POST", "/epics", (exchange, params) -> {
            TypeToken<Epic> typeToken = new TypeToken<>() {
            };
            updateOrAddTask(exchange, typeToken, taskManager::updateEpic, taskManager::createEpic);
        });

        registerEndpoint("DELETE", "/epics/{id}", (exchange, params) -> {
            deleteTask(exchange, Integer.parseInt(params.get("id")), taskManager::removeEpic);
        });
    }
}
