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
import io.github.seokhyunpark.hft.trading.manager.OrderManager;
import io.github.seokhyunpark.hft.trading.manager.RateLimitManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final BinanceClient binanceClient;
    private final OrderManager orderManager;
    private final RateLimitManager rateLimitManager;
    private final TradingProperties tradingProperties;

    @Async("buyOrderExecutor")
    public void executeBuyOrder(OrderParams params) {
        try {
            ResponseEntity<NewOrderResponse> responseEntity = binanceClient.buyLimitMaker(
                    tradingProperties.symbol(),
                    params.qty().toPlainString(),
                    params.price().toPlainString()
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
                log.info("[NEW-BUY] 요청 성공: {}", info);
            }
        } catch (Exception e) {
            log.error("[NEW-BUY] 요청 실패: {}", e.getMessage());
        }
    }

    @Async("buyOrderExecutor")
    public void executeCancelBuyOrder(OrderInfo info) {
        try {
            if (!orderManager.containsBuyOrder(info.orderId())) {
                return;
            }
            ResponseEntity<CancelOrderResponse> responseEntity = binanceClient.cancelOrder(
                    info.symbol(),
                    info.orderId()
            );

            CancelOrderResponse response = responseEntity.getBody();
            if (response != null && response.orderId() != null) {
                orderManager.removeBuyOrder(info.orderId());
                log.info("[CANCEL-BUY] 요청 성공: {}", info);
            }
        } catch (Exception e) {
            log.error("[CANCEL-BUY] 요청 실패: {}", e.getMessage());
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
