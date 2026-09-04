-- =============================================================================
-- V38: Access and labeling updates
-- - Restrict Employees menu access to Super Admin, Admin, Senior HR only
-- - Rename employee data menu to "Change Request (Employee Data)"
-- - Remove Product Owner role/designation and related grants
-- =============================================================================

UPDATE ERM_NAV_MENUS
SET MENU_TITLE = 'Change Request (Employee Data)'
WHERE MENU_CODE = 'employee-data';

DELETE rnm
FROM ERM_ROLE_NAV_MENUS rnm
JOIN ERM_NAV_MENUS m ON m.ID = rnm.MENU_ID
WHERE m.MENU_CODE = 'employees';

INSERT IGNORE INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'employees'
WHERE r.ROLE_NAME IN ('Super Admin', 'Admin', 'Senior HR');

DELETE rnm
FROM ERM_ROLE_NAV_MENUS rnm
JOIN ERM_ROLES r ON r.ID = rnm.ROLE_ID
WHERE r.ROLE_NAME = 'Product Owner';

DELETE ur
FROM ERM_USER_ROLES ur
JOIN ERM_ROLES r ON r.ID = ur.ROLE_ID
WHERE r.ROLE_NAME = 'Product Owner';

UPDATE ERM_USERS u
LEFT JOIN ERM_ROLES r ON r.ID = u.PRIMARY_ROLE_ID
SET u.PRIMARY_ROLE_ID = NULL
WHERE r.ROLE_NAME = 'Product Owner';

DELETE FROM ERM_DESIGNATION_HIERARCHY
WHERE DESIGNATION_ROLE_NAME = 'Product Owner'
   OR REPORTS_TO_ROLE_NAME = 'Product Owner';

DELETE FROM ERM_ROLES
WHERE ROLE_NAME = 'Product Owner';
