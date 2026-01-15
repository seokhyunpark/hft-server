package io.github.seokhyunpark.hft.trading.core;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import io.github.seokhyunpark.hft.exchange.dto.stream.AccountUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.BalanceUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.OrderUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.PartialBookDepth;
import io.github.seokhyunpark.hft.exchange.listener.MarketEventListener;
import io.github.seokhyunpark.hft.exchange.listener.UserEventListener;
import io.github.seokhyunpark.hft.trading.config.TradingProperties;
import io.github.seokhyunpark.hft.trading.dto.OrderInfo;
import io.github.seokhyunpark.hft.trading.dto.OrderParams;
import io.github.seokhyunpark.hft.trading.manager.QuoteAssetManager;
import io.github.seokhyunpark.hft.trading.manager.OrderManager;
import io.github.seokhyunpark.hft.trading.manager.RateLimitManager;
import io.github.seokhyunpark.hft.trading.service.OrderService;
import io.github.seokhyunpark.hft.trading.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingCore implements MarketEventListener, UserEventListener {
    private final TradingStrategy tradingStrategy;
    private final QuoteAssetManager quoteAssetManager;
    private final OrderManager orderManager;
    private final RateLimitManager rateLimitManager;
    private final OrderService orderService;
    private final TradingProperties tradingProperties;

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

        // 중복된 가격 확인
        OrderParams buyParams = tradingStrategy.calculateBuyOrderParams(depth);
        if (buyParams.isInvalid() || orderManager.hasBuyOrderAt(buyParams.price())) {
            log.debug("[REJECT] 중복된 가격: {}", buyParams.price());
            return;
        }

        // Rate Limit 확인
        if (!rateLimitManager.hasRateLimitCapacity()) {
            log.debug("[REJECT] 요청 수 제한 최대: {}", rateLimitManager.getOrderCount());
            return;
        }

        // USD 잔고 확인
        if (!quoteAssetManager.hasQuoteBalanceFor(buyParams.getUsdValue())) {
            log.debug("[REJECT] USD 잔고 부족: [보유: {}, 필요: {}]", quoteAssetManager.getQuoteBalance(), buyParams.getUsdValue());
            return;
        }

        // Open Orders 개수 확인
        if (!orderManager.hasOpenOrderCapacity()) {
            log.debug("[REJECT] Open Orders 개수 최대: {}", orderManager.getOpenOrdersCount());
            return;
        }

        // 매수 주문 (상태 낙관적 업데이트)
        rateLimitManager.onOrderPlaced();
        quoteAssetManager.deductQuoteBalance(buyParams.getUsdValue());
        orderService.executeBuyOrder(buyParams);

        // Buy Orders 개수 관리
        if (orderManager.isBuyOrdersFull()) {
            OrderInfo info = orderManager.getOldestBuyOrder();
            if (info != null) {
                orderService.executeCancelBuyOrder(info);
                log.debug("[REJECT] Buy Orders 개수 최대: {}", orderManager.getBuyOrderCount());
            }
        }
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

    private void handleNewType(OrderUpdate update) {

    }

    private void handleTradeType(OrderUpdate update) {

    }

    private void handleCanceledType(OrderUpdate update) {

    }
}
