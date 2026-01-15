package io.github.seokhyunpark.hft.trading.service;

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
                        response.transactTime()
                );
                orderManager.addBuyOrder(info);
                log.info("[NEW-BUY] 요청 성공: {}", info.orderId());
            }
        } catch (Exception e) {
            log.error("[NEW-BUY] 요청 실패: {}", e.getMessage());
        }
    }

    @Async("buyOrderExecutor")
    public void executeCancelBuyOrder(OrderInfo info) {
        try {
            if (!orderManager.containsBuyOrder(info.orderId())) {
                log.info("[CANCEL-BUY] 알 수 없는 주문: {}", info.orderId());
                return;
            }
            ResponseEntity<CancelOrderResponse> responseEntity = binanceClient.cancelOrder(
                    info.symbol(),
                    info.orderId()
            );

            CancelOrderResponse response = responseEntity.getBody();
            if (response != null && response.orderId() != null) {
                log.info("[CANCEL-BUY] 요청 성공: {}", info.orderId());
            }
        } catch (Exception e) {
            log.error("[CANCEL-BUY] 요청 실패: {}", e.getMessage());
        } finally {
            orderManager.removeBuyOrder(info.orderId());
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
                        response.transactTime()
                );
                orderManager.addSellOrder(info);
                log.info("[NEW-SELL] 요청 성공: {}", info.orderId());
            }
        } catch (Exception e) {
            acquiredLedger.restoreAcquired(pulledInfo);
            log.error("[NEW-SELL] 요청 실패: {}", e.getMessage());
        }
    }

    @Async("sellOrderExecutor")
    public void executeCancelSellOrder(OrderInfo info) {
        try {
            if (!orderManager.containsSellOrder(info.orderId())) {
                log.info("[CANCEL-SELL] 알 수 없는 주문: {}", info.orderId());
                return;
            }
            ResponseEntity<CancelOrderResponse> responseEntity = binanceClient.cancelOrder(
                    info.symbol(),
                    info.orderId()
            );

            CancelOrderResponse response = responseEntity.getBody();
            if (response != null && response.orderId() != null) {
                orderManager.addCanceledOrder(info);
                log.info("[CANCEL-SELL] 요청 성공: {}", info.orderId());
            }
        } catch (Exception e) {
            log.error("[CANCEL-SELL] 요청 실패: {}", e.getMessage());
        } finally {
            orderManager.removeSellOrder(info.orderId());
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
