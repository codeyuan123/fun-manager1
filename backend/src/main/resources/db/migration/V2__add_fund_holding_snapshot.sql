CREATE TABLE IF NOT EXISTS fund_holding_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(16) NOT NULL,
    year_num INT NOT NULL,
    quarter_num TINYINT NOT NULL,
    report_date DATE,
    stock_code VARCHAR(16) NOT NULL,
    stock_name VARCHAR(128) NOT NULL,
    nav_ratio DECIMAL(10,4),
    holding_shares DECIMAL(18,4),
    holding_market_value DECIMAL(18,2),
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_fund_holding_snapshot (fund_code, year_num, quarter_num, stock_code),
    KEY idx_fund_holding_query (fund_code, year_num, quarter_num)
);
