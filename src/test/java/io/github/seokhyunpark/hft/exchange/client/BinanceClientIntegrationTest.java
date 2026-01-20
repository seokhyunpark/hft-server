package io.github.seokhyunpark.hft.exchange.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import lombok.extern.slf4j.Slf4j;

import io.github.seokhyunpark.hft.exchange.dto.rest.CancelOrderResponse;
import io.github.seokhyunpark.hft.exchange.dto.rest.GetAccountResponse;
import io.github.seokhyunpark.hft.exchange.dto.rest.GetAccountResponse.Balance;
import io.github.seokhyunpark.hft.exchange.dto.rest.GetOrderResponse;
import io.github.seokhyunpark.hft.exchange.dto.rest.NewOrderResponse;

@Slf4j
@SpringBootTest(properties = "hft.websocket.enabled=false")
public class BinanceClientIntegrationTest {
    @Autowired
    private BinanceClient binanceClient;

    private static final List<String> STABLE_COINS = Arrays.asList("USDT", "USDC", "FDUSD");
    private static final double MINIMUM_USD = 5.0;

    @Test
    @DisplayName("계좌 조회 및 주문 가능한 스테이블코인으로 주문 및 취소 통합 테스트")
    void integratedAccountAndOrderTest() throws Exception {
        ResponseEntity<GetAccountResponse> accountResponseEntity = binanceClient.getAccount();
        assertThat(accountResponseEntity.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(accountResponseEntity).isNotNull();

        GetAccountResponse accountResponse = accountResponseEntity.getBody();
        assertThat(accountResponse).isNotNull();

        log.info("계좌 UID: {}", accountResponse.uid());
        log.info("보유 잔고 목록:");

        Balance targetBalance = null;

        for (Balance balance : accountResponse.balances()) {
            log.info("\t>> Asset: {}, Free: {}, Locked: {}", balance.asset(), balance.free(), balance.locked());
            if (targetBalance == null
                    && STABLE_COINS.contains(balance.asset())
                    && Double.parseDouble(balance.free()) >= MINIMUM_USD) {
                targetBalance = balance;
            }
        }

        if (targetBalance == null) {
            log.warn("테스트 가능한 스테이블코인(USDT, FDUSD, USDC) 잔고가 {} 미만입니다. 주문 테스트를 스킵합니다.", MINIMUM_USD);
            return;
        }

        String asset = targetBalance.asset();
        String symbol = "BTC" + asset;

        log.info("주문 및 취소 테스트 시작:");

        try {
            // 주문 요청
            ResponseEntity<NewOrderResponse> newOrderResponseEntity = binanceClient.buyLimitMaker(symbol, "0.0001", "50000");
            assertThat(newOrderResponseEntity.getStatusCode().is2xxSuccessful()).isTrue();

            NewOrderResponse newOrderResponse = newOrderResponseEntity.getBody();
            assertThat(newOrderResponse).isNotNull();
            log.info("\t>> 주문번호: {}", newOrderResponse.orderId());
            assertThat(newOrderResponse.orderId()).isGreaterThan(0);

            // 대기
            log.info("\t>> 주문 후 1초 대기 중...");
            Thread.sleep(1000);

            // 주문 조회
            ResponseEntity<GetOrderResponse> getOrderResponseEntity = binanceClient.getOrder(symbol, newOrderResponse.orderId());
            assertThat(getOrderResponseEntity.getStatusCode().is2xxSuccessful()).isTrue();

            GetOrderResponse getOrderResponse = getOrderResponseEntity.getBody();
            assertThat(getOrderResponse).isNotNull();
            log.info("\t>> 주문조회: {}", getOrderResponse);
            assertThat(getOrderResponse.orderId()).isEqualTo(newOrderResponse.orderId());

            // 주문 취소
            ResponseEntity<CancelOrderResponse> cancelOrderResponseEntity = binanceClient.cancelOrder(symbol, newOrderResponse.orderId());
            assertThat(cancelOrderResponseEntity.getStatusCode().is2xxSuccessful()).isTrue();

            CancelOrderResponse cancelOrderResponse = cancelOrderResponseEntity.getBody();
            assertThat(cancelOrderResponse).isNotNull();
            log.info("\t>> 취소된 주문번호: {}", cancelOrderResponse.orderId());
            assertThat(cancelOrderResponse.orderId()).isEqualTo(newOrderResponse.orderId());

        } catch (Exception e) {
            log.error("주문/취소 테스트 중 에러 발생: {}", e.getMessage());
            throw e;
        }
    }
}
