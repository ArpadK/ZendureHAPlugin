package nl.jpoint.zendure.config;

import nl.jpoint.zendure.homeassistant.HomeAssistantProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "zendure.device-ip=192.168.1.100",
    "zendure.device-port=13554",
    "zendure.device-serial=ABC123",
    "zendure.max-charge-power=1000",
    "zendure.max-discharge-power=1000",
    "zendure.poll-interval-seconds=30",
    "homeassistant.websocket-url=ws://localhost/ws",
    "homeassistant.rest-url=http://localhost/api",
    "homeassistant.token=test-token"
})
@DisplayName("HomeAssistantProperties Validation Tests")
class HomeAssistantPropertiesTest {

    @Autowired
    private Validator validator;

    @Test
    @DisplayName("Should reject missing websocketUrl")
    void testMissingWebsocketUrl() {
        HomeAssistantProperties props = new HomeAssistantProperties(
            "",
            "http://supervisor/core/api",
            "token123"
        );

        Set<ConstraintViolation<HomeAssistantProperties>> violations = validator.validate(props);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("websocketUrl")),
            "Expected validation error for missing websocketUrl");
    }

    @Test
    @DisplayName("Should reject missing restUrl")
    void testMissingRestUrl() {
        HomeAssistantProperties props = new HomeAssistantProperties(
            "ws://supervisor/core/websocket",
            "",
            "token123"
        );

        Set<ConstraintViolation<HomeAssistantProperties>> violations = validator.validate(props);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("restUrl")),
            "Expected validation error for missing restUrl");
    }

    @Test
    @DisplayName("Should reject missing token")
    void testMissingToken() {
        HomeAssistantProperties props = new HomeAssistantProperties(
            "ws://supervisor/core/websocket",
            "http://supervisor/core/api",
            ""
        );

        Set<ConstraintViolation<HomeAssistantProperties>> violations = validator.validate(props);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("token")),
            "Expected validation error for missing token");
    }

    @Test
    @DisplayName("Should accept valid Home Assistant configuration")
    void testValidConfiguration() {
        HomeAssistantProperties props = new HomeAssistantProperties(
            "ws://supervisor/core/websocket",
            "http://supervisor/core/api",
            "token123"
        );

        Set<ConstraintViolation<HomeAssistantProperties>> violations = validator.validate(props);

        assertTrue(violations.isEmpty(),
            "Expected no validation errors for valid configuration");
    }

    @Test
    @DisplayName("Should accept Supervisor defaults")
    void testSupervisorDefaults() {
        HomeAssistantProperties props = new HomeAssistantProperties(
            "ws://supervisor/core/websocket",
            "http://supervisor/core/api",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        );

        Set<ConstraintViolation<HomeAssistantProperties>> violations = validator.validate(props);

        assertTrue(violations.isEmpty(),
            "Expected no validation errors for Supervisor defaults");
    }

    @Test
    @DisplayName("Should accept standalone configuration")
    void testStandaloneConfiguration() {
        HomeAssistantProperties props = new HomeAssistantProperties(
            "ws://192.168.1.10:8123/api/websocket",
            "http://192.168.1.10:8123/api",
            "token123"
        );

        Set<ConstraintViolation<HomeAssistantProperties>> violations = validator.validate(props);

        assertTrue(violations.isEmpty(),
            "Expected no validation errors for standalone configuration");
    }

    @Test
    @DisplayName("Should reject blank websocketUrl")
    void testBlankWebsocketUrl() {
        HomeAssistantProperties props = new HomeAssistantProperties(
            "   ",
            "http://supervisor/core/api",
            "token123"
        );

        Set<ConstraintViolation<HomeAssistantProperties>> violations = validator.validate(props);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("websocketUrl")),
            "Expected validation error for blank websocketUrl");
    }
}
