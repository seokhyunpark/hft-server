package io.github.seokhyunpark.hft.trading.manager;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.springframework.stereotype.Component;

import io.github.seokhyunpark.hft.trading.dto.OrderInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderManager {
    private static final int MAX_OPEN_ORDERS = 200;
    private static final int OPEN_ORDERS_MARGIN = 10;
    private static final int BUY_ORDERS_LIMIT = 3;

    private static final Comparator<OrderInfo> canceledOrderComparator = Comparator
            .comparing(OrderInfo::numericPrice)
            .thenComparingLong(OrderInfo::orderId);

    private final Map<Long, OrderInfo> buyOrders = new ConcurrentHashMap<>();
    private final Map<Long, OrderInfo> sellOrders = new ConcurrentHashMap<>();
    private final ConcurrentSkipListSet<OrderInfo> canceledOrders = new ConcurrentSkipListSet<>(canceledOrderComparator);

    // ----------------------------------------------------------------------------------------------------
    // 공통 주문 관리
    // ----------------------------------------------------------------------------------------------------
    public int getOpenOrdersCount() {
        return buyOrders.size() + sellOrders.size();
    }

    public boolean hasOpenOrderCapacity() {
        return buyOrders.size() + sellOrders.size() < MAX_OPEN_ORDERS - OPEN_ORDERS_MARGIN;
    }

    // ----------------------------------------------------------------------------------------------------
    // 매수 주문 관리
    // ----------------------------------------------------------------------------------------------------
    public int getBuyOrderCount() {
        return buyOrders.size();
    }

    public void addBuyOrder(OrderInfo orderInfo) {
        buyOrders.put(orderInfo.orderId(), orderInfo);
    }

    public void removeBuyOrder(long orderId) {
        buyOrders.remove(orderId);
    }

    public boolean hasBuyOrderAt(BigDecimal price) {
        return buyOrders.values().stream()
                .anyMatch(order -> order.numericPrice().compareTo(price) == 0);
    }

    public boolean isBuyOrdersFull() {
        return buyOrders.size() > BUY_ORDERS_LIMIT;
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
    public int getSellOrderCount() {
        return sellOrders.size();
    }

    public void addSellOrder(OrderInfo orderInfo) {
        sellOrders.put(orderInfo.orderId(), orderInfo);
    }

    public void removeSellOrder(long orderId) {
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
}
