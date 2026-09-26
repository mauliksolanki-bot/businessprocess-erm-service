package com.org.erm.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeRoleReferenceService {

    private final EntityManager entityManager;

    public EmployeeRoleReferenceService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void syncEmployeeIds(Long userId) {
        entityManager.flush();
        entityManager.createNativeQuery("""
                UPDATE ERM_USER_ROLES link
                JOIN ERM_USERS employee ON employee.ID = link.USER_ID
                SET link.EMPLOYEE_ID = employee.EMPLOYEE_ID
                WHERE link.USER_ID = :userId
                """)
                .setParameter("userId", userId)
                .executeUpdate();
    }
}
