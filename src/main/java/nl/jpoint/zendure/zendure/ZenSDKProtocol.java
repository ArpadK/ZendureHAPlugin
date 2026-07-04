package nl.jpoint.zendure.zendure;

/**
 * zenSDK protocol constants for local REST API communication.
 *
 * <p>Reference: https://github.com/Zendure/zenSDK (docs/en_properties.md)
 */
public final class ZenSDKProtocol {

    private ZenSDKProtocol() {
        throw new AssertionError("Not instantiable");
    }

    /**
     * API endpoint paths (relative to base URL).
     */
    public static final class Paths {
        private Paths() {
            throw new AssertionError("Not instantiable");
        }

        /**
         * GET /properties/report — read all device properties including SOC and mode.
         */
        public static final String REPORT = "/properties/report";

        /**
         * POST /properties/write — write control properties to device.
         * Body: {"sn":"<serial>","properties":{...}}
         * Content-Type: application/json
         */
        public static final String WRITE = "/properties/write";
    }

    /**
     * Control property keys for POST /properties/write.
     */
    public static final class Properties {
        private Properties() {
            throw new AssertionError("Not instantiable");
        }

        /**
         * Operating mode: 1 = charge, 2 = discharge.
         */
        public static final String AC_MODE = "acMode";

        /**
         * Maximum input (charge) power in watts.
         */
        public static final String INPUT_LIMIT = "inputLimit";

        /**
         * Maximum output (discharge) power in watts.
         */
        public static final String OUTPUT_LIMIT = "outputLimit";

        /**
         * Smart mode flag: 1 = RAM-only write (avoids flash wear, reverts on device reboot).
         */
        public static final String SMART_MODE = "smartMode";
    }

    /**
     * Operating mode values for acMode property.
     */
    public static final class Modes {
        private Modes() {
            throw new AssertionError("Not instantiable");
        }

        /**
         * Charge mode.
         */
        public static final int CHARGE = 1;

        /**
         * Discharge mode.
         */
        public static final int DISCHARGE = 2;
    }

    /**
     * Smart mode values.
     */
    public static final class SmartModes {
        private SmartModes() {
            throw new AssertionError("Not instantiable");
        }

        /**
         * RAM-only write: persists until device reboot, avoids flash wear.
         */
        public static final int RAM_ONLY = 1;
    }

    /**
     * HTTP content type for zenSDK API requests.
     */
    public static final String CONTENT_TYPE = "application/json";
}
