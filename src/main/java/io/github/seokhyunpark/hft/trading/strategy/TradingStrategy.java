package io.github.seokhyunpark.hft.trading.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.seokhyunpark.hft.exchange.dto.stream.PartialBookDepth;
import io.github.seokhyunpark.hft.trading.config.TradingProperties;
import io.github.seokhyunpark.hft.trading.dto.NewOrderParams;
import io.github.seokhyunpark.hft.trading.dto.PositionInfo;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingStrategy {
    private final TradingProperties props;

    private final AtomicReference<BigDecimal> latestBestAskPrice = new AtomicReference<>(BigDecimal.ZERO);

    // ----------------------------------------------------------------------------------------------------
    // 매수 주문 전략
    // ----------------------------------------------------------------------------------------------------
    public NewOrderParams calculateBuyOrderParams(PartialBookDepth depth) {
        BigDecimal price = calculateBuyPrice(depth);
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return new NewOrderParams(BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal qty = calculateBuyQty(price);
        return new NewOrderParams(qty, price);
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

        return usd.compareTo(props.risk().buyWallThresholdUsd()) >= 0;
    }

    private BigDecimal applyPriceOffset(List<String> bid) {
        BigDecimal price = new BigDecimal(bid.getFirst());
        BigDecimal appliedPrice = price.add(props.priceTickSize());
        return props.scalePrice(appliedPrice);
    }

    private BigDecimal calculateBuyQty(BigDecimal price) {
        return props.minOrderSize().divide(
                price, props.qtyTickSize().scale(), RoundingMode.CEILING
        );
    }

    // ----------------------------------------------------------------------------------------------------
    // 매도 주문 전략
    // ----------------------------------------------------------------------------------------------------
    public void updateBestAskPrice(PartialBookDepth depth) {
        if (depth == null || depth.asks() == null || !depth.asks().isEmpty()) {
            return;
        }
        BigDecimal lowestAskPrice = new BigDecimal(depth.asks().getFirst().getFirst());
        BigDecimal bestAskPrice = lowestAskPrice.subtract(props.priceTickSize());
        BigDecimal scaledPrice = props.scalePrice(bestAskPrice);
        latestBestAskPrice.set(scaledPrice);
    }

    public NewOrderParams calculateSellOrderParams(PositionInfo info) {
        return calculateSellOrderParams(info.totalQty(), info.getAvgPrice(props.priceTickSize().scale()));
    }

    public NewOrderParams calculateSellOrderParams(BigDecimal qty, BigDecimal avgBuyPrice) {
        BigDecimal targetAskPrice = avgBuyPrice.multiply(props.risk().targetMultiplier());
        BigDecimal bestAskPrice = targetAskPrice.max(latestBestAskPrice.get());

        BigDecimal scaledPrice = props.scalePrice(bestAskPrice);
        BigDecimal scaledQty = props.scaleQty(qty);

        return new NewOrderParams(scaledQty, scaledPrice);
    }
}
