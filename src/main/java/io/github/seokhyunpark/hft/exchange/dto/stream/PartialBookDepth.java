package io.github.seokhyunpark.hft.exchange.dto.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PartialBookDepth(
        // https://developers.binance.com/docs/binance-spot-api-docs/web-socket-streams#partial-book-depth-streams
        @JsonProperty("lastUpdateId") long lastUpdateId,
        @JsonProperty("bids") List<List<String>> bids,
        @JsonProperty("asks") List<List<String>> asks
) {}
