package com.nexora.platform.profile;

import com.nexora.platform.auth.DomainAccessException;
import com.nexora.platform.tenant.TransactionLocalDatabaseContext;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("database")
public class ProfileService {
    private final TransactionLocalDatabaseContext databaseContext;

    public ProfileService(TransactionLocalDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
    }

    public UserProfile get(UUID subjectId) {
        return databaseContext.forSubject(subjectId, jdbc -> find(jdbc, subjectId).stream()
                .findFirst()
                .orElseThrow(() -> new DomainAccessException(
                        HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "The profile does not exist.")));
    }

    public UserProfile update(UUID subjectId, ProfileUpdate update) {
        return databaseContext.forSubject(subjectId, jdbc -> {
            List<UserProfile> changed = update.expectedVersion() == 0
                    ? insert(jdbc, subjectId, update)
                    : update(jdbc, subjectId, update);
            if (changed.size() != 1) {
                throw new DomainAccessException(
                        HttpStatus.CONFLICT, "VERSION_CONFLICT", "The profile version is stale.");
            }
            return changed.getFirst();
        });
    }

    private List<UserProfile> find(JdbcTemplate jdbc, UUID subjectId) {
        return jdbc.query("""
                SELECT subject_id, display_name, locale, reduced_motion, high_contrast, version
                FROM nexora.profiles WHERE subject_id = ?
                """, ProfileService::map, subjectId);
    }

    private List<UserProfile> insert(JdbcTemplate jdbc, UUID subjectId, ProfileUpdate update) {
        return jdbc.query("""
                INSERT INTO nexora.profiles
                    (subject_id, display_name, locale, reduced_motion, high_contrast)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (subject_id) DO NOTHING
                RETURNING subject_id, display_name, locale, reduced_motion, high_contrast, version
                """, ProfileService::map, subjectId, update.displayName(), update.locale(),
                update.reducedMotion(), update.highContrast());
    }

    private List<UserProfile> update(JdbcTemplate jdbc, UUID subjectId, ProfileUpdate update) {
        return jdbc.query("""
                UPDATE nexora.profiles
                SET display_name = ?, locale = ?, reduced_motion = ?, high_contrast = ?
                WHERE subject_id = ? AND version = ?
                RETURNING subject_id, display_name, locale, reduced_motion, high_contrast, version
                """, ProfileService::map, update.displayName(), update.locale(), update.reducedMotion(),
                update.highContrast(), subjectId, update.expectedVersion());
    }

    private static UserProfile map(java.sql.ResultSet result, int row) throws java.sql.SQLException {
        return new UserProfile(
                result.getObject("subject_id", UUID.class),
                result.getString("display_name"),
                result.getString("locale"),
                result.getBoolean("reduced_motion"),
                result.getBoolean("high_contrast"),
                result.getLong("version"));
    }

    public record ProfileUpdate(
            String displayName, String locale, boolean reducedMotion, boolean highContrast, long expectedVersion) {
    }

    public record UserProfile(
            UUID subjectId,
            String displayName,
            String locale,
            boolean reducedMotion,
            boolean highContrast,
            long version) {
    }
}
