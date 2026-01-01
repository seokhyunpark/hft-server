package io.github.seokhyunpark.hft.exchange.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class NewOrderRequest {
    // https://developers.binance.com/docs/binance-spot-api-docs/rest-api/trading-endpoints#new-order-trade

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("side")
    private String side;

    @JsonProperty("type")
    private String type;

    @JsonProperty("timeInForce")
    private String timeInForce;

    @JsonProperty("quantity")
    private String qty;

    @JsonProperty("quoteOrderQty")
    private String quoteOrderQty;

    @JsonProperty("price")
    private String price;

    @JsonProperty("newClientOrderId")
    private String newClientOrderId;

    @JsonProperty("strategyId")
    private Long strategyId;

    @JsonProperty("strategyType")
    private Integer strategyType;

    @JsonProperty("stopPrice")
    private String stopPrice;

    @JsonProperty("trailingDelta")
    private Long trailingDelta;

    @JsonProperty("icebergQty")
    private String icebergQty;

    @JsonProperty("newOrderRespType")
    private String newOrderRespType;

    @JsonProperty("selfTradePreventionMode")
    private String selfTradePreventionMode;

    @JsonProperty("pegPriceType")
    private String pegPriceType;

    @JsonProperty("pegOffsetValue")
    private Integer pegOffsetValue;

    @JsonProperty("pegOffsetType")
    private String pegOffsetType;

    @JsonProperty("recvWindow")
    private String recvWindow;

    @JsonProperty("timestamp")
    private Long timestamp;
}
