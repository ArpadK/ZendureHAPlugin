package nl.jpoint.zendure.domain.virtualentity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A select (choice) entity that can be set to one of several discrete options.
 *
 * <p>Used for the battery control select (Fast Charge, Fast Discharge, Standby).
 */
public class SelectEntity implements VirtualEntity {

    private final String entityId;
    private final String name;
    private final List<String> options;
    private String currentState;
    private final ObjectMapper objectMapper;

    /**
     * Create a select entity.
     *
     * @param entityId the entity ID (e.g., "select.zendure_battery_mode")
     * @param name the human-readable name
     * @param options the list of allowed option strings
     * @param defaultState the initial state (must be one of options)
     * @param objectMapper for building attributes JSON
     */
    public SelectEntity(
        String entityId,
        String name,
        List<String> options,
        String defaultState,
        ObjectMapper objectMapper
    ) {
        if (!options.contains(defaultState)) {
            throw new IllegalArgumentException("Default state must be one of the options");
        }
        this.entityId = entityId;
        this.name = name;
        this.options = new ArrayList<>(options);
        this.currentState = defaultState;
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
        ArrayNode optionsNode = objectMapper.createArrayNode();
        for (String option : options) {
            optionsNode.add(option);
        }
        attrs.set("options", optionsNode);
        return attrs;
    }

    @Override
    public String type() {
        return "select";
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Update the select state.
     *
     * @param newState the new state value (must be one of the options)
     * @throws IllegalArgumentException if newState is not a valid option
     */
    public void setState(String newState) {
        if (!options.contains(newState)) {
            throw new IllegalArgumentException("State '" + newState + "' is not a valid option");
        }
        this.currentState = newState;
    }

    /**
     * Get the list of available options.
     */
    public List<String> getOptions() {
        return new ArrayList<>(options);
    }
}
