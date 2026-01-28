package io.github.seokhyunpark.hft.exchange.dto.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderUpdate(
        // https://developers.binance.com/docs/binance-spot-api-docs/user-data-stream#order-update
        @JsonProperty("e") String eventType,  // "executionReport"
        @JsonProperty("E") long eventTime,
        @JsonProperty("s") String symbol,
        @JsonProperty("c") String clientOrderId,
        @JsonProperty("S") String side,
        @JsonProperty("o") String orderType,
        @JsonProperty("f") String timeInForce,
        @JsonProperty("q") String orderQty,
        @JsonProperty("p") String orderPrice,
        @JsonProperty("P") String stopPrice,
        @JsonProperty("F") String icebergQty,
        @JsonProperty("g") long orderListId,
        @JsonProperty("C") String originalClientOrderId,
        @JsonProperty("x") String currentExecutionType,
        @JsonProperty("X") String currentOrderStatus,
        @JsonProperty("r") String orderRejectReason,
        @JsonProperty("i") long orderId,
        @JsonProperty("l") String lastExecutedQty,
        @JsonProperty("z") String cumulativeFilledQty,
        @JsonProperty("L") String lastExecutedPrice,
        @JsonProperty("n") String commissionAmount,
        @JsonProperty("N") String commissionAsset,
        @JsonProperty("T") long transactionTime,
        @JsonProperty("t") long tradeId,
        @JsonProperty("v") long preventedMatchId,
        @JsonProperty("I") long executionId,
        @JsonProperty("w") boolean isTheOrderOnTheBook,
        @JsonProperty("m") boolean isThisTradeTheMakerSide,
        @JsonProperty("M") boolean ignore,
        @JsonProperty("O") long orderCreationTime,
        @JsonProperty("Z") String cumulativeQuoteAssetTransactedQty,
        @JsonProperty("Y") String lastQuoteAssetTransactedQty,
        @JsonProperty("Q") String quoteOrderQty,
        @JsonProperty("W") long workingTime,
        @JsonProperty("V") String selfTradePreventionMode
) {
}
