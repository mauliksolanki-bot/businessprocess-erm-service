package com.org.erm.repository;

import com.org.erm.model.ErmUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ErmUserRepository extends JpaRepository<ErmUser, Long> {

    Optional<ErmUser> findByUsernameIgnoreCase(String username);

    Optional<ErmUser> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("""
            SELECT DISTINCT u
            FROM ErmUser u
            JOIN u.roles role
            WHERE LOWER(role.name) = LOWER(:roleName)
              AND u.active = true
              AND LOWER(u.employmentStatus) = 'active'
            ORDER BY u.fullName ASC
            """)
    List<ErmUser> findActiveUsersByRoleName(@Param("roleName") String roleName);

    List<ErmUser> findAllByActiveTrueAndEmploymentStatusIgnoreCaseOrderByFullNameAsc(String employmentStatus);

    List<ErmUser> findAllByReportingManagerUserIdOrderByFullNameAsc(Long reportingManagerUserId);

    @Query("""
            SELECT DISTINCT u
            FROM ErmUser u
            LEFT JOIN FETCH u.roles role
            WHERE (:employeeName IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :employeeName, '%'))
                   OR LOWER(u.employeeId) LIKE LOWER(CONCAT('%', :employeeName, '%')))
              AND (:department IS NULL OR LOWER(u.department) LIKE LOWER(CONCAT('%', :department, '%')))
              AND (:status IS NULL OR LOWER(u.employmentStatus) = LOWER(:status))
              AND (
                :roleName IS NULL OR EXISTS (
                    SELECT 1
                    FROM ErmUser ux JOIN ux.roles rolex
                    WHERE ux.id = u.id
                      AND LOWER(rolex.name) LIKE LOWER(CONCAT('%', :roleName, '%'))
                )
              )
            ORDER BY u.fullName ASC
            """)
    List<ErmUser> searchEmployees(
            @Param("employeeName") String employeeName,
            @Param("roleName") String roleName,
            @Param("department") String department,
            @Param("status") String status
    );

    @Query(value = """
            SELECT DISTINCT u
            FROM ErmUser u
            WHERE (:employeeName IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :employeeName, '%'))
                   OR LOWER(u.employeeId) LIKE LOWER(CONCAT('%', :employeeName, '%')))
              AND (:department IS NULL OR LOWER(u.department) LIKE LOWER(CONCAT('%', :department, '%')))
              AND (:status IS NULL OR LOWER(u.employmentStatus) = LOWER(:status))
              AND (
                :roleName IS NULL OR EXISTS (
                    SELECT 1
                    FROM ErmUser ux JOIN ux.roles rolex
                    WHERE ux.id = u.id
                      AND LOWER(rolex.name) LIKE LOWER(CONCAT('%', :roleName, '%'))
                )
              )
            ORDER BY u.fullName ASC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT u)
            FROM ErmUser u
            LEFT JOIN u.roles role
            WHERE (:employeeName IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :employeeName, '%'))
                   OR LOWER(u.employeeId) LIKE LOWER(CONCAT('%', :employeeName, '%')))
              AND (:department IS NULL OR LOWER(u.department) LIKE LOWER(CONCAT('%', :department, '%')))
              AND (:status IS NULL OR LOWER(u.employmentStatus) = LOWER(:status))
              AND (
                :roleName IS NULL OR EXISTS (
                    SELECT 1
                    FROM ErmUser ux JOIN ux.roles rolex
                    WHERE ux.id = u.id
                      AND LOWER(rolex.name) LIKE LOWER(CONCAT('%', :roleName, '%'))
                )
              )
            """)
    Page<ErmUser> searchEmployeesPage(
            Pageable pageable,
            @Param("employeeName") String employeeName,
            @Param("roleName") String roleName,
            @Param("department") String department,
            @Param("status") String status
    );

    @Query("""
            SELECT u
            FROM ErmUser u
            WHERE u.active = true
              AND LOWER(u.employmentStatus) = 'active'
              AND (
                LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY
              CASE
                WHEN LOWER(u.fullName) LIKE LOWER(CONCAT(:query, '%')) THEN 0
                WHEN LOWER(u.username) LIKE LOWER(CONCAT(:query, '%')) THEN 1
                ELSE 1
              END,
              u.fullName ASC
            """)
    List<ErmUser> searchMentionableUsers(@Param("query") String query, Pageable pageable);

    @Query("""
            SELECT u
            FROM ErmUser u
            WHERE u.active = true
              AND LOWER(u.employmentStatus) = 'active'
              AND LOWER(u.username) IN :usernames
            """)
    List<ErmUser> findActiveUsersByUsernames(@Param("usernames") java.util.Set<String> usernames);
}
