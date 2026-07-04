package nl.jpoint.zendure.zendure;

import nl.jpoint.zendure.domain.device.DeviceClient;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * HTTP REST client for the Zendure zenSDK local API.
 *
 * <p>Provides methods to:
 * - READ device properties (SOC, mode, etc.) via GET /properties/report
 * - WRITE control properties (charge/discharge/standby) via POST /properties/write
 *
 * <p>Base URL is constructed from configured device IP and port.
 * All requests use Content-Type: application/json.
 */
public class ZendureRestClient implements DeviceClient {

    private static final Logger logger = LoggerFactory.getLogger(ZendureRestClient.class);

    private final RestClient restClient;
    private final String baseUrl;
    private final String deviceSerial;

    /**
     * Create a ZendureRestClient.
     *
     * @param deviceIp the device's IP address (e.g. "192.168.1.100")
     * @param devicePort the device's REST API port (e.g. 13554)
     * @param deviceSerial the device's serial number (used in write requests)
     * @param customizer optional RestClient customizer for additional configuration
     */
    public ZendureRestClient(
        String deviceIp,
        int devicePort,
        String deviceSerial,
        RestClientCustomizer... customizer) {
        this.baseUrl = "http://" + deviceIp + ":" + devicePort;
        this.deviceSerial = deviceSerial;

        RestClient.Builder builder = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", ZenSDKProtocol.CONTENT_TYPE);

        for (RestClientCustomizer c : customizer) {
            c.customize(builder);
        }

        this.restClient = builder.build();
    }

    /**
     * GET /properties/report — fetch all device properties.
     *
     * <p>Returns the raw response as a Map. Callers parse this map to extract
     * specific properties like SOC, mode, etc.
     *
     * @return an Optional containing the properties map, or empty if the request fails
     */
    @Override
    public Optional<Map<String, Object>> getPropertiesReport() {
        try {
            logger.debug("Fetching device report from {}{}", baseUrl, ZenSDKProtocol.Paths.REPORT);

            Map<String, Object> result = restClient.get()
                .uri(ZenSDKProtocol.Paths.REPORT)
                .retrieve()
                .body(Map.class);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            logger.warn(
                "Failed to fetch device report: {} ({})",
                e.getClass().getSimpleName(),
                e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * POST /properties/write — write control properties to the device.
     *
     * <p>Constructs a request body with the format:
     * {@code {"sn":"<serial>","properties":{...}}}
     *
     * <p>Note: The Spring RestClient will add Content-Type: application/json
     * automatically based on the default header set in the constructor.
     *
     * @param properties a map of property keys and values (e.g. acMode, inputLimit, etc.)
     * @return an Optional; present if the write succeeded, empty otherwise
     */
    @Override
    public Optional<Void> writeProperties(Map<String, Object> properties) {
        try {
            logger.debug("Writing properties to device: {}", properties);

            Map<String, Object> requestBody = Map.of(
                "sn", deviceSerial,
                "properties", properties
            );

            restClient.post()
                .uri(ZenSDKProtocol.Paths.WRITE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();

            logger.debug("Successfully wrote properties to device");
            return Optional.of((Void) null);
        } catch (Exception e) {
            logger.error(
                "Failed to write properties to device: {} ({})",
                e.getClass().getSimpleName(),
                e.getMessage());
            return Optional.empty();
        }
    }
}
