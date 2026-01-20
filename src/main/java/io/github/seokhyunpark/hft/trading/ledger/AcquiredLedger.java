package io.github.seokhyunpark.hft.trading.ledger;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.seokhyunpark.hft.trading.config.TradingProperties;
import io.github.seokhyunpark.hft.trading.dto.PositionInfo;

@Slf4j
@Component
@RequiredArgsConstructor
public class AcquiredLedger {
    private final TradingProperties properties;

    private final AtomicReference<PositionInfo> acquired = new AtomicReference<>(new PositionInfo());

    public void addAcquired(BigDecimal qty, BigDecimal usdValue) {
        BigDecimal cleanQty = properties.scaleQty(qty);
        PositionInfo added = acquired.updateAndGet(cur -> cur.add(cleanQty, usdValue));
        log.info("[BASE-ASSET] 증가: {}", added);
    }

    public PositionInfo pullAcquired() {
        PositionInfo pulled = acquired.getAndSet(new PositionInfo());
        log.info("[BASE-ASSET] 추출 및 초기화: {}", pulled);
        return pulled;
    }

    public void restoreAcquired(PositionInfo info) {
        PositionInfo restored = acquired.updateAndGet(cur -> cur.add(info.totalQty(), info.totalUsdValue()));
        log.info("[BASE-ASSET] 복구: {}", restored);
    }

    public PositionInfo getAcquired() {
        return acquired.get();
    }

    public boolean isSellable() {
        return acquired.get().totalUsdValue().compareTo(properties.minOrderSize()) >= 0;
    }
}
