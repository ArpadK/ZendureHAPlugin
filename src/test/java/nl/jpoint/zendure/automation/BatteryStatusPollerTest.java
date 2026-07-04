package nl.jpoint.zendure.automation;

import nl.jpoint.zendure.config.ZendureDeviceProperties;
import nl.jpoint.zendure.domain.device.Battery;
import nl.jpoint.zendure.domain.device.DeviceClient;
import nl.jpoint.zendure.domain.value.SensorReading;
import nl.jpoint.zendure.domain.virtualentity.BatteryEntityFactory;
import nl.jpoint.zendure.domain.virtualentity.VirtualEntityRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BatteryStatusPoller.
 */
@DisplayName("BatteryStatusPoller")
class BatteryStatusPollerTest {

    private BatteryStatusPoller poller;
    private Battery battery;
    private TestVirtualEntityRegistry entityRegistry;
    private StubZendureRestClient restClient;
    private ZendureDeviceProperties properties;

    private static final int MAX_CHARGE_POWER = 2400;
    private static final int MAX_DISCHARGE_POWER = 3600;
    private static final int POLL_INTERVAL_SECONDS = 30;

    @BeforeEach
    void setUp() {
        restClient = new StubZendureRestClient();
        battery = new Battery(restClient, MAX_CHARGE_POWER, MAX_DISCHARGE_POWER);
        entityRegistry = new TestVirtualEntityRegistry();
        properties = new ZendureDeviceProperties(
            "localhost",
            13554,
            "test-serial",
            MAX_CHARGE_POWER,
            MAX_DISCHARGE_POWER,
            POLL_INTERVAL_SECONDS
        );

        poller = new BatteryStatusPoller(battery, entityRegistry, properties);
    }

    @Nested
    @DisplayName("Status polling")
    class StatusPollingTests {

        @Test
        @DisplayName("Should publish mode and SOC sensors when device is available")
        void testPublishSensorsWhenDeviceAvailable() {
            restClient.setPropertiesReport(Map.of(
                "acMode", 1,
                "soc", 75
            ));

            poller.pollBatteryStatus();

            List<String> updates = entityRegistry.getSensorStateUpdates();
            assertFalse(updates.isEmpty(), "Sensor state updates should not be empty. Updates: " + updates);
            assertTrue(updates.stream()
                .anyMatch(s -> s.contains("zendure_battery_mode") && s.contains("Charging")),
                "Mode sensor should be updated to Charging. Updates: " + updates);

            assertTrue(updates.stream()
                .anyMatch(s -> s.contains("zendure_battery_soc") && s.contains("75")),
                "SOC sensor should be updated to 75. Updates: " + updates);
        }

        @Test
        @DisplayName("Should reflect mode 1 as Charging")
        void testModeOneAsCharging() {
            restClient.setPropertiesReport(Map.of(
                "acMode", 1,
                "soc", 50
            ));

            poller.pollBatteryStatus();

            assertTrue(entityRegistry.getSensorStateUpdates().stream()
                .anyMatch(s -> s.contains("zendure_battery_mode") && s.contains("Charging")),
                "Mode should be reported as Charging for mode 1");
        }

        @Test
        @DisplayName("Should reflect mode 2 as Discharging")
        void testModeTwoAsDischarging() {
            restClient.setPropertiesReport(Map.of(
                "acMode", 2,
                "soc", 50
            ));

            poller.pollBatteryStatus();

            assertTrue(entityRegistry.getSensorStateUpdates().stream()
                .anyMatch(s -> s.contains("zendure_battery_mode") && s.contains("Discharging")),
                "Mode should be reported as Discharging for mode 2");
        }

        @Test
        @DisplayName("Should handle unknown mode gracefully")
        void testUnknownModeHandling() {
            restClient.setPropertiesReport(Map.of(
                "acMode", 99,
                "soc", 50
            ));

            poller.pollBatteryStatus();

            assertTrue(entityRegistry.getSensorStateUpdates().stream()
                .anyMatch(s -> s.contains("zendure_battery_mode") && s.contains("Unknown")),
                "Unknown mode should be handled gracefully");
        }

        @Test
        @DisplayName("Should publish SOC as percentage string")
        void testSocPublishedAsPercentage() {
            restClient.setPropertiesReport(Map.of(
                "acMode", 1,
                "soc", 42
            ));

            poller.pollBatteryStatus();

            assertTrue(entityRegistry.getSensorStateUpdates().stream()
                .anyMatch(s -> s.contains("zendure_battery_soc") && s.contains("42")),
                "SOC sensor should be updated with numeric value as string");
        }

        @Test
        @DisplayName("Should set sensors to unavailable when device report is null")
        void testUnavailableWhenDeviceUnreachable() {
            restClient.setPropertiesReport(null);

            poller.pollBatteryStatus();

            assertTrue(entityRegistry.getSensorStateUpdates().stream()
                .anyMatch(s -> s.contains("zendure_battery_mode") && s.contains("unavailable")),
                "Mode sensor should be unavailable when device unreachable");

            assertTrue(entityRegistry.getSensorStateUpdates().stream()
                .anyMatch(s -> s.contains("zendure_battery_soc") && s.contains("unavailable")),
                "SOC sensor should be unavailable when device unreachable");
        }

        @Test
        @DisplayName("Should set sensors to unavailable when acMode is missing")
        void testUnavailableWhenModeMissing() {
            restClient.setPropertiesReport(Map.of(
                "soc", 75
            ));

            poller.pollBatteryStatus();

            assertTrue(entityRegistry.getSensorStateUpdates().stream()
                .anyMatch(s -> s.contains("zendure_battery_mode") && s.contains("unavailable")),
                "Mode sensor should be unavailable when acMode missing");
        }

        @Test
        @DisplayName("Should set sensors to unavailable when SOC is missing")
        void testUnavailableWhenSocMissing() {
            restClient.setPropertiesReport(Map.of(
                "acMode", 1
            ));

            poller.pollBatteryStatus();

            assertTrue(entityRegistry.getSensorStateUpdates().stream()
                .anyMatch(s -> s.contains("zendure_battery_soc") && s.contains("unavailable")),
                "SOC sensor should be unavailable when soc missing");
        }

        @Test
        @DisplayName("Should handle communication timeout gracefully")
        void testTimeoutHandling() {
            restClient.setThrowException(new RuntimeException("Connection timeout"));

            // Should not throw
            assertDoesNotThrow(() -> poller.pollBatteryStatus());

            // Should set sensors to unavailable
            assertTrue(entityRegistry.getSensorStateUpdates().stream()
                .anyMatch(s -> s.contains("zendure_battery_mode") && s.contains("unavailable")),
                "Mode sensor should be unavailable on timeout");
            assertTrue(entityRegistry.getSensorStateUpdates().stream()
                .anyMatch(s -> s.contains("zendure_battery_soc") && s.contains("unavailable")),
                "SOC sensor should be unavailable on timeout");
        }

        @Test
        @DisplayName("Should handle unexpected exceptions gracefully")
        void testUnexpectedExceptionHandling() {
            // Create a registry that throws on sensor updates
            TestVirtualEntityRegistry failingRegistry = new TestVirtualEntityRegistry() {
                private int callCount = 0;

                @Override
                public void updateSensorState(String entityId, String newState) {
                    if (callCount == 0 && entityId.equals(BatteryEntityFactory.EntityIds.MODE_SENSOR)) {
                        callCount++;
                        throw new RuntimeException("Registry error");
                    }
                    super.updateSensorState(entityId, newState);
                }
            };

            restClient.setPropertiesReport(Map.of(
                "acMode", 1,
                "soc", 75
            ));

            BatteryStatusPoller failingPoller = new BatteryStatusPoller(battery, failingRegistry, properties);

            // Should not throw despite registry error
            assertDoesNotThrow(() -> failingPoller.pollBatteryStatus());
        }

        @Test
        @DisplayName("Should handle double values from JSON")
        void testDoubleValuesFromJson() {
            restClient.setPropertiesReport(Map.of(
                "acMode", 1.0,
                "soc", 75.5
            ));

            poller.pollBatteryStatus();

            assertTrue(entityRegistry.getSensorStateUpdates().stream()
                .anyMatch(s -> s.contains("zendure_battery_mode") && s.contains("Charging")),
                "Should convert double mode to Charging");

            assertTrue(entityRegistry.getSensorStateUpdates().stream()
                .anyMatch(s -> s.contains("zendure_battery_soc") && s.contains("75")),
                "Should convert double SOC to int string");
        }
    }

    /**
     * Test double for VirtualEntityRegistry to capture updates.
     */
    static class TestVirtualEntityRegistry extends VirtualEntityRegistry {

        private final List<String> sensorStateUpdates = new ArrayList<>();

        public TestVirtualEntityRegistry() {
            super(null);
        }

        @Override
        public void updateSensorState(String entityId, String newState) {
            sensorStateUpdates.add(entityId + "=" + newState);
        }

        List<String> getSensorStateUpdates() {
            return sensorStateUpdates;
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
            // Return present optional on success, empty on failure
            if (writeSuccess) {
                // Void is a special type; we return a present Optional containing null
                Void voidValue = null;
                return Optional.of(voidValue);
            } else {
                return Optional.empty();
            }
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
