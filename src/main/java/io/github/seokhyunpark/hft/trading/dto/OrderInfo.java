package io.github.seokhyunpark.hft.trading.dto;

import java.math.BigDecimal;

public record OrderInfo(
        long orderId,
        String symbol,
        String qty,
        String price,
        BigDecimal numericPrice,
        long updateTime
) {
    public OrderInfo(long orderId, String symbol, String qty, String price, long updateTime) {
        this(orderId, symbol, qty, price, new BigDecimal(price), updateTime);
    }
}
