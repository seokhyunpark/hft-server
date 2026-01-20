package io.github.seokhyunpark.hft.trading.core;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.seokhyunpark.hft.exchange.dto.stream.AccountUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.BalanceUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.OrderUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.PartialBookDepth;
import io.github.seokhyunpark.hft.exchange.listener.MarketEventListener;
import io.github.seokhyunpark.hft.exchange.listener.UserEventListener;
import io.github.seokhyunpark.hft.trading.config.TradingProperties;
import io.github.seokhyunpark.hft.trading.dto.NewOrderParams;
import io.github.seokhyunpark.hft.trading.dto.OrderInfo;
import io.github.seokhyunpark.hft.trading.dto.PositionInfo;
import io.github.seokhyunpark.hft.trading.executor.OrderExecutor;
import io.github.seokhyunpark.hft.trading.manager.OrderManager;
import io.github.seokhyunpark.hft.trading.manager.PositionManager;
import io.github.seokhyunpark.hft.trading.manager.QuoteAssetManager;
import io.github.seokhyunpark.hft.trading.manager.RateLimitManager;
import io.github.seokhyunpark.hft.trading.strategy.TradingStrategy;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingCore implements MarketEventListener, UserEventListener {
    private final TradingStrategy tradingStrategy;
    private final QuoteAssetManager quoteAssetManager;
    private final OrderManager orderManager;
    private final RateLimitManager rateLimitManager;
    private final OrderExecutor orderExecutor;
    private final TradingProperties tradingProperties;
    private final PositionManager positionManager;

    // ----------------------------------------------------------------------------------------------------
    // Market Event
    // ----------------------------------------------------------------------------------------------------
    @Override
    public void onPartialBookDepthReceived(PartialBookDepth depth) {
        if (depth == null || depth.bids() == null || depth.bids().isEmpty()) {
            return;
        }

        // 매도 1호가 가격 업데이트
        tradingStrategy.updateBestAskPrice(depth);

        // Sell Orders 개수 관리
        manageSellOrders();

        // Buy Orders 개수 관리
        if (orderManager.isBuyOrdersFull()) {
            OrderInfo info = orderManager.getOldestBuyOrder();
            if (info != null) {
                orderExecutor.cancelBuyAsync(info);
            }
        }

        NewOrderParams buyParams = tradingStrategy.calculateBuyOrderParams(depth);
        OrderInfo conflictingBuyOrder = orderManager.findConflictingBuyOrder(buyParams.price());
        if (conflictingBuyOrder != null) {
            orderExecutor.cancelBuyAsync(conflictingBuyOrder);
        }

        // 중복된 가격 확인
        if (buyParams.isInvalid()
                || orderManager.hasBuyOrderAt(buyParams.price())
                || orderManager.conflictsWithSellOrders(buyParams.price())) {
            return;
        }

        // Rate Limit 확인
        if (!rateLimitManager.hasRateLimitCapacity()) {
            return;
        }

        // USD 잔고 확인
        if (!quoteAssetManager.hasQuoteBalanceFor(buyParams.getUsdValue())) {
            return;
        }

        // Open Orders 개수 확인
        if (!orderManager.hasOpenOrderCapacity()) {
            return;
        }

        // 매수 주문 (상태 낙관적 업데이트)
        rateLimitManager.onOrderPlaced();
        quoteAssetManager.deductQuoteBalance(buyParams.getUsdValue());
        orderExecutor.buyAsync(buyParams);
    }

    // ----------------------------------------------------------------------------------------------------
    // User Event
    // ----------------------------------------------------------------------------------------------------
    @Override
    public void onAccountUpdateReceived(AccountUpdate accountUpdate) {
        if (accountUpdate == null || accountUpdate.eventType() == null || accountUpdate.balances() == null) {
            return;
        }

        if (!accountUpdate.eventType().equals("outboundAccountPosition")) {
            return;
        }

        for (AccountUpdate.Balance balanceEntry : accountUpdate.balances()) {
            if (balanceEntry.asset().equals(tradingProperties.quoteAsset())) {
                BigDecimal free = new BigDecimal(balanceEntry.free());
                quoteAssetManager.syncQuoteBalance(free);
                break;
            }
        }
    }

    @Override
    public void onBalanceUpdateReceived(BalanceUpdate balanceUpdate) {
        if (balanceUpdate == null || balanceUpdate.eventType() == null) {
            return;
        }

        if (!balanceUpdate.eventType().equals("balanceUpdate")) {
            return;
        }

        if (balanceUpdate.asset().equals(tradingProperties.quoteAsset())) {
            BigDecimal delta = new BigDecimal(balanceUpdate.balanceDelta());
            quoteAssetManager.addQuoteBalance(delta);
        }
    }

    @Override
    public void onOrderUpdateReceived(OrderUpdate orderUpdate) {
        if (orderUpdate == null || !orderUpdate.eventType().equals("executionReport")) {
            return;
        }
        if (orderUpdate.symbol().equals(tradingProperties.symbol())) {
            switch (orderUpdate.currentExecutionType()) {
                case "NEW" -> handleNewType(orderUpdate);
                case "TRADE" -> handleTradeType(orderUpdate);
                case "CANCELED" -> handleCanceledType(orderUpdate);
                default -> log.info("[ORDER-UPDATE] 알 수 없는 타입: {}", orderUpdate.currentExecutionType());
            }
        }
    }

    private void manageSellOrders() {
        if (orderManager.isSellOrdersFull()) {
            OrderInfo deleteInfo = orderManager.getHighestPriceSellOrder();
            if (deleteInfo != null) {
                orderExecutor.cancelSellAsync(deleteInfo);
            }
        } else if (orderManager.isSellOrdersRestorable()) {
            if (!orderManager.hasCanceledOrders()) {
                return;
            }
            if (!rateLimitManager.hasRateLimitCapacity()) {
                return;
            }
            OrderInfo restoreInfo = orderManager.pollLowestPriceCanceledOrder();
            if (restoreInfo != null) {
                orderExecutor.restoreSellAsync(restoreInfo);
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // ORDER_UPDATE TYPE: NEW
    // ----------------------------------------------------------------------------------------------------
    private void handleNewType(OrderUpdate update) {
        switch (update.side()) {
            case "BUY" -> {
                handleNewBuyState(update);
                logNewBuyState(update);
            }
            case "SELL" -> {
                handleNewSellState(update);
                logNewSellState(update);
            }
        }
    }

    private void handleNewBuyState(OrderUpdate update) {
        if (orderManager.containsBuyOrder(update.orderId())) {
            return;
        }

        OrderInfo info = new OrderInfo(
                update.orderId(),
                update.symbol(),
                update.orderQty(),
                update.orderPrice(),
                null
        );
        orderManager.addBuyOrder(info);
    }

    private void handleNewSellState(OrderUpdate update) {
        if (orderManager.containsSellOrder(update.orderId())) {
            return;
        }

        BigDecimal estimatedAvgBuyPrice = tradingProperties.scalePrice(
                tradingProperties.divide(
                        new BigDecimal(update.orderPrice()),
                        tradingProperties.risk().targetMultiplier()
                )
        );

        OrderInfo info = new OrderInfo(
                update.orderId(),
                update.symbol(),
                update.orderQty(),
                update.orderPrice(),
                estimatedAvgBuyPrice
        );
        orderManager.addSellOrder(info);
    }

    private void logNewBuyState(OrderUpdate update) {
        log.info("🟢 [NEW-BUY] 신규 매수 주문 | 가격: {}  | 수량: {} | 주문번호: {}",
                tradingProperties.scalePrice(new BigDecimal(update.orderPrice())),
                tradingProperties.scaleQty(new BigDecimal(update.orderQty())),
                update.orderId()
        );
    }

    private void logNewSellState(OrderUpdate update) {
        log.info("🔴 [NEW-SELL] 신규 매도 주문 | 가격: {} | 수량: {} | 주문번호: {}",
                tradingProperties.scalePrice(new BigDecimal(update.orderPrice())),
                tradingProperties.scaleQty(new BigDecimal(update.orderQty())),
                update.orderId()
        );
    }

    // ----------------------------------------------------------------------------------------------------
    // ORDER_UPDATE TYPE: TRADE
    // ----------------------------------------------------------------------------------------------------
    private void handleTradeType(OrderUpdate update) {
        switch (update.side()) {
            case "BUY" -> {
                handleTradeBuyState(update);
                logTradeBuyState(update);
            }
            case "SELL" -> {
                handleTradeSellState(update);
                logTradeSellState(update);
            }
        }
    }

    private void handleTradeBuyState(OrderUpdate update) {
        BigDecimal executedQty = new BigDecimal(update.lastExecutedQty());
        BigDecimal executedUsdValue = new BigDecimal(update.lastQuoteAssetTransactedQty());
        positionManager.addAcquired(executedQty, executedUsdValue);

        if (update.currentOrderStatus().equals("FILLED")) {
            orderManager.removeBuyOrder(update.orderId());
            rateLimitManager.onOrderFilled();
        }

        if (positionManager.isSellable()) {
            PositionInfo pulledInfo = positionManager.pullAcquired();
            NewOrderParams sellParams = tradingStrategy.calculateSellOrderParams(pulledInfo);
            orderExecutor.sellAsync(sellParams, pulledInfo);
        }
    }

    private void handleTradeSellState(OrderUpdate update) {
        if (update.currentOrderStatus().equals("FILLED")) {
            orderManager.removeSellOrder(update.orderId());
            rateLimitManager.onOrderFilled();
        }
    }

    private void logTradeBuyState(OrderUpdate update) {
        log.info("🟩 [TRADE-BUY] 매수 주문 체결 | 가격: {}  | 수량: {} | 주문번호: {}",
                tradingProperties.scalePrice(new BigDecimal(update.lastExecutedPrice())),
                tradingProperties.scaleQty(new BigDecimal(update.lastExecutedQty())),
                update.orderId()
        );
    }

    private void logTradeSellState(OrderUpdate update) {
        log.info("🟥 [TRADE-SELL] 매도 주문 체결 | 가격: {} | 수량: {} | 주문번호: {}",
                tradingProperties.scalePrice(new BigDecimal(update.lastExecutedPrice())),
                tradingProperties.scaleQty(new BigDecimal(update.lastExecutedQty())),
                update.orderId()
        );
    }

    // ----------------------------------------------------------------------------------------------------
    // ORDER_UPDATE TYPE: CANCELED
    // ----------------------------------------------------------------------------------------------------
    private void handleCanceledType(OrderUpdate update) {
        switch (update.side()) {
            case "BUY" -> {
                handleCanceledBuyState(update);
                logCanceledBuyState(update);
            }
            case "SELL" -> {
                handleCanceledSellState(update);
                logCanceledSellState(update);
            }
        }
    }

    private void handleCanceledBuyState(OrderUpdate update) {
        orderManager.removeBuyOrder(update.orderId());
    }

    private void handleCanceledSellState(OrderUpdate update) {
        orderManager.removeSellOrder(update.orderId());
    }

    private void logCanceledBuyState(OrderUpdate update) {
        log.info("🟧 [CANCELED-BUY] 매수 주문 취소 | 주문번호: {}", update.orderId());
    }

    private void logCanceledSellState(OrderUpdate update) {
        log.info("🟧 [CANCELED-SELL] 매도 주문 취소 | 주문번호: {}", update.orderId());
    }
}
