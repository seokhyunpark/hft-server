package io.github.seokhyunpark.hft.exchange.service;

import io.github.seokhyunpark.hft.exchange.dto.stream.AccountUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.BalanceUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.OrderUpdate;
import io.github.seokhyunpark.hft.exchange.listener.UserEventListener;
import io.github.seokhyunpark.hft.exchange.stream.MarketDataStream;
import io.github.seokhyunpark.hft.exchange.dto.stream.PartialBookDepth;
import io.github.seokhyunpark.hft.exchange.listener.MarketEventListener;
import io.github.seokhyunpark.hft.exchange.stream.UserDataStream;
import io.github.seokhyunpark.hft.exchange.util.SignatureUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeService {
    private final SignatureUtil signatureUtil;

    private MarketDataStream marketDataStream;
    private UserDataStream userDataStream;

    @Value("${hft.stream.market-uri}")
    private String marketUri;

    @Value("${hft.stream.user-uri}")
    private String userUri;

    @Value("${hft.exchange.api-key}")
    private String apiKey;

    @Value("${hft.exchange.private-key-path}")
    private String privateKeyPath;

    @PostConstruct
    public void connect() {
        boolean userStreamConnected = connectUserStream();
        if (userStreamConnected) {
            connectMarketStream();
        }
    }

    private boolean connectUserStream() {
        try {
            URI uri = new URI(userUri);

            UserEventListener userEventListener = new UserEventListener() {
                @Override
                public void onAccountUpdateReceived(AccountUpdate accountUpdate) {
                    log.info("[User: Account] {}", accountUpdate.toString());
                }

                @Override
                public void onBalanceUpdateReceived(BalanceUpdate balanceUpdate) {
                    log.info("[User: Balance] {}", balanceUpdate.toString());
                }

                @Override
                public void onOrderUpdateReceived(OrderUpdate orderUpdate) {
                    log.info("[User: Order] {}", orderUpdate.toString());
                }
            };

            userDataStream = new UserDataStream(uri, userEventListener, apiKey, privateKeyPath, signatureUtil);

            boolean connected = userDataStream.connectBlocking();
            if (!connected) {
                log.error("[User] 웹소켓 연결 실패");
                return false;
            }

            boolean subscribed = userDataStream.awaitUserDataStreamReady(5, TimeUnit.SECONDS);
            if (subscribed) {
                log.info("[User] 세션 로그온 및 구독 성공");
                return true;
            } else {
                log.error("[User] 구독 응답 시간 초과");
                return false;
            }
        } catch (Exception e) {
            log.error("[User] 웹소켓 초기화 실패: {}", e.getMessage());
            return false;
        }
    }

    private void connectMarketStream() {
        try {
            URI uri = new URI(marketUri);

            MarketEventListener marketEventListener = new MarketEventListener() {
                @Override
                public void onPartialBookDepthReceived(PartialBookDepth depth) {
                    if (depth == null) {
                        return;
                    }

                    List<List<String>> bids = depth.bids();
                    List<List<String>> asks = depth.asks();
                    if (bids == null || bids.isEmpty() || asks == null || asks.isEmpty()) {
                        return;
                    }

                    String price = bids.getFirst().getFirst();
                    String qty = bids.getFirst().getLast();

                    log.debug("[Market] 수신 ID: {} | 가격: {}, 수량: {}", depth.lastUpdateId(), price, qty);
                }
            };

            marketDataStream = new MarketDataStream(uri, marketEventListener);
            marketDataStream.connect();

        } catch (Exception e) {
            log.error("[Market] 웹소켓 초기화 실패: {}", e.getMessage());
        }
    }
}
