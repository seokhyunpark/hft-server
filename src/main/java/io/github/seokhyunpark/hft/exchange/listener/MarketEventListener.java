package io.github.seokhyunpark.hft.exchange.listener;

import io.github.seokhyunpark.hft.exchange.dto.stream.PartialBookDepth;

public interface MarketEventListener {
    void onPartialBookDepthReceived(PartialBookDepth depth);
}
