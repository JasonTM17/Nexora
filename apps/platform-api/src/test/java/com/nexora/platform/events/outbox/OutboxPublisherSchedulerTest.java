package com.nexora.platform.events.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nexora.platform.PlatformApiApplication;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

class OutboxPublisherSchedulerTest {

    @Test
    void enabledSchedulerDelegatesOneControlledPublisherPass() {
        OutboxPublisher publisher = mock(OutboxPublisher.class);

        new OutboxPublisherScheduler(publisher).publishAvailable();

        verify(publisher).publishAvailable();
    }

    @Test
    void schedulerIsEnabledAndUsesBoundedConfigurableCadence() throws Exception {
        Method trigger = OutboxPublisherScheduler.class.getDeclaredMethod("publishAvailable");
        Scheduled scheduled = trigger.getAnnotation(Scheduled.class);

        assertThat(PlatformApiApplication.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
        assertThat(scheduled.initialDelayString()).isEqualTo("${nexora.outbox.publisher.initial-delay-millis:1000}");
        assertThat(scheduled.fixedDelayString()).isEqualTo("${nexora.outbox.publisher.poll-delay-millis:5000}");
    }

    @Test
    void enabledDatabaseRuntimeSchedulesAPublisherPassWhileDisabledRuntimeCreatesNoTrigger() throws Exception {
        CountDownLatch scheduled = new CountDownLatch(1);
        OutboxPublisher enabledPublisher = mock(OutboxPublisher.class);
        doAnswer(ignored -> {
            scheduled.countDown();
            return new OutboxPublisher.PublishResult(0, 0, 0, 0, 0);
        }).when(enabledPublisher).publishAvailable();

        try (AnnotationConfigApplicationContext enabled = context(true, enabledPublisher)) {
            assertThat(scheduled.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(enabled.getBeanProvider(OutboxPublisherScheduler.class).getIfAvailable()).isNotNull();
        }
        try (AnnotationConfigApplicationContext disabled = context(false, mock(OutboxPublisher.class))) {
            assertThat(disabled.getBeanProvider(OutboxPublisherScheduler.class).getIfAvailable()).isNull();
        }
    }

    private AnnotationConfigApplicationContext context(boolean enabled, OutboxPublisher publisher) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("database");
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("outbox-test", Map.of(
                "nexora.outbox.publisher.enabled", Boolean.toString(enabled),
                "nexora.outbox.publisher.initial-delay-millis", "1",
                "nexora.outbox.publisher.poll-delay-millis", "1000")));
        context.register(SchedulingConfiguration.class, OutboxPublisherScheduler.class);
        context.registerBean(OutboxPublisher.class, () -> publisher);
        context.refresh();
        return context;
    }

    @Configuration
    @EnableScheduling
    @Profile("database")
    static class SchedulingConfiguration {
    }
}
