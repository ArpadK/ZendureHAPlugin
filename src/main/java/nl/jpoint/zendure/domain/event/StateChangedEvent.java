package nl.jpoint.zendure.domain.event;

import org.springframework.context.ApplicationEvent;

public class StateChangedEvent extends ApplicationEvent {
  private final String entityId;
  private final String newState;

  public StateChangedEvent(Object source, String entityId, String newState) {
    super(source);
    this.entityId = entityId;
    this.newState = newState;
  }

  public String getEntityId() {
    return entityId;
  }

  public String getNewState() {
    return newState;
  }
}
