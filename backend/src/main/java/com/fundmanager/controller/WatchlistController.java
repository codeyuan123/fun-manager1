package com.fundmanager.controller;

import com.fundmanager.common.ApiResponse;
import com.fundmanager.domain.vo.WatchlistItemVO;
import com.fundmanager.service.WatchlistService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public ApiResponse<List<WatchlistItemVO>> list(Authentication authentication) {
        return ApiResponse.ok(watchlistService.list(authentication.getName()));
    }

    @PostMapping("/{fundCode}")
    public ApiResponse<Void> add(Authentication authentication, @PathVariable String fundCode) {
        watchlistService.add(authentication.getName(), fundCode);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{fundCode}")
    public ApiResponse<Void> remove(Authentication authentication, @PathVariable String fundCode) {
        watchlistService.remove(authentication.getName(), fundCode);
        return ApiResponse.ok();
    }
}
