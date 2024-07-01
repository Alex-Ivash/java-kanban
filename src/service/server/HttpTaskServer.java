package service.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpServer;
import converter.DurationAdapter;
import converter.LocalDateTimeAdapter;
import service.managers.Managers;
import service.managers.task.TaskManager;
import service.server.handler.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;

public class HttpTaskServer {
    private final int port = 8080;
    private final HttpServer httpServer;
    private final TaskManager taskManager;
    private final ExceptionHandler exceptionHandler;
    private final Gson gson;

    public HttpTaskServer(TaskManager taskManager) {
        this.taskManager = taskManager;
        this.gson = HttpTaskServer.getGson();
        this.exceptionHandler = new ExceptionHandler(gson);

        try {
            this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);

            httpServer.createContext("/tasks", new TaskHandler(exceptionHandler, gson, taskManager));
            httpServer.createContext("/subtasks", new SubtaskHandler(exceptionHandler, gson, taskManager));
            httpServer.createContext("/epics", new EpicHandler(exceptionHandler, gson, taskManager));
            httpServer.createContext("/history", new HistoryHandler(exceptionHandler, gson, taskManager));
            httpServer.createContext("/prioritized", new PrioritizedHandler(exceptionHandler, gson, taskManager));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpTaskServer() {
        this(Managers.getDefault());
    }

    public static void main(String[] args) {
        HttpTaskServer server = new HttpTaskServer();
        server.start();
    }

    public static Gson getGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .create();
    }

    public void start() {
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(0);
    }
}
