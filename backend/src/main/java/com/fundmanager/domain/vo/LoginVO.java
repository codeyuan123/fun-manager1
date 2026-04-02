package com.fundmanager.domain.vo;

public record LoginVO(
        String token,
        String username,
        String nickname
) {
}
