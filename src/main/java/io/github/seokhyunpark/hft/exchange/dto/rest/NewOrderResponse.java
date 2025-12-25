package io.github.seokhyunpark.hft.exchange.dto.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NewOrderResponse(
        // https://developers.binance.com/docs/binance-spot-api-docs/rest-api/trading-endpoints#new-order-trade
        @JsonProperty("symbol") String symbol,
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("orderListId") Long orderListId,
        @JsonProperty("clientOrderId") String clientOrderId,
        @JsonProperty("transactTime") Long transactTime
) {}
