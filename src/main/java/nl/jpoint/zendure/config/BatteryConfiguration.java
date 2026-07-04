package nl.jpoint.zendure.config;

import nl.jpoint.zendure.domain.device.Battery;
import nl.jpoint.zendure.zendure.ZendureRestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the Battery device abstraction.
 */
@Configuration
public class BatteryConfiguration {

    /**
     * Create the Zendure REST client bean.
     *
     * @param properties Zendure device configuration properties
     * @return a configured ZendureRestClient
     */
    @Bean
    public ZendureRestClient zendureRestClient(ZendureDeviceProperties properties) {
        return new ZendureRestClient(
            properties.deviceIp(),
            properties.devicePort(),
            properties.deviceSerial()
        );
    }

    /**
     * Create the Battery device abstraction bean.
     *
     * @param restClient the REST client for zenSDK communication
     * @param properties Zendure device configuration properties
     * @return a configured Battery device
     */
    @Bean
    public Battery battery(ZendureRestClient restClient, ZendureDeviceProperties properties) {
        return new Battery(
            restClient,
            properties.maxChargePower(),
            properties.maxDischargePower()
        );
    }
}
