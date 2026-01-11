package io.github.seokhyunpark.hft.trading.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hft.trading")
public record TradingProperties(
        String symbol,
        String baseAsset,
        String quoteAsset,
        String reserveAsset,
        BigDecimal minOrderSize,
        BigDecimal priceTickSize,
        BigDecimal qtyTickSize
) {}
