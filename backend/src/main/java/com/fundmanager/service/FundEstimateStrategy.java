package com.fundmanager.service;

import java.util.Optional;

public interface FundEstimateStrategy {
    boolean supports(FundEstimateContext context);

    Optional<FundEstimateComputation> estimate(FundEstimateContext context);
}
