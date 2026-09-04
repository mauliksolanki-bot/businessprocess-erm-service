-- =============================================================================
-- V37: Remove extra support-specific roles created by V36
-- Reuse existing IT support roles only.
-- =============================================================================

DELETE ur
FROM ERM_USER_ROLES ur
JOIN ERM_ROLES r ON r.ID = ur.ROLE_ID
WHERE r.ROLE_NAME IN (
    'Support Agent',
    'Security Incident Analyst',
    'Support Manager',
    'Support Admin'
);

DELETE FROM ERM_ROLES
WHERE ROLE_NAME IN (
    'Support Agent',
    'Security Incident Analyst',
    'Support Manager',
    'Support Admin'
);

DELETE FROM ERM_DESIGNATION_HIERARCHY
WHERE DESIGNATION_ROLE_NAME IN (
    'Support Agent',
    'Security Incident Analyst',
    'Support Manager',
    'Support Admin'
);
