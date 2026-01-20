package io.github.seokhyunpark.hft.trading.init;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.github.seokhyunpark.hft.exchange.client.BinanceClient;
import io.github.seokhyunpark.hft.exchange.dto.rest.GetAccountResponse.Balance;
import io.github.seokhyunpark.hft.trading.manager.QuoteAssetManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetManagerInitializer {
    private final BinanceClient binanceClient;
    private final QuoteAssetManager quoteAssetManager;

    @Value("${hft.trading.quote-asset}")
    private String quoteAsset;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        Balance quoteBalance = binanceClient.getBalance(quoteAsset);
        if (quoteBalance == null || quoteBalance.free() == null) {
            log.debug("[INIT-ASSET-FAIL] AssetManager 초기화 실패");
            return;
        }

        BigDecimal balance = new BigDecimal(quoteBalance.free());
        quoteAssetManager.syncQuoteBalance(balance);
        log.debug("[INIT-ASSET-SUCCESS] AssetManager 초기화 성공");
    }
}
