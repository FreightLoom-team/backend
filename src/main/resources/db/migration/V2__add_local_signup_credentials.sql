ALTER TABLE users
    ALTER COLUMN external_subject DROP NOT NULL,
    ADD COLUMN first_name VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN last_name VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN password_hash VARCHAR(100);

ALTER TABLE users
    ALTER COLUMN first_name DROP DEFAULT,
    ALTER COLUMN last_name DROP DEFAULT;

ALTER TABLE organization_members
    DROP CONSTRAINT organization_members_role_check,
    ADD CONSTRAINT organization_members_role_check
        CHECK (role IN ('OWNER', 'ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER', 'VIEWER'));
