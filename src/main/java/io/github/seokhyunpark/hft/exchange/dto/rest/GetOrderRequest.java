package io.github.seokhyunpark.hft.exchange.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class GetOrderRequest {
    // https://developers.binance.com/docs/binance-spot-api-docs/rest-api/account-endpoints#query-order-user_data

    @JsonProperty("symbol")
    String symbol;

    @JsonProperty("orderId")
    long orderId;

    @JsonProperty("origClientOrderId")
    String origClientOrderId;

    @JsonProperty("recvWindow")
    String recvWindow;

    @JsonProperty("timestamp")
    long timestamp;
}
