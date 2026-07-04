package nl.jpoint.zendure.homeassistant;

import nl.jpoint.zendure.domain.event.StateChangedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Listens for state_changed events from Home Assistant WebSocket
 * and dispatches them as application events for watched/owned entity IDs.
 */
@Component
public class StateChangedEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(StateChangedEventDispatcher.class);

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Set<String> watchedEntityIds = new HashSet<>();

    public StateChangedEventDispatcher(
        ApplicationEventPublisher eventPublisher,
        ObjectMapper objectMapper
    ) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Register an entity ID to be monitored for state changes.
     *
     * @param entityId the entity ID to watch (e.g., "input_select.my_select")
     */
    public void watch(String entityId) {
        watchedEntityIds.add(entityId);
        log.debug("Watching entity: {}", entityId);
    }

    /**
     * Unregister an entity ID from being monitored.
     */
    public void unwatch(String entityId) {
        watchedEntityIds.remove(entityId);
        log.debug("Stopped watching entity: {}", entityId);
    }

    /**
     * Process an incoming Home Assistant event and dispatch if it matches a watched entity.
     * This is called from HomeAssistantClient when a state_changed event is received.
     *
     * @param eventData the raw event data from Home Assistant
     */
    public void processStateChangedEvent(Object eventData) {
        try {
            nl.jpoint.zendure.homeassistant.dto.StateChangedEvent dtoEvent = objectMapper.convertValue(eventData, nl.jpoint.zendure.homeassistant.dto.StateChangedEvent.class);

            if (watchedEntityIds.contains(dtoEvent.entityId())) {
                log.debug("Dispatching state change for watched entity: {}", dtoEvent.entityId());
                eventPublisher.publishEvent(new StateChangedEvent(this, dtoEvent.entityId(), dtoEvent.newState().state()));
            }
        } catch (Exception e) {
            log.warn("Failed to parse state_changed event: {}", e.getMessage(), e);
        }
    }

    /**
     * Get the set of currently watched entity IDs.
     */
    public Set<String> getWatchedEntityIds() {
        return new HashSet<>(watchedEntityIds);
    }
}
