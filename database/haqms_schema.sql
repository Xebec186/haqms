CREATE DATABASE haqms; 
USE haqms; 
CREATE TABLE patients (
    patient_id        BIGINT UNSIGNED      NOT NULL AUTO_INCREMENT,
    ghana_card_number VARCHAR(20)       NULL,
    first_name        VARCHAR(100)      NOT NULL,
    last_name         VARCHAR(100)      NOT NULL,
    date_of_birth     DATE              NOT NULL,
    gender            ENUM('MALE','FEMALE','OTHER') NOT NULL,
    phone_number      VARCHAR(15)       NOT NULL,
    email             VARCHAR(150)      NULL,
    address           VARCHAR(300)      NULL,
    created_at        DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active         BOOLEAN           NOT NULL DEFAULT TRUE,
    PRIMARY KEY (patient_id),
    UNIQUE KEY uq_ghana_card (ghana_card_number),
    UNIQUE KEY uq_phone (phone_number)
); 

CREATE TABLE departments (
    department_id   BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    name            VARCHAR(150)  NOT NULL,
    description     VARCHAR(300)  NULL,
    location        VARCHAR(150)  NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (department_id),
    UNIQUE KEY uq_dept_name (name)
); 

CREATE TABLE healthcare_providers (
    provider_id     BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    department_id   BIGINT UNSIGNED  NOT NULL,
    first_name      VARCHAR(100)  NOT NULL,
    last_name       VARCHAR(100)  NOT NULL,
    specialisation  VARCHAR(150)  NULL,
    license_number  VARCHAR(50)   NOT NULL,
    phone_number    VARCHAR(15)   NULL,
    email           VARCHAR(150)  NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (provider_id),
    UNIQUE KEY uq_license (license_number),
    CONSTRAINT fk_provider_dept
        FOREIGN KEY (department_id) REFERENCES departments(department_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE provider_schedules (
    schedule_id     BIGINT UNSIGNED        NOT NULL AUTO_INCREMENT,
    provider_id     BIGINT UNSIGNED        NOT NULL,
    schedule_date   DATE                NOT NULL,
    start_time      TIME                NOT NULL,
    end_time        TIME                NOT NULL,
    max_slots       TINYINT UNSIGNED    NOT NULL DEFAULT 20,
    is_available    BOOLEAN             NOT NULL DEFAULT TRUE,
	created_at          DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (schedule_id),
    UNIQUE KEY uq_provider_date_slot (provider_id, schedule_date, start_time),
    CONSTRAINT fk_schedule_provider
        FOREIGN KEY (provider_id) REFERENCES healthcare_providers(provider_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_times CHECK (end_time > start_time),
    CONSTRAINT chk_slots CHECK (max_slots BETWEEN 1 AND 100)
);  

CREATE TABLE appointments (
    appointment_id       BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT,
    patient_id           BIGINT UNSIGNED    NOT NULL,
    provider_id          BIGINT UNSIGNED    NOT NULL,
    department_id        BIGINT UNSIGNED    NOT NULL,
    schedule_id          BIGINT UNSIGNED    NOT NULL,
    appointment_date     DATE            NOT NULL,
    reason               VARCHAR(500)    NOT NULL,
    appointment_priority ENUM(
                             'EMERGENCY',
                             'URGENT',
                             'REGULAR'
                         )               NOT NULL DEFAULT 'REGULAR',
    status               ENUM(
                             'SCHEDULED','CONFIRMED',
                             'CANCELLED','NO_SHOW','COMPLETED'
                         )               NOT NULL DEFAULT 'SCHEDULED',
    booked_by_user_id    BIGINT UNSIGNED    NULL,
    cancellation_reason  VARCHAR(300)    NULL,
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (appointment_id),
    CONSTRAINT fk_appt_patient
        FOREIGN KEY (patient_id)    REFERENCES patients(patient_id)              ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_appt_provider
        FOREIGN KEY (provider_id)   REFERENCES healthcare_providers(provider_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_appt_department
        FOREIGN KEY (department_id) REFERENCES departments(department_id)        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_appt_schedule
        FOREIGN KEY (schedule_id)   REFERENCES provider_schedules(schedule_id)   ON UPDATE CASCADE ON DELETE RESTRICT
); 


CREATE TABLE queues (
    queue_id            BIGINT UNSIGNED      NOT NULL AUTO_INCREMENT,
    provider_id         BIGINT UNSIGNED      NOT NULL,
    queue_date          DATE              NOT NULL,
    status              ENUM('OPEN','PAUSED','CLOSED') NOT NULL DEFAULT 'OPEN',
    current_position    SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    total_registered    SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    opened_at           DATETIME          NULL,
    closed_at           DATETIME          NULL,
    created_at          DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (queue_id),
    UNIQUE KEY uq_provider_date (provider_id, queue_date),
    CONSTRAINT fk_queue_provider
        FOREIGN KEY (provider_id) REFERENCES healthcare_providers(provider_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE queue_entries (
    entry_id            BIGINT UNSIGNED      NOT NULL AUTO_INCREMENT,
    queue_id            BIGINT UNSIGNED      NOT NULL,
    appointment_id      BIGINT UNSIGNED      NOT NULL,
    patient_id          BIGINT UNSIGNED      NOT NULL,
    queue_position      SMALLINT UNSIGNED NOT NULL,
    status              ENUM('WAITING','CALLED','SERVING','COMPLETED','MISSED')
                                          NOT NULL DEFAULT 'WAITING',
    checked_in_at       DATETIME          NULL,
    called_at           DATETIME          NULL,
    serving_started_at  DATETIME          NULL,
    completed_at        DATETIME          NULL,
    wait_minutes        SMALLINT UNSIGNED NULL,
    created_at          DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (entry_id),
    UNIQUE KEY uq_appointment_entry (appointment_id),
    UNIQUE KEY uq_queue_position (queue_id, queue_position),
    CONSTRAINT fk_entry_queue        FOREIGN KEY (queue_id)       REFERENCES queues(queue_id)           ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_entry_appointment  FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_entry_patient      FOREIGN KEY (patient_id)     REFERENCES patients(patient_id)       ON UPDATE CASCADE ON DELETE RESTRICT
); 

CREATE TABLE roles (
    role_id     BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(50)         NOT NULL,
    description VARCHAR(200)        NULL,
    PRIMARY KEY (role_id),
    UNIQUE KEY uq_role_name (role_name)
);

-- Seed data:
INSERT INTO roles (role_name, description) VALUES
    ('ADMIN',         'Full system administration access'),
    ('PROVIDER',      'Healthcare provider -- view/manage own appointments, manage queues'),
    ('PATIENT',       'Patient -- book and view own appointments'); 
    
CREATE TABLE system_users (
    user_id         BIGINT UNSIGNED      NOT NULL AUTO_INCREMENT,
    role_id         BIGINT UNSIGNED  NOT NULL,
    username        VARCHAR(100)      NOT NULL,
    password_hash   VARCHAR(255)      NOT NULL,
    email           VARCHAR(150)      NOT NULL,
    patient_id      BIGINT UNSIGNED      NULL,
    provider_id     BIGINT UNSIGNED      NULL,
    is_active       BOOLEAN           NOT NULL DEFAULT TRUE,
    last_login      DATETIME          NULL,
    created_at      DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_user_email (email),
    CONSTRAINT fk_user_role     FOREIGN KEY (role_id)     REFERENCES roles(role_id)                   ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_user_patient  FOREIGN KEY (patient_id)  REFERENCES patients(patient_id)             ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_user_provider FOREIGN KEY (provider_id) REFERENCES healthcare_providers(provider_id) ON UPDATE CASCADE ON DELETE SET NULL
); 

CREATE TABLE audit_logs (
    log_id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED     NULL,
    table_name      VARCHAR(100)     NOT NULL,
    record_id       INT UNSIGNED     NOT NULL,
    action          ENUM('INSERT','UPDATE','DELETE') NOT NULL,
    old_values      JSON             NULL,
    new_values      JSON             NULL,
    ip_address      VARCHAR(45)      NULL,
    performed_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_audit_user (user_id),
    KEY idx_audit_table_record (table_name, record_id),
    KEY idx_audit_performed (performed_at)
); 