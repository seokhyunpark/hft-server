package io.github.seokhyunpark.hft.trading.manager;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class QuoteAssetManager {
    private final AtomicReference<BigDecimal> quoteBalance = new AtomicReference<>(BigDecimal.ZERO);

    public void addQuoteBalance(BigDecimal delta) {
        if (delta != null) {
            BigDecimal balance = quoteBalance.updateAndGet(current -> current.add(delta));
            log.debug("[QUOTE-SERVER-ADD] {}", balance.toPlainString());
        }
    }

    public void syncQuoteBalance(BigDecimal amount) {
        if (amount != null) {
            quoteBalance.set(amount);
            log.debug("[QUOTE-SERVER-SYNC] {}", amount.toPlainString());
        }
    }

    public void deductQuoteBalance(BigDecimal amount) {
        if (amount != null) {
            BigDecimal balance = quoteBalance.updateAndGet(current -> current.subtract(amount));
            log.debug("[QUOTE-LOCAL-DEDUCT] {}", balance.toPlainString());
        }
    }

    public boolean hasQuoteBalanceFor(BigDecimal amount) {
        return quoteBalance.get().compareTo(amount) >= 0;
    }
}
