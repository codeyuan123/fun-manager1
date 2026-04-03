package com.fundmanager.repository;

import com.fundmanager.domain.entity.FundEstimate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FundEstimateRepository extends JpaRepository<FundEstimate, Long> {
    List<FundEstimate> findByFundCodeOrderByEstimateTimeDesc(String fundCode, Pageable pageable);

    List<FundEstimate> findByFundCodeAndEstimateTimeBetweenOrderByEstimateTimeAsc(
            String fundCode,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<FundEstimate> findTopByFundCodeOrderByEstimateTimeDesc(String fundCode);
}
