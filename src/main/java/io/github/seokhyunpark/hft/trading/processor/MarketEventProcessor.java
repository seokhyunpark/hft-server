package io.github.seokhyunpark.hft.trading.processor;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.seokhyunpark.hft.exchange.dto.stream.PartialBookDepth;
import io.github.seokhyunpark.hft.exchange.listener.MarketEventListener;
import io.github.seokhyunpark.hft.trading.dto.NewOrderParams;
import io.github.seokhyunpark.hft.trading.dto.OrderInfo;
import io.github.seokhyunpark.hft.trading.executor.OrderExecutor;
import io.github.seokhyunpark.hft.trading.manager.OrderManager;
import io.github.seokhyunpark.hft.trading.manager.QuoteAssetManager;
import io.github.seokhyunpark.hft.trading.manager.RateLimitManager;
import io.github.seokhyunpark.hft.trading.strategy.TradingStrategy;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketEventProcessor implements MarketEventListener {
    private final OrderExecutor orderExecutor;
    private final OrderManager orderManager;
    private final QuoteAssetManager quoteAssetManager;
    private final RateLimitManager rateLimitManager;
    private final TradingStrategy tradingStrategy;

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
}
