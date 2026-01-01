package io.github.seokhyunpark.hft.exchange.dto.stream;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PartialBookDepthTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("PartialBookDepth 문자열이 DTO로 정상 파싱되어야 한다.")
    void parsingSuccess() throws Exception {
        String data = """
                {
                  "lastUpdateId": 37530297893,
                  "bids": [
                    [
                      "90994.23000000",
                      "0.05930000"
                    ],
                    [
                      "90994.09000000",
                      "0.05930000"
                    ],
                    [
                      "90993.97000000",
                      "0.00027000"
                    ],
                    [
                      "90993.16000000",
                      "0.00007000"
                    ],
                    [
                      "90993.14000000",
                      "0.00006000"
                    ],
                    [
                      "90992.99000000",
                      "0.00014000"
                    ],
                    [
                      "90992.49000000",
                      "0.00021000"
                    ],
                    [
                      "90991.85000000",
                      "0.05930000"
                    ],
                    [
                      "90991.72000000",
                      "0.05930000"
                    ],
                    [
                      "90991.59000000",
                      "0.05930000"
                    ],
                    [
                      "90991.49000000",
                      "0.00021000"
                    ],
                    [
                      "90991.46000000",
                      "0.05930000"
                    ],
                    [
                      "90991.33000000",
                      "0.05930000"
                    ],
                    [
                      "90991.28000000",
                      "0.05930000"
                    ],
                    [
                      "90991.24000000",
                      "0.00027000"
                    ],
                    [
                      "90989.91000000",
                      "0.00824000"
                    ],
                    [
                      "90989.86000000",
                      "0.05930000"
                    ],
                    [
                      "90989.58000000",
                      "0.00006000"
                    ],
                    [
                      "90988.53000000",
                      "0.00020000"
                    ],
                    [
                      "90988.51000000",
                      "0.00027000"
                    ]
                  ],
                  "asks": [
                    [
                      "90997.70000000",
                      "0.00642000"
                    ],
                    [
                      "90997.71000000",
                      "0.05930000"
                    ],
                    [
                      "90998.30000000",
                      "0.04089000"
                    ],
                    [
                      "90999.43000000",
                      "0.00027000"
                    ],
                    [
                      "90999.70000000",
                      "0.01567000"
                    ],
                    [
                      "90999.94000000",
                      "0.05257000"
                    ],
                    [
                      "91000.00000000",
                      "0.05934000"
                    ],
                    [
                      "91000.08000000",
                      "0.11024000"
                    ],
                    [
                      "91000.26000000",
                      "0.00329000"
                    ],
                    [
                      "91000.32000000",
                      "0.00019000"
                    ],
                    [
                      "91000.90000000",
                      "0.00883000"
                    ],
                    [
                      "91002.02000000",
                      "0.00009000"
                    ],
                    [
                      "91002.17000000",
                      "0.00027000"
                    ],
                    [
                      "91002.26000000",
                      "0.04073000"
                    ],
                    [
                      "91002.37000000",
                      "0.01607000"
                    ],
                    [
                      "91002.86000000",
                      "0.00008000"
                    ],
                    [
                      "91003.74000000",
                      "0.00006000"
                    ],
                    [
                      "91004.17000000",
                      "0.11023000"
                    ],
                    [
                      "91004.41000000",
                      "0.00050000"
                    ],
                    [
                      "91004.78000000",
                      "0.00819000"
                    ]
                  ]
                }
                """;

        PartialBookDepth depth = mapper.readValue(data, PartialBookDepth.class);

        assertThat(depth.lastUpdateId()).isEqualTo(37530297893L);
        assertThat(depth.bids()).hasSize(20);
        assertThat(depth.asks()).hasSize(20);
        assertThat(depth.bids().getFirst().getFirst()).isEqualTo("90994.23000000");
        assertThat(depth.bids().getFirst().getLast()).isEqualTo("0.05930000");
    }

    @Test
    @DisplayName("PartialBookDepth 문자열에 모르는 필드가 있어도 에러 없이 파싱되어야 한다.")
    void ignoreUnknown() throws Exception {
        String data = """
                {
                  "lastUpdateId": 37530297893,
                  "bids": [
                    [
                      "90994.23000000",
                      "0.05930000"
                    ]
                  ],
                  "asks": [
                    [
                      "90997.70000000",
                      "0.00642000"
                    ]
                  ],
                  "unknown": "unknownField"
                }
                """;
        PartialBookDepth depth = mapper.readValue(data, PartialBookDepth.class);

        assertThat(depth.lastUpdateId()).isEqualTo(37530297893L);
        assertThat(depth.bids().getFirst().getFirst()).isEqualTo("90994.23000000");
        assertThat(depth.bids().getFirst().getLast()).isEqualTo("0.05930000");
    }
}
