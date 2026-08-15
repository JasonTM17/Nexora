package com.nexora.platform.identity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/** Unit tests for account lockout logic. */
@ExtendWith(MockitoExtension.class)
class AccountLockoutServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    private AccountLockoutService service;
    private final UUID subjectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AccountLockoutService(jdbc, 5, 15);
    }

    @Test
    void notLockedWhenNoLockout() {
        when(jdbc.queryForObject(anyString(), eq(OffsetDateTime.class), eq(subjectId)))
                .thenReturn(null);

        assertFalse(service.isLocked(subjectId));
    }

    @Test
    void lockedWhenLockoutInFuture() {
        when(jdbc.queryForObject(anyString(), eq(OffsetDateTime.class), eq(subjectId)))
                .thenReturn(OffsetDateTime.now().plusMinutes(10));

        assertTrue(service.isLocked(subjectId));
    }

    @Test
    void notLockedWhenLockoutExpired() {
        when(jdbc.queryForObject(anyString(), eq(OffsetDateTime.class), eq(subjectId)))
                .thenReturn(OffsetDateTime.now().minusMinutes(1));

        assertFalse(service.isLocked(subjectId));
    }

    @Test
    void recordSuccessResetsCount() {
        service.recordSuccess(subjectId);
        verify(jdbc).update(anyString(), eq(subjectId));
    }

    @Test
    void recordFailureIncrementsBelowThreshold() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(subjectId)))
                .thenReturn(3);

        service.recordFailure(subjectId);
        verify(jdbc, times(1)).update(anyString(), any(), eq(subjectId));
    }

    @Test
    void recordFailureLocksAtThreshold() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(subjectId)))
                .thenReturn(4);

        service.recordFailure(subjectId);
        verify(jdbc, times(1)).update(anyString(), any(), any(), eq(subjectId));
    }
}
