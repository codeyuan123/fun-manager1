package com.fundmanager.repository;

import com.fundmanager.domain.entity.FundInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FundInfoRepository extends JpaRepository<FundInfo, Long> {
    Optional<FundInfo> findByFundCode(String fundCode);

    List<FundInfo> findTop20ByFundCodeContainingOrFundNameContainingOrderByFundCodeAsc(String code, String name);
}
