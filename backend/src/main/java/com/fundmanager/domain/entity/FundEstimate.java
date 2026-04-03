package com.fundmanager.domain.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fund_estimate")
public class FundEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fund_code", nullable = false, length = 16)
    private String fundCode;

    @Column(name = "estimate_time", nullable = false)
    private LocalDateTime estimateTime;

    @Column(name = "estimate_nav", nullable = false, precision = 12, scale = 6)
    private BigDecimal estimateNav;

    @Column(name = "estimate_growth_rate", precision = 10, scale = 4)
    private BigDecimal estimateGrowthRate;

    @Column(name = "estimate_source", length = 32)
    private String estimateSource;

    @Column(name = "estimate_confidence", length = 16)
    private String estimateConfidence;

    @Column(name = "holding_coverage_rate", precision = 10, scale = 4)
    private BigDecimal holdingCoverageRate;

    @Column(name = "quoted_coverage_rate", precision = 10, scale = 4)
    private BigDecimal quotedCoverageRate;

    @Column(length = 64)
    private String source;

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

    public LocalDateTime getEstimateTime() {
        return estimateTime;
    }

    public void setEstimateTime(LocalDateTime estimateTime) {
        this.estimateTime = estimateTime;
    }

    public BigDecimal getEstimateNav() {
        return estimateNav;
    }

    public void setEstimateNav(BigDecimal estimateNav) {
        this.estimateNav = estimateNav;
    }

    public BigDecimal getEstimateGrowthRate() {
        return estimateGrowthRate;
    }

    public void setEstimateGrowthRate(BigDecimal estimateGrowthRate) {
        this.estimateGrowthRate = estimateGrowthRate;
    }

    public String getEstimateSource() {
        return estimateSource;
    }

    public void setEstimateSource(String estimateSource) {
        this.estimateSource = estimateSource;
    }

    public String getEstimateConfidence() {
        return estimateConfidence;
    }

    public void setEstimateConfidence(String estimateConfidence) {
        this.estimateConfidence = estimateConfidence;
    }

    public BigDecimal getHoldingCoverageRate() {
        return holdingCoverageRate;
    }

    public void setHoldingCoverageRate(BigDecimal holdingCoverageRate) {
        this.holdingCoverageRate = holdingCoverageRate;
    }

    public BigDecimal getQuotedCoverageRate() {
        return quotedCoverageRate;
    }

    public void setQuotedCoverageRate(BigDecimal quotedCoverageRate) {
        this.quotedCoverageRate = quotedCoverageRate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
