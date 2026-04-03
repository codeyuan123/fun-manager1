ALTER TABLE fund_estimate
    ADD COLUMN estimate_source VARCHAR(32) NULL AFTER estimate_growth_rate,
    ADD COLUMN estimate_confidence VARCHAR(16) NULL AFTER estimate_source,
    ADD COLUMN holding_coverage_rate DECIMAL(10,4) NULL AFTER estimate_confidence,
    ADD COLUMN quoted_coverage_rate DECIMAL(10,4) NULL AFTER holding_coverage_rate;
