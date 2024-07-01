package service.server.handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Task;
import service.managers.task.TaskManager;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseHttpHandler implements HttpHandler {
    protected ExceptionHandler exceptionHandler;
    protected Gson gson;
    protected TaskManager taskManager;
    protected final Map<String, LinkedHashMap<String, EndpointHandler<HttpExchange, Map<String, String>>>> endPoints = new HashMap<>();

    public BaseHttpHandler(ExceptionHandler exceptionHandler, Gson gson, TaskManager taskManager) {
        this.exceptionHandler = exceptionHandler;
        this.gson = gson;
        this.taskManager = taskManager;
        setupRoutes();
    }

    protected abstract void setupRoutes();

    @Override
    public void handle(HttpExchange exchange) {
        try (exchange) {
            try {
                dispatchRequest(exchange);
            } catch (Exception exception) {
                try {
                    exceptionHandler.handle(exchange, exception);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected final void sendResponse(HttpExchange exchange, int statusCode, int responseLength, String responseBody) throws IOException {
        try (PrintWriter bodyPrinter = new PrintWriter(new OutputStreamWriter(exchange.getResponseBody(), StandardCharsets.UTF_8))) {
            exchange.sendResponseHeaders(statusCode, responseLength);

            if (responseLength >= 0) {
                bodyPrinter.println(responseBody);
            }
        }
    }

    private void dispatchRequest(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        LinkedHashMap<String, EndpointHandler<HttpExchange, Map<String, String>>> methodHandlers = endPoints.get(requestMethod);

        if (methodHandlers == null) {
            String allowedMethods = String.join(",", endPoints.keySet());
            exchange.getResponseHeaders().add("Allow", allowedMethods);
            sendResponse(exchange, 405, -1, "");
            return;
        }

        handleEndpoints(exchange, endPoints.get(requestMethod));
    }

    protected void registerEndpoint(String method, String path, EndpointHandler<HttpExchange, Map<String, String>> handler) {
        endPoints.computeIfAbsent(method, m -> new LinkedHashMap<>()).put(path, handler);
    }

    protected void handleEndpoints(HttpExchange exchange, Map<String, EndpointHandler<HttpExchange, Map<String, String>>> routes) throws IOException {
        String path = exchange.getRequestURI().toString();

        for (var entry : routes.entrySet()) {
            String pathTemplate = entry.getKey();
            EndpointHandler<HttpExchange, Map<String, String>> handler = entry.getValue();

            Matcher m = Pattern.compile("\\{([^/{}]+)}").matcher(pathTemplate);
            StringBuilder pathTemplateToRegex = new StringBuilder();

            while (m.find()) {
                String paramName = m.group(1);
                m.appendReplacement(pathTemplateToRegex, "(?<" + paramName + ">[^/]+)");
            }

            m.appendTail(pathTemplateToRegex);

            if (pathTemplateToRegex.isEmpty()) {
                pathTemplateToRegex.append(pathTemplate);
            }

            Matcher matcher = Pattern.compile(pathTemplateToRegex + "/?$").matcher(path);

            if (matcher.matches()) {
                Map<String, String> requestPathParameters = new HashMap<>();

                for (String groupName : matcher.namedGroups().keySet()) {
                    requestPathParameters.put(groupName, matcher.group(groupName));
                }

                handler.accept(exchange, requestPathParameters);
                return;
            }
        }

        sendResponse(exchange, 404, -1, "");
    }

    // Наверное, подход с использованием нижеследующих обобщенных методов сильно затрудняет читабельность. Таким образом я хотел вынести общую логику обработки в одно место
    // До того, как я решил так сделать, логика обработки по каждому эндпойнту передавалась в лямбде handler в каждом отдельном хендлере, возможно, так и стоило оставить.

    protected <T extends Task> void respondWithATaskList(HttpExchange exchange,
                                                         Supplier<List<T>> tasksListSupplier) throws IOException {
        String responseBody = gson.toJson(tasksListSupplier.get());
        sendResponse(exchange, 200, 0, responseBody);
    }

    protected <T extends Task> void respondWithATaskList(HttpExchange exchange,
                                                         Integer containerId,
                                                         Function<Integer, List<T>> tasksListSupplier) throws IOException {
        String responseBody = gson.toJson(tasksListSupplier.apply(containerId));
        sendResponse(exchange, 200, 0, responseBody);
    }

    protected void respondWithATask(HttpExchange exchange,
                                    Integer taskId,
                                    Function<Integer, ? extends Task> taskSupplier) throws IOException {
        String responseBody = gson.toJson(taskSupplier.apply(taskId));
        sendResponse(exchange, 200, 0, responseBody);
    }

    protected <T extends Task> void updateOrAddTask(HttpExchange exchange,
                                                    TypeToken<T> taskToken,
                                                    Function<T, T> taskUpdater,
                                                    Function<T, T> taskCreator) throws IOException {
        String requestBody = new String(exchange.getRequestBody().readAllBytes());
        T task = gson.fromJson(requestBody, taskToken);

        if (task.getId() == null) {
            taskCreator.apply(task);
        } else {
            taskUpdater.apply(task);
        }

        sendResponse(exchange, 201, -1, "");
    }

    protected void deleteTask(HttpExchange exchange,
                              Integer taskId,
                              Consumer<Integer> taskRemover) throws IOException {
        taskRemover.accept(taskId);

        sendResponse(exchange, 204, -1, "");
    }
}
