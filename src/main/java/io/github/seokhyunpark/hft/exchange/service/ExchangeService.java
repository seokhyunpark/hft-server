package io.github.seokhyunpark.hft.exchange.service;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.seokhyunpark.hft.exchange.stream.MarketDataStream;
import io.github.seokhyunpark.hft.exchange.stream.UserDataStream;
import io.github.seokhyunpark.hft.exchange.util.SignatureUtil;
import io.github.seokhyunpark.hft.trading.core.TradingCore;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeService {
    private final SignatureUtil signatureUtil;
    private final TradingCore tradingCore;

    private MarketDataStream marketDataStream;
    private UserDataStream userDataStream;

    @Value("${hft.websocket.enabled}")
    private boolean websocketEnabled;

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
        if (!websocketEnabled) {
            log.info("웹소켓 연결 설정이 비활성화되어 있습니다. 연결을 건너뜁니다.");
            return;
        }

        if (connectUserStream()) {
            connectMarketStream();
        }
    }

    private boolean connectUserStream() {
        try {
            URI uri = new URI(userUri);
            userDataStream = new UserDataStream(uri, tradingCore, apiKey, privateKeyPath, signatureUtil);

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
            marketDataStream = new MarketDataStream(uri, tradingCore);
            marketDataStream.connect();

        } catch (Exception e) {
            log.error("[Market] 웹소켓 초기화 실패: {}", e.getMessage());
        }
    }
}
