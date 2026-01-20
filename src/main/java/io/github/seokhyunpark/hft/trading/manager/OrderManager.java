package io.github.seokhyunpark.hft.trading.manager;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.seokhyunpark.hft.trading.config.TradingProperties;
import io.github.seokhyunpark.hft.trading.dto.OrderInfo;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderManager {
    private final TradingProperties tradingProperties;

    private final Map<Long, OrderInfo> buyOrders = new ConcurrentHashMap<>();
    private final Map<Long, OrderInfo> sellOrders = new ConcurrentHashMap<>();
    private final Queue<OrderInfo> canceledOrders = new PriorityBlockingQueue<>(
            2000, Comparator.comparing(OrderInfo::numericPrice)
    );

    private final Set<Long> closedOrders = Collections.synchronizedSet(
            Collections.newSetFromMap(new LinkedHashMap<>(1000, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                    return size() > 1000;
                }
            })
    );

    // ----------------------------------------------------------------------------------------------------
    // 공통 주문 관리
    // ----------------------------------------------------------------------------------------------------
    public boolean hasOpenOrderCapacity() {
        return buyOrders.size() + sellOrders.size() < tradingProperties.risk().maxOpenOrders();
    }

    public OrderInfo findConflictingBuyOrder(BigDecimal newPrice) {
        for (OrderInfo info : buyOrders.values()) {
            if (isConflicting(info.numericPrice(), newPrice)) {
                return info;
            }
        }
        return null;
    }

    public boolean hasConflictingWithHoldings(BigDecimal newPrice) {
        for (OrderInfo info : sellOrders.values()) {
            if (isConflicting(info.avgBuyPrice(), newPrice)) {
                return true;
            }
        }
        return false;
    }

    private boolean isConflicting(BigDecimal existingPrice, BigDecimal newPrice) {
        BigDecimal diff = existingPrice.subtract(newPrice).abs();
        BigDecimal limit = existingPrice.multiply(tradingProperties.risk().priceConflictThreshold());
        return diff.compareTo(limit) < 0;
    }

    // ----------------------------------------------------------------------------------------------------
    // 매수 주문 관리
    // ----------------------------------------------------------------------------------------------------
    public void addBuyOrder(OrderInfo orderInfo) {
        if (closedOrders.contains(orderInfo.orderId())) {
            log.debug("[CLOSED-BUY] 이미 종료된 매수 주문 재등록 방지 | 주문번호: {}", orderInfo.orderId());
            return;
        }
        buyOrders.put(orderInfo.orderId(), orderInfo);
    }

    public void removeBuyOrder(long orderId) {
        closedOrders.add(orderId);
        buyOrders.remove(orderId);
    }

    public boolean hasBuyOrderAt(BigDecimal price) {
        return buyOrders.values().stream()
                .anyMatch(order -> order.numericPrice().compareTo(price) == 0);
    }

    public boolean isBuyOrdersFull() {
        return buyOrders.size() > tradingProperties.risk().buyOrdersLimit();
    }

    public OrderInfo getOldestBuyOrder() {
        return buyOrders.values().stream()
                .min(Comparator.comparingLong(OrderInfo::orderId))
                .orElse(null);
    }

    public boolean containsBuyOrder(long orderId) {
        return buyOrders.containsKey(orderId);
    }

    // ----------------------------------------------------------------------------------------------------
    // 매도 주문 관리
    // ----------------------------------------------------------------------------------------------------
    public void addSellOrder(OrderInfo orderInfo) {
        if (closedOrders.contains(orderInfo.orderId())) {
            log.debug("[CLOSED-SELL] 이미 종료된 매도 주문 재등록 방지 | 주문번호: {}", orderInfo.orderId());
            return;
        }
        sellOrders.put(orderInfo.orderId(), orderInfo);
    }

    public void removeSellOrder(long orderId) {
        closedOrders.add(orderId);
        sellOrders.remove(orderId);
    }

    public OrderInfo getHighestPriceSellOrder() {
        return sellOrders.values().stream()
                .max(Comparator.comparing(OrderInfo::numericPrice))
                .orElse(null);
    }

    public boolean containsSellOrder(long orderId) {
        return sellOrders.containsKey(orderId);
    }

    public boolean isSellOrdersFull() {
        return sellOrders.size() > tradingProperties.risk().maxSellOrders();
    }

    public boolean isSellOrdersRestorable() {
        return sellOrders.size() < tradingProperties.risk().minSellOrders();
    }

    // ----------------------------------------------------------------------------------------------------
    // 취소된 주문 관리
    // ----------------------------------------------------------------------------------------------------
    public void addCanceledOrder(OrderInfo orderInfo) {
        canceledOrders.add(orderInfo);
    }

    public OrderInfo pollLowestPriceCanceledOrder() {
        return canceledOrders.poll();
    }

    public boolean hasCanceledOrders() {
        return !canceledOrders.isEmpty();
    }
}
