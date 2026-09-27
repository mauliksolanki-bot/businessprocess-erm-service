package com.org.erm.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Stores the HRBP selected for a project change request without coupling the
 * main change-request table to optional schema changes in older deployments. */
@Repository
public class ErmProjectChangeHrAssociationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ErmProjectChangeHrAssociationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void save(Long projectChangeRequestId, Long associatedHrUserId) {
        jdbcTemplate.update("""
                INSERT INTO ERM_PROJECT_CHANGE_HRBP (PROJECT_CHANGE_REQUEST_ID, ASSOCIATED_HR_USER_ID)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE ASSOCIATED_HR_USER_ID = VALUES(ASSOCIATED_HR_USER_ID)
                """, projectChangeRequestId, associatedHrUserId);
    }

    @Transactional(readOnly = true)
    public Optional<Long> findAssociatedHrUserId(Long projectChangeRequestId) {
        return jdbcTemplate.query("""
                SELECT ASSOCIATED_HR_USER_ID
                FROM ERM_PROJECT_CHANGE_HRBP
                WHERE PROJECT_CHANGE_REQUEST_ID = ?
                """, resultSet -> resultSet.next() ? Optional.of(resultSet.getLong(1)) : Optional.empty(),
                projectChangeRequestId);
    }
}
