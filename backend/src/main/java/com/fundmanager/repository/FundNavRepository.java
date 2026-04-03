package com.fundmanager.repository;

import com.fundmanager.domain.entity.FundNav;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FundNavRepository extends JpaRepository<FundNav, Long> {
    Optional<FundNav> findByFundCodeAndNavDate(String fundCode, LocalDate navDate);

    Optional<FundNav> findFirstByFundCodeAndNavDateLessThanEqualOrderByNavDateDesc(String fundCode, LocalDate navDate);

    List<FundNav> findByFundCodeOrderByNavDateDesc(String fundCode, Pageable pageable);

    List<FundNav> findByFundCodeOrderByNavDateAsc(String fundCode);

    List<FundNav> findByFundCodeAndNavDateBetweenOrderByNavDateAsc(String fundCode, LocalDate start, LocalDate end);

    List<FundNav> findByFundCodeAndNavDateGreaterThanEqualOrderByNavDateAsc(String fundCode, LocalDate start);

    List<FundNav> findByFundCodeInAndNavDateBetweenOrderByNavDateAsc(List<String> fundCodes, LocalDate start, LocalDate end);
}
