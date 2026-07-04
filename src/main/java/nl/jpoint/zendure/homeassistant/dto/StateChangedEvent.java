package nl.jpoint.zendure.homeassistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a state_changed event from Home Assistant.
 * Fired when any entity's state changes.
 */
public record StateChangedEvent(
    @JsonProperty("entity_id")
    String entityId,

    @JsonProperty("old_state")
    EntityState oldState,

    @JsonProperty("new_state")
    EntityState newState
) {
}
