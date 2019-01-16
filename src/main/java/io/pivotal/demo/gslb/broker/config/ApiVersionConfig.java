package io.pivotal.demo.gslb.broker.config;

import org.springframework.cloud.servicebroker.model.BrokerApiVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiVersionConfig {
    @Bean
    public BrokerApiVersion brokerApiVersion() {
        return new BrokerApiVersion(BrokerApiVersion.API_VERSION_ANY);
    }
}
