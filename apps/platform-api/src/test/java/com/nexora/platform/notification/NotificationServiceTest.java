package com.nexora.platform.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexora.platform.notification.NotificationService.CreateNotificationCommand;
import com.nexora.platform.notification.NotificationService.Notification;
import com.nexora.platform.tenant.TenantContext;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/** Unit tests for notification creation and retrieval. */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private NotificationChannel channel;

    private NotificationService service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final TenantContext context = new TenantContext(userId, orgId, null, 0L, "owner");

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new NotificationService(jdbc, List.of(channel));
    }

    @Test
    void notifyCreatesAndReturnsId() {
        UUID expectedId = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), eq(UUID.class), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(expectedId);

        var command = new CreateNotificationCommand(
                userId, "system.alert", "normal", "Welcome", "Hello!", null,
                Map.of(), OffsetDateTime.now().plusDays(30));

        UUID result = service.notify(context, command);

        assertEquals(expectedId, result);
        verify(channel).deliver(context, expectedId, command);
    }

    @Test
    void notifyDoesNotFailWhenChannelFails() {
        UUID expectedId = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), eq(UUID.class), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(expectedId);
        doThrow(new RuntimeException("channel down")).when(channel)
                .deliver(any(), any(), any());

        var command = new CreateNotificationCommand(
                userId, "system.alert", "normal", "Welcome", "Hello!", null,
                Map.of(), OffsetDateTime.now().plusDays(30));

        // Should NOT throw — channel failure is swallowed
        UUID result = service.notify(context, command);
        assertEquals(expectedId, result);
    }

    @Test
    void markReadReturnsTrueWhenUpdated() {
        when(jdbc.update(anyString(), any(), any(), any())).thenReturn(1);

        boolean result = service.markRead(context, UUID.randomUUID(), userId);

        assertTrue(result);
    }

    @Test
    void countUnreadReturnsZeroWhenNone() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(), any())).thenReturn(0L);

        long result = service.countUnread(context, userId);

        assertEquals(0L, result);
    }
}
