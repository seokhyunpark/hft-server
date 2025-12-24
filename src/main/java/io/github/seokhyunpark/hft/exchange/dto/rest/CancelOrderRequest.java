package io.github.seokhyunpark.hft.exchange.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class CancelOrderRequest {
    // https://developers.binance.com/docs/binance-spot-api-docs/rest-api/trading-endpoints#cancel-order-trade

    @JsonProperty("symbol")
    String symbol;

    @JsonProperty("orderId")
    long orderId;

    @JsonProperty("origClientOrderId")
    String origClientOrderId;

    @JsonProperty("newClientOrderId")
    String newClientOrderId;

    @JsonProperty("cancelRestrictions")
    String cancelRestrictions;

    @JsonProperty("recvWindow")
    String recvWindow;

    @JsonProperty("timestamp")
    long timestamp;
}
