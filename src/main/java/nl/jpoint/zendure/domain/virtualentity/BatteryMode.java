package nl.jpoint.zendure.domain.virtualentity;

/**
 * The available battery control modes displayed in the Home Assistant select.
 */
public enum BatteryMode {
    FAST_CHARGE("Fast Charge"),
    FAST_DISCHARGE("Fast Discharge"),
    STANDBY("Standby");

    private final String displayName;

    BatteryMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Parse a display name to the corresponding BatteryMode.
     *
     * @param displayName the display name (e.g., "Fast Charge")
     * @return the BatteryMode, or null if not found
     */
    public static BatteryMode fromDisplayName(String displayName) {
        for (BatteryMode mode : BatteryMode.values()) {
            if (mode.displayName.equals(displayName)) {
                return mode;
            }
        }
        return null;
    }
}
