package nl.jpoint.zendure.homeassistant;

import nl.jpoint.zendure.homeassistant.dto.StateChangedEvent;
import org.springframework.context.ApplicationEvent;

/**
 * Application event published when a watched entity's state changes in Home Assistant.
 * Allows interested components to react to state changes without direct coupling
 * to the WebSocket layer.
 */
public class StateChangedApplicationEvent extends ApplicationEvent {

    private final StateChangedEvent event;

    public StateChangedApplicationEvent(Object source, StateChangedEvent event) {
        super(source);
        this.event = event;
    }

    public StateChangedEvent getEvent() {
        return event;
    }

    public String getEntityId() {
        return event.entityId();
    }

    public String getNewState() {
        return event.newState() != null ? event.newState().state() : null;
    }
}
