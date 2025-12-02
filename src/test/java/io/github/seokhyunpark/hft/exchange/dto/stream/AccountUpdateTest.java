package io.github.seokhyunpark.hft.exchange.dto.stream;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AccountUpdateTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("AccountUpdate JSON이 DTO로 정상 파싱되어야 한다.")
    void parsingSuccess() throws Exception {
        String json = """
                {
                  "subscriptionId": 0,
                  "event": {
                    "e": "outboundAccountPosition",
                    "E": 1564034571105,
                    "u": 1564034571073,
                    "B":
                    [
                      {
                        "a": "ETH",
                        "f": "10000.000000",
                        "l": "0.000000"
                      }
                    ]
                  }
                }
                """;

        JsonNode payload = mapper.readTree(json).get("event");
        AccountUpdate update = mapper.treeToValue(payload, AccountUpdate.class);

        assertThat(update.eventType()).isEqualTo("outboundAccountPosition");
        assertThat(update.eventTime()).isEqualTo(1564034571105L);
        assertThat(update.lastUpdateTime()).isEqualTo(1564034571073L);

        assertThat(update.balances()).hasSize(1);
        assertThat(update.balances().getFirst().asset()).isEqualTo("ETH");
        assertThat(update.balances().getFirst().free()).isEqualTo("10000.000000");
        assertThat(update.balances().getFirst().locked()).isEqualTo("0.000000");
    }
}
