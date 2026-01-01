package io.github.seokhyunpark.hft.exchange.client;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.seokhyunpark.hft.exchange.dto.rest.CancelOrderRequest;
import io.github.seokhyunpark.hft.exchange.dto.rest.CancelOrderResponse;
import io.github.seokhyunpark.hft.exchange.dto.rest.GetAccountRequest;
import io.github.seokhyunpark.hft.exchange.dto.rest.GetAccountResponse;
import io.github.seokhyunpark.hft.exchange.dto.rest.NewOrderRequest;
import io.github.seokhyunpark.hft.exchange.dto.rest.NewOrderResponse;
import io.github.seokhyunpark.hft.exchange.util.SignatureUtil;

@Component
public class BinanceClient {
    private static final String BASE_URL = "https://api.binance.com";

    private final PrivateKey privateKey;
    private final SignatureUtil signatureUtil;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BinanceClient(
            @Value("${hft.exchange.api-key}") String apiKey,
            @Value("${hft.exchange.private-key-path}") String privateKeyPath,
            SignatureUtil signatureUtil,
            ObjectMapper objectMapper
    ) throws Exception {
        this.privateKey = signatureUtil.loadPrivateKey(privateKeyPath);
        this.signatureUtil = signatureUtil;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("X-MBX-APIKEY", apiKey)
                .build();
    }

    public NewOrderResponse buyMarket(String symbol, String qty) {
        NewOrderRequest request = new NewOrderRequest();
        request.setSymbol(symbol);
        request.setSide("BUY");
        request.setType("MARKET");
        request.setQty(qty);
        request.setTimestamp(getCurrentTimestamp());

        return sendRequest(
                "/api/v3/order",
                "POST",
                request,
                NewOrderResponse.class
        );
    }

    public NewOrderResponse sellMarket(String symbol, String qty) {
        NewOrderRequest request = new NewOrderRequest();
        request.setSymbol(symbol);
        request.setSide("SELL");
        request.setType("MARKET");
        request.setQty(qty);
        request.setTimestamp(getCurrentTimestamp());

        return sendRequest(
                "/api/v3/order",
                "POST",
                request,
                NewOrderResponse.class
        );
    }

    public NewOrderResponse buyLimitMaker(String symbol, String qty, String price) {
        NewOrderRequest request = new NewOrderRequest();
        request.setSymbol(symbol);
        request.setSide("BUY");
        request.setType("LIMIT_MAKER");
        request.setQty(qty);
        request.setPrice(price);
        request.setTimestamp(getCurrentTimestamp());

        return sendRequest(
                "/api/v3/order",
                "POST",
                request,
                NewOrderResponse.class
        );
    }

    public NewOrderResponse sellLimitMaker(String symbol, String qty, String price) {
        NewOrderRequest request = new NewOrderRequest();
        request.setSymbol(symbol);
        request.setSide("SELL");
        request.setType("LIMIT_MAKER");
        request.setQty(qty);
        request.setPrice(price);
        request.setTimestamp(getCurrentTimestamp());

        return sendRequest(
                "/api/v3/order",
                "POST",
                request,
                NewOrderResponse.class
        );
    }

    public CancelOrderResponse cancelOrder(String symbol, long orderId) {
        CancelOrderRequest request = new CancelOrderRequest();
        request.setSymbol(symbol);
        request.setOrderId(orderId);
        request.setTimestamp(getCurrentTimestamp());

        return sendRequest(
                "/api/v3/order",
                "DELETE",
                request,
                CancelOrderResponse.class
        );
    }

    public GetAccountResponse getAccount() {
        GetAccountRequest request = new GetAccountRequest();
        request.setOmitZeroBalances(true);
        request.setTimestamp(getCurrentTimestamp());

        return sendRequest(
                "/api/v3/account",
                "GET",
                request,
                GetAccountResponse.class
        );
    }

    private <T> T sendRequest(String endpoint, String method, Object requestDto, Class<T> responseType) {
        try {
            Map<String, String> params = objectMapper.convertValue(requestDto, new TypeReference<>() {
            });
            String queryString = buildQueryString(params);
            URI uri = URI.create(BASE_URL + endpoint + "?" + queryString);
            return restClient.method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri(uri)
                    .retrieve()
                    .body(responseType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildQueryString(Map<String, String> params) throws Exception {
        String queryString = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + urlEncode(e.getValue()))
                .collect(Collectors.joining("&"));
        String signature = signatureUtil.generateSignature(queryString, privateKey);
        return queryString + "&signature=" + urlEncode(signature);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
}
