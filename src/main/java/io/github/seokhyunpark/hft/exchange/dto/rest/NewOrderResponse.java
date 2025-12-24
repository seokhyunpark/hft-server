package io.github.seokhyunpark.hft.exchange.dto.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NewOrderResponse(
        // https://developers.binance.com/docs/binance-spot-api-docs/rest-api/trading-endpoints#new-order-trade
        @JsonProperty("symbol") String symbol,
        @JsonProperty("orderId") long orderId,
        @JsonProperty("orderListId") long orderListId,
        @JsonProperty("clientOrderId") String clientOrderId,
        @JsonProperty("transactTime") long transactTime
) {}
