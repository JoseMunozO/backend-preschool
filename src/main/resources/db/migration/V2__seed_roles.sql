INSERT INTO roles (code, name, description)
VALUES
    ('SUPER_ADMIN', 'Super Admin', 'Full system access'),
    ('ADMIN', 'Admin', 'Administrative access'),
    ('DIRECTOR', 'Director', 'Director access'),
    ('TEACHER', 'Teacher', 'Teacher access'),
    ('FINANCE', 'Finance', 'Finance access'),
    ('PARENT', 'Parent', 'Parent or guardian portal access')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = COALESCE(roles.description, VALUES(description));
