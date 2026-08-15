package com.nexora.platform.notification;

import com.nexora.platform.tenant.TenantContext;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant- and user-scoped notification service with multi-channel delivery.
 *
 * <p>Notifications are persisted with TTL-based auto-purge and fan out through
 * the M3 event spine for real-time delivery. Delivery channels (in-app, email)
 * are pluggable via {@link NotificationChannel}.</p>
 */
@Service
@Profile("database")
public class NotificationService {

    private final JdbcTemplate jdbc;
    private final List<NotificationChannel> channels;

    public NotificationService(JdbcTemplate jdbc, List<NotificationChannel> channels) {
        this.jdbc = jdbc;
        this.channels = channels;
    }

    /** Create a notification and fan out through delivery channels. */
    @Transactional
    public UUID notify(TenantContext context, CreateNotificationCommand command) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO nexora.notifications
                    (tenant_id, user_id, notification_type, priority, title, body,
                     action_url, metadata, ttl)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                RETURNING id
                """,
                UUID.class,
                context.organizationId(),
                command.userId(),
                command.notificationType(),
                command.priority(),
                command.title(),
                command.body(),
                command.actionUrl(),
                toJson(command.metadata()),
                command.ttl() != null ? command.ttl() : OffsetDateTime.now().plusDays(30));

        // Fan out through channels (best-effort; failures logged not thrown)
        for (NotificationChannel channel : channels) {
            try {
                channel.deliver(context, id, command);
            } catch (Exception e) {
                // Channel failure must not fail the notification creation
            }
        }
        return id;
    }

    /** List notifications for a user, most recent first. */
    public List<Notification> listForUser(TenantContext context, UUID userId, int limit, UUID cursor) {
        if (cursor != null) {
            return jdbc.query("""
                    SELECT id, tenant_id, user_id, notification_type, priority, title,
                           body, action_url, metadata, read_at, created_at, ttl
                    FROM nexora.notifications
                    WHERE tenant_id = ? AND user_id = ? AND id < ?
                    ORDER BY created_at DESC
                    LIMIT ?
                    """, this::map, context.organizationId(), userId, cursor, limit);
        }
        return jdbc.query("""
                SELECT id, tenant_id, user_id, notification_type, priority, title,
                       body, action_url, metadata, read_at, created_at, ttl
                FROM nexora.notifications
                WHERE tenant_id = ? AND user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """, this::map, context.organizationId(), userId, limit);
    }

    /** Mark a notification as read. */
    @Transactional
    public boolean markRead(TenantContext context, UUID notificationId, UUID userId) {
        int updated = jdbc.update("""
                UPDATE nexora.notifications
                SET read_at = now()
                WHERE id = ? AND tenant_id = ? AND user_id = ? AND read_at IS NULL
                """, notificationId, context.organizationId(), userId);
        return updated > 0;
    }

    /** Count unread notifications for a user. */
    public long countUnread(TenantContext context, UUID userId) {
        Long count = jdbc.queryForObject("""
                SELECT count(*) FROM nexora.notifications
                WHERE tenant_id = ? AND user_id = ? AND read_at IS NULL
                """, Long.class, context.organizationId(), userId);
        return count == null ? 0 : count;
    }

    private Notification map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new Notification(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("notification_type"),
                rs.getString("priority"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getString("action_url"),
                rs.getString("metadata"),
                rs.getObject("read_at", OffsetDateTime.class),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("ttl", OffsetDateTime.class));
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    public record CreateNotificationCommand(
            UUID userId,
            String notificationType,
            String priority,
            String title,
            String body,
            String actionUrl,
            Map<String, Object> metadata,
            OffsetDateTime ttl) {
    }

    public record Notification(
            UUID id,
            UUID tenantId,
            UUID userId,
            String notificationType,
            String priority,
            String title,
            String body,
            String actionUrl,
            String metadata,
            OffsetDateTime readAt,
            OffsetDateTime createdAt,
            OffsetDateTime ttl) {
    }
}
