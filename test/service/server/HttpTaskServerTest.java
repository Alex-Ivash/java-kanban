package service.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import exception.NotFoundException;
import exception.OverlappingException;
import model.Epic;
import model.Subtask;
import model.Task;
import model.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.managers.Managers;
import service.managers.task.InMemoryTaskManager;
import service.managers.task.TaskManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HttpTaskServer")
class HttpTaskServerTest {
    private static HttpTaskServer server;
    private static HttpClient client;
    private static TaskManager taskManager;
    private static Gson gson = HttpTaskServer.getGson();
    private static final String SERVER_URI_STRING = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        taskManager = new InMemoryTaskManager(Managers.getDefaultHistory());
        server = new HttpTaskServer(taskManager);
        client = HttpClient.newHttpClient();

        server.start();

        taskManager.removeAllTasks();
        taskManager.removeAllEpics();
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.stop();
    }

    private HttpResponse<String> sendRequest(String method, String path, String body) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URI_STRING + path))
                .method(method, HttpRequest.BodyPublishers.ofString(body)).build();

        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpResponse<String> sendRequest(String method, String path) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URI_STRING + path))
                .method(method, HttpRequest.BodyPublishers.noBody()).build();

        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Догадываюсь, что названия тестов плохие, но, исходя из моей реализации таск сервера и его хендлеров контекста - я не придумал ничего лучше.

    @Test
    @DisplayName("На запрос 'GET /tasks' возвращается список всех Task в формате Json")
    void GETtasks_returnsListOfTasks_onGETRequest() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "task1", "task1");
        Task task2 = new Task(TaskStatus.NEW, "task2", "task2");
        Epic epic = new Epic("epic", "epic");

        taskManager.createTask(task1);
        taskManager.createTask(task2);
        taskManager.createEpic(epic);

        //when
        HttpResponse<String> response = sendRequest("GET", "/tasks");

        //then
        assertAll(
                () -> assertEquals(gson.toJson(taskManager.getAllTasks()), response.body().trim(), "Возвращаемый ответ не совпадает с ожидаемым json"),
                () -> assertEquals(200, response.statusCode(), "Код статуса должен быть 200")
        );
    }

    @Test
    @DisplayName("На запрос 'GET /tasks' при отсутствии задач Task возвращается пустой массив Json")
    void GETtasks_returnsEmptyArray_onNoTasks() {
        //given
        //when
        HttpResponse<String> response = sendRequest("GET", "/tasks");

        //then
        assertAll(
                () -> assertEquals(new JsonArray().toString(), response.body().trim(), "Возвращаемый ответ не совпадает с ожидаемым json"),
                () -> assertEquals(200, response.statusCode(), "Код статуса должен быть 200")
        );
    }

    @Test
    @DisplayName("На запрос 'GET /subtasks' возвращается список всех Subtask в формате Json")
    void GETsubtasks_returnsListOfSubtasks_onGETRequest() {
        //given
        Epic epic = new Epic("epic", "epic");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "task1", "task1", 0);
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "task2", "task2", 0);

        taskManager.createEpic(epic);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        //when
        HttpResponse<String> response = sendRequest("GET", "/subtasks");

        //then
        assertAll(
                () -> assertEquals(gson.toJson(taskManager.getAllSubTasks()), response.body().trim(), "Возвращаемый ответ не совпадает с ожидаемым json"),
                () -> assertEquals(200, response.statusCode(), "Код статуса должен быть 200")
        );
    }

    @Test
    @DisplayName("На запрос 'GET /subtasks' при отсутствии задач Subtask возвращается пустой массив Json")
    void GETsubtasks_returnsEmptyArray_whenNoSubtasks() {
        //given
        //when
        HttpResponse<String> response = sendRequest("GET", "/subtasks");

        //then
        assertAll(
                () -> assertEquals(new JsonArray().toString(), response.body().trim(), "Возвращаемый ответ не совпадает с ожидаемым json"),
                () -> assertEquals(200, response.statusCode(), "Код статуса должен быть 200")
        );
    }

    @Test
    @DisplayName("На запрос 'GET /epics' возвращается список всех Epic в формате Json")
    void GETepics_returnsListOfEpics_onGETRequest() {
        //given
        Epic epic1 = new Epic("epic", "epic");
        Epic epic2 = new Epic("epic", "epic");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "task1", "task1", 0);
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "task2", "task2", 1);

        taskManager.createEpic(epic1);
        taskManager.createEpic(epic2);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        //when
        HttpResponse<String> response = sendRequest("GET", "/epics");

        //then
        assertAll(
                () -> assertEquals(gson.toJson(taskManager.getAllEpics()), response.body().trim(), "Возвращаемый ответ не совпадает с ожидаемым json"),
                () -> assertEquals(200, response.statusCode(), "Код статуса должен быть 200")
        );
    }

    @Test
    @DisplayName("На запрос 'GET /epics' при отсутствии задач Epic возвращается пустой массив Json")
    void GETepics_returnsEmptyArray_whenNoEpicsExist() {
        //given
        //when
        HttpResponse<String> response = sendRequest("GET", "/epics");

        //then
        assertAll(
                () -> assertEquals(new JsonArray().toString(), response.body().trim(), "Возвращаемый ответ не совпадает с ожидаемым json"),
                () -> assertEquals(200, response.statusCode(), "Код статуса должен быть 200")
        );
    }

    @Test
    @DisplayName("На запрос 'GET /history' возвращается список истории в формате Json")
    void GEThistory_returnsHistoryList_inJsonFormat() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "task1", "task1");
        Epic epic1 = new Epic("epic", "epic");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "task1", "task1", 1);

        taskManager.createTask(task1);
        taskManager.createEpic(epic1);
        taskManager.createSubtask(subtask1);

        //when
        taskManager.getSubtask(2);
        taskManager.getTask(0);
        taskManager.getEpic(1);

        HttpResponse<String> response = sendRequest("GET", "/history");

        //then
        assertAll(
                () -> assertEquals(gson.toJson(taskManager.getHistory()), response.body().trim(), "Возвращаемый ответ не совпадает с ожидаемым json"),
                () -> assertEquals(200, response.statusCode(), "Код статуса должен быть 200")
        );
    }

    @Test
    @DisplayName("На запрос 'GET /prioritized' возвращается список истории в формате Json")
    void GETprioritized_returnsHistoryList_inJsonFormat() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "task1", "task1", LocalDateTime.now().minusDays(10), Duration.ofDays(1));
        Epic epic1 = new Epic("epic", "epic");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "task1", "task1", 1, LocalDateTime.now(), Duration.ofDays(1));
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "task1", "task1", 1, LocalDateTime.now().minusDays(7), Duration.ofDays(1));

        taskManager.createTask(task1);
        taskManager.createEpic(epic1);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        //when
        HttpResponse<String> response = sendRequest("GET", "/prioritized");

        //then
        assertAll(
                () -> assertEquals(gson.toJson(taskManager.getPrioritizedTasks()), response.body().trim(), "Возвращаемый ответ не совпадает с ожидаемым json"),
                () -> assertEquals(200, response.statusCode(), "Код статуса должен быть 200")
        );
    }

    @Test
    @DisplayName("На запрос 'GET /tasks/{id}' возвращается Task в формате Json")
    void GETtaskById_returnsTask_inJsonFormat() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "task1", "task1", LocalDateTime.now().minusDays(10), Duration.ofDays(1));
        taskManager.createTask(task1);

        //when
        HttpResponse<String> response = sendRequest("GET", "/tasks/0");

        //then
        assertAll(
                () -> assertEquals(gson.toJson(taskManager.getTask(0)), response.body().trim(), "Возвращаемый ответ не совпадает с ожидаемым json"),
                () -> assertEquals(200, response.statusCode(), "Код статуса должен быть 200")
        );
    }

    @Test
    @DisplayName("На запрос 'GET /tasks/{id}' с id, которому не соответствует ни одна задача, возвращается статус 404 и ошибка в формате Json")
    void GETtaskById_returns404Error_whenTaskNotFound() {
        //given
        //when
        HttpResponse<String> response = sendRequest("GET", "/tasks/0");

        //then
        try {
            taskManager.getTask(0);
        } catch (NotFoundException e) {
            String exceptionMessage = e.getMessage();

            JsonObject exceptionJsonObject = gson.fromJson(response.body(), JsonObject.class);
            assertAll(
                    () -> assertEquals(exceptionMessage, exceptionJsonObject.get("message").getAsString()),
                    () -> assertEquals(404, response.statusCode(), "Код статуса должен быть 404")
            );
        }
    }

    @Test
    @DisplayName("На запрос 'GET /subtasks/{id}' возвращается Subtask в формате Json")
    void GETsubtaskById_returnsSubtask_inJsonFormat() {
        //given
        Epic epic1 = new Epic("epic", "epic");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "task1", "task1", 0, LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createEpic(epic1);
        taskManager.createSubtask(subtask1);

        //when
        HttpResponse<String> response = sendRequest("GET", "/subtasks/1");

        //then
        assertAll(
                () -> assertEquals(gson.toJson(taskManager.getSubtask(1)), response.body().trim(), "Возвращаемый ответ не совпадает с ожидаемым json"),
                () -> assertEquals(200, response.statusCode(), "Код статуса должен быть 200")
        );
    }

    @Test
    @DisplayName("На запрос 'GET /subtasks/{id}' с id, которому не соответствует ни одна задача, возвращается статус 404 и ошибка в формате Json")
    void GETsubtaskById_returns404Error_whenSubtaskNotFound() {
        //given
        //when
        HttpResponse<String> response = sendRequest("GET", "/subtasks/0");

        //then
        try {
            taskManager.getSubtask(0);
        } catch (NotFoundException e) {
            String exceptionMessage = e.getMessage();

            JsonObject exceptionJsonObject = gson.fromJson(response.body(), JsonObject.class);
            assertAll(
                    () -> assertEquals(exceptionMessage, exceptionJsonObject.get("message").getAsString()),
                    () -> assertEquals(404, response.statusCode(), "Код статуса должен быть 404")
            );
        }
    }

    @Test
    @DisplayName("На запрос 'GET /epics/{id}' возвращается Subtask в формате Json")
    void GETepicById_returnsEpic_inJsonFormat() {
        //given
        Epic epic1 = new Epic("epic", "epic");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "task1", "task1", 0, LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createEpic(epic1);
        taskManager.createSubtask(subtask1);

        //when
        HttpResponse<String> response = sendRequest("GET", "/epics/0");

        //then
        assertAll(
                () -> assertEquals(gson.toJson(taskManager.getEpic(0)), response.body().trim(), "Возвращаемый ответ не совпадает с ожидаемым json"),
                () -> assertEquals(200, response.statusCode(), "Код статуса должен быть 200")
        );
    }

    @Test
    @DisplayName("На запрос 'GET /epics/{id}' с id, которому не соответствует ни одна задача, возвращается статус 404 и ошибка в формате Json")
    void GETepicById_returns404Error_whenEpicNotFound() {
        //given
        //when
        HttpResponse<String> response = sendRequest("GET", "/epics/0");

        //then
        try {
            taskManager.getEpic(0);
        } catch (NotFoundException e) {
            String exceptionMessage = e.getMessage();

            JsonObject exceptionJsonObject = gson.fromJson(response.body(), JsonObject.class);
            assertAll(
                    () -> assertEquals(exceptionMessage, exceptionJsonObject.get("message").getAsString()),
                    () -> assertEquals(404, response.statusCode(), "Код статуса должен быть 404")
            );
        }
    }

    @Test
    @DisplayName("На запрос 'GET /epics/{id}/subtasks' возвращается список Subtask-ов эпика по полученному id в формате Json")
    void GETepicSubtasks_returnsSubtasksListOfEpic_byEpicId_inJsonFormat() {
        //given
        Epic epic1 = new Epic("epic", "epic");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "task1", "task1", 0, LocalDateTime.now(), Duration.ofDays(1));
        Subtask subtask2 = new Subtask(TaskStatus.NEW, "task1", "task1", 0, LocalDateTime.now().minusDays(7), Duration.ofDays(1));

        taskManager.createEpic(epic1);
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        //when
        HttpResponse<String> response = sendRequest("GET", "/epics/0/subtasks");

        //then
        assertAll(
                () -> assertEquals(gson.toJson(taskManager.getEpicSubtasks(0)), response.body().trim(), "Возвращаемый ответ не совпадает с ожидаемым json"),
                () -> assertEquals(200, response.statusCode(), "Код статуса должен быть 200")
        );
    }

    @Test
    @DisplayName("На запрос 'GET /epics/{id}/subtasks' с id, которому не соответствует ни одна задача, возвращается статус 404 и ошибка в формате Json")
    void GETepicSubtasks_returns404Error_whenEpicNotFound() {
        //given
        //when
        HttpResponse<String> response = sendRequest("GET", "/epics/0/subtasks");

        //then
        try {
            taskManager.getEpicSubtasks(0);
        } catch (NotFoundException e) {
            String exceptionMessage = e.getMessage();

            JsonObject exceptionJsonObject = gson.fromJson(response.body(), JsonObject.class);
            assertAll(
                    () -> assertEquals(exceptionMessage, exceptionJsonObject.get("message").getAsString()),
                    () -> assertEquals(404, response.statusCode(), "Код статуса должен быть 404")
            );
        }
    }

    @Test
    @DisplayName("На запрос 'DELETE /tasks/{id}' удаляется Task по полученному id")
    void DELETEtaskById_deletesTask_byId() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "task1", "task1", LocalDateTime.now().minusDays(10), Duration.ofDays(1));
        taskManager.createTask(task1);

        //when
        HttpResponse<String> response = sendRequest("DELETE", "/tasks/0");

        //then
        assertAll(
                () -> assertThrows(NotFoundException.class, () -> taskManager.getTask(0), "Задача не удаляется"),
                () -> assertEquals(204, response.statusCode(), "Код статуса должен быть 204")
        );
    }

    @Test
    @DisplayName("На запрос 'DELETE /subtasks/{id}' удаляется Subtask по полученному id")
    void DELETEsubtaskById_deletesSubtask_byId() {
        //given
        Epic epic1 = new Epic("epic", "epic");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "task1", "task1", 0, LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createEpic(epic1);
        taskManager.createSubtask(subtask1);

        //when
        HttpResponse<String> response = sendRequest("DELETE", "/subtasks/1");

        //then
        assertAll(
                () -> assertThrows(NotFoundException.class, () -> taskManager.getSubtask(1), "Подзадача не удаляется"),
                () -> assertEquals(0, taskManager.getEpic(0).getSubtasksIds().size(), "Подзадача не удаляется из эпика"),
                () -> assertEquals(204, response.statusCode(), "Код статуса должен быть 204")
        );
    }

    @Test
    @DisplayName("На запрос 'DELETE /epics/{id}' удаляется Epic по полученному id")
    void DELETEepicById_deletesEpic_byId() {
        //given
        Epic epic1 = new Epic("epic", "epic");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "task1", "task1", 0, LocalDateTime.now(), Duration.ofDays(1));
        taskManager.createEpic(epic1);
        taskManager.createSubtask(subtask1);

        //when
        HttpResponse<String> response = sendRequest("DELETE", "/epics/0");

        //then
        assertAll(
                () -> assertThrows(NotFoundException.class, () -> taskManager.getEpic(0), "Эпик не удаляется"),
                () -> assertThrows(NotFoundException.class, () -> taskManager.getSubtask(1), "Подзадачи эпика не удаляются"),
                () -> assertEquals(204, response.statusCode(), "Код статуса должен быть 204")
        );
    }

    @Test
    @DisplayName("На запрос 'POST /tasks' при наличии id задачи в теле запроса Task с этим id обновится согласно телу запроса")
    void POSTtaskWithId_updatesTask_withGivenId_ifIdExistsInRequestBody() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "task1", "task1", LocalDateTime.now().minusDays(10), Duration.ofDays(1));
        taskManager.createTask(task1);
        Task updatedTask = new Task(0, TaskStatus.DONE, "newName", "newDescription", LocalDateTime.now().minusDays(5), Duration.ofHours(1));
        String updatedTaskInJson = gson.toJson(updatedTask);

        //when
        HttpResponse<String> response = sendRequest("POST", "/tasks", updatedTaskInJson);

        //then
        assertAll(
                () -> assertEquals(updatedTaskInJson, gson.toJson(taskManager.getTask(0)), "Задача не обновляется"),
                () -> assertEquals(201, response.statusCode(), "Код статуса должен быть 201")
        );
    }

    @Test
    @DisplayName("На запрос 'POST /tasks' при ошибке в формате Json в теле возвращается статус 400 и ошибка в формате Json")
    void POSTtaskJsonParseException_returns400Error_whenJsonFormatIsIncorrect() {
        //given
        Task updatedTask = new Task(TaskStatus.DONE, "newName", "newDescription", LocalDateTime.now().minusDays(5), Duration.ofHours(1));
        String updatedTaskInJson = gson.toJson(updatedTask);
        updatedTaskInJson = updatedTaskInJson.replaceFirst("\\{", "");

        //when
        HttpResponse<String> response = sendRequest("POST", "/tasks", updatedTaskInJson);

        //then
        try {
            gson.fromJson(updatedTaskInJson, Task.class);
        } catch (JsonParseException e) {
            String exceptionMessage = e.getMessage();
            JsonObject exceptionJsonObject = gson.fromJson(response.body(), JsonObject.class);

            assertAll(
                    () -> assertEquals(exceptionMessage, exceptionJsonObject.get("message").getAsString()),
                    () -> assertEquals(400, response.statusCode(), "Код статуса должен быть 400")
            );
        }
    }

    @Test
    @DisplayName("При запросе 'POST /tasks' с задачей без id в теле и при пересечении времени исполнения переданного Task с существующими возвращается статус 406 и ошибка в формате Json")
    void POSTtaskWithIdOverlappingException_returns406Error_whenTaskWithoutIdAndTimeOverlap() {
        //given
        Task task1 = new Task(TaskStatus.NEW, "task1", "task1", LocalDateTime.now().minusDays(10), Duration.ofDays(1));
        taskManager.createTask(task1);
        Task updatedTask = new Task(TaskStatus.DONE, "newName", "newDescription", task1.getStartTime(), Duration.ofHours(1));
        String updatedTaskInJson = gson.toJson(updatedTask);

        //when
        HttpResponse<String> response = sendRequest("POST", "/tasks", updatedTaskInJson);

        //then
        try {
            taskManager.createTask(updatedTask);
        } catch (OverlappingException e) {
            String exceptionMessage = e.getMessage();
            JsonObject exceptionJsonObject = gson.fromJson(response.body(), JsonObject.class);

            assertAll(
                    () -> assertEquals(exceptionMessage, exceptionJsonObject.get("message").getAsString()),
                    () -> assertEquals(406, response.statusCode(), "Код статуса должен быть 406")
            );
        }
    }

    @Test
    @DisplayName("На запрос 'POST /tasks' при отсутствии id задачи в теле запроса будет создана новая Task")
    void POSTtaskWithoutId_createsNewTask_whenNoIdInRequestBody() {
        //given
        Task postTask = new Task(TaskStatus.DONE, "newName", "newDescription", LocalDateTime.now().minusDays(5), Duration.ofHours(1));

        //when
        HttpResponse<String> response = sendRequest("POST", "/tasks", gson.toJson(postTask));
        postTask.setId(0);
        String postTaskInJson = gson.toJson(postTask);

        //then
        assertAll(
                () -> assertEquals(postTaskInJson, gson.toJson(taskManager.getTask(0)), "Задача не добавляется"),
                () -> assertEquals(201, response.statusCode(), "Код статуса должен быть 201")
        );
    }

    @Test
    @DisplayName("На запрос 'POST /subtasks' при наличии id задачи в теле запроса Subtask с этим id обновится согласно телу запроса")
    void POSTsubtaskWithId_updatesSubtaskWithGivenId_ifIdExistsInRequestBody() {
        //given
        Epic epic1 = new Epic("epic", "epic");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "task1", "task1", 0, LocalDateTime.now().minusDays(10), Duration.ofDays(1));
        taskManager.createEpic(epic1);
        taskManager.createSubtask(subtask1);
        Subtask updatedTask = new Subtask(1, TaskStatus.IN_PROGRESS, "newName", "newDescription", 0, LocalDateTime.now().minusDays(5), Duration.ofHours(1));
        String updatedTaskInJson = gson.toJson(updatedTask);

        //when
        HttpResponse<String> response = sendRequest("POST", "/subtasks", updatedTaskInJson);

        //then
        assertAll(
                () -> assertEquals(updatedTaskInJson, gson.toJson(taskManager.getSubtask(1)), "Задача не обновляется"),
                () -> assertEquals(201, response.statusCode(), "Код статуса должен быть 201")
        );
    }

    @Test
    @DisplayName("На запрос 'POST /subtasks' при ошибке в формате Json в теле возвращается статус 400 и ошибка в формате Json")
    void POSTsubtaskJsonParseException_returns400Error_whenJsonFormatIsIncorrect() {
        //given
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "task1", "task1", 0, LocalDateTime.now().minusDays(10), Duration.ofDays(1));
        String updatedTaskInJson = gson.toJson(subtask1);
        updatedTaskInJson = updatedTaskInJson.replaceFirst("\\{", "");

        //when
        HttpResponse<String> response = sendRequest("POST", "/subtasks", updatedTaskInJson);

        //then
        try {
            gson.fromJson(updatedTaskInJson, Task.class);
        } catch (JsonParseException e) {
            String exceptionMessage = e.getMessage();
            JsonObject exceptionJsonObject = gson.fromJson(response.body(), JsonObject.class);

            assertAll(
                    () -> assertEquals(exceptionMessage, exceptionJsonObject.get("message").getAsString()),
                    () -> assertEquals(400, response.statusCode(), "Код статуса должен быть 400")
            );
        }
    }

    @Test
    @DisplayName("При запросе 'POST /tasks' с задачей без id в теле и при пересечении времени исполнения переданного Task с существующими возвращается статус 406 и ошибка в формате Json")
    void POSTsubtaskWithIdOverlappingException_returns406Error_whenTaskWithoutIdAndTimeOverlap() {
        //given
        Epic epic1 = new Epic("epic", "epic");
        Subtask subtask1 = new Subtask(TaskStatus.NEW, "task1", "task1", 0, LocalDateTime.now().minusDays(10), Duration.ofDays(1));
        taskManager.createEpic(epic1);
        taskManager.createSubtask(subtask1);
        Subtask updatedTask = new Subtask(TaskStatus.IN_PROGRESS, "newName", "newDescription", 0, subtask1.getStartTime(), Duration.ofHours(1));
        String updatedTaskInJson = gson.toJson(updatedTask);

        //when
        HttpResponse<String> response = sendRequest("POST", "/tasks", updatedTaskInJson);

        //then
        try {
            taskManager.createSubtask(updatedTask);
        } catch (OverlappingException e) {
            String exceptionMessage = e.getMessage();
            JsonObject exceptionJsonObject = gson.fromJson(response.body(), JsonObject.class);

            assertAll(
                    () -> assertEquals(exceptionMessage, exceptionJsonObject.get("message").getAsString()),
                    () -> assertEquals(406, response.statusCode(), "Код статуса должен быть 406")
            );
        }
    }

    @Test
    @DisplayName("На запрос 'POST /subtasks' с id, которому не соответствует ни одна задача, возвращается статус 404 и ошибка в формате Json")
    void POSTsubtaskWithIdNotFoundException_returns404Error_whenTaskNotFound() {
        //given
        Subtask subtask = new Subtask(TaskStatus.NEW, "", "", 0, LocalDateTime.now(), Duration.ofHours(1));

        //when
        HttpResponse<String> response = sendRequest("POST", "/subtasks", gson.toJson(subtask));

        //then
        try {
            taskManager.createSubtask(subtask);
        } catch (NotFoundException e) {
            String exceptionMessage = e.getMessage();
            JsonObject exceptionJsonObject = gson.fromJson(response.body(), JsonObject.class);

            assertAll(
                    () -> assertEquals(exceptionMessage, exceptionJsonObject.get("message").getAsString()),
                    () -> assertEquals(404, response.statusCode(), "Код статуса должен быть 404")
            );
        }
    }

    @Test
    @DisplayName("На запрос 'POST /subtasks' при отсутствии id задачи в теле запроса будет создана новая Subtask")
    void POSTsubtaskWithoutId_createsNewSubtask_whenNoIdInRequestBody() {
        //given
        Epic epic1 = new Epic("epic", "epic");
        taskManager.createEpic(epic1);
        Subtask postTask = new Subtask(TaskStatus.DONE, "name", "description", 0, LocalDateTime.now().minusDays(5), Duration.ofHours(1));

        //when
        HttpResponse<String> response = sendRequest("POST", "/subtasks", gson.toJson(postTask));
        postTask.setId(1);
        String postTaskInJson = gson.toJson(postTask);

        //then
        assertAll(
                () -> assertEquals(postTaskInJson, gson.toJson(taskManager.getSubtask(1)), "Задача не добавляется"),
                () -> assertEquals(201, response.statusCode(), "Код статуса должен быть 201")
        );
    }

    @Test
    @DisplayName("На запрос 'POST /epics' при наличии id задачи в теле запроса Epic с этим id обновится согласно телу запроса")
    void POSTepicWithId_updatesEpicWithGivenId_ifIdExistsInRequestBody() {
        //given
        Epic epic1 = new Epic("epic", "epic");
        taskManager.createEpic(epic1);
        Epic existingEpic = taskManager.getEpic(0);
        Epic updatedEpic = new Epic(0, TaskStatus.IN_PROGRESS, "newName", "newDescription", LocalDateTime.now().minusDays(5), Duration.ofHours(1));
        updatedEpic.setEndTime(LocalDateTime.now().plusDays(7));
        String updatedTaskInJson = gson.toJson(updatedEpic);

        //when
        HttpResponse<String> response = sendRequest("POST", "/epics", updatedTaskInJson);

        //then
        assertAll(
                () -> assertEquals(existingEpic.getName(), updatedEpic.getName(), "Имя не обновилось"),
                () -> assertEquals(existingEpic.getDescription(), updatedEpic.getDescription(), "Описание не обновилось"),
                () -> assertNotEquals(existingEpic.getStartTime(), updatedEpic.getStartTime(), "Время начала обновилось"),
                () -> assertNotEquals(existingEpic.getDuration(), updatedEpic.getDuration(), "Продолжительность обновилась"),
                () -> assertNotEquals(existingEpic.getEndTime(), updatedEpic.getEndTime(), "Время окончания обновилось"),
                () -> assertNotEquals(existingEpic.getStatus(), updatedEpic.getStatus(), "Статус обновился"),
                () -> assertEquals(201, response.statusCode(), "Код статуса должен быть 201")
        );
    }

    @Test
    @DisplayName("На запрос 'POST /epics' при ошибке в формате Json в теле возвращается статус 400 и ошибка в формате Json")
    void POSTepicJsonParseException_returns400Error_whenJsonFormatIsIncorrect() {
        //given
        Epic epic = new Epic(0, TaskStatus.IN_PROGRESS, "newName", "newDescription", LocalDateTime.now().minusDays(5), Duration.ofHours(1));
        epic.setEndTime(LocalDateTime.now().plusDays(1));
        String updatedTaskInJson = gson.toJson(epic);
        updatedTaskInJson = updatedTaskInJson.replaceFirst("\\{", "");

        //when
        HttpResponse<String> response = sendRequest("POST", "/epics", updatedTaskInJson);

        //then
        try {
            gson.fromJson(updatedTaskInJson, Task.class);
        } catch (JsonParseException e) {
            String exceptionMessage = e.getMessage();
            JsonObject exceptionJsonObject = gson.fromJson(response.body(), JsonObject.class);

            assertAll(
                    () -> assertEquals(exceptionMessage, exceptionJsonObject.get("message").getAsString()),
                    () -> assertEquals(400, response.statusCode(), "Код статуса должен быть 400")
            );
        }
    }

    @Test
    @DisplayName("На запрос 'POST /epics' при отсутствии id задачи в теле запроса будет создана новая Epic")
    void POSTepicWithoutId_createsNewEpic_whenNoIdInRequestBody() {
        //given
        Epic postEpic = new Epic(null, TaskStatus.DONE, "newName", "newDescription", LocalDateTime.now().minusDays(5), Duration.ofHours(1));
        postEpic.setEndTime(LocalDateTime.now().plusDays(7));

        //when
        HttpResponse<String> response = sendRequest("POST", "/epics", gson.toJson(postEpic));
        postEpic.setId(0);

        //then
        Epic existingEpic = taskManager.getEpic(0);
        assertAll(
                () -> assertEquals(existingEpic.getName(), postEpic.getName(), "Имя не установилось"),
                () -> assertEquals(existingEpic.getDescription(), postEpic.getDescription(), "Описание не установилось"),
                () -> assertNotEquals(existingEpic.getStartTime(), postEpic.getStartTime(), "Время начала установилось"),
                () -> assertNotEquals(existingEpic.getDuration(), postEpic.getDuration(), "Продолжительность установилась"),
                () -> assertNotEquals(existingEpic.getEndTime(), postEpic.getEndTime(), "Время окончания установилось"),
                () -> assertNotEquals(existingEpic.getStatus(), postEpic.getStatus(), "Статус установился"),
                () -> assertEquals(201, response.statusCode(), "Код статуса должен быть 201")
        );
    }
}
