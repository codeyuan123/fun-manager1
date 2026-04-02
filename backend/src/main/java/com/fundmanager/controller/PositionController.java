package com.fundmanager.controller;

import com.fundmanager.common.ApiResponse;
import com.fundmanager.domain.dto.TradeRequest;
import com.fundmanager.domain.vo.PositionItemVO;
import com.fundmanager.domain.vo.TransactionVO;
import com.fundmanager.service.PositionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    public ApiResponse<List<PositionItemVO>> list(Authentication authentication) {
        return ApiResponse.ok(positionService.list(authentication.getName()));
    }

    @PostMapping("/transactions/buy")
    public ApiResponse<Void> buy(Authentication authentication, @Valid @RequestBody TradeRequest request) {
        positionService.buy(authentication.getName(), request);
        return ApiResponse.ok();
    }

    @PostMapping("/transactions/sell")
    public ApiResponse<Void> sell(Authentication authentication, @Valid @RequestBody TradeRequest request) {
        positionService.sell(authentication.getName(), request);
        return ApiResponse.ok();
    }

    @GetMapping("/{fundCode}/transactions")
    public ApiResponse<List<TransactionVO>> transactions(Authentication authentication, @PathVariable String fundCode) {
        return ApiResponse.ok(positionService.transactions(authentication.getName(), fundCode));
    }
}
