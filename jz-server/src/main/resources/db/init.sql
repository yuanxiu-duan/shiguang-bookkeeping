CREATE DATABASE IF NOT EXISTS moneykeeper DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE moneykeeper;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    nickname VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(50) NOT NULL,
    type ENUM('EXPENSE', 'INCOME') NOT NULL,
    icon VARCHAR(20),
    parent_id BIGINT,
    sort_order INT NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    account_book_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_type (user_id, type),
    INDEX idx_parent (parent_id),
    INDEX idx_account_book_id (account_book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    type ENUM('EXPENSE', 'INCOME') NOT NULL,
    category_id BIGINT NOT NULL,
    note VARCHAR(255),
    transaction_date DATE NOT NULL,
    account_book_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, transaction_date),
    INDEX idx_user_category (user_id, category_id),
    INDEX idx_user_type (user_id, type),
    INDEX idx_account_book_id (account_book_id),
    INDEX idx_user_account_book_date (user_id, account_book_id, transaction_date DESC),
    INDEX idx_user_account_book_type (user_id, account_book_id, type),
    INDEX idx_user_account_book_created (user_id, account_book_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT,
    month VARCHAR(7) NOT NULL,
    amount_limit DECIMAL(10,2) NOT NULL,
    alert_threshold INT NOT NULL DEFAULT 80,
    account_book_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_month_category (user_id, month, category_id),
    INDEX idx_user_month (user_id, month),
    INDEX idx_account_book_id (account_book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS budget_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    month VARCHAR(7) NOT NULL,
    alert_date DATE NOT NULL,
    is_notified TINYINT(1) NOT NULL DEFAULT 0,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    budget_limit DECIMAL(10,2) NOT NULL,
    used_amount DECIMAL(10,2) NOT NULL,
    percentage INT NOT NULL,
    over_amount DECIMAL(10,2),
    account_book_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_category_month_date (user_id, category_id, month, alert_date),
    INDEX idx_user_id (user_id),
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_account_book_id (account_book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(20) DEFAULT '#409EFF',
    account_book_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_name (user_id, name),
    INDEX idx_user_id (user_id),
    INDEX idx_account_book_id (account_book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS transaction_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_tag_id (tag_id),
    INDEX idx_transaction_tag_transaction_id (transaction_id),
    INDEX idx_transaction_tag_tag_id (tag_id),
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS account_books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_name (user_id, name),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS account_book_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_book_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    invited_by BIGINT,
    alias VARCHAR(50) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_book_user (account_book_id, user_id),
    KEY idx_user_status (user_id, status),
    KEY idx_book_status (account_book_id, status),
    CONSTRAINT fk_member_book FOREIGN KEY (account_book_id) REFERENCES account_books(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_inviter FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO categories (user_id, name, type, icon, parent_id, sort_order, is_default) VALUES
(NULL, '餐饮', 'EXPENSE', '🍽️', NULL, 1, TRUE),
(NULL, '交通', 'EXPENSE', '🚗', NULL, 2, TRUE),
(NULL, '日用品', 'EXPENSE', '🛒', NULL, 3, TRUE),
(NULL, '娱乐', 'EXPENSE', '🎮', NULL, 4, TRUE),
(NULL, '居住', 'EXPENSE', '🏠', NULL, 5, TRUE),
(NULL, '医疗', 'EXPENSE', '💊', NULL, 6, TRUE),
(NULL, '教育', 'EXPENSE', '📚', NULL, 7, TRUE),
(NULL, '其他支出', 'EXPENSE', '📤', NULL, 99, TRUE),
(NULL, '工资', 'INCOME', '💰', NULL, 1, TRUE),
(NULL, '奖金', 'INCOME', '🎁', NULL, 2, TRUE),
(NULL, '投资', 'INCOME', '📈', NULL, 3, TRUE),
(NULL, '兼职', 'INCOME', '💼', NULL, 4, TRUE),
(NULL, '其他收入', 'INCOME', '📥', NULL, 99, TRUE);
