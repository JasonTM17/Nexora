package com.nexora.platform.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prometheus metrics configuration for the platform API.
 *
 * <p>Exposes standard JVM, HTTP, and custom business metrics via the
 * /actuator/prometheus endpoint. All metrics are tagged with service name
 * for multi-service dashboards.</p>
 */
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("service", "platform-api")
                .meterFilter(MeterFilter.deny(id -> {
                    // Filter out high-cardinality URIs (path params) from HTTP metrics
                    String uri = id.getTag("uri");
                    return uri != null && uri.matches("/api/v1/[^/]+/[^/]+/.*");
                }));
    }
}
