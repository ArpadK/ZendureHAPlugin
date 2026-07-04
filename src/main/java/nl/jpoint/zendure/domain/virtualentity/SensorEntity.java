package nl.jpoint.zendure.domain.virtualentity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A read-only sensor entity that reports a value and optional attributes.
 *
 * <p>Used for status sensors (mode, state-of-charge).
 */
public class SensorEntity implements VirtualEntity {

    private final String entityId;
    private final String name;
    private final String type;
    private String currentState;
    private String unitOfMeasurement;
    private final ObjectMapper objectMapper;

    /**
     * Create a sensor entity.
     *
     * @param entityId the entity ID (e.g., "sensor.zendure_battery_soc")
     * @param name the human-readable name
     * @param type the sensor type (e.g., "sensor")
     * @param initialState the initial state value
     * @param unitOfMeasurement optional unit (e.g., "%", "mode"), or null
     * @param objectMapper for building attributes JSON
     */
    public SensorEntity(
        String entityId,
        String name,
        String type,
        String initialState,
        String unitOfMeasurement,
        ObjectMapper objectMapper
    ) {
        this.entityId = entityId;
        this.name = name;
        this.type = type;
        this.currentState = initialState;
        this.unitOfMeasurement = unitOfMeasurement;
        this.objectMapper = objectMapper;
    }

    @Override
    public String entityId() {
        return entityId;
    }

    @Override
    public String state() {
        return currentState;
    }

    @Override
    public JsonNode attributes() {
        var attrs = objectMapper.createObjectNode();
        if (unitOfMeasurement != null) {
            attrs.put("unit_of_measurement", unitOfMeasurement);
        }
        return attrs;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Update the sensor state.
     *
     * @param newState the new state value
     */
    public void setState(String newState) {
        this.currentState = newState;
    }

    /**
     * Set the unit of measurement for this sensor.
     *
     * @param unit the unit string (e.g., "%", "mode")
     */
    public void setUnitOfMeasurement(String unit) {
        this.unitOfMeasurement = unit;
    }

    /**
     * Get the current unit of measurement.
     */
    public String getUnitOfMeasurement() {
        return unitOfMeasurement;
    }
}
