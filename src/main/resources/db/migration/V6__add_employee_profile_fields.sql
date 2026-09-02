ALTER TABLE ERM_USERS
    ADD COLUMN FULL_NAME VARCHAR(150) NOT NULL DEFAULT 'Unknown User',
    ADD COLUMN DEPARTMENT VARCHAR(100) NOT NULL DEFAULT 'General',
    ADD COLUMN EMPLOYMENT_STATUS VARCHAR(30) NOT NULL DEFAULT 'Active';

UPDATE ERM_USERS
SET FULL_NAME = CASE USERNAME
    WHEN 'superadmin' THEN 'Super Admin User'
    WHEN 'admin' THEN 'Admin User'
    WHEN 'ceo' THEN 'Chief Executive Officer'
    WHEN 'cfo' THEN 'Chief Financial Officer'
    WHEN 'cto' THEN 'Chief Technology Officer'
    WHEN 'chro' THEN 'Chief Human Resources Officer'
    WHEN 'hrhead' THEN 'HR Head User'
    WHEN 'seniorhr' THEN 'Senior HR User'
    WHEN 'juniorhr' THEN 'Junior HR User'
    WHEN 'projectowner' THEN 'Project Owner User'
    WHEN 'programmanager' THEN 'Program Manager User'
    WHEN 'deliverymanager' THEN 'Delivery Manager User'
    WHEN 'projectmanager' THEN 'Project Manager User'
    WHEN 'teamlead' THEN 'Team Lead User'
    WHEN 'employee' THEN 'Employee User'
    WHEN 'intern' THEN 'Intern User'
    ELSE 'Unknown User'
END;

UPDATE ERM_USERS
SET DEPARTMENT = CASE USERNAME
    WHEN 'ceo' THEN 'Executive'
    WHEN 'cfo' THEN 'Finance'
    WHEN 'cto' THEN 'Engineering'
    WHEN 'chro' THEN 'Human Resources'
    WHEN 'hrhead' THEN 'Human Resources'
    WHEN 'seniorhr' THEN 'Human Resources'
    WHEN 'juniorhr' THEN 'Human Resources'
    WHEN 'projectowner' THEN 'Engineering'
    WHEN 'programmanager' THEN 'Program Management'
    WHEN 'deliverymanager' THEN 'Delivery'
    WHEN 'projectmanager' THEN 'Engineering'
    WHEN 'teamlead' THEN 'Engineering'
    WHEN 'employee' THEN 'Engineering'
    WHEN 'intern' THEN 'Engineering'
    ELSE 'Operations'
END;

UPDATE ERM_USERS
SET EMPLOYMENT_STATUS = CASE
    WHEN IS_ACTIVE = 1 THEN 'Active'
    ELSE 'Inactive'
END;

CREATE INDEX IDX_ERM_USERS_FULL_NAME ON ERM_USERS (FULL_NAME);
CREATE INDEX IDX_ERM_USERS_DEPARTMENT ON ERM_USERS (DEPARTMENT);
CREATE INDEX IDX_ERM_USERS_EMPLOYMENT_STATUS ON ERM_USERS (EMPLOYMENT_STATUS);