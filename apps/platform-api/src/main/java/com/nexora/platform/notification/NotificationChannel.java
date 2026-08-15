package com.nexora.platform.notification;

import com.nexora.platform.tenant.TenantContext;
import java.util.UUID;

/**
 * Pluggable notification delivery channel.
 *
 * <p>Channels are best-effort: a channel failure must not fail the notification
 * creation. Implementations include in-app (Realtime descriptor) and email.</p>
 */
public interface NotificationChannel {

    /** Deliver a notification through this channel. */
    void deliver(TenantContext context, UUID notificationId,
                 NotificationService.CreateNotificationCommand command);
}
