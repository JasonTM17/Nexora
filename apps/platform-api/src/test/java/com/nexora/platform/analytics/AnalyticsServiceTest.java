package com.nexora.platform.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexora.platform.analytics.AnalyticsService.AnalyticsEvent;
import com.nexora.platform.analytics.AnalyticsService.EventCount;
import com.nexora.platform.analytics.AnalyticsService.RecordEventCommand;
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
import org.springframework.jdbc.core.RowMapper;

/** Unit tests for analytics event recording and aggregation. */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private AnalyticsService service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final TenantContext context = new TenantContext(subjectId, orgId, null, 0L, "owner");

    @Test
    void recordEventReturnsIdAndPassesTenant() {
        UUID expectedId = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), eq(UUID.class), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(expectedId);

        var command = new RecordEventCommand(
                "page.viewed", "page", UUID.randomUUID(),
                Map.of("path", "/home"), Map.of("referrer", "direct"), null);

        UUID result = service.recordEvent(context, command);

        assertEquals(expectedId, result);
        verify(jdbc).queryForObject(anyString(), eq(UUID.class), eq(orgId), eq(subjectId),
                eq("page.viewed"), any(), any(), any(), any(), any());
    }

    @Test
    void aggregateByTypeReturnsCounts() {
        List<EventCount> expected = List.of(
                new EventCount("page.viewed", 42L),
                new EventCount("flag.evaluated", 10L));
        doReturn(expected).when(jdbc).query(anyString(),
                any(RowMapper.class), eq(orgId), any(OffsetDateTime.class));

        List<EventCount> result = service.aggregateByType(context, OffsetDateTime.now().minusDays(7));

        assertEquals(2, result.size());
        assertEquals("page.viewed", result.get(0).eventType());
        assertEquals(42L, result.get(0).count());
    }

    @Test
    void recentEventsWithoutCursor() {
        List<AnalyticsEvent> expected = List.of();
        doReturn(expected).when(jdbc).query(anyString(),
                any(RowMapper.class), eq(orgId), eq(25));

        List<AnalyticsEvent> result = service.recentEvents(context, 25, null);

        assertNotNull(result);
    }
}
