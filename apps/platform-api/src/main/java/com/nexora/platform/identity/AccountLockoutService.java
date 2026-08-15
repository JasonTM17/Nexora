package com.nexora.platform.identity;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application-layer account lockout after consecutive failed auth attempts.
 *
 * <p>Since authentication is delegated to an external provider (Supabase), this
 * service tracks failed API authorization attempts (e.g., invalid tokens,
 * membership failures) and gates re-authentication after a threshold.</p>
 *
 * <p>Lockout policy: N consecutive failures → lock for window minutes. A
 * successful authentication resets the counter.</p>
 */
@Service
@Profile("database")
public class AccountLockoutService {

    private final JdbcTemplate jdbc;
    private final int maxFailures;
    private final int lockoutMinutes;

    public AccountLockoutService(JdbcTemplate jdbc,
                                  @Value("${nexora.security.max-failed-logins:5}") int maxFailures,
                                  @Value("${nexora.security.lockout-minutes:15}") int lockoutMinutes) {
        this.jdbc = jdbc;
        this.maxFailures = maxFailures;
        this.lockoutMinutes = lockoutMinutes;
    }

    /** Check whether the account is currently locked. */
    public boolean isLocked(UUID subjectId) {
        OffsetDateTime lockedUntil = jdbc.queryForObject(
                "SELECT locked_until FROM nexora.profiles WHERE subject_id = ?",
                OffsetDateTime.class, subjectId);
        return lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now());
    }

    /** Record a failed auth attempt. Locks account if threshold reached. */
    @Transactional
    public void recordFailure(UUID subjectId) {
        Integer count = jdbc.queryForObject(
                "SELECT failed_login_count FROM nexora.profiles WHERE subject_id = ?",
                Integer.class, subjectId);
        if (count == null) {
            return;
        }
        int newCount = count + 1;
        if (newCount >= maxFailures) {
            jdbc.update(
                    "UPDATE nexora.profiles SET failed_login_count = ?, locked_until = ? WHERE subject_id = ?",
                    newCount, OffsetDateTime.now().plusMinutes(lockoutMinutes), subjectId);
        } else {
            jdbc.update(
                    "UPDATE nexora.profiles SET failed_login_count = ? WHERE subject_id = ?",
                    newCount, subjectId);
        }
    }

    /** Reset failure count on successful authentication. */
    @Transactional
    public void recordSuccess(UUID subjectId) {
        jdbc.update(
                "UPDATE nexora.profiles SET failed_login_count = 0, locked_until = NULL WHERE subject_id = ?",
                subjectId);
    }
}
