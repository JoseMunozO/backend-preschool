-- LOCAL DEVELOPMENT ONLY.
--
-- This script is mounted into docker-entrypoint-initdb.d for the local
-- docker-compose stack. It creates the schema AND seeds fake demo data
-- (students, parents, payments, etc.) so the app is immediately usable
-- for manual testing.
--
-- Do NOT point this at a real/production database. The schema itself is
-- also defined as a real Flyway migration (V1__initial_schema.sql), so a
-- clean production database only needs Flyway (spring.flyway.enabled=true)
-- against an empty schema - it will build the schema with zero demo data.
CREATE DATABASE IF NOT EXISTS preschool_admin_db_v2
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE preschool_admin_db_v2;

CREATE TABLE roles (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'active',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE class_groups (
    group_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    school_year VARCHAR(20) NOT NULL,
    level_name VARCHAR(100),
    capacity INT,
    age_min_months SMALLINT,
    age_max_months SMALLINT,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_class_groups_name_year (name, school_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE students (
    student_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_code VARCHAR(50) UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    group_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    enrollment_date DATE NOT NULL,
    withdrawal_date DATE,
    medical_notes TEXT,
    allergies TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_students_group
        FOREIGN KEY (group_id) REFERENCES class_groups(group_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE parents (
    parent_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(30),
    address VARCHAR(255),
    preferred_language VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_parents_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_guardians (
    student_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL,
    relationship_type VARCHAR(30) NOT NULL,
    is_primary_contact BOOLEAN NOT NULL DEFAULT FALSE,
    is_billing_contact BOOLEAN NOT NULL DEFAULT FALSE,
    is_authorized_pickup BOOLEAN NOT NULL DEFAULT TRUE,
    lives_with_student BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id, parent_id),
    CONSTRAINT fk_student_guardians_student
        FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_student_guardians_parent
        FOREIGN KEY (parent_id) REFERENCES parents(parent_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE staff (
    staff_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE,
    employee_code VARCHAR(50) UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(30),
    position_title VARCHAR(100) NOT NULL,
    staff_type VARCHAR(30) NOT NULL,
    hire_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE staff_group_assignments (
    staff_group_assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    role_in_group VARCHAR(30) NOT NULL DEFAULT 'teacher',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_staff_group_assignment (staff_id, group_id, start_date),
    CONSTRAINT fk_staff_group_assignments_staff
        FOREIGN KEY (staff_id) REFERENCES staff(staff_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_staff_group_assignments_group
        FOREIGN KEY (group_id) REFERENCES class_groups(group_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE charge_types (
    charge_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    recurrence_type VARCHAR(20) NOT NULL DEFAULT 'one_time',
    default_amount DECIMAL(10,2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_charges (
    student_charge_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    charge_type_id BIGINT NOT NULL,
    due_date DATE NOT NULL,
    billing_period_start DATE,
    billing_period_end DATE,
    amount_due DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    description VARCHAR(255),
    created_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_charges_student
        FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_student_charges_charge_type
        FOREIGN KEY (charge_type_id) REFERENCES charge_types(charge_type_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_student_charges_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(user_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT,
    received_by_staff_id BIGINT,
    payment_date DATE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    reference_number VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_parent
        FOREIGN KEY (parent_id) REFERENCES parents(parent_id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_payments_received_by
        FOREIGN KEY (received_by_staff_id) REFERENCES staff(staff_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payment_allocations (
    payment_allocation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    student_charge_id BIGINT NOT NULL,
    amount_allocated DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_payment_allocations (payment_id, student_charge_id),
    CONSTRAINT fk_payment_allocations_payment
        FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_payment_allocations_charge
        FOREIGN KEY (student_charge_id) REFERENCES student_charges(student_charge_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE materials (
    material_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) UNIQUE,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100),
    unit VARCHAR(50),
    quantity_on_hand INT NOT NULL DEFAULT 0,
    minimum_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE material_movements (
    material_movement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL,
    movement_type VARCHAR(10) NOT NULL,
    quantity INT NOT NULL,
    movement_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    performed_by_user_id BIGINT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_material_movements_material
        FOREIGN KEY (material_id) REFERENCES materials(material_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_material_movements_user
        FOREIGN KEY (performed_by_user_id) REFERENCES users(user_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE schedule_slots (
    schedule_slot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    primary_staff_id BIGINT,
    day_of_week TINYINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    activity_title VARCHAR(150) NOT NULL,
    room_name VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedule_slots_group
        FOREIGN KEY (group_id) REFERENCES class_groups(group_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_schedule_slots_staff
        FOREIGN KEY (primary_staff_id) REFERENCES staff(staff_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (role_id, code, name, description) VALUES
    (1, 'SUPER_ADMIN', 'Super Admin', 'Full system access'),
    (2, 'ADMIN', 'Admin', 'Administrative access'),
    (3, 'DIRECTOR', 'Director', 'Director access'),
    (4, 'TEACHER', 'Teacher', 'Teacher access'),
    (5, 'FINANCE', 'Finance', 'Finance access'),
    (6, 'PARENT', 'Parent', 'Parent or guardian portal access');

INSERT INTO users (user_id, email, phone, password_hash, status, email_verified, phone_verified) VALUES
    (1, 'admin@school.com', '+46000000001', '$2a$10$dk9rrTOVgWsP62/tL6ZzoOJcp8Na1tXcouCUunVkwWzW4dpc33Dl2', 'active', TRUE, TRUE),
    (2, 'parent.demo@school.com', '+46000000002', '$2a$10$qlnQJmgdSqezkI8XOte2XOvKu0P3jmuRv5iVD6x0DDUtJUSvYV0iK', 'active', TRUE, TRUE),
    (3, 'teacher@school.com', '+46000000003', '$2a$10$dk9rrTOVgWsP62/tL6ZzoOJcp8Na1tXcouCUunVkwWzW4dpc33Dl2', 'active', TRUE, TRUE),
    (4, 'director@school.com', '+46000000004', '$2a$10$dk9rrTOVgWsP62/tL6ZzoOJcp8Na1tXcouCUunVkwWzW4dpc33Dl2', 'active', TRUE, TRUE),
    (5, 'finance@school.com', '+46000000005', '$2a$10$dk9rrTOVgWsP62/tL6ZzoOJcp8Na1tXcouCUunVkwWzW4dpc33Dl2', 'active', TRUE, TRUE),
    (6, 'assistant@school.com', '+46000000006', '$2a$10$dk9rrTOVgWsP62/tL6ZzoOJcp8Na1tXcouCUunVkwWzW4dpc33Dl2', 'active', TRUE, TRUE),
    (7, 'parent.sofia@school.com', '+46000000007', '$2a$10$qlnQJmgdSqezkI8XOte2XOvKu0P3jmuRv5iVD6x0DDUtJUSvYV0iK', 'active', TRUE, TRUE),
    (8, 'parent.noah@school.com', '+46000000008', '$2a$10$qlnQJmgdSqezkI8XOte2XOvKu0P3jmuRv5iVD6x0DDUtJUSvYV0iK', 'active', TRUE, TRUE),
    (9, 'parent.emma@school.com', '+46000000009', '$2a$10$qlnQJmgdSqezkI8XOte2XOvKu0P3jmuRv5iVD6x0DDUtJUSvYV0iK', 'active', TRUE, TRUE);

INSERT INTO user_roles (user_id, role_id) VALUES
    (1, 2),
    (2, 6),
    (3, 4),
    (4, 3),
    (5, 5),
    (6, 4),
    (7, 6),
    (8, 6),
    (9, 6);

INSERT INTO class_groups (group_id, name, school_year, level_name, capacity, age_min_months, age_max_months, status, notes) VALUES
    (1, 'Sunflower Room', '2026', 'Toddlers', 14, 18, 36, 'active', 'Young toddler group'),
    (2, 'Rainbow Room', '2026', 'Preschool', 18, 36, 54, 'active', 'Mixed preschool group'),
    (3, 'Forest Room', '2026', 'Pre-K', 20, 54, 72, 'active', 'School readiness group');

INSERT INTO students (student_id, student_code, first_name, last_name, birth_date, group_id, status, enrollment_date, medical_notes, allergies, notes) VALUES
    (1, 'STU-001', 'Lucas', 'Andersson', '2020-04-12', 3, 'active', '2026-01-15', 'Uses inhaler if prescribed by family doctor', 'Peanuts', 'Confident reader, enjoys building blocks'),
    (2, 'STU-002', 'Sofia', 'Lindberg', '2021-08-03', 2, 'active', '2026-02-01', NULL, 'Lactose intolerance', 'Settling well after lunch'),
    (3, 'STU-003', 'Noah', 'Eriksson', '2022-01-20', 2, 'active', '2026-02-12', NULL, NULL, 'Enjoys music and outdoor play'),
    (4, 'STU-004', 'Emma', 'Nilsson', '2020-11-05', 3, 'active', '2026-01-22', 'Speech therapy follow-up monthly', NULL, 'Needs quiet transition time'),
    (5, 'STU-005', 'Maya', 'Garcia', '2023-03-18', 1, 'active', '2026-03-01', NULL, 'Eggs', 'New toddler enrollment'),
    (6, 'STU-006', 'Oliver', 'Brown', '2021-05-29', 2, 'pending', '2026-05-20', NULL, NULL, 'Pending final documents');

INSERT INTO parents (parent_id, user_id, first_name, last_name, email, phone, address, preferred_language, status, notes) VALUES
    (1, 2, 'Demo', 'Parent', 'parent.demo@school.com', '+46000000002', 'Demo Street 12', 'en', 'active', 'Main parent demo account'),
    (2, 7, 'Anna', 'Lindberg', 'parent.sofia@school.com', '+46000000007', 'Birch Road 8', 'sv', 'active', 'Primary contact for Sofia'),
    (3, 8, 'Erik', 'Eriksson', 'parent.noah@school.com', '+46000000008', 'Lake Avenue 4', 'sv', 'active', 'Billing contact for Noah and Maya'),
    (4, 9, 'Maria', 'Nilsson', 'parent.emma@school.com', '+46000000009', 'Forest Lane 16', 'en', 'active', 'Authorized pickup on weekdays'),
    (5, NULL, 'Carlos', 'Garcia', 'carlos.garcia@example.com', '+46000000010', 'Harbor Street 2', 'es', 'active', 'Secondary contact without portal user'),
    (6, NULL, 'Julia', 'Brown', 'julia.brown@example.com', '+46000000011', 'Market Square 5', 'en', 'inactive', 'Inactive demo contact');

INSERT INTO student_guardians (student_id, parent_id, relationship_type, is_primary_contact, is_billing_contact, is_authorized_pickup, lives_with_student) VALUES
    (1, 1, 'guardian', TRUE, TRUE, TRUE, TRUE),
    (2, 2, 'mother', TRUE, TRUE, TRUE, TRUE),
    (3, 3, 'father', TRUE, TRUE, TRUE, TRUE),
    (4, 4, 'mother', TRUE, TRUE, TRUE, TRUE),
    (5, 3, 'relative', FALSE, TRUE, TRUE, FALSE),
    (5, 5, 'father', TRUE, FALSE, TRUE, TRUE),
    (6, 6, 'mother', TRUE, TRUE, TRUE, TRUE);

INSERT INTO staff (staff_id, user_id, employee_code, first_name, last_name, email, phone, position_title, staff_type, hire_date, status) VALUES
    (1, 3, 'STAFF-001', 'Demo', 'Teacher', 'teacher@school.com', '+46000000003', 'Lead Teacher', 'teacher', '2026-01-01', 'active'),
    (2, 4, 'STAFF-002', 'Diana', 'Director', 'director@school.com', '+46000000004', 'Center Director', 'director', '2025-08-01', 'active'),
    (3, 5, 'STAFF-003', 'Felix', 'Finance', 'finance@school.com', '+46000000005', 'Finance Officer', 'admin', '2025-09-15', 'active'),
    (4, 6, 'STAFF-004', 'Sara', 'Assistant', 'assistant@school.com', '+46000000006', 'Assistant Teacher', 'teacher', '2026-02-01', 'active');

INSERT INTO staff_group_assignments (staff_group_assignment_id, staff_id, group_id, role_in_group, is_primary, start_date) VALUES
    (1, 1, 3, 'teacher', TRUE, '2026-01-01'),
    (2, 4, 1, 'teacher', TRUE, '2026-02-01'),
    (3, 1, 2, 'coordinator', FALSE, '2026-01-01'),
    (4, 4, 2, 'assistant', FALSE, '2026-02-01'),
    (5, 2, 1, 'coordinator', FALSE, '2026-01-01'),
    (6, 2, 2, 'coordinator', FALSE, '2026-01-01'),
    (7, 2, 3, 'coordinator', FALSE, '2026-01-01');

INSERT INTO charge_types (charge_type_id, code, name, recurrence_type, default_amount, active) VALUES
    (1, 'MONTHLY_FEE', 'Monthly fee', 'monthly', 950.00, TRUE),
    (2, 'MEAL_PLAN', 'Meal plan', 'monthly', 120.00, TRUE),
    (3, 'FIELD_TRIP', 'Field trip', 'one_time', 35.00, TRUE),
    (4, 'MATERIAL_FEE', 'Material fee', 'custom', 45.00, TRUE);

INSERT INTO student_charges (student_charge_id, student_id, charge_type_id, due_date, billing_period_start, billing_period_end, amount_due, status, description, created_by_user_id) VALUES
    (1, 1, 1, '2026-05-31', '2026-05-01', '2026-05-31', 950.00, 'pending', 'May tuition', 1),
    (2, 2, 1, '2026-05-31', '2026-05-01', '2026-05-31', 950.00, 'paid', 'May tuition', 1),
    (3, 2, 2, '2026-05-31', '2026-05-01', '2026-05-31', 120.00, 'paid', 'May meal plan', 1),
    (4, 3, 1, '2026-05-31', '2026-05-01', '2026-05-31', 950.00, 'partially_paid', 'May tuition', 1),
    (5, 4, 3, '2026-05-15', NULL, NULL, 35.00, 'overdue', 'Museum field trip', 1),
    (6, 5, 4, '2026-06-05', NULL, NULL, 45.00, 'pending', 'Starter craft kit', 1),
    (7, 6, 1, '2026-06-30', '2026-06-01', '2026-06-30', 950.00, 'pending', 'June tuition preview', 1);

INSERT INTO payments (payment_id, parent_id, received_by_staff_id, payment_date, total_amount, payment_method, reference_number, notes) VALUES
    (1, 2, 3, '2026-05-06', 1070.00, 'card', 'DEMO-CARD-001', 'Sofia May invoice paid in full'),
    (2, 3, 3, '2026-05-07', 400.00, 'bank_transfer', 'DEMO-TRF-001', 'Partial payment for Noah May tuition');

INSERT INTO payment_allocations (payment_allocation_id, payment_id, student_charge_id, amount_allocated) VALUES
    (1, 1, 2, 950.00),
    (2, 1, 3, 120.00),
    (3, 2, 4, 400.00);

INSERT INTO materials (material_id, sku, name, category, unit, quantity_on_hand, minimum_quantity, status, notes) VALUES
    (1, 'DEMO-MAT-001', 'A4 drawing paper', 'Arts and crafts', 'pack', 18, 6, 'active', 'Daily drawing and painting'),
    (2, 'DEMO-MAT-002', 'Washable paint set', 'Arts and crafts', 'set', 5, 3, 'active', 'Primary color classroom set'),
    (3, 'DEMO-MAT-003', 'Glue sticks', 'Arts and crafts', 'box', 2, 5, 'active', 'Low stock demo item'),
    (4, 'DEMO-MAT-004', 'Building blocks', 'Learning toys', 'box', 12, 4, 'active', 'Shared STEM shelf'),
    (5, 'DEMO-MAT-005', 'Picture books', 'Library', 'book', 42, 12, 'active', 'Classroom reading corner'),
    (6, 'DEMO-MAT-006', 'Nap mats', 'Rest time', 'unit', 16, 10, 'active', 'Cleaned weekly'),
    (7, 'DEMO-MAT-007', 'First aid bandages', 'Health and safety', 'box', 1, 4, 'active', 'Urgent restock example'),
    (8, 'DEMO-MAT-008', 'Outdoor chalk', 'Outdoor play', 'bucket', 9, 3, 'active', 'Playground activities'),
    (9, 'DEMO-MAT-009', 'Tablet protectors', 'Technology', 'unit', 4, 2, 'active', 'Digital portfolio devices'),
    (10, 'DEMO-MAT-010', 'Old sensory bins', 'Archived', 'unit', 0, 0, 'archived', 'Archived demo material');

INSERT INTO material_movements (material_movement_id, material_id, movement_type, quantity, performed_by_user_id, notes) VALUES
    (1, 1, 'in', 20, 1, 'Initial stock'),
    (2, 1, 'out', 2, 3, 'Weekly classroom use'),
    (3, 2, 'in', 6, 1, 'Supplier delivery'),
    (4, 2, 'out', 1, 4, 'Rainbow Room painting activity'),
    (5, 3, 'out', 3, 4, 'Craft activity restock trigger'),
    (6, 4, 'in', 12, 1, 'Initial stock'),
    (7, 7, 'out', 2, 2, 'Health room usage'),
    (8, 8, 'in', 10, 1, 'Outdoor play delivery'),
    (9, 8, 'out', 1, 3, 'Playground activity');

INSERT INTO schedule_slots (schedule_slot_id, group_id, primary_staff_id, day_of_week, start_time, end_time, activity_title, room_name, notes) VALUES
    (1, 1, 4, 1, '09:00:00', '09:30:00', 'Toddler morning circle', 'Sunflower Room', 'Songs and attendance'),
    (2, 1, 4, 2, '10:00:00', '10:45:00', 'Sensory play', 'Sunflower Room', 'Water table and textures'),
    (3, 2, 1, 1, '09:00:00', '10:00:00', 'Morning circle', 'Rainbow Room', 'Calendar and group talk'),
    (4, 2, 4, 3, '10:30:00', '11:15:00', 'Music and movement', 'Rainbow Room', 'Gross motor activity'),
    (5, 2, 1, 5, '13:00:00', '14:00:00', 'Art studio', 'Creative Atelier', 'Painting and collage'),
    (6, 3, 1, 1, '10:00:00', '11:00:00', 'Pre-K literacy', 'Forest Room', 'Story sequencing'),
    (7, 3, 1, 2, '13:00:00', '14:00:00', 'Outdoor science', 'Garden', 'Weather observation'),
    (8, 3, 2, 4, '15:00:00', '15:30:00', 'Family pickup briefing', 'Forest Room', 'Director visit');
