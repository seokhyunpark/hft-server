package io.github.seokhyunpark.hft.trading.executor;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.seokhyunpark.hft.exchange.client.BinanceClient;
import io.github.seokhyunpark.hft.exchange.dto.rest.CancelOrderResponse;
import io.github.seokhyunpark.hft.exchange.dto.rest.NewOrderResponse;
import io.github.seokhyunpark.hft.trading.config.TradingProperties;
import io.github.seokhyunpark.hft.trading.dto.NewOrderParams;
import io.github.seokhyunpark.hft.trading.dto.OrderInfo;
import io.github.seokhyunpark.hft.trading.dto.PositionInfo;
import io.github.seokhyunpark.hft.trading.manager.OrderManager;
import io.github.seokhyunpark.hft.trading.manager.PositionManager;
import io.github.seokhyunpark.hft.trading.manager.RateLimitManager;
import io.github.seokhyunpark.hft.trading.strategy.TradingStrategy;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExecutor {
    private final BinanceClient binanceClient;
    private final PositionManager positionManager;
    private final OrderManager orderManager;
    private final RateLimitManager rateLimitManager;
    private final TradingProperties tradingProperties;
    private final TradingStrategy tradingStrategy;

    @Async("buyOrderExecutor")
    public void buyAsync(NewOrderParams params) {
        try {
            ResponseEntity<NewOrderResponse> responseEntity = binanceClient.buyLimitMaker(
                    tradingProperties.symbol(),
                    tradingProperties.scaleQty(params.qty()).toPlainString(),
                    tradingProperties.scalePrice(params.price()).toPlainString()
            );
            updateRateLimit(responseEntity);

            NewOrderResponse response = responseEntity.getBody();
            if (response != null && response.orderId() != null) {
                OrderInfo info = new OrderInfo(
                        response.orderId(),
                        response.symbol(),
                        params.qty().toPlainString(),
                        params.price().toPlainString(),
                        null
                );
                orderManager.addBuyOrder(info);
                log.debug("[NEW-BUY] 신규 매수 주문 요청 성공 | 주문번호: {}", info.orderId());
            }
        } catch (Exception e) {
            log.error("[NEW-BUY] 신규 매수 주문 요청 실패 | 에러 메시지: {}", e.getMessage());
        }
    }

    @Async("buyOrderExecutor")
    public void cancelBuyAsync(OrderInfo info) {
        try {
            if (!orderManager.containsBuyOrder(info.orderId())) {
                log.debug("[CANCEL-BUY] 이미 체결 또는 취소된 매수 주문 | 주문번호: {}", info.orderId());
                return;
            }
            orderManager.removeBuyOrder(info.orderId());

            ResponseEntity<CancelOrderResponse> responseEntity = binanceClient.cancelOrder(
                    info.symbol(),
                    info.orderId()
            );

            CancelOrderResponse response = responseEntity.getBody();
            if (response != null && response.orderId() != null) {
                log.debug("[CANCEL-BUY] 매수 주문 취소 요청 성공 | 주문번호: {}", info.orderId());
            }
        } catch (Exception e) {
            log.error("[CANCEL-BUY] 매수 주문 취소 요청 실패 | 주문번호: : {}", info.orderId());
        }
    }

    @Async("sellOrderExecutor")
    public void sellAsync(NewOrderParams params, PositionInfo pulledInfo) {
        try {
            ResponseEntity<NewOrderResponse> responseEntity = binanceClient.sellLimitMaker(
                    tradingProperties.symbol(),
                    tradingProperties.scaleQty(params.qty()).toPlainString(),
                    tradingProperties.scalePrice(params.price()).toPlainString()
            );
            updateRateLimit(responseEntity);

            NewOrderResponse response = responseEntity.getBody();
            if (response != null && response.orderId() != null) {
                OrderInfo info = new OrderInfo(
                        response.orderId(),
                        response.symbol(),
                        params.qty().toPlainString(),
                        params.price().toPlainString(),
                        tradingProperties.scalePrice(
                                tradingProperties.divide(pulledInfo.totalUsdValue(), pulledInfo.totalQty())
                        )
                );
                orderManager.addSellOrder(info);
                log.debug("[NEW-SELL] 신규 매도 주문 요청 성공 | 주문번호: {}", info.orderId());
            }
        } catch (Exception e) {
            positionManager.restoreAcquired(pulledInfo);
            log.error("[NEW-SELL] 신규 매도 주문 요청 실패 | 에러 메시지: {}", e.getMessage());
        }
    }

    @Async("sellOrderExecutor")
    public void restoreSellAsync(OrderInfo info) {
        try {
            BigDecimal qty = new BigDecimal(info.qty());
            BigDecimal avgBuyPrice = info.avgBuyPrice();
            NewOrderParams sellParams = tradingStrategy.calculateSellOrderParams(qty, avgBuyPrice);

            ResponseEntity<NewOrderResponse> responseEntity = binanceClient.sellLimitMaker(
                    info.symbol(),
                    sellParams.qty().toPlainString(),
                    sellParams.price().toPlainString()
            );
            updateRateLimit(responseEntity);

            NewOrderResponse response = responseEntity.getBody();
            if (response != null && response.orderId() != null) {
                OrderInfo newInfo = new OrderInfo(
                        response.orderId(),
                        response.symbol(),
                        sellParams.qty().toPlainString(),
                        sellParams.price().toPlainString(),
                        info.avgBuyPrice()
                );
                orderManager.addSellOrder(newInfo);
                log.info("[RESTORE-SELL] 매도 주문 복구 성공 | 기존ID: {} -> 신규ID: {}", info.orderId(), newInfo.orderId());
            }
        } catch (Exception e) {
            orderManager.addCanceledOrder(info);
            log.error("[RESTORE-SELL] 매도 주문 복구 실패 | 에러 메시지: {}", e.getMessage());
        }
    }

    @Async("sellOrderExecutor")
    public void cancelSellAsync(OrderInfo info) {
        try {
            if (!orderManager.containsSellOrder(info.orderId())) {
                log.debug("[CANCEL-SELL] 이미 체결 또는 취소된 매도 주문 | 주문번호: {}", info.orderId());
                return;
            }
            orderManager.removeSellOrder(info.orderId());

            ResponseEntity<CancelOrderResponse> responseEntity = binanceClient.cancelOrder(
                    info.symbol(),
                    info.orderId()
            );

            CancelOrderResponse response = responseEntity.getBody();
            if (response != null && response.orderId() != null) {
                orderManager.addCanceledOrder(info);
                log.debug("[CANCEL-SELL] 매도 주문 취소 요청 성공 | 주문번호: {}", info.orderId());
            }
        } catch (Exception e) {
            log.error("[CANCEL-SELL] 매도 주문 취소 요청 실패 | 주문번호: : {}", info.orderId());
        }
    }

    private void updateRateLimit(ResponseEntity<?> responseEntity) {
        if (responseEntity == null) {
            return;
        }
        String orderCount10s = responseEntity.getHeaders().getFirst("X-MBX-ORDER-COUNT-10s");
        rateLimitManager.updateOrderCount(orderCount10s);
    }
}
