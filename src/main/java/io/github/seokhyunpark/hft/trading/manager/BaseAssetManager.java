package io.github.seokhyunpark.hft.trading.manager;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import io.github.seokhyunpark.hft.trading.config.TradingProperties;
import io.github.seokhyunpark.hft.trading.dto.PositionSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaseAssetManager {
    private final TradingProperties properties;

    private final AtomicReference<PositionSnapshot> state = new AtomicReference<>(new PositionSnapshot());

    public void addFree(BigDecimal qty, BigDecimal usdValue) {
        BigDecimal cleanQty = properties.scaleQty(qty);
        state.updateAndGet(cur -> cur.withFree(cur.free().plus(cleanQty, usdValue)));
        log.info("[BASE-ASSET] FREE 증가 (QTY: {} | USD: {})", qty, usdValue);
    }

    public void transferFreeToLocked(BigDecimal qty, BigDecimal usdValue) {
        state.updateAndGet(cur -> {
            BigDecimal cleanQty = properties.scaleQty(qty);
            return new PositionSnapshot(
                    cur.free().minus(cleanQty, usdValue),
                    cur.locked().plus(cleanQty, usdValue)
            );
        });
        log.info("[BASE-ASSET] FREE 감소, LOCKED 증가 (QTY: {} | USD: {})", qty, usdValue);
    }

    public void deductLocked(BigDecimal qty, BigDecimal usdValue) {
        BigDecimal cleanQty = properties.scaleQty(qty);
        state.updateAndGet(cur -> cur.withLocked(cur.locked().minus(cleanQty, usdValue)));
        log.info("[BASE-ASSET] LOCKED 감소 (QTY: {} | USD: {})", qty, usdValue);
    }

    public void transferLockedToFree(BigDecimal qty, BigDecimal usdValue) {
        state.updateAndGet(cur -> {
            BigDecimal cleanQty = properties.scaleQty(qty);
            return new PositionSnapshot(
                    cur.free().plus(cleanQty, usdValue),
                    cur.locked().minus(cleanQty, usdValue)
            );
        });
        log.info("[BASE-ASSET] FREE 증가, LOCKED 감소 (QTY: {} | USD: {})", qty, usdValue);
    }

    public PositionSnapshot getSnapshot() {
        return state.get();
    }
}
