package com.fundmanager.service;

import com.fundmanager.domain.entity.FundInfo;
import com.fundmanager.domain.entity.FundWatchlist;
import com.fundmanager.domain.vo.WatchlistItemVO;
import com.fundmanager.repository.FundInfoRepository;
import com.fundmanager.repository.FundWatchlistRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WatchlistService {

    private final FundWatchlistRepository watchlistRepository;
    private final FundInfoRepository fundInfoRepository;
    private final CurrentUserService currentUserService;
    private final FundQuoteService quoteService;

    public WatchlistService(FundWatchlistRepository watchlistRepository,
                            FundInfoRepository fundInfoRepository,
                            CurrentUserService currentUserService,
                            FundQuoteService quoteService) {
        this.watchlistRepository = watchlistRepository;
        this.fundInfoRepository = fundInfoRepository;
        this.currentUserService = currentUserService;
        this.quoteService = quoteService;
    }

    public List<WatchlistItemVO> list(String username) {
        Long userId = currentUserService.getUserId(username);
        return watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(item -> toWatchItem(item.getFundCode()))
                .toList();
    }

    @Transactional
    public void add(String username, String fundCode) {
        Long userId = currentUserService.getUserId(username);
        watchlistRepository.findByUserIdAndFundCode(userId, fundCode).ifPresentOrElse(
                existing -> {
                },
                () -> {
                    FundWatchlist item = new FundWatchlist();
                    item.setUserId(userId);
                    item.setFundCode(fundCode);
                    item.setCreatedAt(LocalDateTime.now());
                    watchlistRepository.save(item);
                }
        );
        quoteService.loadSnapshot(fundCode, false);
    }

    @Transactional
    public void remove(String username, String fundCode) {
        Long userId = currentUserService.getUserId(username);
        watchlistRepository.deleteByUserIdAndFundCode(userId, fundCode);
    }

    private WatchlistItemVO toWatchItem(String fundCode) {
        FundQuoteSnapshot snapshot = quoteService.loadSnapshot(fundCode, false);
        FundInfo info = fundInfoRepository.findByFundCode(fundCode).orElse(null);
        return new WatchlistItemVO(
                fundCode,
                info == null ? snapshot.fundName() : info.getFundName(),
                info == null ? "UNKNOWN" : info.getFundType(),
                snapshot.currentNav(),
                snapshot.estimateGrowthRate(),
                snapshot.estimateTime()
        );
    }
}
