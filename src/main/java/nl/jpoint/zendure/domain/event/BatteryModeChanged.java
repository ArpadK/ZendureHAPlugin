package nl.jpoint.zendure.domain.event;

import nl.jpoint.zendure.domain.virtualentity.BatteryMode;
import org.springframework.context.ApplicationEvent;

public class BatteryModeChanged extends ApplicationEvent {
  private final BatteryMode previousMode;
  private final BatteryMode newMode;

  public BatteryModeChanged(Object source, BatteryMode previousMode, BatteryMode newMode) {
    super(source);
    this.previousMode = previousMode;
    this.newMode = newMode;
  }

  public BatteryMode getPreviousMode() {
    return previousMode;
  }

  public BatteryMode getNewMode() {
    return newMode;
  }
}
