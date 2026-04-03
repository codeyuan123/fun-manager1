package com.fundmanager.repository;

import com.fundmanager.domain.entity.FundHoldingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FundHoldingSnapshotRepository extends JpaRepository<FundHoldingSnapshot, Long> {
    List<FundHoldingSnapshot> findByFundCodeOrderByYearNumDescQuarterNumDescNavRatioDesc(String fundCode);

    List<FundHoldingSnapshot> findByFundCodeAndYearNumAndQuarterNumOrderByNavRatioDesc(
            String fundCode,
            Integer yearNum,
            Byte quarterNum
    );

    void deleteByFundCodeAndYearNumAndQuarterNum(String fundCode, Integer yearNum, Byte quarterNum);
}
