package nl.jpoint.zendure.homeassistant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for Home Assistant connection.
 * Bound from application.yml under the 'homeassistant' prefix.
 * Includes defaults suitable for Home Assistant Supervisor add-ons.
 */
@ConfigurationProperties(prefix = "homeassistant")
@Validated
public record HomeAssistantProperties(
    @NotBlank(message = "Home Assistant WebSocket URL must not be blank")
    String websocketUrl,

    @NotBlank(message = "Home Assistant REST URL must not be blank")
    String restUrl,

    @NotBlank(message = "Home Assistant token must not be blank")
    String token
) {
}
