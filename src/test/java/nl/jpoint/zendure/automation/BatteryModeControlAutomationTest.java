package nl.jpoint.zendure.automation;

import nl.jpoint.zendure.domain.device.Battery;
import nl.jpoint.zendure.domain.device.DeviceClient;
import nl.jpoint.zendure.domain.virtualentity.BatteryEntityFactory;
import nl.jpoint.zendure.domain.virtualentity.BatteryMode;
import nl.jpoint.zendure.domain.virtualentity.VirtualEntityRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BatteryModeControlAutomation.
 */
@DisplayName("BatteryModeControlAutomation")
class BatteryModeControlAutomationTest {

    private BatteryModeControlAutomation automation;
    private Battery battery;
    private TestVirtualEntityRegistry entityRegistry;
    private StubZendureRestClient restClient;
    private Consumer<String> modeSelectListener;

    private static final int MAX_CHARGE_POWER = 2400;
    private static final int MAX_DISCHARGE_POWER = 3600;

    @BeforeEach
    void setUp() {
        restClient = new StubZendureRestClient();
        battery = new Battery(restClient, MAX_CHARGE_POWER, MAX_DISCHARGE_POWER);
        entityRegistry = new TestVirtualEntityRegistry();

        automation = new BatteryModeControlAutomation(battery, entityRegistry);

        // Get the registered listener for mode select changes
        modeSelectListener = entityRegistry.getModeSelectListener();
        assertNotNull(modeSelectListener, "Mode select listener should have been registered");
    }

    @Nested
    @DisplayName("Mode select change handling")
    class ModeSelectChangeTests {

        @Test
        @DisplayName("Should invoke fast charge command when select changes to Fast Charge")
        void testFastChargeSelection() {
            restClient.setWriteSuccess(true);

            modeSelectListener.accept(BatteryMode.FAST_CHARGE.displayName());

            // Verify the fast charge command was sent
            assertEquals(1, restClient.getLastWritePayloads().size());
            Map<String, Object> payload = restClient.getLastWritePayloads().get(0);
            assertEquals(1, payload.get("acMode"), "Should send charge mode (1)");
            assertEquals(MAX_CHARGE_POWER, payload.get("inputLimit"));
            assertEquals(1, payload.get("smartMode"));
        }

        @Test
        @DisplayName("Should invoke fast discharge command when select changes to Fast Discharge")
        void testFastDischargeSelection() {
            restClient.setWriteSuccess(true);

            modeSelectListener.accept(BatteryMode.FAST_DISCHARGE.displayName());

            // Verify the fast discharge command was sent
            assertEquals(1, restClient.getLastWritePayloads().size());
            Map<String, Object> payload = restClient.getLastWritePayloads().get(0);
            assertEquals(2, payload.get("acMode"), "Should send discharge mode (2)");
            assertEquals(MAX_DISCHARGE_POWER, payload.get("outputLimit"));
            assertEquals(1, payload.get("smartMode"));
        }

        @Test
        @DisplayName("Should invoke standby command when select changes to Standby")
        void testStandbySelection() {
            restClient.setWriteSuccess(true);

            modeSelectListener.accept(BatteryMode.STANDBY.displayName());

            // Verify the standby command was sent
            assertEquals(1, restClient.getLastWritePayloads().size());
            Map<String, Object> payload = restClient.getLastWritePayloads().get(0);
            assertEquals(0, payload.get("inputLimit"), "Should zero input limit");
            assertEquals(0, payload.get("outputLimit"), "Should zero output limit");
            assertEquals(1, payload.get("smartMode"));
            assertNull(payload.get("acMode"), "Standby should not include acMode");
        }

        @Test
        @DisplayName("Should log failure without updating status when command fails")
        void testCommandFailureDoesNotUpdateStatus() {
            restClient.setWriteSuccess(false);

            // Should not throw, just log
            assertDoesNotThrow(() ->
                modeSelectListener.accept(BatteryMode.FAST_CHARGE.displayName())
            );

            // Verify no status update was called on failure
            assertEquals(0, entityRegistry.getSelectStateUpdates().size(),
                "No select state updates should occur on command failure");
            assertEquals(0, entityRegistry.getSensorStateUpdates().size(),
                "No sensor state updates should occur on command failure");
        }

        @Test
        @DisplayName("Should log error when device rejects command")
        void testDeviceRejection() {
            restClient.setWriteSuccess(false);

            // Should handle gracefully without throwing
            assertDoesNotThrow(() ->
                modeSelectListener.accept(BatteryMode.FAST_DISCHARGE.displayName())
            );

            // Verify at least one command was attempted
            assertEquals(1, restClient.getLastWritePayloads().size());
        }

        @Test
        @DisplayName("Should log error when device communication times out")
        void testCommunicationTimeout() {
            restClient.setThrowException(new RuntimeException("Connection timeout"));

            // Should handle gracefully without throwing
            assertDoesNotThrow(() ->
                modeSelectListener.accept(BatteryMode.STANDBY.displayName())
            );
        }

        @Test
        @DisplayName("Should ignore unknown mode selections")
        void testUnknownModeSelection() {
            restClient.setWriteSuccess(true);

            // Should handle gracefully without throwing
            assertDoesNotThrow(() ->
                modeSelectListener.accept("Unknown Mode")
            );

            // Verify no command was sent
            assertEquals(0, restClient.getLastWritePayloads().size());
        }
    }

    /**
     * Test double for VirtualEntityRegistry to avoid Mockito issues with Java 25.
     */
    static class TestVirtualEntityRegistry extends VirtualEntityRegistry {

        private Consumer<String> modeSelectListener;
        private final List<String> selectStateUpdates = new ArrayList<>();
        private final List<String> sensorStateUpdates = new ArrayList<>();

        public TestVirtualEntityRegistry() {
            super(null);
        }

        @Override
        public void onStateChange(String entityId, Consumer<String> listener) {
            if (BatteryEntityFactory.EntityIds.CONTROL_SELECT.equals(entityId)) {
                this.modeSelectListener = listener;
            }
        }

        @Override
        public void updateSelectState(String entityId, String newState) {
            selectStateUpdates.add(entityId + "=" + newState);
        }

        @Override
        public void updateSensorState(String entityId, String newState) {
            sensorStateUpdates.add(entityId + "=" + newState);
        }

        Consumer<String> getModeSelectListener() {
            return modeSelectListener;
        }

        List<String> getSelectStateUpdates() {
            return selectStateUpdates;
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
