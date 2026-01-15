package io.github.seokhyunpark.hft.trading.config;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hft.trading")
public record TradingProperties(
        String symbol,
        String baseAsset,
        String quoteAsset,
        String reserveAsset,
        BigDecimal minOrderSize,
        BigDecimal priceTickSize,
        BigDecimal qtyTickSize,
        BigDecimal buyWallThresholdUsd,
        BigDecimal targetMargin
) {
    public BigDecimal scaleQty(BigDecimal qty) {
        return qty.setScale(qtyTickSize.scale(), RoundingMode.DOWN);
    }

    public BigDecimal scalePrice(BigDecimal price) {
        return price.setScale(priceTickSize.scale(), RoundingMode.DOWN);
    }
}
