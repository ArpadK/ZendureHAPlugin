package nl.jpoint.zendure.homeassistant;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON message structure for Home Assistant WebSocket protocol.
 * Used for both sending and receiving messages.
 */
public record HomeAssistantMessage(
    @JsonProperty("id")
    Long id,

    @JsonProperty("type")
    String type,

    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("event")
    Object event,

    @JsonProperty("success")
    Boolean success,

    @JsonProperty("error")
    MessageError error
) {
    /**
     * Create an auth message with the given token.
     */
    public static HomeAssistantMessage auth(String token) {
        return new HomeAssistantMessage(null, "auth", token, null, null, null);
    }

    /**
     * Create a subscribe_events message with the given id.
     */
    public static HomeAssistantMessage subscribeEvents(Long id, String eventType) {
        return new HomeAssistantMessage(id, "subscribe_events", null, null, null, null);
    }

    public record MessageError(
        @JsonProperty("code")
        String code,

        @JsonProperty("message")
        String message
    ) {
    }
}
