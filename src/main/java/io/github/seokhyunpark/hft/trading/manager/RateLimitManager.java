package io.github.seokhyunpark.hft.trading.manager;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RateLimitManager {
    private static final int MIN_COUNT = 0;
    private static final int MAKER_FILL_DECREMENT = 5;

    private static final int LIMIT = 100;
    private static final int SAFETY_MARGIN = 5;
    private static final int RESET_WINDOW_MS = 10000;

    private final AtomicInteger orderCount = new AtomicInteger(MIN_COUNT);
    private final AtomicLong currentWindowId = new AtomicLong(System.currentTimeMillis() / RESET_WINDOW_MS);

    public void onOrderPlaced() {
        int count = orderCount.incrementAndGet();
        log.debug("[RATE-LIMIT-NEW] {}/{}", count, LIMIT);
    }

    public void onOrderFilled() {
        int count = orderCount.updateAndGet(current -> Math.max(MIN_COUNT, current - MAKER_FILL_DECREMENT));
        log.debug("[RATE-LIMIT-FILLED] {}/{}", count, LIMIT);
    }

    public void syncOrderCount(int count) {
        orderCount.set(count);
        log.debug("[RATE-LIMIT-SERVER] {}/{}", count, LIMIT);
    }

    public boolean hasRateLimitCapacity() {
        refreshWindow();
        return orderCount.get() < (LIMIT - SAFETY_MARGIN);
    }

    private void refreshWindow() {
        long newWindowId = System.currentTimeMillis() / RESET_WINDOW_MS;
        long lastWindowId = currentWindowId.get();

        if (newWindowId > lastWindowId) {
            if (currentWindowId.compareAndSet(lastWindowId, newWindowId)) {
                orderCount.set(0);
                log.debug("[RATE-LIMIT-WINDOW-REFRESH]");
            }
        }
    }
}
