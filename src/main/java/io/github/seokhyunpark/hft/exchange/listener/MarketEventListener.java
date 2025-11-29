package io.github.seokhyunpark.hft.exchange.listener;

import io.github.seokhyunpark.hft.exchange.dto.PartialBookDepth;

public interface MarketEventListener {
    void onPartialBookDepthReceived(PartialBookDepth partialBookDepth);
}
