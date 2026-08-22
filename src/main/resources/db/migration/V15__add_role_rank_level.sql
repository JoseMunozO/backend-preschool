ALTER TABLE roles ADD COLUMN rank_level INT NOT NULL DEFAULT 0;

UPDATE roles SET rank_level = 100 WHERE code = 'SUPER_ADMIN';
UPDATE roles SET rank_level = 90 WHERE code IN ('ADMIN', 'DIRECTOR');
UPDATE roles SET rank_level = 10 WHERE code IN ('TEACHER', 'FINANCE');
UPDATE roles SET rank_level = 0 WHERE code = 'PARENT';
