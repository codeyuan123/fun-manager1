package com.fundmanager.domain.dto;

import java.util.List;

public record EstimateRefreshRequest(List<String> fundCodes) {
}
