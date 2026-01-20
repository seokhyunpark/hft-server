package io.github.seokhyunpark.hft.exchange.dto.stream;

import java.util.UUID;

public record LogonRequest(
        String id,
        String method,
        LogonParams params
) {
    public static LogonRequest of(String apiKey, String signature, long timestamp) {
        return new LogonRequest(
                UUID.randomUUID().toString(),
                "session.logon",
                new LogonParams(apiKey, signature, timestamp)
        );
    }

    public record LogonParams(
            String apiKey,
            String signature,
            long timestamp
    ) {
    }
}
