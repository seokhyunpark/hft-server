package io.github.seokhyunpark.hft.exchange.dto.stream;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class OrderUpdateTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("OrderUpdate JSON이 DTO로 정상 파싱되어야 한다.")
    void parsingSuccess() throws Exception {
        String json = """
                {
                  "subscriptionId": 0,
                  "event": {
                    "e": "executionReport",
                    "E": 1499405658658,
                    "s": "ETHBTC",
                    "c": "mUvoqJxFIILMdfAW5iGSOW",
                    "S": "BUY",
                    "o": "LIMIT",
                    "f": "GTC",
                    "q": "1.00000000",
                    "p": "0.10264410",
                    "P": "0.00000000",
                    "F": "0.00000000",
                    "g": -1,
                    "C": "",
                    "x": "NEW",
                    "X": "NEW",
                    "r": "NONE",
                    "i": 4293153,
                    "l": "0.00000000",
                    "z": "0.00000000",
                    "L": "0.00000000",
                    "n": "0",
                    "N": null,
                    "T": 1499405658657,
                    "t": -1,
                    "v": 3,
                    "I": 8641984,
                    "w": true,
                    "m": false,
                    "M": false,
                    "O": 1499405658657,
                    "Z": "0.00000000",
                    "Y": "0.00000000",
                    "Q": "0.00000000",
                    "W": 1499405658657,
                    "V": "NONE"
                  }
                }
                """;

        JsonNode payload = mapper.readTree(json).get("event");
        OrderUpdate update = mapper.treeToValue(payload, OrderUpdate.class);

        assertThat(update.eventType()).isEqualTo("executionReport");
        assertThat(update.eventTime()).isEqualTo(1499405658658L);
        assertThat(update.symbol()).isEqualTo("ETHBTC");
        assertThat(update.clientOrderId()).isEqualTo("mUvoqJxFIILMdfAW5iGSOW");
        assertThat(update.side()).isEqualTo("BUY");
        assertThat(update.orderType()).isEqualTo("LIMIT");
        assertThat(update.timeInForce()).isEqualTo("GTC");
        assertThat(update.orderQty()).isEqualTo("1.00000000");
        assertThat(update.orderPrice()).isEqualTo("0.10264410");
        assertThat(update.stopPrice()).isEqualTo("0.00000000");
        assertThat(update.icebergQty()).isEqualTo("0.00000000");
        assertThat(update.orderListId()).isEqualTo(-1L);
        assertThat(update.originalClientOrderId()).isEqualTo("");
        assertThat(update.currentExecutionType()).isEqualTo("NEW");
        assertThat(update.currentOrderStatus()).isEqualTo("NEW");
        assertThat(update.orderRejectReason()).isEqualTo("NONE");
        assertThat(update.orderId()).isEqualTo(4293153L);
        assertThat(update.lastExecutedQty()).isEqualTo("0.00000000");
        assertThat(update.cumulativeFilledQty()).isEqualTo("0.00000000");
        assertThat(update.lastExecutedPrice()).isEqualTo("0.00000000");
        assertThat(update.commissionAmount()).isEqualTo("0");
        assertThat(update.commissionAsset()).isEqualTo(null);
        assertThat(update.transactionTime()).isEqualTo(1499405658657L);
        assertThat(update.tradeId()).isEqualTo(-1L);
        assertThat(update.preventedMatchId()).isEqualTo(3L);
        assertThat(update.executionId()).isEqualTo(8641984L);
        assertThat(update.isTheOrderOnTheBook()).isEqualTo(true);
        assertThat(update.isThisTradeTheMakerSide()).isEqualTo(false);
        assertThat(update.ignore()).isEqualTo(false);
        assertThat(update.orderCreationTime()).isEqualTo(1499405658657L);
        assertThat(update.cumulativeQuoteAssetTransactedQty()).isEqualTo("0.00000000");
        assertThat(update.lastQuoteAssetTransactedQty()).isEqualTo("0.00000000");
        assertThat(update.quoteOrderQty()).isEqualTo("0.00000000");
        assertThat(update.workingTime()).isEqualTo(1499405658657L);
        assertThat(update.selfTradePreventionMode()).isEqualTo("NONE");
    }
}
