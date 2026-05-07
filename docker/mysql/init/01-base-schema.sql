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
    (3, 'teacher@school.com', '+46000000003', '$2a$10$dk9rrTOVgWsP62/tL6ZzoOJcp8Na1tXcouCUunVkwWzW4dpc33Dl2', 'active', TRUE, TRUE);

INSERT INTO user_roles (user_id, role_id) VALUES
    (1, 2),
    (2, 6),
    (3, 4);

INSERT INTO class_groups (group_id, name, school_year, level_name, capacity, status) VALUES
    (1, 'Demo Group', '2026', 'Preschool', 20, 'active');

INSERT INTO students (student_id, student_code, first_name, last_name, birth_date, group_id, status, enrollment_date) VALUES
    (1, 'STU-001', 'Lucas', 'Andersson', '2020-04-12', 1, 'active', '2026-01-15');

INSERT INTO parents (parent_id, user_id, first_name, last_name, email, phone, status) VALUES
    (1, 2, 'Demo', 'Parent', 'parent.demo@school.com', '+46000000002', 'active');

INSERT INTO student_guardians (student_id, parent_id, relationship_type, is_primary_contact, is_billing_contact, is_authorized_pickup, lives_with_student) VALUES
    (1, 1, 'guardian', TRUE, TRUE, TRUE, TRUE);

INSERT INTO staff (staff_id, user_id, employee_code, first_name, last_name, email, phone, position_title, staff_type, hire_date, status) VALUES
    (1, 3, 'STAFF-001', 'Demo', 'Teacher', 'teacher@school.com', '+46000000003', 'Teacher', 'teacher', '2026-01-01', 'active');

INSERT INTO staff_group_assignments (staff_group_assignment_id, staff_id, group_id, role_in_group, is_primary, start_date) VALUES
    (1, 1, 1, 'teacher', TRUE, '2026-01-01');

INSERT INTO charge_types (charge_type_id, code, name, recurrence_type, default_amount, active) VALUES
    (1, 'MONTHLY_FEE', 'Monthly fee', 'monthly', 100.00, TRUE);

INSERT INTO student_charges (student_charge_id, student_id, charge_type_id, due_date, billing_period_start, billing_period_end, amount_due, status, description, created_by_user_id) VALUES
    (1, 1, 1, '2026-05-31', '2026-05-01', '2026-05-31', 100.00, 'pending', 'Demo monthly fee', 1);

INSERT INTO materials (material_id, sku, name, category, unit, quantity_on_hand, minimum_quantity, status) VALUES
    (1, 'DEMO-MAT-001', 'Demo paper', 'classroom', 'pack', 10, 2, 'active');

INSERT INTO schedule_slots (schedule_slot_id, group_id, primary_staff_id, day_of_week, start_time, end_time, activity_title, room_name) VALUES
    (1, 1, 1, 1, '09:00:00', '10:00:00', 'Morning circle', 'Room A');
