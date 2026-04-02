package com.fundmanager.repository;

import com.fundmanager.domain.entity.FundWatchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FundWatchlistRepository extends JpaRepository<FundWatchlist, Long> {
    List<FundWatchlist> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<FundWatchlist> findByUserIdAndFundCode(Long userId, String fundCode);

    void deleteByUserIdAndFundCode(Long userId, String fundCode);
}
