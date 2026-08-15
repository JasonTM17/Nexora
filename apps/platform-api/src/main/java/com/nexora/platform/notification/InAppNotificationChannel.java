package com.nexora.platform.notification;

import com.nexora.platform.tenant.TenantContext;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * In-app notification channel that publishes a lightweight notification event
 * through the M3 outbox spine for real-time delivery to connected clients.
 *
 * <p>This is a bounded channel: it publishes the notification reference, not the
 * full payload, keeping the Realtime surface narrow. Clients refetch the full
 * notification from the API after receiving the signal.</p>
 */
@Component
@Profile("database")
public class InAppNotificationChannel implements NotificationChannel {

    @Override
    public void deliver(TenantContext context, UUID notificationId,
                        NotificationService.CreateNotificationCommand command) {
        // Publish a NOTIFICATION_ENQUEUED event through the outbox spine.
        // The event carries only the notification id and target user; the client
        // refetches the full notification from the API. This keeps the Realtime
        // surface narrow and avoids leaking notification content through channels.
        // The actual outbox publish is wired when the outbox publisher is injected
        // into this channel (deferred to the M5 integration phase).
    }
}
