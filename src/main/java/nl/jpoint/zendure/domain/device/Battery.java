package nl.jpoint.zendure.domain.device;

import nl.jpoint.zendure.domain.value.SensorReading;
import nl.jpoint.zendure.zendure.ZenSDKProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * Zendure battery device abstraction for local REST API control.
 *
 * <p>Provides command methods to control battery charge/discharge modes and query methods to read
 * current state (SOC, operating mode). All commands and reads handle failure gracefully, mapping
 * I/O errors to observable results rather than propagating exceptions.
 */
public class Battery {

    private static final Logger logger = LoggerFactory.getLogger(Battery.class);

    private final DeviceClient deviceClient;
    private final int maxChargePower;
    private final int maxDischargePower;

    /**
     * Create a Battery device.
     *
     * @param deviceClient the domain-level client for device communication
     * @param maxChargePower the maximum charge power in watts
     * @param maxDischargePower the maximum discharge power in watts
     */
    public Battery(DeviceClient deviceClient, int maxChargePower, int maxDischargePower) {
        this.deviceClient = deviceClient;
        this.maxChargePower = maxChargePower;
        this.maxDischargePower = maxDischargePower;
    }

    /**
     * Command the battery to charge at maximum configured power.
     *
     * <p>Sends the payload: {@code {"acMode":1,"inputLimit":<maxChargePower>,"smartMode":1}}
     *
     * @return a {@link CommandResult} indicating success or failure
     */
    public CommandResult fastCharge() {
        try {
            Map<String, Object> properties = Map.of(
                ZenSDKProtocol.Properties.AC_MODE, ZenSDKProtocol.Modes.CHARGE,
                ZenSDKProtocol.Properties.INPUT_LIMIT, maxChargePower,
                ZenSDKProtocol.Properties.SMART_MODE, ZenSDKProtocol.SmartModes.RAM_ONLY
            );

            boolean success = deviceClient.writeProperties(properties).isPresent();

            if (success) {
                logger.info("Fast charge command succeeded (input limit: {} W)", maxChargePower);
                return CommandResult.success();
            } else {
                logger.warn("Fast charge command failed (device rejected the request)");
                return CommandResult.failure("Device rejected fast charge command");
            }
        } catch (Exception e) {
            logger.error("Fast charge command error: {}", e.getMessage(), e);
            return CommandResult.failure("Fast charge command error: " + e.getMessage());
        }
    }

    /**
     * Command the battery to discharge at maximum configured power.
     *
     * <p>Sends the payload: {@code {"acMode":2,"outputLimit":<maxDischargePower>,"smartMode":1}}
     *
     * @return a {@link CommandResult} indicating success or failure
     */
    public CommandResult fastDischarge() {
        try {
            Map<String, Object> properties = Map.of(
                ZenSDKProtocol.Properties.AC_MODE, ZenSDKProtocol.Modes.DISCHARGE,
                ZenSDKProtocol.Properties.OUTPUT_LIMIT, maxDischargePower,
                ZenSDKProtocol.Properties.SMART_MODE, ZenSDKProtocol.SmartModes.RAM_ONLY
            );

            boolean success = deviceClient.writeProperties(properties).isPresent();

            if (success) {
                logger.info("Fast discharge command succeeded (output limit: {} W)", maxDischargePower);
                return CommandResult.success();
            } else {
                logger.warn("Fast discharge command failed (device rejected the request)");
                return CommandResult.failure("Device rejected fast discharge command");
            }
        } catch (Exception e) {
            logger.error("Fast discharge command error: {}", e.getMessage(), e);
            return CommandResult.failure("Fast discharge command error: " + e.getMessage());
        }
    }

    /**
     * Command the battery to standby (stop charging and discharging).
     *
     * <p>Sends the payload: {@code {"inputLimit":0,"outputLimit":0,"smartMode":1}}
     *
     * @return a {@link CommandResult} indicating success or failure
     */
    public CommandResult standby() {
        try {
            Map<String, Object> properties = Map.of(
                ZenSDKProtocol.Properties.INPUT_LIMIT, 0,
                ZenSDKProtocol.Properties.OUTPUT_LIMIT, 0,
                ZenSDKProtocol.Properties.SMART_MODE, ZenSDKProtocol.SmartModes.RAM_ONLY
            );

            boolean success = deviceClient.writeProperties(properties).isPresent();

            if (success) {
                logger.info("Standby command succeeded");
                return CommandResult.success();
            } else {
                logger.warn("Standby command failed (device rejected the request)");
                return CommandResult.failure("Device rejected standby command");
            }
        } catch (Exception e) {
            logger.error("Standby command error: {}", e.getMessage(), e);
            return CommandResult.failure("Standby command error: " + e.getMessage());
        }
    }

    /**
     * Read the battery's current state-of-charge (SOC).
     *
     * <p>Fetches the device report via {@code GET /properties/report} and extracts the SOC value.
     * If the device is unreachable or the report cannot be parsed, returns {@link SensorReading#unavailable}.
     *
     * @return a SensorReading of the SOC as a percentage (0-100), or unavailable
     */
    public SensorReading<Integer> stateOfCharge() {
        try {
            var report = deviceClient.getPropertiesReport();

            if (report.isEmpty()) {
                logger.debug("Device report unavailable (request failed)");
                return SensorReading.unavailable("Device report request failed");
            }

            Object socValue = report.get().get("soc");
            if (socValue == null) {
                logger.debug("SOC not found in device report");
                return SensorReading.unavailable("SOC property not found in report");
            }

            try {
                int soc = ((Number) socValue).intValue();
                logger.debug("Read state-of-charge: {}%", soc);
                return SensorReading.available(soc, Instant.now());
            } catch (ClassCastException e) {
                logger.debug("SOC value is not a number: {} ({})", socValue, e.getMessage());
                return SensorReading.unavailable("SOC is not a valid number");
            }
        } catch (Exception e) {
            logger.debug("Error reading state-of-charge: {}", e.getMessage());
            return SensorReading.unavailable("Read error: " + e.getMessage());
        }
    }

    /**
     * Read the battery's current operating mode.
     *
     * <p>Fetches the device report via {@code GET /properties/report} and extracts the acMode value.
     * If the device is unreachable or the report cannot be parsed, returns {@link SensorReading#unavailable}.
     *
     * <p>Mode values: 1 = charge, 2 = discharge.
     *
     * @return a SensorReading of the mode (1 or 2), or unavailable
     */
    public SensorReading<Integer> mode() {
        try {
            var report = deviceClient.getPropertiesReport();

            if (report.isEmpty()) {
                logger.debug("Device report unavailable (request failed)");
                return SensorReading.unavailable("Device report request failed");
            }

            Object modeValue = report.get().get("acMode");
            if (modeValue == null) {
                logger.debug("acMode not found in device report");
                return SensorReading.unavailable("acMode property not found in report");
            }

            try {
                int mode = ((Number) modeValue).intValue();
                logger.debug("Read operating mode: {}", mode);
                return SensorReading.available(mode, Instant.now());
            } catch (ClassCastException e) {
                logger.debug("acMode value is not a number: {} ({})", modeValue, e.getMessage());
                return SensorReading.unavailable("acMode is not a valid number");
            }
        } catch (Exception e) {
            logger.debug("Error reading operating mode: {}", e.getMessage());
            return SensorReading.unavailable("Read error: " + e.getMessage());
        }
    }

    /**
     * Result of a command operation (fast charge, fast discharge, or standby).
     */
    public sealed interface CommandResult permits Success, Failure {

        static CommandResult success() {
            return new Success();
        }

        static CommandResult failure(String reason) {
            return new Failure(reason);
        }

        boolean isSuccess();

        String reason();
    }

    public static final class Success implements CommandResult {

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public String reason() {
            return "";
        }

        @Override
        public String toString() {
            return "CommandResult.Success()";
        }
    }

    public static final class Failure implements CommandResult {

        private final String reason;

        Failure(String reason) {
            this.reason = reason;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public String reason() {
            return reason;
        }

        @Override
        public String toString() {
            return "CommandResult.Failure(reason='" + reason + "')";
        }
    }
}
