-- Campus Resource Exchange schema (MySQL 8.x)
-- Run with: mysql -u campus_app -p campus_exchange < database/schema.sql
-- The database itself is created once by an administrator (see README).

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS exchange_transactions;
DROP TABLE IF EXISTS resource_requests;
DROP TABLE IF EXISTS resources;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    user_id             INT AUTO_INCREMENT PRIMARY KEY,
    full_name           VARCHAR(120) NOT NULL,
    email               VARCHAR(190) NOT NULL,
    password_hash       VARCHAR(60) NOT NULL,
    student_id          VARCHAR(40) NULL,
    department          VARCHAR(100) NULL,
    year_of_study       TINYINT NULL,
    role                ENUM('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    verification_status ENUM('PENDING', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_student_id UNIQUE (student_id),
    CONSTRAINT chk_users_year CHECK (year_of_study IS NULL
                                     OR year_of_study BETWEEN 1 AND 8)
) ENGINE = InnoDB;

CREATE TABLE resources (
    resource_id     INT AUTO_INCREMENT PRIMARY KEY,
    owner_id        INT NOT NULL,
    title           VARCHAR(150) NOT NULL,
    description     TEXT NOT NULL,
    category        VARCHAR(80) NOT NULL,
    resource_type   ENUM('BOOK', 'LAB_EQUIPMENT', 'STUDY_MATERIAL', 'DAILY_ESSENTIAL') NOT NULL,
    `condition`     ENUM('NEW', 'LIKE_NEW', 'GOOD', 'FAIR') NOT NULL,
    exchange_type   ENUM('SELL', 'LEND', 'DONATE') NOT NULL,
    price           DECIMAL(10, 2) NULL,
    lending_terms   VARCHAR(500) NULL,
    course_code     VARCHAR(20) NULL,
    pickup_location VARCHAR(150) NOT NULL,
    image_path      VARCHAR(255) NULL,
    status          ENUM('AVAILABLE', 'RESERVED', 'LENT', 'UNAVAILABLE', 'INACTIVE')
                    NOT NULL DEFAULT 'AVAILABLE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_resources_owner FOREIGN KEY (owner_id)
        REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT chk_resources_price CHECK (price IS NULL OR price >= 0),
    INDEX idx_resources_search (status, exchange_type, resource_type, `condition`),
    INDEX idx_resources_course (course_code),
    INDEX idx_resources_owner (owner_id),
    INDEX idx_resources_price (price)
) ENGINE = InnoDB;

CREATE TABLE resource_requests (
    request_id   INT AUTO_INCREMENT PRIMARY KEY,
    resource_id  INT NOT NULL,
    requester_id INT NOT NULL,
    message      VARCHAR(1000) NULL,
    status       ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'COMPLETED')
                 NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                 ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_requests_resource FOREIGN KEY (resource_id)
        REFERENCES resources (resource_id) ON DELETE RESTRICT,
    CONSTRAINT fk_requests_requester FOREIGN KEY (requester_id)
        REFERENCES users (user_id) ON DELETE RESTRICT,
    INDEX idx_requests_resource_status (resource_id, status),
    INDEX idx_requests_requester_status (requester_id, status)
) ENGINE = InnoDB;

CREATE TABLE exchange_transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    request_id     INT NOT NULL,
    owner_id       INT NOT NULL,
    requester_id   INT NOT NULL,
    exchange_type  ENUM('SELL', 'LEND', 'DONATE') NOT NULL,
    status         ENUM('PENDING_HANDOVER', 'ACTIVE_LOAN', 'COMPLETED')
                   NOT NULL DEFAULT 'PENDING_HANDOVER',
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at   TIMESTAMP NULL,
    returned_at    TIMESTAMP NULL,
    CONSTRAINT uq_transactions_request UNIQUE (request_id),
    CONSTRAINT fk_transactions_request FOREIGN KEY (request_id)
        REFERENCES resource_requests (request_id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_owner FOREIGN KEY (owner_id)
        REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_requester FOREIGN KEY (requester_id)
        REFERENCES users (user_id) ON DELETE RESTRICT,
    INDEX idx_transactions_owner (owner_id, status),
    INDEX idx_transactions_requester (requester_id, status)
) ENGINE = InnoDB;

CREATE TABLE reviews (
    review_id      INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id INT NOT NULL,
    reviewer_id    INT NOT NULL,
    rating         TINYINT NOT NULL,
    comment        VARCHAR(1000) NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reviews_transaction UNIQUE (transaction_id),
    CONSTRAINT fk_reviews_transaction FOREIGN KEY (transaction_id)
        REFERENCES exchange_transactions (transaction_id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id)
        REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    INDEX idx_reviews_reviewer (reviewer_id)
) ENGINE = InnoDB;
