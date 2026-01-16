package io.github.seokhyunpark.hft.trading.dto;

import java.math.BigDecimal;

public record OrderInfo(
        long orderId,
        String symbol,
        String qty,
        String price,
        BigDecimal numericPrice
) {
    public OrderInfo(long orderId, String symbol, String qty, String price) {
        this(orderId, symbol, qty, price, new BigDecimal(price));
    }
}
