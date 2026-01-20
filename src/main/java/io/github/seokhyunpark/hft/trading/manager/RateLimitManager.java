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

    public int getOrderCount() {
        return orderCount.get();
    }

    public boolean hasRateLimitCapacity() {
        refreshWindow();
        int currentCount = orderCount.get();
        return currentCount < (LIMIT - SAFETY_MARGIN);
    }

    public void onOrderPlaced() {
        int count = orderCount.incrementAndGet();
        log.debug("[LIMIT-LOCAL] 신규 주문 (+1) | 현재 상태: {}/{}", count, LIMIT);
    }

    public void onOrderFilled() {
        int count = orderCount.updateAndGet(current -> Math.max(MIN_COUNT, current - MAKER_FILL_DECREMENT));
        log.debug("[LIMIT-LOCAL] 주문 체결 (-{}) | 현재 상태: {}/{}", MAKER_FILL_DECREMENT, count, LIMIT);
    }

    public void updateOrderCount(String rawCount) {
        if (rawCount == null || rawCount.isBlank()) {
            return;
        }
        try {
            int count = Integer.parseInt(rawCount);
            orderCount.set(count);
            log.debug("[LIMIT-SERVER] 동기화 완료 | 현재 상태: {}/{}", count, LIMIT);
        } catch (Exception e) {
            log.error("[LIMIT-SERVER] 동기화 실패: {}", e.getMessage());
        }
    }

    private void refreshWindow() {
        long newWindowId = System.currentTimeMillis() / RESET_WINDOW_MS;
        long lastWindowId = currentWindowId.get();

        if (newWindowId > lastWindowId) {
            if (currentWindowId.compareAndSet(lastWindowId, newWindowId)) {
                orderCount.set(0);
                log.debug("[LIMIT-WINDOW] 상태 초기화 (Window ID: {})", newWindowId);
            }
        }
    }
}
