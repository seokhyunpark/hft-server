package io.github.seokhyunpark.hft.exchange.stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import io.github.seokhyunpark.hft.exchange.dto.stream.PartialBookDepth;
import io.github.seokhyunpark.hft.exchange.listener.MarketEventListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MarketDataStream extends WebSocketClient {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MarketEventListener marketEventListener;

    public MarketDataStream(URI uri, MarketEventListener marketEventListener) {
        super(uri);
        this.marketEventListener = marketEventListener;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        log.info("[Market] 웹소켓 연결 성공");
    }

    @Override
    public void onMessage(String message) {
        try {
            PartialBookDepth depth = objectMapper.readValue(message, PartialBookDepth.class);
            marketEventListener.onPartialBookDepthReceived(depth);
        } catch (Exception e) {
            log.error("[Market] onMessage 에러 발생: {}", e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("[Market] 웹소켓 연결 종료 (Code: {}, Reason: {})", code, reason);
    }

    @Override
    public void onError(Exception e) {
        log.error("[Market] 웹소켓 에러 발생");
    }
}
