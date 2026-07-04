package nl.jpoint.zendure.homeassistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents the state of an entity in Home Assistant.
 */
public record EntityState(
    @JsonProperty("state")
    String state,

    @JsonProperty("attributes")
    JsonNode attributes,

    @JsonProperty("last_changed")
    String lastChanged,

    @JsonProperty("last_updated")
    String lastUpdated
) {
}
