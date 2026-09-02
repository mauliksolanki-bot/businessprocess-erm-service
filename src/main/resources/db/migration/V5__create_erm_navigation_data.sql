CREATE TABLE ERM_NAV_MENUS (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    MENU_CODE VARCHAR(100) NOT NULL UNIQUE,
    MENU_TITLE VARCHAR(120) NOT NULL,
    MENU_PATH VARCHAR(255) NOT NULL UNIQUE,
    MENU_ICON VARCHAR(100) NULL,
    SORT_ORDER INT NOT NULL,
    IS_ACTIVE TINYINT(1) NOT NULL DEFAULT 1,
    CREATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ERM_ROLE_NAV_MENUS (
    ROLE_ID BIGINT NOT NULL,
    MENU_ID BIGINT NOT NULL,
    PRIMARY KEY (ROLE_ID, MENU_ID),
    CONSTRAINT FK_ERM_ROLE_NAV_MENUS_ROLE FOREIGN KEY (ROLE_ID) REFERENCES ERM_ROLES (ID),
    CONSTRAINT FK_ERM_ROLE_NAV_MENUS_MENU FOREIGN KEY (MENU_ID) REFERENCES ERM_NAV_MENUS (ID)
);

INSERT INTO ERM_NAV_MENUS (MENU_CODE, MENU_TITLE, MENU_PATH, MENU_ICON, SORT_ORDER) VALUES
('dashboard', 'Dashboard', '/dashboard', 'layout-dashboard', 1),
('employees', 'Employees', '/employees', 'users', 2),
('projects', 'Projects', '/projects', 'kanban-square', 3),
('attendance', 'Attendance', '/attendance', 'calendar-check-2', 4),
('leaves', 'Leaves', '/leaves', 'plane', 5),
('settings', 'Settings', '/settings', 'settings', 6);

INSERT INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'dashboard';

INSERT INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'employees'
WHERE r.ROLE_NAME IN ('Super Admin', 'Admin', 'CHRO', 'HR Head', 'Senior HR', 'Junior HR', 'Team Lead');

INSERT INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'projects'
WHERE r.ROLE_NAME IN ('Super Admin', 'Admin', 'CTO', 'Project Owner', 'Program Manager', 'Delivery Manager', 'Project Manager', 'Team Lead');

INSERT INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'attendance'
WHERE r.ROLE_NAME IN ('Super Admin', 'Admin', 'CHRO', 'HR Head', 'Senior HR', 'Junior HR', 'Team Lead', 'Employee', 'Intern');

INSERT INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'leaves'
WHERE r.ROLE_NAME IN ('Super Admin', 'Admin', 'CHRO', 'HR Head', 'Senior HR', 'Junior HR', 'Team Lead', 'Employee', 'Intern');

INSERT INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'settings'
WHERE r.ROLE_NAME IN ('Super Admin', 'Admin', 'CEO', 'CFO', 'CTO', 'CHRO');
