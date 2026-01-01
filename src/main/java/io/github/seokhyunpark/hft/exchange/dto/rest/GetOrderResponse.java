package io.github.seokhyunpark.hft.exchange.dto.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GetOrderResponse(
        // https://developers.binance.com/docs/binance-spot-api-docs/rest-api/account-endpoints#query-order-user_data
        @JsonProperty("symbol") String symbol,
        @JsonProperty("orderId") long orderId,
        @JsonProperty("orderListId") long orderListId,
        @JsonProperty("clientOrderId") String clientOrderId,
        @JsonProperty("price") String price,
        @JsonProperty("origQty") String origQty,
        @JsonProperty("executedQty") String executedQty,
        @JsonProperty("cummulativeQuoteQty") String cummulativeQuoteQty,
        @JsonProperty("status") String status,
        @JsonProperty("timeInForce") String timeInForce,
        @JsonProperty("type") String type,
        @JsonProperty("side") String side,
        @JsonProperty("stopPrice") String stopPrice,
        @JsonProperty("icebergQty") String icebergQty,
        @JsonProperty("time") long time,
        @JsonProperty("updateTime") long updateTime,
        @JsonProperty("isWorking") boolean isWorking,
        @JsonProperty("workingTime") long workingTime,
        @JsonProperty("origQuoteOrderQty") String origQuoteOrderQty,
        @JsonProperty("selfTradePreventionMode") String selfTradePreventionMode
) {}
