package nl.jpoint.zendure.automation;

import nl.jpoint.zendure.config.ZendureDeviceProperties;
import nl.jpoint.zendure.domain.device.Battery;
import nl.jpoint.zendure.domain.value.SensorReading;
import nl.jpoint.zendure.domain.virtualentity.BatteryEntityFactory;
import nl.jpoint.zendure.domain.virtualentity.VirtualEntityRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled poller that reads the battery's current state and publishes status sensors.
 *
 * <p>Runs at a configurable interval (from {@link ZendureDeviceProperties#pollIntervalSeconds}).
 * On each poll, reads the device report and updates the mode and SOC status sensors.
 * If the device is unreachable, sensors are set to unavailable.
 */
@Component
public class BatteryStatusPoller {

    private static final Logger log = LoggerFactory.getLogger(BatteryStatusPoller.class);

    private final Battery battery;
    private final VirtualEntityRegistry entityRegistry;
    private final long pollIntervalMillis;

    public BatteryStatusPoller(
        Battery battery,
        VirtualEntityRegistry entityRegistry,
        ZendureDeviceProperties properties
    ) {
        this.battery = battery;
        this.entityRegistry = entityRegistry;
        this.pollIntervalMillis = properties.pollIntervalSeconds() * 1000L;
    }

    /**
     * Poll the device and update status sensors.
     * Runs at the configured poll interval.
     */
    @Scheduled(
        initialDelayString = "${zendure.device.poll-initial-delay:5000}",
        fixedDelayString = "${zendure.device.poll-interval:30000}"
    )
    public void pollBatteryStatus() {
        try {
            log.debug("Polling battery status...");

            // Read current mode
            SensorReading<Integer> modeReading = battery.mode();
            updateModeSensor(modeReading);

            // Read current SOC
            SensorReading<Integer> socReading = battery.stateOfCharge();
            updateSocSensor(socReading);

            log.debug("Battery status poll completed");
        } catch (Exception e) {
            log.error("Unexpected error during battery status poll: {}", e.getMessage(), e);
            // Mark sensors as unavailable on unexpected error
            entityRegistry.updateSensorState(
                BatteryEntityFactory.EntityIds.MODE_SENSOR,
                "unavailable"
            );
            entityRegistry.updateSensorState(
                BatteryEntityFactory.EntityIds.SOC_SENSOR,
                "unavailable"
            );
        }
    }

    /**
     * Update the mode status sensor based on the reading.
     */
    private void updateModeSensor(SensorReading<Integer> modeReading) {
        if (modeReading.isAvailable()) {
            int mode = modeReading.value().orElse(-1);
            String modeStr = formatMode(mode);
            log.debug("Updating mode sensor: {}", modeStr);
            entityRegistry.updateSensorState(
                BatteryEntityFactory.EntityIds.MODE_SENSOR,
                modeStr
            );
        } else {
            log.debug("Device unavailable; setting mode sensor to unavailable");
            entityRegistry.updateSensorState(
                BatteryEntityFactory.EntityIds.MODE_SENSOR,
                "unavailable"
            );
        }
    }

    /**
     * Update the SOC status sensor based on the reading.
     */
    private void updateSocSensor(SensorReading<Integer> socReading) {
        if (socReading.isAvailable()) {
            int soc = socReading.value().orElse(-1);
            log.debug("Updating SOC sensor: {}%", soc);
            entityRegistry.updateSensorState(
                BatteryEntityFactory.EntityIds.SOC_SENSOR,
                String.valueOf(soc)
            );
        } else {
            log.debug("Device unavailable; setting SOC sensor to unavailable");
            entityRegistry.updateSensorState(
                BatteryEntityFactory.EntityIds.SOC_SENSOR,
                "unavailable"
            );
        }
    }

    /**
     * Format a numeric mode value as a human-readable string.
     *
     * @param mode the mode (1 = charge, 2 = discharge, other = unknown)
     * @return a formatted mode string
     */
    private String formatMode(int mode) {
        return switch (mode) {
            case 1 -> "Charging";
            case 2 -> "Discharging";
            default -> "Unknown (" + mode + ")";
        };
    }
}
