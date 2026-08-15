package com.nexora.platform.analytics;

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
 * Product analytics event collection and aggregation.
 *
 * <p>Events are appended tenant-scoped and support both real-time fan-out
 * (via the M3 event spine) and batch aggregation queries for dashboards.</p>
 */
@Service
@Profile("database")
public class AnalyticsService {

    private final JdbcTemplate jdbc;

    public AnalyticsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Record an analytics event. Idempotent when idempotencyKey is provided. */
    @Transactional
    public UUID recordEvent(TenantContext context, RecordEventCommand command) {
        return jdbc.queryForObject("""
                INSERT INTO nexora.analytics_events
                    (tenant_id, subject_id, event_type, resource_type, resource_id,
                     properties, client_context, idempotency_key)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)
                ON CONFLICT (tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL
                DO UPDATE SET idempotency_key = EXCLUDED.idempotency_key
                RETURNING id
                """,
                UUID.class,
                context.organizationId(),
                context.subjectId(),
                command.eventType(),
                command.resourceType(),
                command.resourceId(),
                toJson(command.properties()),
                toJson(command.clientContext()),
                command.idempotencyKey());
    }

    /** Aggregate event counts by type for a time window. */
    public List<EventCount> aggregateByType(TenantContext context, OffsetDateTime since) {
        return jdbc.query("""
                SELECT event_type, count(*) AS event_count
                FROM nexora.analytics_events
                WHERE tenant_id = ? AND recorded_at >= ?
                GROUP BY event_type
                ORDER BY event_count DESC
                """,
                (rs, row) -> new EventCount(
                        rs.getString("event_type"),
                        rs.getLong("event_count")),
                context.organizationId(), since);
    }

    /** Recent events for the tenant, cursor-paginated. */
    public List<AnalyticsEvent> recentEvents(TenantContext context, int limit, UUID cursor) {
        if (cursor != null) {
            return jdbc.query("""
                    SELECT id, tenant_id, subject_id, event_type, resource_type, resource_id,
                           properties, client_context, recorded_at, idempotency_key
                    FROM nexora.analytics_events
                    WHERE tenant_id = ? AND id < ?
                    ORDER BY recorded_at DESC
                    LIMIT ?
                    """, this::map, context.organizationId(), cursor, limit);
        }
        return jdbc.query("""
                SELECT id, tenant_id, subject_id, event_type, resource_type, resource_id,
                       properties, client_context, recorded_at, idempotency_key
                FROM nexora.analytics_events
                WHERE tenant_id = ?
                ORDER BY recorded_at DESC
                LIMIT ?
                """, this::map, context.organizationId(), limit);
    }

    private AnalyticsEvent map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new AnalyticsEvent(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("subject_id", UUID.class),
                rs.getString("event_type"),
                rs.getString("resource_type"),
                rs.getObject("resource_id", UUID.class),
                rs.getString("properties"),
                rs.getString("client_context"),
                rs.getObject("recorded_at", OffsetDateTime.class),
                rs.getString("idempotency_key"));
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

    public record RecordEventCommand(
            String eventType,
            String resourceType,
            UUID resourceId,
            Map<String, Object> properties,
            Map<String, Object> clientContext,
            String idempotencyKey) {
    }

    public record AnalyticsEvent(
            UUID id,
            UUID tenantId,
            UUID subjectId,
            String eventType,
            String resourceType,
            UUID resourceId,
            String properties,
            String clientContext,
            OffsetDateTime recordedAt,
            String idempotencyKey) {
    }

    public record EventCount(String eventType, long count) {
    }
}
