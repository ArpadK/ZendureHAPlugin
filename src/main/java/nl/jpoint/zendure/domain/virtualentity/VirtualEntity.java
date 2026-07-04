package nl.jpoint.zendure.domain.virtualentity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a virtual entity owned by the plugin and published to Home Assistant.
 *
 * <p>Each entity has a domain-scoped entity ID, a current state, and optional attributes.
 * The registry manages publication to HA and republishes on (re)authentication.
 */
public interface VirtualEntity {

    /**
     * The unique entity ID in Home Assistant (e.g., "select.zendure_battery_mode").
     * Must be in the format "{domain}.{unique_id}".
     */
    String entityId();

    /**
     * The current state value (e.g., "Standby", "50").
     */
    String state();

    /**
     * Optional JSON attributes (e.g., options for select, unit_of_measurement for sensor).
     * May return null if no attributes are needed.
     */
    JsonNode attributes();

    /**
     * The type of entity (e.g., "select", "sensor").
     * Used for constructing the entity ID and organizing entities.
     */
    String type();

    /**
     * A human-readable name for the entity in Home Assistant.
     */
    String name();
}
