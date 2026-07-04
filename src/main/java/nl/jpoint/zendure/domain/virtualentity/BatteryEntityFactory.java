package nl.jpoint.zendure.domain.virtualentity;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/**
 * Factory for creating the battery-related virtual entities (control select and status sensors).
 */
public final class BatteryEntityFactory {

    private BatteryEntityFactory() {
        throw new AssertionError("Not instantiable");
    }

    /**
     * Entity IDs for the battery control and status entities.
     */
    public static final class EntityIds {
        private EntityIds() {
            throw new AssertionError("Not instantiable");
        }

        public static final String CONTROL_SELECT = "select.zendure_battery_mode";
        public static final String MODE_SENSOR = "sensor.zendure_battery_mode";
        public static final String SOC_SENSOR = "sensor.zendure_battery_soc";
    }

    /**
     * Create the battery control select entity.
     * Options: Fast Charge, Fast Discharge, Standby (default: Standby)
     *
     * @param objectMapper for building entity attributes
     * @return the control select entity
     */
    public static SelectEntity createControlSelect(ObjectMapper objectMapper) {
        List<String> options = List.of(
            BatteryMode.FAST_CHARGE.displayName(),
            BatteryMode.FAST_DISCHARGE.displayName(),
            BatteryMode.STANDBY.displayName()
        );

        return new SelectEntity(
            EntityIds.CONTROL_SELECT,
            "Zendure Battery Mode",
            options,
            BatteryMode.STANDBY.displayName(),
            objectMapper
        );
    }

    /**
     * Create the battery operating mode sensor.
     * Displays the current operating mode read from the device.
     *
     * @param objectMapper for building entity attributes
     * @return the mode sensor entity (initially unavailable)
     */
    public static SensorEntity createModeSensor(ObjectMapper objectMapper) {
        return new SensorEntity(
            EntityIds.MODE_SENSOR,
            "Zendure Battery Mode (Status)",
            "sensor",
            "unavailable",
            null,
            objectMapper
        );
    }

    /**
     * Create the battery state-of-charge sensor.
     * Displays the current SOC percentage read from the device.
     *
     * @param objectMapper for building entity attributes
     * @return the SOC sensor entity (initially unavailable)
     */
    public static SensorEntity createSocSensor(ObjectMapper objectMapper) {
        return new SensorEntity(
            EntityIds.SOC_SENSOR,
            "Zendure Battery State of Charge",
            "sensor",
            "unavailable",
            "%",
            objectMapper
        );
    }
}
