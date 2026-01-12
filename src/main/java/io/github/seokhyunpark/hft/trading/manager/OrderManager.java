package io.github.seokhyunpark.hft.trading.manager;

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
}
