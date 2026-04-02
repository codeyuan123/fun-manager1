package com.fundmanager.repository;

import com.fundmanager.domain.entity.FundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FundTransactionRepository extends JpaRepository<FundTransaction, Long> {
    List<FundTransaction> findByUserIdAndFundCodeOrderByTradeDateDescIdDesc(Long userId, String fundCode);
}
