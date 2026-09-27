-- Hibernate writes ERM_USER_ROLES through the many-to-many mapping, which only
-- supplies USER_ID and ROLE_ID. EmployeeRoleReferenceService fills EMPLOYEE_ID
-- immediately after the user/role association is flushed in the same transaction.
-- Keep the FK in place while allowing that short, transactional null state.
ALTER TABLE ERM_USER_ROLES MODIFY COLUMN EMPLOYEE_ID VARCHAR(50) NULL;
