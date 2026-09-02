DELETE rnm
FROM ERM_ROLE_NAV_MENUS rnm
JOIN ERM_ROLES r ON r.ID = rnm.ROLE_ID
JOIN ERM_NAV_MENUS m ON m.ID = rnm.MENU_ID
WHERE m.MENU_CODE = 'employees'
  AND r.ROLE_NAME IN ('Team Lead', 'IT Support Lead');
