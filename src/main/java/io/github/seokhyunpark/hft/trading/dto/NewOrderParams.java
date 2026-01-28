package io.github.seokhyunpark.hft.trading.dto;

import java.math.BigDecimal;

public record NewOrderParams(
        BigDecimal qty,
        BigDecimal price
) {
    public boolean isInvalid() {
        return qty.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(BigDecimal.ZERO) <= 0;
    }

    public BigDecimal getUsdValue() {
        if (isInvalid()) {
            return BigDecimal.ZERO;
        }
        return price.multiply(qty);
    }
}
