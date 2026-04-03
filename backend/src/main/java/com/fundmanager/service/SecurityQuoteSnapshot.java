package com.fundmanager.service;

import java.math.BigDecimal;

public record SecurityQuoteSnapshot(
        String securityCode,
        String securityName,
        BigDecimal price,
        BigDecimal changeRate
) {
}
