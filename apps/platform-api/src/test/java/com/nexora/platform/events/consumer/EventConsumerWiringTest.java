package com.nexora.platform.events.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.nexora.platform.PlatformApiApplication;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.MapPropertySource;

class EventConsumerWiringTest {

    @Test
    void consumerPropertiesResolveBoundedLocalDefaults() {
        EventConsumerProperties defaults = new EventConsumerProperties(
                true, null, null, null, null, null);

        assertThat(defaults.natsUrl()).isEqualTo("nats://127.0.0.1:14222");
        assertThat(defaults.stream()).isEqualTo("NEXORA_EVENTS");
        assertThat(defaults.subject()).isEqualTo("nexora.events.workflow");
        assertThat(defaults.durable()).isEqualTo("platform-api-event-ledger");
    }

    @Test
    void disabledDatabaseRuntimeCreatesNoConsumerSubscriptionBean() {
        try (AnnotationConfigApplicationContext disabled = context(false)) {
            assertThat(disabled.getBeanProvider(EventLedgerSubscription.class).getIfAvailable()).isNull();
        }
    }

    @Test
    void applicationScansConsumerConfigurationProperties() {
        assertThat(PlatformApiApplication.class.getAnnotation(
                org.springframework.boot.context.properties.ConfigurationPropertiesScan.class)
                .basePackageClasses()).contains(EventConsumerProperties.class);
    }

    private AnnotationConfigApplicationContext context(boolean enabled) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("database");
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("consumer-test", Map.of(
                "nexora.events.consumer.enabled", Boolean.toString(enabled))));
        context.registerBean(EventConsumerProperties.class, () -> new EventConsumerProperties(
                enabled, null, null, null, null, null));
        context.registerBean(NatsJetStreamEventHandler.class,
                () -> new NatsJetStreamEventHandler(mock(EventLedgerConsumer.class)));
        context.refresh();
        return context;
    }
}
