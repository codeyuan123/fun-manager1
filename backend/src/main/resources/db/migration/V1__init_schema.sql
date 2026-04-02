CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS fund_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(16) NOT NULL UNIQUE,
    fund_name VARCHAR(128) NOT NULL,
    fund_type VARCHAR(64),
    risk_level VARCHAR(32),
    management_company VARCHAR(128),
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS fund_nav (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(16) NOT NULL,
    nav_date DATE NOT NULL,
    unit_nav DECIMAL(12,6) NOT NULL,
    accumulated_nav DECIMAL(12,6),
    daily_growth_rate DECIMAL(10,4),
    source VARCHAR(64),
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_nav_code_date (fund_code, nav_date),
    KEY idx_nav_code_date (fund_code, nav_date)
);

CREATE TABLE IF NOT EXISTS fund_estimate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(16) NOT NULL,
    estimate_time DATETIME NOT NULL,
    estimate_nav DECIMAL(12,6) NOT NULL,
    estimate_growth_rate DECIMAL(10,4),
    source VARCHAR(64),
    created_at DATETIME NOT NULL,
    KEY idx_est_code_time (fund_code, estimate_time)
);

CREATE TABLE IF NOT EXISTS fund_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    fund_code VARCHAR(16) NOT NULL,
    transaction_type VARCHAR(16) NOT NULL,
    trade_date DATE NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    shares DECIMAL(18,4) NOT NULL,
    fee DECIMAL(18,2) NOT NULL,
    nav DECIMAL(12,6) NOT NULL,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL,
    KEY idx_tx_user_code (user_id, fund_code),
    KEY idx_tx_trade_date (trade_date)
);

CREATE TABLE IF NOT EXISTS fund_position (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    fund_code VARCHAR(16) NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL,
    total_shares DECIMAL(18,4) NOT NULL,
    average_cost_nav DECIMAL(12,6) NOT NULL,
    current_cost DECIMAL(18,2) NOT NULL,
    last_trade_date DATE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_pos_user_code (user_id, fund_code),
    KEY idx_pos_user (user_id)
);

CREATE TABLE IF NOT EXISTS fund_watchlist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    fund_code VARCHAR(16) NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_watch_user_code (user_id, fund_code),
    KEY idx_watch_user (user_id)
);
