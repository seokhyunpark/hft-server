package io.github.seokhyunpark.hft.trading.manager;

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
public class PositionManager {
    private final TradingProperties properties;

    private final AtomicReference<PositionInfo> position = new AtomicReference<>(new PositionInfo());

    public void addPosition(BigDecimal qty, BigDecimal usdValue) {
        BigDecimal cleanQty = properties.scaleQty(qty);
        PositionInfo added = position.updateAndGet(cur -> cur.add(cleanQty, usdValue));
        log.debug("[POSITION] 증가: {}", added);
    }

    public PositionInfo pullPosition() {
        PositionInfo pulled = position.getAndSet(new PositionInfo());
        log.debug("[POSITION] 추출 및 초기화: {}", pulled);
        return pulled;
    }

    public void restorePosition(PositionInfo info) {
        PositionInfo restored = position.updateAndGet(cur -> cur.add(info.totalQty(), info.totalUsdValue()));
        log.debug("[POSITION] 복구: {}", restored);
    }

    public boolean isSellable() {
        return position.get().totalUsdValue().compareTo(properties.minOrderSize()) >= 0;
    }
}
