package nl.jpoint.zendure.domain.event;

import org.springframework.context.ApplicationEvent;

public class HomeAssistantAuthenticatedEvent extends ApplicationEvent {
  public HomeAssistantAuthenticatedEvent(Object source) {
    super(source);
  }
}
