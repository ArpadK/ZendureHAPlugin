package nl.jpoint.zendure.automation;

import nl.jpoint.zendure.domain.device.Battery;
import nl.jpoint.zendure.domain.virtualentity.BatteryEntityFactory;
import nl.jpoint.zendure.domain.virtualentity.BatteryMode;
import nl.jpoint.zendure.domain.virtualentity.VirtualEntityRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Automation that translates battery mode control select changes into device commands.
 *
 * <p>When the mode control select changes value, invokes the corresponding Battery command
 * (fastCharge, fastDischarge, or standby). On command failure, logs the error and does not
 * optimistically update the status — the next scheduled poll will reflect the actual device state.
 */
@Component
public class BatteryModeControlAutomation {

    private static final Logger log = LoggerFactory.getLogger(BatteryModeControlAutomation.class);

    private final Battery battery;
    private final VirtualEntityRegistry entityRegistry;

    public BatteryModeControlAutomation(Battery battery, VirtualEntityRegistry entityRegistry) {
        this.battery = battery;
        this.entityRegistry = entityRegistry;

        // Register listener for mode control select changes
        entityRegistry.onStateChange(
            BatteryEntityFactory.EntityIds.CONTROL_SELECT,
            this::onModeSelectChanged
        );
    }

    /**
     * Listener invoked when the mode control select changes.
     *
     * @param newState the new select state (e.g., "Fast Charge", "Fast Discharge", "Standby")
     */
    private void onModeSelectChanged(String newState) {
        log.info("Battery mode control select changed to: {}", newState);

        Battery.CommandResult result;
        if (BatteryMode.FAST_CHARGE.displayName().equals(newState)) {
            result = battery.fastCharge();
        } else if (BatteryMode.FAST_DISCHARGE.displayName().equals(newState)) {
            result = battery.fastDischarge();
        } else if (BatteryMode.STANDBY.displayName().equals(newState)) {
            result = battery.standby();
        } else {
            log.warn("Unknown battery mode: {}", newState);
            return;
        }

        if (!result.isSuccess()) {
            log.error("Battery command failed: {}", result.reason());
            // Do not update status optimistically; let the scheduled poller reflect actual device state
        } else {
            log.info("Battery command succeeded");
        }
    }
}
