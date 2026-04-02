package com.fundmanager.domain.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fund_position")
public class FundPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "fund_code", nullable = false, length = 16)
    private String fundCode;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_shares", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalShares;

    @Column(name = "average_cost_nav", nullable = false, precision = 12, scale = 6)
    private BigDecimal averageCostNav;

    @Column(name = "current_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal currentCost;

    @Column(name = "last_trade_date")
    private LocalDate lastTradeDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFundCode() {
        return fundCode;
    }

    public void setFundCode(String fundCode) {
        this.fundCode = fundCode;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalShares() {
        return totalShares;
    }

    public void setTotalShares(BigDecimal totalShares) {
        this.totalShares = totalShares;
    }

    public BigDecimal getAverageCostNav() {
        return averageCostNav;
    }

    public void setAverageCostNav(BigDecimal averageCostNav) {
        this.averageCostNav = averageCostNav;
    }

    public BigDecimal getCurrentCost() {
        return currentCost;
    }

    public void setCurrentCost(BigDecimal currentCost) {
        this.currentCost = currentCost;
    }

    public LocalDate getLastTradeDate() {
        return lastTradeDate;
    }

    public void setLastTradeDate(LocalDate lastTradeDate) {
        this.lastTradeDate = lastTradeDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
