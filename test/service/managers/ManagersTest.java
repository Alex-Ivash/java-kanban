package service.managers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Managers")
class ManagersTest {

    @Test
    @DisplayName("Возвращает проинициализированные и готовые к работе экземпляры менеджеров")
    void getDefault_getDefaultHistory_shouldReturnInitializedInstances() {
        assertNotNull(Managers.getDefault(), "TaskManager не проинициализирован");
        assertNotNull(Managers.getDefaultHistory(), "HistoryManager не проинициализирован");
    }
}
