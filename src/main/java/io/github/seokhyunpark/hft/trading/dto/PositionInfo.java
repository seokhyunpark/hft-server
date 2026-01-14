package io.github.seokhyunpark.hft.trading.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record PositionInfo(
        BigDecimal totalQty,
        BigDecimal totalUsdValue
) {
    public PositionInfo() {
        this(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public BigDecimal getAvgPrice() {
        if (totalQty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalUsdValue.divide(totalQty, RoundingMode.CEILING);
    }

    public PositionInfo plus(BigDecimal qty, BigDecimal usd) {
        return new PositionInfo(totalQty.add(qty), totalUsdValue.add(usd));
    }

    public PositionInfo minus(BigDecimal qty, BigDecimal usd) {
        BigDecimal nextQty = totalQty.subtract(qty);
        if (nextQty.compareTo(BigDecimal.ZERO) <= 0) {
            return new PositionInfo();
        }
        return new PositionInfo(nextQty, totalUsdValue.subtract(usd));
    }
}
