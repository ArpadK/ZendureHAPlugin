package nl.jpoint.zendure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Configuration properties for Zendure device connection and control.
 * Bound from application.yml under the 'zendure' prefix.
 */
@ConfigurationProperties(prefix = "zendure")
@Validated
public record ZendureDeviceProperties(
    @NotBlank(message = "Device IP must not be blank")
    String deviceIp,

    @Positive(message = "Device port must be a positive number")
    int devicePort,

    @NotBlank(message = "Device serial must not be blank")
    String deviceSerial,

    @NotNull(message = "Max charge power must not be null")
    @Positive(message = "Max charge power must be a positive value")
    Integer maxChargePower,

    @NotNull(message = "Max discharge power must not be null")
    @Positive(message = "Max discharge power must be a positive value")
    Integer maxDischargePower,

    @Positive(message = "Poll interval seconds must be a positive number")
    int pollIntervalSeconds
) {
    /**
     * Default port for Zendure zenSDK REST API.
     * Should be passed to the constructor when port is omitted from configuration.
     */
    public static final int DEFAULT_DEVICE_PORT = 13554;
}
