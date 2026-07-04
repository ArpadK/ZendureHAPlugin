package nl.jpoint.zendure.domain.virtualentity;

import nl.jpoint.zendure.domain.event.HomeAssistantAuthenticatedEvent;
import nl.jpoint.zendure.domain.event.StateChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Registry for plugin-owned virtual entities in Home Assistant.
 *
 * <p>Manages the set of owned entities, publishes their state to HA on startup and (re)auth,
 * and delivers inbound state changes to registered listeners.
 *
 * <p>Owned entities are published via the REST API and are not created/deleted dynamically —
 * they exist for the lifetime of the plugin.
 */
@Component
public class VirtualEntityRegistry {

    private static final Logger log = LoggerFactory.getLogger(VirtualEntityRegistry.class);

    private final HAClient haClient;
    private final List<VirtualEntity> registeredEntities = new ArrayList<>();
    private final Map<String, List<Consumer<String>>> stateChangeListeners = new HashMap<>();

    public VirtualEntityRegistry(HAClient haClient) {
        this.haClient = haClient;
    }

    /**
     * Register a virtual entity in the registry.
     * The entity is published to HA immediately if already authenticated.
     *
     * @param entity the entity to register
     */
    public void register(VirtualEntity entity) {
        registeredEntities.add(entity);
        log.debug("Registered virtual entity: {} ({})", entity.entityId(), entity.name());

        // Initialize listener list for this entity
        stateChangeListeners.putIfAbsent(entity.entityId(), new ArrayList<>());

        // If already authenticated, publish immediately
        if (haClient != null) {
            publishEntityState(entity);
        }
    }

    /**
     * Subscribe to state changes for a specific entity.
     * When the entity's state changes via inbound HA events, the listener is invoked with the new state.
     *
     * @param entityId the entity ID to listen to
     * @param listener a callback that receives the new state string
     */
    public void onStateChange(String entityId, Consumer<String> listener) {
        stateChangeListeners.computeIfAbsent(entityId, k -> new ArrayList<>()).add(listener);
        log.debug("Registered state change listener for: {}", entityId);
    }

    /**
     * Get all registered entities.
     */
    public List<VirtualEntity> getRegisteredEntities() {
        return new ArrayList<>(registeredEntities);
    }

    /**
     * Publish a single entity's state to Home Assistant.
     */
    private void publishEntityState(VirtualEntity entity) {
        try {
            haClient.setState(entity.entityId(), entity.state(), entity.attributes());
            log.debug("Published state for {}: {}", entity.entityId(), entity.state());
        } catch (Exception e) {
            log.warn("Error publishing entity state for {}: {}", entity.entityId(), e.getMessage(), e);
        }
    }

    /**
     * On Home Assistant authentication (initial or reconnect), republish all entity states.
     * This ensures entities survive plugin/HA restarts.
     */
    @EventListener
    public void onHomeAssistantAuthenticated(HomeAssistantAuthenticatedEvent event) {
        log.info("Home Assistant authenticated; republishing all virtual entities");
        for (VirtualEntity entity : registeredEntities) {
            publishEntityState(entity);
        }
    }

    /**
     * Handle inbound state changes from Home Assistant.
     * Delivers changes to registered listeners if the entity is in the registry.
     */
    @EventListener
    public void onStateChanged(StateChangedEvent event) {
        String entityId = event.getEntityId();
        String newState = event.getNewState();

        // Check if this is one of our owned entities
        boolean isOwned = registeredEntities.stream()
            .anyMatch(e -> e.entityId().equals(entityId));

        if (!isOwned) {
            return;
        }

        log.debug("Received state change for owned entity {}: {}", entityId, newState);

        // Notify all listeners for this entity
        List<Consumer<String>> listeners = stateChangeListeners.get(entityId);
        if (listeners != null) {
            for (Consumer<String> listener : listeners) {
                try {
                    listener.accept(newState);
                } catch (Exception e) {
                    log.error("Error in state change listener for {}: {}", entityId, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Manually update a select entity's state in memory and publish to HA.
     * Used by automations after successfully executing a command.
     *
     * @param entityId the entity ID
     * @param newState the new state value
     */
    public void updateSelectState(String entityId, String newState) {
        VirtualEntity entity = registeredEntities.stream()
            .filter(e -> e.entityId().equals(entityId))
            .findFirst()
            .orElse(null);

        if (entity == null) {
            log.warn("Cannot update state for unregistered entity: {}", entityId);
            return;
        }

        if (!(entity instanceof SelectEntity select)) {
            log.warn("Cannot update state for non-select entity: {}", entityId);
            return;
        }

        try {
            select.setState(newState);
            publishEntityState(select);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid state for select {}: {}", entityId, newState);
        }
    }

    /**
     * Update a sensor entity's state in memory and publish to HA.
     * Used by automations and pollers to update status sensors.
     *
     * @param entityId the entity ID
     * @param newState the new state value
     */
    public void updateSensorState(String entityId, String newState) {
        VirtualEntity entity = registeredEntities.stream()
            .filter(e -> e.entityId().equals(entityId))
            .findFirst()
            .orElse(null);

        if (entity == null) {
            log.warn("Cannot update state for unregistered entity: {}", entityId);
            return;
        }

        if (!(entity instanceof SensorEntity sensor)) {
            log.warn("Cannot update state for non-sensor entity: {}", entityId);
            return;
        }

        sensor.setState(newState);
        publishEntityState(sensor);
    }
}
