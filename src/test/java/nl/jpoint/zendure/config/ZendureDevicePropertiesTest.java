package nl.jpoint.zendure.config;

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
@DisplayName("ZendureDeviceProperties Validation Tests")
class ZendureDevicePropertiesTest {

    @Autowired
    private Validator validator;

    @Test
    @DisplayName("Should reject missing deviceIp")
    void testMissingDeviceIp() {
        ZendureDeviceProperties props = new ZendureDeviceProperties(
            "",
            13554,
            "ABC123",
            1000,
            1000,
            30
        );

        Set<ConstraintViolation<ZendureDeviceProperties>> violations = validator.validate(props);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("deviceIp")),
            "Expected validation error for missing deviceIp");
    }

    @Test
    @DisplayName("Should reject blank deviceIp")
    void testBlankDeviceIp() {
        ZendureDeviceProperties props = new ZendureDeviceProperties(
            "   ",
            13554,
            "ABC123",
            1000,
            1000,
            30
        );

        Set<ConstraintViolation<ZendureDeviceProperties>> violations = validator.validate(props);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("deviceIp")),
            "Expected validation error for blank deviceIp");
    }

    @Test
    @DisplayName("Should reject missing deviceSerial")
    void testMissingDeviceSerial() {
        ZendureDeviceProperties props = new ZendureDeviceProperties(
            "192.168.1.100",
            13554,
            "",
            1000,
            1000,
            30
        );

        Set<ConstraintViolation<ZendureDeviceProperties>> violations = validator.validate(props);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("deviceSerial")),
            "Expected validation error for missing deviceSerial");
    }

    @Test
    @DisplayName("Should reject non-positive maxChargePower")
    void testNonPositiveMaxChargePower() {
        ZendureDeviceProperties props = new ZendureDeviceProperties(
            "192.168.1.100",
            13554,
            "ABC123",
            0,
            1000,
            30
        );

        Set<ConstraintViolation<ZendureDeviceProperties>> violations = validator.validate(props);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("maxChargePower")),
            "Expected validation error for non-positive maxChargePower");
    }

    @Test
    @DisplayName("Should reject negative maxChargePower")
    void testNegativeMaxChargePower() {
        ZendureDeviceProperties props = new ZendureDeviceProperties(
            "192.168.1.100",
            13554,
            "ABC123",
            -500,
            1000,
            30
        );

        Set<ConstraintViolation<ZendureDeviceProperties>> violations = validator.validate(props);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("maxChargePower")),
            "Expected validation error for negative maxChargePower");
    }

    @Test
    @DisplayName("Should reject non-positive maxDischargePower")
    void testNonPositiveMaxDischargePower() {
        ZendureDeviceProperties props = new ZendureDeviceProperties(
            "192.168.1.100",
            13554,
            "ABC123",
            1000,
            0,
            30
        );

        Set<ConstraintViolation<ZendureDeviceProperties>> violations = validator.validate(props);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("maxDischargePower")),
            "Expected validation error for non-positive maxDischargePower");
    }

    @Test
    @DisplayName("Should accept valid configuration")
    void testValidConfiguration() {
        ZendureDeviceProperties props = new ZendureDeviceProperties(
            "192.168.1.100",
            13554,
            "ABC123",
            1000,
            1000,
            30
        );

        Set<ConstraintViolation<ZendureDeviceProperties>> violations = validator.validate(props);

        assertTrue(violations.isEmpty(),
            "Expected no validation errors for valid configuration");
    }

    @Test
    @DisplayName("Should accept devicePort with default value")
    void testDevicePortDefault() {
        ZendureDeviceProperties props = new ZendureDeviceProperties(
            "192.168.1.100",
            ZendureDeviceProperties.DEFAULT_DEVICE_PORT,
            "ABC123",
            1000,
            1000,
            30
        );

        Set<ConstraintViolation<ZendureDeviceProperties>> violations = validator.validate(props);

        assertTrue(violations.isEmpty(),
            "Expected no validation errors when using default port");
        assertEquals(13554, props.devicePort(),
            "Expected default port to be 13554");
    }

    @Test
    @DisplayName("Should accept positive pollIntervalSeconds")
    void testPositivePollInterval() {
        ZendureDeviceProperties props = new ZendureDeviceProperties(
            "192.168.1.100",
            13554,
            "ABC123",
            1000,
            1000,
            60
        );

        Set<ConstraintViolation<ZendureDeviceProperties>> violations = validator.validate(props);

        assertTrue(violations.isEmpty(),
            "Expected no validation errors for positive poll interval");
    }

    @Test
    @DisplayName("Should reject non-positive pollIntervalSeconds")
    void testNonPositivePollInterval() {
        ZendureDeviceProperties props = new ZendureDeviceProperties(
            "192.168.1.100",
            13554,
            "ABC123",
            1000,
            1000,
            0
        );

        Set<ConstraintViolation<ZendureDeviceProperties>> violations = validator.validate(props);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("pollIntervalSeconds")),
            "Expected validation error for non-positive pollIntervalSeconds");
    }

    @Test
    @DisplayName("Should accept large positive power values")
    void testLargePowerValues() {
        ZendureDeviceProperties props = new ZendureDeviceProperties(
            "192.168.1.100",
            13554,
            "ABC123",
            50000,
            50000,
            30
        );

        Set<ConstraintViolation<ZendureDeviceProperties>> violations = validator.validate(props);

        assertTrue(violations.isEmpty(),
            "Expected no validation errors for large power values");
    }
}
