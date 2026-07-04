package nl.jpoint.zendure.domain.device;

import nl.jpoint.zendure.domain.value.SensorReading;
import nl.jpoint.zendure.domain.device.DeviceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Battery device control and state reading.
 */
@DisplayName("Battery")
class BatteryTest {

    private Battery battery;
    private StubZendureRestClient restClient;
    private static final int MAX_CHARGE_POWER = 2400;
    private static final int MAX_DISCHARGE_POWER = 3600;

    @BeforeEach
    void setUp() {
        restClient = new StubZendureRestClient();
        battery = new Battery(restClient, MAX_CHARGE_POWER, MAX_DISCHARGE_POWER);
    }

    @Nested
    @DisplayName("Fast charge command")
    class FastChargeTests {

        @Test
        @DisplayName("Should send correct payload for fast charge")
        void testFastChargePayload() {
            restClient.setWriteSuccess(true);

            Battery.CommandResult result = battery.fastCharge();

            assertTrue(result.isSuccess());
            assertEquals(1, restClient.getLastWritePayloads().size());

            Map<String, Object> payload = restClient.getLastWritePayloads().get(0);
            assertEquals(1, payload.get("acMode"), "acMode should be 1 (charge)");
            assertEquals(MAX_CHARGE_POWER, payload.get("inputLimit"), "inputLimit should match configured max");
            assertEquals(1, payload.get("smartMode"), "smartMode should be 1 (RAM-only)");
        }

        @Test
        @DisplayName("Should return success when device accepts fast charge")
        void testFastChargeSuccess() {
            restClient.setWriteSuccess(true);

            Battery.CommandResult result = battery.fastCharge();

            assertTrue(result.isSuccess());
            assertEquals("", result.reason());
        }

        @Test
        @DisplayName("Should return failure when device rejects fast charge")
        void testFastChargeFailure() {
            restClient.setWriteSuccess(false);

            Battery.CommandResult result = battery.fastCharge();

            assertFalse(result.isSuccess());
            assertTrue(result.reason().contains("Device rejected"));
        }

        @Test
        @DisplayName("Should return failure on exception during fast charge")
        void testFastChargeException() {
            restClient.setThrowException(new RuntimeException("Connection timeout"));

            Battery.CommandResult result = battery.fastCharge();

            assertFalse(result.isSuccess());
            assertTrue(result.reason().contains("error"));
        }
    }

    @Nested
    @DisplayName("Fast discharge command")
    class FastDischargeTests {

        @Test
        @DisplayName("Should send correct payload for fast discharge")
        void testFastDischargePayload() {
            restClient.setWriteSuccess(true);

            Battery.CommandResult result = battery.fastDischarge();

            assertTrue(result.isSuccess());
            assertEquals(1, restClient.getLastWritePayloads().size());

            Map<String, Object> payload = restClient.getLastWritePayloads().get(0);
            assertEquals(2, payload.get("acMode"), "acMode should be 2 (discharge)");
            assertEquals(MAX_DISCHARGE_POWER, payload.get("outputLimit"), "outputLimit should match configured max");
            assertEquals(1, payload.get("smartMode"), "smartMode should be 1 (RAM-only)");
        }

        @Test
        @DisplayName("Should return success when device accepts fast discharge")
        void testFastDischargeSuccess() {
            restClient.setWriteSuccess(true);

            Battery.CommandResult result = battery.fastDischarge();

            assertTrue(result.isSuccess());
            assertEquals("", result.reason());
        }

        @Test
        @DisplayName("Should return failure when device rejects fast discharge")
        void testFastDischargeFailure() {
            restClient.setWriteSuccess(false);

            Battery.CommandResult result = battery.fastDischarge();

            assertFalse(result.isSuccess());
            assertTrue(result.reason().contains("Device rejected"));
        }

        @Test
        @DisplayName("Should return failure on exception during fast discharge")
        void testFastDischargeException() {
            restClient.setThrowException(new RuntimeException("Connection timeout"));

            Battery.CommandResult result = battery.fastDischarge();

            assertFalse(result.isSuccess());
            assertTrue(result.reason().contains("error"));
        }
    }

    @Nested
    @DisplayName("Standby command")
    class StandbyTests {

        @Test
        @DisplayName("Should send correct payload for standby")
        void testStandbyPayload() {
            restClient.setWriteSuccess(true);

            Battery.CommandResult result = battery.standby();

            assertTrue(result.isSuccess());
            assertEquals(1, restClient.getLastWritePayloads().size());

            Map<String, Object> payload = restClient.getLastWritePayloads().get(0);
            assertEquals(0, payload.get("inputLimit"), "inputLimit should be 0");
            assertEquals(0, payload.get("outputLimit"), "outputLimit should be 0");
            assertEquals(1, payload.get("smartMode"), "smartMode should be 1 (RAM-only)");
            assertNull(payload.get("acMode"), "acMode should not be present in standby payload");
        }

        @Test
        @DisplayName("Should return success when device accepts standby")
        void testStandbySuccess() {
            restClient.setWriteSuccess(true);

            Battery.CommandResult result = battery.standby();

            assertTrue(result.isSuccess());
            assertEquals("", result.reason());
        }

        @Test
        @DisplayName("Should return failure when device rejects standby")
        void testStandbyFailure() {
            restClient.setWriteSuccess(false);

            Battery.CommandResult result = battery.standby();

            assertFalse(result.isSuccess());
            assertTrue(result.reason().contains("Device rejected"));
        }

        @Test
        @DisplayName("Should return failure on exception during standby")
        void testStandbyException() {
            restClient.setThrowException(new RuntimeException("Connection timeout"));

            Battery.CommandResult result = battery.standby();

            assertFalse(result.isSuccess());
            assertTrue(result.reason().contains("error"));
        }
    }

    @Nested
    @DisplayName("State of charge reading")
    class StateOfChargeTests {

        @Test
        @DisplayName("Should parse SOC from device report")
        void testStateOfChargeSuccess() {
            restClient.setPropertiesReport(Map.of(
                "soc", 75,
                "acMode", 1
            ));

            SensorReading<Integer> reading = battery.stateOfCharge();

            assertTrue(reading.isAvailable());
            assertEquals(75, reading.value().orElse(-1));
        }

        @Test
        @DisplayName("Should handle SOC as double from JSON")
        void testStateOfChargeAsDouble() {
            restClient.setPropertiesReport(Map.of(
                "soc", 75.5,
                "acMode", 1
            ));

            SensorReading<Integer> reading = battery.stateOfCharge();

            assertTrue(reading.isAvailable());
            assertEquals(75, reading.value().orElse(-1));
        }

        @Test
        @DisplayName("Should return unavailable when report is null")
        void testStateOfChargeUnreachable() {
            restClient.setPropertiesReport(null);

            SensorReading<Integer> reading = battery.stateOfCharge();

            assertFalse(reading.isAvailable());
            assertTrue(reading.value().isEmpty());
        }

        @Test
        @DisplayName("Should return unavailable when SOC is missing from report")
        void testStateOfChargeMissing() {
            restClient.setPropertiesReport(Map.of(
                "acMode", 1
            ));

            SensorReading<Integer> reading = battery.stateOfCharge();

            assertFalse(reading.isAvailable());
            assertTrue(reading.value().isEmpty());
        }

        @Test
        @DisplayName("Should return unavailable when SOC is not a number")
        void testStateOfChargeInvalidType() {
            restClient.setPropertiesReport(Map.of(
                "soc", "invalid"
            ));

            SensorReading<Integer> reading = battery.stateOfCharge();

            assertFalse(reading.isAvailable());
            assertTrue(reading.value().isEmpty());
        }

        @Test
        @DisplayName("Should return unavailable on exception")
        void testStateOfChargeException() {
            restClient.setThrowException(new RuntimeException("Connection timeout"));

            SensorReading<Integer> reading = battery.stateOfCharge();

            assertFalse(reading.isAvailable());
            assertTrue(reading.value().isEmpty());
        }
    }

    @Nested
    @DisplayName("Operating mode reading")
    class ModeTests {

        @Test
        @DisplayName("Should parse acMode from device report")
        void testModeSuccess() {
            restClient.setPropertiesReport(Map.of(
                "soc", 75,
                "acMode", 1
            ));

            SensorReading<Integer> reading = battery.mode();

            assertTrue(reading.isAvailable());
            assertEquals(1, reading.value().orElse(-1));
        }

        @Test
        @DisplayName("Should handle acMode as double from JSON")
        void testModeAsDouble() {
            restClient.setPropertiesReport(Map.of(
                "soc", 75,
                "acMode", 2.0
            ));

            SensorReading<Integer> reading = battery.mode();

            assertTrue(reading.isAvailable());
            assertEquals(2, reading.value().orElse(-1));
        }

        @Test
        @DisplayName("Should return unavailable when report is null")
        void testModeUnreachable() {
            restClient.setPropertiesReport(null);

            SensorReading<Integer> reading = battery.mode();

            assertFalse(reading.isAvailable());
            assertTrue(reading.value().isEmpty());
        }

        @Test
        @DisplayName("Should return unavailable when acMode is missing from report")
        void testModeMissing() {
            restClient.setPropertiesReport(Map.of(
                "soc", 75
            ));

            SensorReading<Integer> reading = battery.mode();

            assertFalse(reading.isAvailable());
            assertTrue(reading.value().isEmpty());
        }

        @Test
        @DisplayName("Should return unavailable when acMode is not a number")
        void testModeInvalidType() {
            restClient.setPropertiesReport(Map.of(
                "acMode", "invalid"
            ));

            SensorReading<Integer> reading = battery.mode();

            assertFalse(reading.isAvailable());
            assertTrue(reading.value().isEmpty());
        }

        @Test
        @DisplayName("Should return unavailable on exception")
        void testModeException() {
            restClient.setThrowException(new RuntimeException("Connection timeout"));

            SensorReading<Integer> reading = battery.mode();

            assertFalse(reading.isAvailable());
            assertTrue(reading.value().isEmpty());
        }
    }

    /**
     * Stub REST client for testing Battery without hitting real endpoints.
     */
    static class StubZendureRestClient implements DeviceClient {

        private Map<String, Object> propertiesReport;
        private boolean writeSuccess = true;
        private RuntimeException exceptionToThrow;
        private java.util.List<Map<String, Object>> lastWritePayloads = new java.util.ArrayList<>();

        @Override
        public Optional<Map<String, Object>> getPropertiesReport() {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return Optional.ofNullable(propertiesReport);
        }

        @Override
        public Optional<Void> writeProperties(Map<String, Object> properties) {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            lastWritePayloads.add(Map.copyOf(properties));
            return writeSuccess ? Optional.of((Void) null) : Optional.empty();
        }

        void setPropertiesReport(Map<String, Object> report) {
            this.propertiesReport = report;
        }

        void setWriteSuccess(boolean success) {
            this.writeSuccess = success;
        }

        void setThrowException(RuntimeException exception) {
            this.exceptionToThrow = exception;
        }

        java.util.List<Map<String, Object>> getLastWritePayloads() {
            return lastWritePayloads;
        }
    }
}
