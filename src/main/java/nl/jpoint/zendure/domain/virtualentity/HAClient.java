package nl.jpoint.zendure.domain.virtualentity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Domain-level abstraction for Home Assistant communication.
 * Infrastructure (HomeAssistantRestClient) implements this.
 */
public interface HAClient {
  void setState(String entityId, String state, JsonNode attributes) throws Exception;
  String getState(String entityId);
}
