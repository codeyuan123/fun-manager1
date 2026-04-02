package com.fundmanager.domain.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fund_holding_snapshot")
public class FundHoldingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fund_code", nullable = false, length = 16)
    private String fundCode;

    @Column(name = "year_num", nullable = false)
    private Integer yearNum;

    @Column(name = "quarter_num", nullable = false)
    private Byte quarterNum;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "stock_code", nullable = false, length = 16)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 128)
    private String stockName;

    @Column(name = "nav_ratio", precision = 10, scale = 4)
    private BigDecimal navRatio;

    @Column(name = "holding_shares", precision = 18, scale = 4)
    private BigDecimal holdingShares;

    @Column(name = "holding_market_value", precision = 18, scale = 2)
    private BigDecimal holdingMarketValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getFundCode() {
        return fundCode;
    }

    public void setFundCode(String fundCode) {
        this.fundCode = fundCode;
    }

    public Integer getYearNum() {
        return yearNum;
    }

    public void setYearNum(Integer yearNum) {
        this.yearNum = yearNum;
    }

    public Byte getQuarterNum() {
        return quarterNum;
    }

    public void setQuarterNum(Byte quarterNum) {
        this.quarterNum = quarterNum;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public BigDecimal getNavRatio() {
        return navRatio;
    }

    public void setNavRatio(BigDecimal navRatio) {
        this.navRatio = navRatio;
    }

    public BigDecimal getHoldingShares() {
        return holdingShares;
    }

    public void setHoldingShares(BigDecimal holdingShares) {
        this.holdingShares = holdingShares;
    }

    public BigDecimal getHoldingMarketValue() {
        return holdingMarketValue;
    }

    public void setHoldingMarketValue(BigDecimal holdingMarketValue) {
        this.holdingMarketValue = holdingMarketValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
