package io.github.seokhyunpark.hft.trading.dto;

public record PositionSnapshot(
        PositionInfo free,
        PositionInfo locked
) {
    public PositionSnapshot() {
        this(new PositionInfo(), new PositionInfo());
    }

    public PositionSnapshot withFree(PositionInfo newFree) {
        return new PositionSnapshot(newFree, locked);
    }

    public PositionSnapshot withLocked(PositionInfo newLocked) {
        return new PositionSnapshot(free, newLocked);
    }
}
