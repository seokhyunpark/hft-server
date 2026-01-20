package io.github.seokhyunpark.hft.trading.init;

import java.math.BigDecimal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.seokhyunpark.hft.exchange.client.BinanceClient;
import io.github.seokhyunpark.hft.exchange.dto.rest.GetAccountResponse.Balance;
import io.github.seokhyunpark.hft.trading.config.TradingProperties;
import io.github.seokhyunpark.hft.trading.manager.QuoteAssetManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoteAssetManagerInitializer {
    private final BinanceClient binanceClient;
    private final QuoteAssetManager quoteAssetManager;
    private final TradingProperties tradingProperties;
    private final ConfigurableApplicationContext context;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        Balance quoteBalance = binanceClient.getBalance(tradingProperties.quoteAsset());
        if (quoteBalance == null || quoteBalance.free() == null) {
            int exitCode = SpringApplication.exit(context, () -> 1);
            System.exit(exitCode);

            log.error("[INIT-QUOTE-ASSET-FAIL] AssetManager 초기화 실패");
            return;
        }

        BigDecimal balance = new BigDecimal(quoteBalance.free());
        quoteAssetManager.syncQuoteBalance(balance);
        log.debug("[INIT-QUOTE-ASSET-SUCCESS] AssetManager 초기화 성공");
    }
}
