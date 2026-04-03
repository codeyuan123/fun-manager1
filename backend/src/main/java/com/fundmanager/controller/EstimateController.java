package com.fundmanager.controller;

import com.fundmanager.common.ApiResponse;
import com.fundmanager.domain.dto.EstimateRefreshRequest;
import com.fundmanager.domain.vo.EstimateRefreshSummaryVO;
import com.fundmanager.service.EstimateRefreshService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estimates")
public class EstimateController {

    private final EstimateRefreshService estimateRefreshService;

    public EstimateController(EstimateRefreshService estimateRefreshService) {
        this.estimateRefreshService = estimateRefreshService;
    }

    @PostMapping("/refresh")
    public ApiResponse<EstimateRefreshSummaryVO> refresh(Authentication authentication,
                                                         @RequestBody(required = false) EstimateRefreshRequest request) {
        return ApiResponse.ok(estimateRefreshService.refreshForUser(authentication.getName(), request));
    }
}
