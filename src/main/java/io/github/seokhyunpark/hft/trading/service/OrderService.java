package io.github.seokhyunpark.hft.trading.service;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.github.seokhyunpark.hft.exchange.client.BinanceClient;
import io.github.seokhyunpark.hft.exchange.dto.rest.CancelOrderResponse;
import io.github.seokhyunpark.hft.exchange.dto.rest.NewOrderResponse;
import io.github.seokhyunpark.hft.trading.config.TradingProperties;
import io.github.seokhyunpark.hft.trading.dto.OrderInfo;
import io.github.seokhyunpark.hft.trading.dto.OrderParams;
import io.github.seokhyunpark.hft.trading.dto.PositionInfo;
import io.github.seokhyunpark.hft.trading.ledger.AcquiredLedger;
import io.github.seokhyunpark.hft.trading.manager.OrderManager;
import io.github.seokhyunpark.hft.trading.manager.RateLimitManager;
import io.github.seokhyunpark.hft.trading.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final BinanceClient binanceClient;
    private final AcquiredLedger acquiredLedger;
    private final OrderManager orderManager;
    private final RateLimitManager rateLimitManager;
    private final TradingProperties tradingProperties;
    private final TradingStrategy tradingStrategy;

    @Async("buyOrderExecutor")
    public void executeBuyOrder(OrderParams params) {
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
    public void executeCancelBuyOrder(OrderInfo info) {
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
    public void executeSellOrder(OrderParams params, PositionInfo pulledInfo) {
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
            acquiredLedger.restoreAcquired(pulledInfo);
            log.error("[NEW-SELL] 신규 매도 주문 요청 실패 | 에러 메시지: {}", e.getMessage());
        }
    }

    @Async("sellOrderExecutor")
    public void executeRestoreSellOrder(OrderInfo info) {
        try {
            BigDecimal qty = new BigDecimal(info.qty());
            BigDecimal avgBuyPrice = info.avgBuyPrice();
            OrderParams sellParams = tradingStrategy.calculateSellOrderParams(qty, avgBuyPrice);

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
    public void executeCancelSellOrder(OrderInfo info) {
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
