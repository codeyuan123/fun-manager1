package com.fundmanager.controller;

import com.fundmanager.common.ApiResponse;
import com.fundmanager.domain.dto.BacktestFundsRunRequest;
import com.fundmanager.domain.dto.BacktestStrategiesRunRequest;
import com.fundmanager.domain.vo.BacktestResultVO;
import com.fundmanager.service.BacktestService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/backtests")
public class BacktestController {

    private final BacktestService backtestService;

    public BacktestController(BacktestService backtestService) {
        this.backtestService = backtestService;
    }

    @PostMapping("/strategies/run")
    public ApiResponse<List<BacktestResultVO>> runStrategies(@RequestBody BacktestStrategiesRunRequest request) {
        return ApiResponse.ok(backtestService.runStrategies(request));
    }

    @PostMapping("/funds/run")
    public ApiResponse<List<BacktestResultVO>> runFunds(@RequestBody BacktestFundsRunRequest request) {
        return ApiResponse.ok(backtestService.runFunds(request));
    }
}
