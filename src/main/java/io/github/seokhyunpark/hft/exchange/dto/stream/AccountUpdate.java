package io.github.seokhyunpark.hft.exchange.dto.stream;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountUpdate(
        // https://developers.binance.com/docs/binance-spot-api-docs/user-data-stream#account-update
        @JsonProperty("e") String eventType,  // "outboundAccountPosition"
        @JsonProperty("E") long eventTime,
        @JsonProperty("u") long lastUpdateTime,
        @JsonProperty("B") List<Balance> balances
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Balance(
            @JsonProperty("a") String asset,
            @JsonProperty("f") String free,
            @JsonProperty("l") String locked
    ) {}
}
