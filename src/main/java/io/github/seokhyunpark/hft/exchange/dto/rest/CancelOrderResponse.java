package io.github.seokhyunpark.hft.exchange.dto.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CancelOrderResponse(
        // https://developers.binance.com/docs/binance-spot-api-docs/rest-api/trading-endpoints#cancel-order-trade
        @JsonProperty("symbol") String symbol,
        @JsonProperty("origClientOrderId") String origClientOrderId,
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("orderListId") Long orderListId,
        @JsonProperty("clientOrderId") String clientOrderId,
        @JsonProperty("transactTime") Long transactTime,
        @JsonProperty("price") String price,
        @JsonProperty("origQty") String origQty,
        @JsonProperty("executedQty") String executedQty,
        @JsonProperty("origQuoteOrderQty") String origQuoteOrderQty,
        @JsonProperty("cummulativeQuoteQty") String cummulativeQuoteQty,
        @JsonProperty("status") String status,
        @JsonProperty("timeInForce") String timeInForce,
        @JsonProperty("type") String type,
        @JsonProperty("side") String side,
        @JsonProperty("selfTradePreventionMode") String selfTradePreventionMode
) {
}
