package com.defi.lending.report;

import com.defi.lending.model.HealthBand;

import java.math.BigDecimal;

/**
 * One row of the risk report: a borrower and the values that explain the rank.
 * Only positions that have debt appear here, so {@code healthFactor} is always defined.
 */
public final class PositionRisk {

    private final String accountId;
    private final String displayName;
    private final BigDecimal healthFactor;
    private final HealthBand band;
    private final BigDecimal collateralValue;
    private final BigDecimal debtValue;

    public PositionRisk(String accountId, String displayName, BigDecimal healthFactor,
                        HealthBand band, BigDecimal collateralValue, BigDecimal debtValue) {
        // TODO: reject a blank id or name, a null factor or band, or a negative collateral or debt value.
        this.accountId = accountId;
        this.displayName = displayName;
        this.healthFactor = healthFactor;
        this.band = band;
        this.collateralValue = collateralValue;
        this.debtValue = debtValue;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getHealthFactor() {
        return healthFactor;
    }

    public HealthBand getBand() {
        return band;
    }

    public BigDecimal getCollateralValue() {
        return collateralValue;
    }

    public BigDecimal getDebtValue() {
        return debtValue;
    }
}
