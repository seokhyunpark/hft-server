package io.github.seokhyunpark.hft.exchange.dto.stream;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class BalanceUpdateTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("BalanceUpdate JSON이 DTO로 정상 파싱되어야 한다.")
    void parsingSuccess() throws Exception {
        String json = """
                {
                  "subscriptionId": 0,
                  "event": {
                    "e": "balanceUpdate",
                    "E": 1573200697110,
                    "a": "BTC",
                    "d": "100.00000000",
                    "T": 1573200697068
                  }
                }
                """;

        JsonNode payload = mapper.readTree(json).get("event");
        BalanceUpdate update = mapper.treeToValue(payload, BalanceUpdate.class);

        assertThat(update.eventType()).isEqualTo("balanceUpdate");
        assertThat(update.eventTime()).isEqualTo(1573200697110L);
        assertThat(update.asset()).isEqualTo("BTC");
        assertThat(update.balanceDelta()).isEqualTo("100.00000000");
        assertThat(update.clearTime()).isEqualTo(1573200697068L);
    }
}
