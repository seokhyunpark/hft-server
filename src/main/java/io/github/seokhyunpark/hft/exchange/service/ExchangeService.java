package io.github.seokhyunpark.hft.exchange.service;

import io.github.seokhyunpark.hft.exchange.stream.MarketDataStream;
import io.github.seokhyunpark.hft.exchange.dto.PartialBookDepth;
import io.github.seokhyunpark.hft.exchange.listener.MarketEventListener;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
public class ExchangeService {
    private MarketDataStream marketDataStream;

    @Value("${hft.stream.market-uri}")
    private String marketUri;

    @PostConstruct
    public void connect() {
        connectMarketStream();
    }

    private void connectMarketStream() {
        try {
            URI uri = new URI(marketUri);

            marketDataStream = new MarketDataStream(uri, new MarketEventListener() {
                @Override
                public void onPartialBookDepthReceived(PartialBookDepth depth) {
                    if (depth == null) {
                        return;
                    }

                    List<List<String>> bids = depth.bids();
                    List<List<String>> asks = depth.asks();
                    if (bids == null || bids.isEmpty() || asks == null || asks.isEmpty()) {
                        return;
                    }

                    String price = bids.getFirst().getFirst();
                    String qty = bids.getFirst().getLast();

                    log.debug("[Market] 수신 ID: {} | 가격: {}, 수량: {}", depth.lastUpdateId(), price, qty);
                }
            });
            marketDataStream.connect();

        } catch (Exception e) {
            log.error("[Market] WebSocket 초기화 실패: {}", e.getMessage());
        }
    }
}
