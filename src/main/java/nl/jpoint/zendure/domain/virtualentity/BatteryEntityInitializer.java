package nl.jpoint.zendure.domain.virtualentity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initializes the battery virtual entities and registers them with the entity registry.
 * Called on application startup to create and register the control select and status sensors.
 */
@Component
public class BatteryEntityInitializer {

    private static final Logger log = LoggerFactory.getLogger(BatteryEntityInitializer.class);

    private final VirtualEntityRegistry registry;
    private final ObjectMapper objectMapper;

    public BatteryEntityInitializer(VirtualEntityRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    /**
     * On application startup, create and register the battery entities.
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("Initializing battery virtual entities");

        // Create and register the control select
        SelectEntity controlSelect = BatteryEntityFactory.createControlSelect(objectMapper);
        registry.register(controlSelect);

        // Create and register the mode status sensor
        SensorEntity modeSensor = BatteryEntityFactory.createModeSensor(objectMapper);
        registry.register(modeSensor);

        // Create and register the SOC status sensor
        SensorEntity socSensor = BatteryEntityFactory.createSocSensor(objectMapper);
        registry.register(socSensor);

        log.info("Battery virtual entities initialized: {} entities registered", 3);
    }
}
