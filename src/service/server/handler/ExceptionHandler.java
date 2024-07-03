package service.server.handler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import exception.NotFoundException;
import exception.OverlappingException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class ExceptionHandler {
    Gson gson;

    public ExceptionHandler(Gson gson) {
        this.gson = gson;
    }

    public void handle(HttpExchange exchange, Exception exception) throws IOException {
        switch (exception) {
            case NotFoundException e -> exchange.sendResponseHeaders(404, 0);
            case OverlappingException e -> exchange.sendResponseHeaders(406, 0);
            case JsonParseException e -> exchange.sendResponseHeaders(400, 0);
            default -> exchange.sendResponseHeaders(500, 0);
        }

        sendExceptionData(exchange, exception);
    }

    private void sendExceptionData(HttpExchange exchange, Exception e) {
        String exceptionMessage = e.getMessage();
        List<String> exceptionStackTrace = Arrays.stream(e.getStackTrace())
                .map(StackTraceElement::toString)
                .toList();

        JsonObject jsonExceptionData = new JsonObject();
        JsonArray jsonStackTraceArray = gson.toJsonTree(exceptionStackTrace).getAsJsonArray();

        jsonExceptionData.addProperty("message", exceptionMessage);
        jsonExceptionData.add("stackTrace", jsonStackTraceArray);

        try (PrintWriter exceptionDataPrinter = new PrintWriter(new OutputStreamWriter(exchange.getResponseBody(), StandardCharsets.UTF_8))) {
            exceptionDataPrinter.println(jsonExceptionData);
        }
    }
}
