package nl.jpoint.zendure.config;

import nl.jpoint.zendure.homeassistant.HomeAssistantProperties;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;

/**
 * Performs startup validation of required configuration properties.
 * Implements fail-fast validation to catch configuration errors early
 * before the application fully initializes.
 */
@Component
public class ConfigurationValidator {

    private final ZendureDeviceProperties zendureProperties;
    private final HomeAssistantProperties haProperties;
    private final Validator validator;

    public ConfigurationValidator(
        ZendureDeviceProperties zendureProperties,
        HomeAssistantProperties haProperties,
        Validator validator
    ) {
        this.zendureProperties = zendureProperties;
        this.haProperties = haProperties;
        this.validator = validator;
    }

    /**
     * Validates all configuration properties on application startup.
     * Throws IllegalStateException if any required property is missing or invalid.
     */
    @EventListener
    public void onApplicationContextInitialized(ApplicationContextInitializedEvent event) {
        validateZendureProperties();
        validateHomeAssistantProperties();
    }

    /**
     * Validates Zendure device configuration.
     * Required: deviceIp, deviceSerial, maxChargePower, maxDischargePower (all positive)
     *
     * @throws IllegalStateException if validation fails
     */
    public void validateZendureProperties() {
        Set<ConstraintViolation<ZendureDeviceProperties>> violations =
            validator.validate(zendureProperties);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                .map(v -> String.format("%s: %s", v.getPropertyPath(), v.getMessage()))
                .reduce((a, b) -> a + "; " + b)
                .orElse("Unknown validation error");

            throw new IllegalStateException(
                "Invalid Zendure device configuration: " + errors
            );
        }
    }

    /**
     * Validates Home Assistant connection configuration.
     * Required: websocketUrl, restUrl, token
     *
     * @throws IllegalStateException if validation fails
     */
    public void validateHomeAssistantProperties() {
        Set<ConstraintViolation<HomeAssistantProperties>> violations =
            validator.validate(haProperties);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                .map(v -> String.format("%s: %s", v.getPropertyPath(), v.getMessage()))
                .reduce((a, b) -> a + "; " + b)
                .orElse("Unknown validation error");

            throw new IllegalStateException(
                "Invalid Home Assistant configuration: " + errors
            );
        }
    }
}
