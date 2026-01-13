package io.github.seokhyunpark.hft.trading.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import io.github.seokhyunpark.hft.exchange.dto.stream.PartialBookDepth;
import io.github.seokhyunpark.hft.trading.config.TradingProperties;
import io.github.seokhyunpark.hft.trading.dto.OrderParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingStrategy {
    private final TradingProperties tradingProperties;

    private final AtomicReference<BigDecimal> latestBestAskPrice = new AtomicReference<>(BigDecimal.ZERO);

    // ----------------------------------------------------------------------------------------------------
    // 매수 주문 전략
    // ----------------------------------------------------------------------------------------------------
    public OrderParams calculateBuyOrderParams(PartialBookDepth depth) {
        BigDecimal price = calculateBuyPrice(depth);
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return new OrderParams(BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal qty = calculateBuyQty(price);
        return new OrderParams(price, qty);
    }

    private BigDecimal calculateBuyPrice(PartialBookDepth depth) {
        return depth.bids().stream()
                .max(Comparator.comparing(bid -> new BigDecimal(bid.getLast())))
                .filter(this::isStrongBuyWall)
                .map(this::applyPriceOffset)
                .orElse(BigDecimal.ZERO);
    }

    private boolean isStrongBuyWall(List<String> bid) {
        BigDecimal price = new BigDecimal(bid.getFirst());
        BigDecimal qty = new BigDecimal(bid.getLast());
        BigDecimal usd = price.multiply(qty);

        return usd.compareTo(tradingProperties.buyWallThresholdUsd()) >= 0;
    }

    private BigDecimal applyPriceOffset(List<String> bid) {
        BigDecimal rawPrice = new BigDecimal(bid.getFirst()).add(tradingProperties.priceTickSize());
        return rawPrice.setScale(tradingProperties.priceTickSize().scale(), RoundingMode.FLOOR);
    }

    private BigDecimal calculateBuyQty(BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return tradingProperties.minOrderSize().divide(price, tradingProperties.qtyTickSize().scale(), RoundingMode.CEILING);
    }

    // ----------------------------------------------------------------------------------------------------
    // 매도 주문 전략
    // ----------------------------------------------------------------------------------------------------
    public void updateBestAskPrice(PartialBookDepth depth) {
        if (depth != null && depth.asks() != null && !depth.asks().isEmpty()) {
            latestBestAskPrice.set(new BigDecimal(depth.asks().getFirst().getFirst()));
        }
    }
}
