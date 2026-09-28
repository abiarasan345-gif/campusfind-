CREATE DATABASE IF NOT EXISTS campusfind;
USE campusfind;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS found_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    item_name VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    location VARCHAR(200) NOT NULL,
    date_found DATE NOT NULL,
    finder_phone_number VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    case_state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_found_staff FOREIGN KEY (staff_user_id) REFERENCES users(id),
    CONSTRAINT fk_found_category FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_found_status (status),
    INDEX idx_found_category (category_id),
    INDEX idx_found_location (location)
);

CREATE TABLE IF NOT EXISTS lost_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    item_name VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    location VARCHAR(200) NOT NULL,
    date_lost DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    case_state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    matched_found_item_id BIGINT UNIQUE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lost_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_lost_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_lost_matched_found FOREIGN KEY (matched_found_item_id) REFERENCES found_items(id),
    INDEX idx_lost_status (status),
    INDEX idx_lost_category (category_id),
    INDEX idx_lost_location (location)
);

INSERT INTO categories (category_name) VALUES
('ID Card'),('Bottle'),('Charger'),('Calculator'),('Mobile Phone'),
('Wallet'),('Keys'),('Bag'),('Books'),('Other')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name);
