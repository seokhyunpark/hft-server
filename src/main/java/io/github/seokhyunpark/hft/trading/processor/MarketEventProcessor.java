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
        if (depth == null) {
            return;
        }
        tradingStrategy.updateBestAskPrice(depth);

        manageBuyOrdersCapacity();
        manageSellOrdersCapacity();

        NewOrderParams buyParams = tradingStrategy.calculateBuyOrderParams(depth);
        manageConflictingBuyOrder(buyParams);
        if (isBuyOrderInvalid(buyParams) || !hasExecutionCapacity(buyParams)) {
            return;
        }

        executeBuyOrder(buyParams);
    }

    private void manageBuyOrdersCapacity() {
        if (orderManager.isBuyOrdersFull()) {
            OrderInfo info = orderManager.getOldestBuyOrder();
            if (info != null) {
                orderExecutor.cancelBuyAsync(info);
            }
        }
    }

    private void manageSellOrdersCapacity() {
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

    private void manageConflictingBuyOrder(NewOrderParams params) {
        OrderInfo info = orderManager.findConflictingBuyOrder(params.price());
        if (info != null) {
            orderExecutor.cancelBuyAsync(info);
        }
    }

    private boolean isBuyOrderInvalid(NewOrderParams params) {
        return params.isInvalid()
                || orderManager.hasBuyOrderAt(params.price())
                || orderManager.conflictsWithSellOrders(params.price());
    }

    private boolean hasExecutionCapacity(NewOrderParams params) {
        return rateLimitManager.hasRateLimitCapacity()
                && orderManager.hasOpenOrderCapacity()
                && quoteAssetManager.hasQuoteBalanceFor(params.getUsdValue());
    }

    private void executeBuyOrder(NewOrderParams params) {
        rateLimitManager.onOrderPlaced();
        quoteAssetManager.deductQuoteBalance(params.getUsdValue());
        orderExecutor.buyAsync(params);
    }
}
