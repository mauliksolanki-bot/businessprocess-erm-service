-- Grant Attendance to every existing role so all authenticated users can see the tab.
INSERT IGNORE INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
         JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'attendance';
