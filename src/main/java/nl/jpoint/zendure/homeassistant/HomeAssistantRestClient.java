package nl.jpoint.zendure.homeassistant;

import nl.jpoint.zendure.domain.virtualentity.HAClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * REST client for Home Assistant /api/states endpoint.
 * Used to read and write entity state values.
 */
@Component
public class HomeAssistantRestClient implements HAClient {

    private static final Logger log = LoggerFactory.getLogger(HomeAssistantRestClient.class);

    private final RestClient restClient;
    private final HomeAssistantProperties properties;
    private final ObjectMapper objectMapper;

    public HomeAssistantRestClient(
        HomeAssistantProperties properties,
        ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
            .baseUrl(properties.restUrl())
            .defaultHeader("Authorization", "Bearer " + properties.token())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    /**
     * Read the current state of an entity from Home Assistant.
     *
     * @param entityId the entity ID (e.g., "input_select.my_select")
     * @return the state value, or null if not found
     */
    @Override
    public String getState(String entityId) {
        try {
            String response = restClient.get()
                .uri("/states/{entity_id}", entityId)
                .retrieve()
                .body(String.class);

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode stateNode = root.get("state");
                if (stateNode != null) {
                    return stateNode.asText();
                }
            }
            return null;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                log.debug("Entity not found: {}", entityId);
                return null;
            }
            log.warn("Failed to get state for {}: {}", entityId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Error reading entity state {}: {}", entityId, e.getMessage());
            return null;
        }
    }

    /**
     * Update the state of an entity in Home Assistant.
     *
     * @param entityId the entity ID
     * @param state    the new state value
     * @param attributes optional attributes JSON object
     * @throws Exception if the update fails
     */
    @Override
    public void setState(String entityId, String state, JsonNode attributes) throws Exception {
        String body = buildStateUpdateBody(state, attributes);

        restClient.post()
            .uri("/states/{entity_id}", entityId)
            .body(body)
            .retrieve()
            .body(String.class);

        log.debug("State updated for {}: {}", entityId, state);
    }

    /**
     * Update the state of an entity without attributes.
     */
    public void setState(String entityId, String state) throws Exception {
        setState(entityId, state, null);
    }

    /**
     * Build the JSON body for a state update request.
     */
    private String buildStateUpdateBody(String state, JsonNode attributes) throws Exception {
        var body = objectMapper.createObjectNode();
        body.put("state", state);
        if (attributes != null) {
            body.set("attributes", attributes);
        }
        return objectMapper.writeValueAsString(body);
    }
}
