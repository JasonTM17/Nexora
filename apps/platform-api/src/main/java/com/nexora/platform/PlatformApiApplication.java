package com.nexora.platform;

import com.nexora.platform.config.PlatformProperties;
import com.nexora.platform.events.outbox.OutboxPublisherProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = {PlatformProperties.class, OutboxPublisherProperties.class})
@EnableScheduling
public class PlatformApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApiApplication.class, args);
    }
}
