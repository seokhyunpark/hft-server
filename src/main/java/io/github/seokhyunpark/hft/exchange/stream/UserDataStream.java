package io.github.seokhyunpark.hft.exchange.stream;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import io.github.seokhyunpark.hft.exchange.dto.stream.AccountUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.BalanceUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.LogonRequest;
import io.github.seokhyunpark.hft.exchange.dto.stream.OrderUpdate;
import io.github.seokhyunpark.hft.exchange.listener.UserEventListener;
import io.github.seokhyunpark.hft.exchange.util.SignatureUtil;

@Slf4j
public class UserDataStream extends WebSocketClient {
    private final CountDownLatch userDataStreamReady = new CountDownLatch(1);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserEventListener userEventListener;

    private final String apiKey;
    private final PrivateKey privateKey;
    private final SignatureUtil signatureUtil;

    public UserDataStream(URI uri, UserEventListener userEventListener, String apiKey, String privateKeyPath,
                          SignatureUtil signatureUtil) throws Exception {
        super(uri);
        this.userEventListener = userEventListener;
        this.apiKey = apiKey;
        this.privateKey = signatureUtil.loadPrivateKey(privateKeyPath);
        this.signatureUtil = signatureUtil;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        log.info("[User] 웹소켓 연결 성공");
        logon();
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            if (node.has("id")) {
                handleResponse(node);
                return;
            }

            JsonNode payload = node.has("event") ? node.get("event") : node;
            if (!payload.has("e")) {
                log.debug("[User] 알 수 없는 메시지: {}", message);
                return;
            }

            String eventType = payload.get("e").asText();
            switch (eventType) {
                case "outboundAccountPosition" -> {
                    AccountUpdate accountUpdate = objectMapper.treeToValue(payload, AccountUpdate.class);
                    userEventListener.onAccountUpdateReceived(accountUpdate);
                }
                case "balanceUpdate" -> {
                    BalanceUpdate balanceUpdate = objectMapper.treeToValue(payload, BalanceUpdate.class);
                    userEventListener.onBalanceUpdateReceived(balanceUpdate);
                }
                case "executionReport" -> {
                    OrderUpdate orderUpdate = objectMapper.treeToValue(payload, OrderUpdate.class);
                    userEventListener.onOrderUpdateReceived(orderUpdate);
                }
                default -> {
                    log.debug("[User] 알 수 없는 이벤트: {}", eventType);
                }
            }
        } catch (Exception e) {
            log.error("[User] onMessage 에러 발생: {}", e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("[User] 웹소켓 연결 종료 (Code: {}, Reason: {})", code, reason);
    }

    @Override
    public void onError(Exception e) {
        log.error("[User] 웹소켓 에러 발생");
    }

    public boolean awaitUserDataStreamReady(long timeout, TimeUnit unit) throws InterruptedException {
        return userDataStreamReady.await(timeout, unit);
    }

    private void logon() {
        // https://developers.binance.com/docs/binance-spot-api-docs/websocket-api/account-requests
        try {
            long timestamp = System.currentTimeMillis();

            String payload = "apiKey=" + urlEncode(apiKey) + "&timestamp=" + urlEncode(String.valueOf(timestamp));
            String signature = signatureUtil.generateSignature(payload, privateKey);

            LogonRequest logonRequest = LogonRequest.of(apiKey, signature, timestamp);
            String request = objectMapper.writeValueAsString(logonRequest);

            log.info("[User] 세션 로그온 요청 전송");
            send(request);
        } catch (Exception e) {
            log.error("[User] 세션 로그온 실패: {}", e.getMessage());
            close();
        }
    }

    private void subscribe() {
        // https://developers.binance.com/docs/binance-spot-api-docs/websocket-api/user-data-stream-requests
        try {
            Map<String, Object> message = Map.of(
                    "id", UUID.randomUUID().toString(),
                    "method", "userDataStream.subscribe"
            );

            String request = objectMapper.writeValueAsString(message);

            log.info("[User] 구독 요청 전송");
            send(request);
        } catch (Exception e) {
            log.error("[User] 구독 실패: {}", e.getMessage());
        }
    }

    private void handleResponse(JsonNode node) {
        if (node.has("status") && node.get("status").asInt() != 200) {
            log.error("[User] 요청 실패: {}", node);
            return;
        }

        JsonNode result = node.get("result");
        if (result == null) {
            return;
        }

        if (result.has("apiKey")) {
            log.info("[User] 세션 로그온 성공");
            subscribe();
        } else if (result.has("subscriptionId")) {
            userDataStreamReady.countDown();
            log.info("[User] 구독 성공");
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
