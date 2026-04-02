package com.fundmanager.controller;

import com.fundmanager.common.ApiResponse;
import com.fundmanager.domain.vo.*;
import com.fundmanager.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewVO> overview(Authentication authentication) {
        return ApiResponse.ok(dashboardService.overview(authentication.getName()));
    }

    @GetMapping("/trend")
    public ApiResponse<List<TrendPointVO>> trend(Authentication authentication) {
        return ApiResponse.ok(dashboardService.trend(authentication.getName()));
    }

    @GetMapping("/distribution")
    public ApiResponse<List<DistributionItemVO>> distribution(Authentication authentication) {
        return ApiResponse.ok(dashboardService.distribution(authentication.getName()));
    }

    @GetMapping("/ranking")
    public ApiResponse<Map<String, List<RankingItemVO>>> ranking(Authentication authentication) {
        return ApiResponse.ok(dashboardService.ranking(authentication.getName()));
    }
}
