package nl.jpoint.zendure.domain.device;

import nl.jpoint.zendure.domain.value.SensorReading;
import java.util.Map;
import java.util.Optional;

/**
 * Domain-level abstraction for device communication.
 * Infrastructure (ZendureRestClient) implements this.
 */
public interface DeviceClient {
  Optional<Map<String, Object>> getPropertiesReport();
  Optional<Void> writeProperties(Map<String, Object> properties);
}
