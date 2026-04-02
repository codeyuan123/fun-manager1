package com.fundmanager.domain.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fund_nav")
public class FundNav {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fund_code", nullable = false, length = 16)
    private String fundCode;

    @Column(name = "nav_date", nullable = false)
    private LocalDate navDate;

    @Column(name = "unit_nav", nullable = false, precision = 12, scale = 6)
    private BigDecimal unitNav;

    @Column(name = "accumulated_nav", precision = 12, scale = 6)
    private BigDecimal accumulatedNav;

    @Column(name = "daily_growth_rate", precision = 10, scale = 4)
    private BigDecimal dailyGrowthRate;

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

    public LocalDate getNavDate() {
        return navDate;
    }

    public void setNavDate(LocalDate navDate) {
        this.navDate = navDate;
    }

    public BigDecimal getUnitNav() {
        return unitNav;
    }

    public void setUnitNav(BigDecimal unitNav) {
        this.unitNav = unitNav;
    }

    public BigDecimal getAccumulatedNav() {
        return accumulatedNav;
    }

    public void setAccumulatedNav(BigDecimal accumulatedNav) {
        this.accumulatedNav = accumulatedNav;
    }

    public BigDecimal getDailyGrowthRate() {
        return dailyGrowthRate;
    }

    public void setDailyGrowthRate(BigDecimal dailyGrowthRate) {
        this.dailyGrowthRate = dailyGrowthRate;
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
