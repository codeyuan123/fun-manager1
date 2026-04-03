package com.fundmanager.service;

import com.fundmanager.common.BusinessException;
import com.fundmanager.domain.dto.TradeRequest;
import com.fundmanager.domain.entity.FundInfo;
import com.fundmanager.domain.entity.FundPosition;
import com.fundmanager.domain.entity.FundTransaction;
import com.fundmanager.domain.vo.PositionItemVO;
import com.fundmanager.domain.vo.TransactionVO;
import com.fundmanager.repository.FundInfoRepository;
import com.fundmanager.repository.FundPositionRepository;
import com.fundmanager.repository.FundTransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PositionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final FundPositionRepository positionRepository;
    private final FundTransactionRepository transactionRepository;
    private final FundInfoRepository fundInfoRepository;
    private final FundQuoteService quoteService;
    private final CurrentUserService currentUserService;

    public PositionService(FundPositionRepository positionRepository,
                           FundTransactionRepository transactionRepository,
                           FundInfoRepository fundInfoRepository,
                           FundQuoteService quoteService,
                           CurrentUserService currentUserService) {
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
        this.fundInfoRepository = fundInfoRepository;
        this.quoteService = quoteService;
        this.currentUserService = currentUserService;
    }

    public List<PositionItemVO> list(String username) {
        Long userId = currentUserService.getUserId(username);
        return listByUserId(userId);
    }

    public List<PositionItemVO> listByUserId(Long userId) {
        return positionRepository.findByUserIdOrderByFundCodeAsc(userId).stream()
                .filter(p -> p.getTotalShares().compareTo(BigDecimal.ZERO) > 0)
                .map(this::toPositionView)
                .toList();
    }

    public List<TransactionVO> transactions(String username, String fundCode) {
        Long userId = currentUserService.getUserId(username);
        return transactionRepository.findByUserIdAndFundCodeOrderByTradeDateDescIdDesc(userId, fundCode).stream()
                .map(tx -> new TransactionVO(
                        tx.getId(),
                        tx.getTransactionType(),
                        tx.getTradeDate(),
                        tx.getAmount(),
                        tx.getShares(),
                        tx.getFee(),
                        tx.getNav(),
                        tx.getRemark(),
                        tx.getCreatedAt()
                )).toList();
    }

    @Transactional
    public void buy(String username, TradeRequest req) {
        Long userId = currentUserService.getUserId(username);
        BigDecimal fee = nvl(req.fee());
        BigDecimal shares = normalizeShares(req.shares(), req.amount(), req.nav());
        LocalDate tradeDate = req.tradeDate() == null ? LocalDate.now() : req.tradeDate();

        FundPosition position = positionRepository.findByUserIdAndFundCode(userId, req.fundCode())
                .orElseGet(() -> createPosition(userId, req.fundCode()));

        position.setTotalAmount(position.getTotalAmount().add(req.amount()));
        position.setTotalShares(position.getTotalShares().add(shares));
        position.setCurrentCost(position.getCurrentCost().add(req.amount()).add(fee));
        position.setAverageCostNav(safeDivide(position.getCurrentCost(), position.getTotalShares(), 6));
        position.setLastTradeDate(tradeDate);
        position.setUpdatedAt(LocalDateTime.now());
        positionRepository.save(position);

        saveTransaction(userId, "BUY", req, shares, fee, tradeDate);
        upsertFundInfo(req.fundCode(), req.fundName());
    }

    @Transactional
    public void sell(String username, TradeRequest req) {
        Long userId = currentUserService.getUserId(username);
        BigDecimal fee = nvl(req.fee());
        BigDecimal shares = normalizeShares(req.shares(), req.amount(), req.nav());
        LocalDate tradeDate = req.tradeDate() == null ? LocalDate.now() : req.tradeDate();

        FundPosition position = positionRepository.findByUserIdAndFundCode(userId, req.fundCode())
                .orElseThrow(() -> new BusinessException("未找到对应持仓"));
        if (position.getTotalShares().compareTo(shares) < 0) {
            throw new BusinessException("卖出份额超过当前持仓");
        }

        BigDecimal avgCostPerShare = safeDivide(position.getCurrentCost(), position.getTotalShares(), 10);
        BigDecimal costReduction = avgCostPerShare.multiply(shares).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingShares = position.getTotalShares().subtract(shares);
        BigDecimal remainingCost = position.getCurrentCost().subtract(costReduction);
        if (remainingCost.compareTo(BigDecimal.ZERO) < 0) {
            remainingCost = BigDecimal.ZERO;
        }

        position.setTotalShares(remainingShares);
        position.setCurrentCost(remainingCost);
        position.setAverageCostNav(remainingShares.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : safeDivide(remainingCost, remainingShares, 6));
        position.setLastTradeDate(tradeDate);
        position.setUpdatedAt(LocalDateTime.now());
        positionRepository.save(position);

        saveTransaction(userId, "SELL", req, shares, fee, tradeDate);
        upsertFundInfo(req.fundCode(), req.fundName());
    }

    private PositionItemVO toPositionView(FundPosition position) {
        FundQuoteSnapshot snapshot = quoteService.loadSnapshot(position.getFundCode(), false);
        FundInfo fundInfo = fundInfoRepository.findByFundCode(position.getFundCode()).orElse(null);
        BigDecimal marketValue = position.getTotalShares().multiply(snapshot.currentNav()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal profit = marketValue.subtract(position.getCurrentCost()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal profitRate = position.getCurrentCost().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profit.divide(position.getCurrentCost(), 4, RoundingMode.HALF_UP);

        BigDecimal baseNav = snapshot.previousNav() != null ? snapshot.previousNav() : snapshot.latestNav();
        BigDecimal todayProfit = position.getTotalShares()
                .multiply(snapshot.currentNav().subtract(baseNav))
                .setScale(2, RoundingMode.HALF_UP);

        return new PositionItemVO(
                position.getFundCode(),
                fundInfo == null ? snapshot.fundName() : fundInfo.getFundName(),
                fundInfo == null ? "UNKNOWN" : fundInfo.getFundType(),
                position.getTotalShares(),
                position.getCurrentCost(),
                snapshot.currentNav(),
                marketValue,
                profit,
                profitRate,
                todayProfit,
                position.getLastTradeDate(),
                snapshot.estimateSource(),
                snapshot.estimateConfidence(),
                snapshot.holdingCoverageRate(),
                snapshot.quotedCoverageRate(),
                snapshot.estimateTime()
        );
    }

    private void saveTransaction(Long userId, String type, TradeRequest req, BigDecimal shares, BigDecimal fee, LocalDate tradeDate) {
        FundTransaction tx = new FundTransaction();
        tx.setUserId(userId);
        tx.setFundCode(req.fundCode());
        tx.setTransactionType(type);
        tx.setTradeDate(tradeDate);
        tx.setAmount(req.amount());
        tx.setShares(shares);
        tx.setFee(fee);
        tx.setNav(req.nav());
        tx.setRemark(req.remark());
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    private FundPosition createPosition(Long userId, String fundCode) {
        FundPosition position = new FundPosition();
        position.setUserId(userId);
        position.setFundCode(fundCode);
        position.setTotalAmount(BigDecimal.ZERO);
        position.setTotalShares(BigDecimal.ZERO);
        position.setAverageCostNav(BigDecimal.ZERO);
        position.setCurrentCost(BigDecimal.ZERO);
        position.setCreatedAt(LocalDateTime.now());
        position.setUpdatedAt(LocalDateTime.now());
        return position;
    }

    private void upsertFundInfo(String fundCode, String fundName) {
        FundInfo info = fundInfoRepository.findByFundCode(fundCode).orElseGet(FundInfo::new);
        if (info.getId() == null) {
            info.setFundCode(fundCode);
            info.setCreatedAt(LocalDateTime.now());
            info.setStatus((byte) 1);
        }
        if (fundName != null && !fundName.isBlank()) {
            info.setFundName(fundName);
        } else if (info.getFundName() == null || info.getFundName().isBlank()) {
            info.setFundName(fundCode);
        }
        if (info.getFundType() == null) {
            info.setFundType("UNKNOWN");
        }
        info.setUpdatedAt(LocalDateTime.now());
        fundInfoRepository.save(info);
    }

    private BigDecimal normalizeShares(BigDecimal shares, BigDecimal amount, BigDecimal nav) {
        if (shares != null && shares.compareTo(BigDecimal.ZERO) > 0) {
            return shares.setScale(4, RoundingMode.HALF_UP);
        }
        return amount.divide(nav, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal safeDivide(BigDecimal left, BigDecimal right, int scale) {
        if (right.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return left.divide(right, scale, RoundingMode.HALF_UP);
    }
}

