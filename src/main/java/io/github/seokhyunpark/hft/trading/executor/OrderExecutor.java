package io.github.seokhyunpark.hft.trading.executor;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper;
    private final BinanceClient binanceClient;
    private final TradingProperties props;
    private final OrderManager orderManager;
    private final PositionManager positionManager;
    private final RateLimitManager rateLimitManager;
    private final TradingStrategy tradingStrategy;

    @Async("buyOrderExecutor")
    public void buyAsync(NewOrderParams params) {
        try {
            ResponseEntity<NewOrderResponse> responseEntity = binanceClient.buyLimitMaker(
                    props.symbol(),
                    props.scaleQty(params.qty()).toPlainString(),
                    props.scalePrice(params.price()).toPlainString()
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
                log.debug("[NEW-BUY] OK | ID: {}", info.orderId());
            }
        } catch (HttpClientErrorException e) {
            log.warn("⚠️[NEW-BUY] FAIL | REASON: {}", extractErrorMessage(e));
        } catch (Exception e) {
            log.error("[NEW-BUY] ERROR | MESSAGE: {}", e.getMessage());
        }
    }

    @Async("buyOrderExecutor")
    public void cancelBuyAsync(OrderInfo info) {
        try {
            if (!orderManager.containsBuyOrder(info.orderId())) {
                log.debug("[CANCEL-BUY] SKIP | ID: {}", info.orderId());
                return;
            }
            orderManager.removeBuyOrder(info.orderId());

            ResponseEntity<CancelOrderResponse> responseEntity = binanceClient.cancelOrder(
                    info.symbol(),
                    info.orderId()
            );

            CancelOrderResponse response = responseEntity.getBody();
            if (response != null && response.orderId() != null) {
                log.debug("[CANCEL-BUY] OK | ID: {}", info.orderId());
            }
        } catch (HttpClientErrorException e) {
            log.warn("⚠️[CANCEL-BUY] FAIL | ID: {} | REASON: {}", info.orderId(), extractErrorMessage(e));
        } catch (Exception e) {
            log.error("[CANCEL-BUY] ERROR | ID: {} | MESSAGE: {}", info.orderId(), e.getMessage());
        }
    }

    @Async("sellOrderExecutor")
    public void sellAsync(NewOrderParams params, PositionInfo pulledInfo) {
        try {
            ResponseEntity<NewOrderResponse> responseEntity = binanceClient.sellLimitMaker(
                    props.symbol(),
                    props.scaleQty(params.qty()).toPlainString(),
                    props.scalePrice(params.price()).toPlainString()
            );
            updateRateLimit(responseEntity);

            NewOrderResponse response = responseEntity.getBody();
            if (response != null && response.orderId() != null) {
                OrderInfo info = new OrderInfo(
                        response.orderId(),
                        response.symbol(),
                        params.qty().toPlainString(),
                        params.price().toPlainString(),
                        props.scalePrice(props.divide(pulledInfo.totalUsdValue(), pulledInfo.totalQty()))
                );
                orderManager.addSellOrder(info);
                log.debug("[NEW-SELL] OK | ID: {}", info.orderId());
            }
        } catch (HttpClientErrorException e) {
            positionManager.restorePosition(pulledInfo);
            log.warn("⚠️[NEW-SELL] FAIL | REASON: {}", extractErrorMessage(e));
        } catch (Exception e) {
            positionManager.restorePosition(pulledInfo);
            log.error("[NEW-SELL] ERROR | MESSAGE: {}", e.getMessage());
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
                log.debug("[RESTORE-SELL] OK | ID: {}", newInfo.orderId());
            }
        } catch (HttpClientErrorException e) {
            orderManager.addCanceledOrder(info);
            log.warn("⚠️[RESTORE-SELL] FAIL | REASON: {}", extractErrorMessage(e));
        } catch (Exception e) {
            orderManager.addCanceledOrder(info);
            log.error("[RESTORE-SELL] ERROR | MESSAGE: {}", e.getMessage());
        }
    }

    @Async("sellOrderExecutor")
    public void cancelSellAsync(OrderInfo info) {
        try {
            if (!orderManager.containsSellOrder(info.orderId())) {
                log.debug("[CANCEL-SELL] SKIP | ID: {}", info.orderId());
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
                log.debug("[CANCEL-SELL] OK | ID: {}", info.orderId());
            }
        } catch (HttpClientErrorException e) {
            log.warn("⚠️[CANCEL-SELL] FAIL | ID: {} | REASON: {}", info.orderId(), extractErrorMessage(e));
        } catch (Exception e) {
            log.error("[CANCEL-SELL] ERROR | ID: {} | MESSAGE: {}", info.orderId(), e.getMessage());
        }
    }

    private void updateRateLimit(ResponseEntity<?> responseEntity) {
        if (responseEntity == null) {
            return;
        }

        String rawCount = responseEntity.getHeaders().getFirst("X-MBX-ORDER-COUNT-10s");
        if (rawCount != null && rawCount.matches("\\d+")) {
            int count = Integer.parseInt(rawCount);
            rateLimitManager.syncOrderCount(count);
        }
    }

    private String extractErrorMessage(HttpClientErrorException e) {
        try {
            return objectMapper.readTree(e.getResponseBodyAsString())
                    .path("msg")
                    .asText(e.getResponseBodyAsString());
        } catch (Exception err) {
            return e.getMessage();
        }
    }
}
