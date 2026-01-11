package io.github.seokhyunpark.hft.trading.dto;

import java.math.BigDecimal;

public record OrderParams(BigDecimal price, BigDecimal qty) {
    public boolean isInvalid() {
        if (price == null || qty == null) {
            return true;
        }
        return price.compareTo(BigDecimal.ZERO) <= 0 || qty.compareTo(BigDecimal.ZERO) <= 0;
    }
}
