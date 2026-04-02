package com.fundmanager.repository;

import com.fundmanager.domain.entity.FundPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FundPositionRepository extends JpaRepository<FundPosition, Long> {
    List<FundPosition> findByUserIdOrderByFundCodeAsc(Long userId);

    Optional<FundPosition> findByUserIdAndFundCode(Long userId, String fundCode);
}
