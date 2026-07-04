package nl.jpoint.zendure.homeassistant;

import org.springframework.context.ApplicationEvent;

/**
 * Published when the HomeAssistant WebSocket client successfully authenticates.
 * Listeners can react to this event to initialize dependent subsystems.
 */
public class HomeAssistantAuthenticatedEvent extends ApplicationEvent {

    public HomeAssistantAuthenticatedEvent(Object source) {
        super(source);
    }
}
