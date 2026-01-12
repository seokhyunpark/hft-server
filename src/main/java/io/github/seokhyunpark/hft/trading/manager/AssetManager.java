package io.github.seokhyunpark.hft.trading.manager;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AssetManager {
    private final AtomicReference<BigDecimal> quoteBalance = new AtomicReference<>(BigDecimal.ZERO);

    public BigDecimal getQuoteBalance() {
        return quoteBalance.get();
    }

    public boolean hasQuoteBalanceFor(BigDecimal amount) {
        return quoteBalance.get().compareTo(amount) >= 0;
    }

    public void syncQuoteBalance(BigDecimal amount) {
        if (amount == null) {
            return;
        }
        quoteBalance.set(amount);
        log.debug("[USD-SERVER] 동기화 완료 | 현재 잔고: {}", amount.toPlainString());
    }

    public void deductQuoteBalance(BigDecimal amount) {
        if (amount == null) {
            return;
        }
        BigDecimal balance = quoteBalance.updateAndGet(current -> current.subtract(amount));
        log.debug("[USD-LOCAL] 주문 비용 선차감 (-{}) | 현재 잔고: {}", amount.toPlainString(), balance.toPlainString());
    }
}
