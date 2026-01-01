package io.github.seokhyunpark.hft.exchange.dto.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BalanceUpdate(
        // https://developers.binance.com/docs/binance-spot-api-docs/user-data-stream#balance-update
        @JsonProperty("e") String eventType,  // "balanceUpdate"
        @JsonProperty("E") long eventTime,
        @JsonProperty("a") String asset,
        @JsonProperty("d") String balanceDelta,
        @JsonProperty("T") long clearTime
) {}
