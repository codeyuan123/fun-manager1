package com.fundmanager.controller;

import com.fundmanager.common.ApiResponse;
import com.fundmanager.domain.vo.FundDetailVO;
import com.fundmanager.domain.vo.FundEstimateVO;
import com.fundmanager.domain.vo.FundNavVO;
import com.fundmanager.domain.vo.FundSearchItemVO;
import com.fundmanager.service.FundService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/funds")
public class FundController {

    private final FundService fundService;

    public FundController(FundService fundService) {
        this.fundService = fundService;
    }

    @GetMapping("/search")
    public ApiResponse<List<FundSearchItemVO>> search(@RequestParam(required = false, defaultValue = "") String keyword) {
        return ApiResponse.ok(fundService.search(keyword));
    }

    @GetMapping("/{fundCode}")
    public ApiResponse<FundDetailVO> detail(@PathVariable String fundCode) {
        return ApiResponse.ok(fundService.getDetail(fundCode));
    }

    @GetMapping("/{fundCode}/estimate")
    public ApiResponse<FundEstimateVO> estimate(@PathVariable String fundCode) {
        return ApiResponse.ok(fundService.getEstimate(fundCode));
    }

    @GetMapping("/{fundCode}/nav-history")
    public ApiResponse<List<FundNavVO>> navHistory(@PathVariable String fundCode,
                                                   @RequestParam(defaultValue = "30") int limit) {
        return ApiResponse.ok(fundService.navHistory(fundCode, limit));
    }
}
