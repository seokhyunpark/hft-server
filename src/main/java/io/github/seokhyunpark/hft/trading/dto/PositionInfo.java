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

    public BigDecimal getAvgPrice(int scale) {
        if (totalQty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalUsdValue.divide(totalQty, scale, RoundingMode.HALF_UP);
    }

    public PositionInfo add(BigDecimal qty, BigDecimal usd) {
        return new PositionInfo(totalQty.add(qty), totalUsdValue.add(usd));
    }
}
