package com.defi.lending.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Haircut valuation for {@link VolatileToken}. */
class VolatileTokenTest {

    @Test
    void unitValueAppliesTheVolatilityBuffer() {
        // Normal: unit value is oraclePrice * (1 - volatilityBuffer).
        VolatileToken ethereum = new VolatileToken("ETH", "Ethereum", 18, new BigDecimal("0.005"));
        BigDecimal oraclePrice = new BigDecimal("1001");
        BigDecimal unitValue = ethereum.unitValue(oraclePrice);

        assertEquals(0, unitValue.compareTo(oraclePrice.multiply(BigDecimal.ONE.subtract(new BigDecimal("0.005")))));
    }

    @Test
    void zeroBufferPassesTheOraclePriceThrough() {
        // Edge: a buffer of 0 returns the oracle price unchanged.
        VolatileToken ethereum = new VolatileToken("ETH", "Ethereum", 18, new BigDecimal("0.000"));
        BigDecimal oraclePrice = new BigDecimal("1001");
        BigDecimal unitValue = ethereum.unitValue(oraclePrice);

        assertEquals(0, unitValue.compareTo(oraclePrice));
    }

    @Test
    void zeroPriceStaysZero() {
        // Edge: an oracle price of zero values the token at zero. The buffer does not make it negative.
        VolatileToken ethereum = new VolatileToken("ETH", "Ethereum", 18, new BigDecimal("0.005"));
        BigDecimal oraclePrice = new BigDecimal("0");
        BigDecimal unitValue = ethereum.unitValue(oraclePrice);

        assertEquals(0, unitValue.compareTo(BigDecimal.ZERO));
    }

    @Test
    void negativePriceIsRejected() {
        // Invalid: a null or negative oracle price throws IllegalArgumentException.
        VolatileToken ethereum = new VolatileToken("ETH", "Ethereum", 18, new BigDecimal("0.005"));
        BigDecimal negativePrice = new BigDecimal("-1");
        BigDecimal zeroPrice = new BigDecimal("0");

        assertThrows(IllegalArgumentException.class, () -> ethereum.unitValue(negativePrice));
        assertThrows(IllegalArgumentException.class, () -> ethereum.unitValue(null));
        assertEquals(0, BigDecimal.ZERO.compareTo(ethereum.unitValue(zeroPrice)));
    }
}
