-- =============================================================================
-- V30: Add IT Support channel, Director, and Product Owner designations
--
-- New roles:
--   Application Support Specialist, IT Security, IT Support Lead,
--   IT Support Manager, Director, Product Owner
--
-- New hierarchy channels:
--   IT Support:  App Support Specialist ─┐
--                                         ├─> IT Support Lead -> IT Support Manager -> Director -> CTO -> CEO
--                IT Security            ─┘
--
--   Product:     Product Owner -> Director -> CTO
--
-- NOTE: Director is a shared reporting node that both channels converge into.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. New roles
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO ERM_ROLES (ROLE_NAME, ROLE_DESCRIPTION) VALUES
('Application Support Specialist', 'IT application support specialist'),
('IT Security',                    'IT security specialist'),
('IT Support Lead',                'IT support team lead'),
('IT Support Manager',             'IT support manager'),
('Director',                       'Director – technology / product'),
('Product Owner',                  'Product owner');

-- ---------------------------------------------------------------------------
-- 2. New designation hierarchy entries
-- ---------------------------------------------------------------------------

-- IT Support channel — lower to higher
INSERT INTO ERM_DESIGNATION_HIERARCHY (DESIGNATION_ROLE_NAME, REPORTS_TO_ROLE_NAME, SORT_ORDER, IS_ACTIVE)
SELECT 'Application Support Specialist', 'IT Support Lead', 115, 1
WHERE NOT EXISTS (SELECT 1 FROM ERM_DESIGNATION_HIERARCHY WHERE DESIGNATION_ROLE_NAME = 'Application Support Specialist');

INSERT INTO ERM_DESIGNATION_HIERARCHY (DESIGNATION_ROLE_NAME, REPORTS_TO_ROLE_NAME, SORT_ORDER, IS_ACTIVE)
SELECT 'IT Security', 'IT Support Lead', 120, 1
WHERE NOT EXISTS (SELECT 1 FROM ERM_DESIGNATION_HIERARCHY WHERE DESIGNATION_ROLE_NAME = 'IT Security');

INSERT INTO ERM_DESIGNATION_HIERARCHY (DESIGNATION_ROLE_NAME, REPORTS_TO_ROLE_NAME, SORT_ORDER, IS_ACTIVE)
SELECT 'IT Support Lead', 'IT Support Manager', 130, 1
WHERE NOT EXISTS (SELECT 1 FROM ERM_DESIGNATION_HIERARCHY WHERE DESIGNATION_ROLE_NAME = 'IT Support Lead');

INSERT INTO ERM_DESIGNATION_HIERARCHY (DESIGNATION_ROLE_NAME, REPORTS_TO_ROLE_NAME, SORT_ORDER, IS_ACTIVE)
SELECT 'IT Support Manager', 'Director', 140, 1
WHERE NOT EXISTS (SELECT 1 FROM ERM_DESIGNATION_HIERARCHY WHERE DESIGNATION_ROLE_NAME = 'IT Support Manager');

-- Director — shared convergence node, reports to CTO
INSERT INTO ERM_DESIGNATION_HIERARCHY (DESIGNATION_ROLE_NAME, REPORTS_TO_ROLE_NAME, SORT_ORDER, IS_ACTIVE)
SELECT 'Director', 'CTO', 150, 1
WHERE NOT EXISTS (SELECT 1 FROM ERM_DESIGNATION_HIERARCHY WHERE DESIGNATION_ROLE_NAME = 'Director');

-- CTO now explicitly reports to CEO (CTO was a terminal node previously)
INSERT INTO ERM_DESIGNATION_HIERARCHY (DESIGNATION_ROLE_NAME, REPORTS_TO_ROLE_NAME, SORT_ORDER, IS_ACTIVE)
SELECT 'CTO', 'CEO', 160, 1
WHERE NOT EXISTS (SELECT 1 FROM ERM_DESIGNATION_HIERARCHY WHERE DESIGNATION_ROLE_NAME = 'CTO');

-- Product Owner channel — reports to Director (-> CTO -> CEO via Director's chain)
INSERT INTO ERM_DESIGNATION_HIERARCHY (DESIGNATION_ROLE_NAME, REPORTS_TO_ROLE_NAME, SORT_ORDER, IS_ACTIVE)
SELECT 'Product Owner', 'Director', 155, 1
WHERE NOT EXISTS (SELECT 1 FROM ERM_DESIGNATION_HIERARCHY WHERE DESIGNATION_ROLE_NAME = 'Product Owner');

-- ---------------------------------------------------------------------------
-- 3. Demo users (password: Admin@123)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO ERM_USERS (USERNAME, EMAIL, PASSWORD_HASH, FULL_NAME, DEPARTMENT, EMPLOYMENT_STATUS, IS_ACTIVE) VALUES
('appsupport',      'appsupport@erm.local',      '$2a$10$YlYBXJIZ2GeCWVn1MaQbIOF7Pj1mC04lhlyiR.Jz7J4RSYeLW1PLq', 'App Support User',       'IT Support',  'Active', 1),
('itsecurity',      'itsecurity@erm.local',       '$2a$10$YlYBXJIZ2GeCWVn1MaQbIOF7Pj1mC04lhlyiR.Jz7J4RSYeLW1PLq', 'IT Security User',       'IT Security', 'Active', 1),
('itsupportlead',   'itsupportlead@erm.local',    '$2a$10$YlYBXJIZ2GeCWVn1MaQbIOF7Pj1mC04lhlyiR.Jz7J4RSYeLW1PLq', 'IT Support Lead User',   'IT Support',  'Active', 1),
('itsupportmanager','itsupportmanager@erm.local', '$2a$10$YlYBXJIZ2GeCWVn1MaQbIOF7Pj1mC04lhlyiR.Jz7J4RSYeLW1PLq', 'IT Support Manager User','IT Support',  'Active', 1),
('director',        'director@erm.local',          '$2a$10$YlYBXJIZ2GeCWVn1MaQbIOF7Pj1mC04lhlyiR.Jz7J4RSYeLW1PLq', 'Director User',          'Technology',  'Active', 1),
('productowner',    'productowner@erm.local',      '$2a$10$YlYBXJIZ2GeCWVn1MaQbIOF7Pj1mC04lhlyiR.Jz7J4RSYeLW1PLq', 'Product Owner User',     'Product',     'Active', 1);

-- ---------------------------------------------------------------------------
-- 4. Assign roles to demo users
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO ERM_USER_ROLES (USER_ID, ROLE_ID)
SELECT U.ID, R.ID FROM ERM_USERS U JOIN ERM_ROLES R ON R.ROLE_NAME = 'Application Support Specialist' WHERE U.USERNAME = 'appsupport'
UNION ALL
SELECT U.ID, R.ID FROM ERM_USERS U JOIN ERM_ROLES R ON R.ROLE_NAME = 'IT Security'                    WHERE U.USERNAME = 'itsecurity'
UNION ALL
SELECT U.ID, R.ID FROM ERM_USERS U JOIN ERM_ROLES R ON R.ROLE_NAME = 'IT Support Lead'                WHERE U.USERNAME = 'itsupportlead'
UNION ALL
SELECT U.ID, R.ID FROM ERM_USERS U JOIN ERM_ROLES R ON R.ROLE_NAME = 'IT Support Manager'             WHERE U.USERNAME = 'itsupportmanager'
UNION ALL
SELECT U.ID, R.ID FROM ERM_USERS U JOIN ERM_ROLES R ON R.ROLE_NAME = 'Director'                       WHERE U.USERNAME = 'director'
UNION ALL
SELECT U.ID, R.ID FROM ERM_USERS U JOIN ERM_ROLES R ON R.ROLE_NAME = 'Product Owner'                  WHERE U.USERNAME = 'productowner';

-- ---------------------------------------------------------------------------
-- 5. Set PRIMARY_ROLE_ID for new users (mirrors V20 pattern)
-- ---------------------------------------------------------------------------
UPDATE ERM_USERS u
JOIN (
    SELECT ur.USER_ID, MIN(ur.ROLE_ID) AS PRIMARY_ROLE_ID
    FROM ERM_USER_ROLES ur
    GROUP BY ur.USER_ID
) role_map ON role_map.USER_ID = u.ID
SET u.PRIMARY_ROLE_ID = role_map.PRIMARY_ROLE_ID
WHERE u.PRIMARY_ROLE_ID IS NULL
  AND u.USERNAME IN ('appsupport', 'itsecurity', 'itsupportlead', 'itsupportmanager', 'director', 'productowner');

-- ---------------------------------------------------------------------------
-- 6. Set reporting managers for new demo users
--    (IT Support chain: app/security -> support lead -> support mgr -> director -> cto)
--    (Product Owner chain: product owner -> director)
-- ---------------------------------------------------------------------------
UPDATE ERM_USERS u
SET u.REPORTING_MANAGER_ROLE_NAME = (
    SELECT h.REPORTS_TO_ROLE_NAME
    FROM ERM_USER_ROLES ur
    JOIN ERM_ROLES r ON r.ID = ur.ROLE_ID
    JOIN ERM_DESIGNATION_HIERARCHY h ON h.DESIGNATION_ROLE_NAME = r.ROLE_NAME
    WHERE ur.USER_ID = u.ID
      AND h.IS_ACTIVE = 1
    LIMIT 1
)
WHERE u.USERNAME IN ('appsupport', 'itsecurity', 'itsupportlead', 'itsupportmanager', 'director', 'productowner');

UPDATE ERM_USERS u
SET u.REPORTING_MANAGER_USER_ID = (
    SELECT MIN(m.ID)
    FROM ERM_ROLES mr
    JOIN ERM_USER_ROLES mur ON mur.ROLE_ID = mr.ID
    JOIN ERM_USERS m ON m.ID = mur.USER_ID
    WHERE mr.ROLE_NAME = u.REPORTING_MANAGER_ROLE_NAME
      AND m.IS_ACTIVE = 1
      AND LOWER(m.EMPLOYMENT_STATUS) = 'active'
      AND m.ID <> u.ID
)
WHERE u.USERNAME IN ('appsupport', 'itsecurity', 'itsupportlead', 'itsupportmanager', 'director', 'productowner')
  AND u.REPORTING_MANAGER_ROLE_NAME IS NOT NULL;

-- Also update CTO's reporting manager to CEO now that the chain is set
UPDATE ERM_USERS u
SET u.REPORTING_MANAGER_ROLE_NAME = 'CEO',
    u.REPORTING_MANAGER_USER_ID = (
        SELECT MIN(m.ID)
        FROM ERM_ROLES mr
        JOIN ERM_USER_ROLES mur ON mur.ROLE_ID = mr.ID
        JOIN ERM_USERS m ON m.ID = mur.USER_ID
        WHERE mr.ROLE_NAME = 'CEO'
          AND m.IS_ACTIVE = 1
          AND LOWER(m.EMPLOYMENT_STATUS) = 'active'
    )
WHERE u.USERNAME = 'cto';

-- ---------------------------------------------------------------------------
-- 7. Navigation menu grants for new roles
-- ---------------------------------------------------------------------------

-- Dashboard: all new roles
INSERT IGNORE INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'dashboard'
WHERE r.ROLE_NAME IN (
    'Application Support Specialist',
    'IT Security',
    'IT Support Lead',
    'IT Support Manager',
    'Director',
    'Product Owner'
);

-- Employees: IT Support Lead and above (IT Support Manager, Director)
INSERT IGNORE INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'employees'
WHERE r.ROLE_NAME IN (
    'IT Support Lead',
    'IT Support Manager',
    'Director'
);

-- Projects: IT Support Manager, Director, Product Owner
INSERT IGNORE INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'projects'
WHERE r.ROLE_NAME IN (
    'IT Support Manager',
    'Director',
    'Product Owner'
);

-- Attendance: individual contributors and leads
INSERT IGNORE INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'attendance'
WHERE r.ROLE_NAME IN (
    'Application Support Specialist',
    'IT Security',
    'IT Support Lead',
    'IT Support Manager'
);

-- Leaves: all new roles (V17 only covered roles that existed at that time)
INSERT IGNORE INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'leaves'
WHERE r.ROLE_NAME IN (
    'Application Support Specialist',
    'IT Security',
    'IT Support Lead',
    'IT Support Manager',
    'Director',
    'Product Owner'
);

-- Settings: Director (executive level, similar to CTO)
INSERT IGNORE INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'settings'
WHERE r.ROLE_NAME IN ('Director');
