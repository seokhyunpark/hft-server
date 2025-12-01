package io.github.seokhyunpark.hft.exchange.stream;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.security.PrivateKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.seokhyunpark.hft.exchange.dto.AccountUpdate;
import io.github.seokhyunpark.hft.exchange.dto.BalanceUpdate;
import io.github.seokhyunpark.hft.exchange.dto.OrderUpdate;
import io.github.seokhyunpark.hft.exchange.listener.UserEventListener;
import io.github.seokhyunpark.hft.exchange.util.SignatureUtil;

public class UserDataStreamTest {
    static class TestUserListener implements UserEventListener {
        AccountUpdate receivedAccount;
        BalanceUpdate receivedBalance;
        OrderUpdate receivedOrder;

        @Override
        public void onAccountUpdateReceived(AccountUpdate accountUpdate) {
            this.receivedAccount = accountUpdate;
        }

        @Override
        public void onBalanceUpdateReceived(BalanceUpdate balanceUpdate) {
            this.receivedBalance = balanceUpdate;
        }

        @Override
        public void onOrderUpdateReceived(OrderUpdate orderUpdate) {
            this.receivedOrder = orderUpdate;
        }
    }

    static class StubSignatureUtil extends SignatureUtil {
        @Override
        public PrivateKey loadPrivateKey(String path) {
            return null;
        }
    }

    private final TestUserListener listener = new TestUserListener();
    private final StubSignatureUtil stubUtil = new StubSignatureUtil();
    private final UserDataStream stream;

    public UserDataStreamTest() throws Exception {
        this.stream = new UserDataStream(URI.create("wss://test.com"), listener, "dummyKey", "dummyPath", stubUtil);
    }

    @Test
    @DisplayName("outboundAccountPosition 이벤트가 오면 onAccountUpdateReceived가 호출되어야 한다.")
    void shouldCallAccountListener() {
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

        stream.onMessage(json);

        assertThat(listener.receivedAccount).isNotNull();
        assertThat(listener.receivedBalance).isNull();
        assertThat(listener.receivedOrder).isNull();

        assertThat(listener.receivedAccount.eventType()).isEqualTo("outboundAccountPosition");
        assertThat(listener.receivedAccount.eventTime()).isEqualTo(1564034571105L);
        assertThat(listener.receivedAccount.lastUpdateTime()).isEqualTo(1564034571073L);

        assertThat(listener.receivedAccount.balances()).hasSize(1);
        assertThat(listener.receivedAccount.balances().getFirst().asset()).isEqualTo("ETH");
        assertThat(listener.receivedAccount.balances().getFirst().free()).isEqualTo("10000.000000");
        assertThat(listener.receivedAccount.balances().getFirst().locked()).isEqualTo("0.000000");
    }

    @Test
    @DisplayName("balanceUpdate 이벤트가 오면 onBalanceUpdateReceived가 호출되어야 한다.")
    void shouldCallBalanceListener() {
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

        stream.onMessage(json);

        assertThat(listener.receivedAccount).isNull();
        assertThat(listener.receivedBalance).isNotNull();
        assertThat(listener.receivedOrder).isNull();

        assertThat(listener.receivedBalance.eventType()).isEqualTo("balanceUpdate");
        assertThat(listener.receivedBalance.eventTime()).isEqualTo(1573200697110L);
        assertThat(listener.receivedBalance.asset()).isEqualTo("BTC");
        assertThat(listener.receivedBalance.balanceDelta()).isEqualTo("100.00000000");
        assertThat(listener.receivedBalance.clearTime()).isEqualTo(1573200697068L);
    }

    @Test
    @DisplayName("executionReport 이벤트가 오면 onOrderUpdateReceived가 호출되어야 한다.")
    void shouldCallOrderListener() {
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

        stream.onMessage(json);

        assertThat(listener.receivedAccount).isNull();
        assertThat(listener.receivedBalance).isNull();
        assertThat(listener.receivedOrder).isNotNull();

        assertThat(listener.receivedOrder.eventType()).isEqualTo("executionReport");
        assertThat(listener.receivedOrder.eventTime()).isEqualTo(1499405658658L);
        assertThat(listener.receivedOrder.symbol()).isEqualTo("ETHBTC");
        assertThat(listener.receivedOrder.clientOrderId()).isEqualTo("mUvoqJxFIILMdfAW5iGSOW");
        assertThat(listener.receivedOrder.side()).isEqualTo("BUY");
        assertThat(listener.receivedOrder.orderType()).isEqualTo("LIMIT");
        assertThat(listener.receivedOrder.timeInForce()).isEqualTo("GTC");
        assertThat(listener.receivedOrder.orderQty()).isEqualTo("1.00000000");
        assertThat(listener.receivedOrder.orderPrice()).isEqualTo("0.10264410");
        assertThat(listener.receivedOrder.stopPrice()).isEqualTo("0.00000000");
        assertThat(listener.receivedOrder.icebergQty()).isEqualTo("0.00000000");
        assertThat(listener.receivedOrder.orderListId()).isEqualTo(-1L);
        assertThat(listener.receivedOrder.originalClientOrderId()).isEqualTo("");
        assertThat(listener.receivedOrder.currentExecutionType()).isEqualTo("NEW");
        assertThat(listener.receivedOrder.currentOrderStatus()).isEqualTo("NEW");
        assertThat(listener.receivedOrder.orderRejectReason()).isEqualTo("NONE");
        assertThat(listener.receivedOrder.orderId()).isEqualTo(4293153L);
        assertThat(listener.receivedOrder.lastExecutedQty()).isEqualTo("0.00000000");
        assertThat(listener.receivedOrder.cumulativeFilledQty()).isEqualTo("0.00000000");
        assertThat(listener.receivedOrder.lastExecutedPrice()).isEqualTo("0.00000000");
        assertThat(listener.receivedOrder.commissionAmount()).isEqualTo("0");
        assertThat(listener.receivedOrder.commissionAsset()).isEqualTo(null);
        assertThat(listener.receivedOrder.transactionTime()).isEqualTo(1499405658657L);
        assertThat(listener.receivedOrder.tradeId()).isEqualTo(-1L);
        assertThat(listener.receivedOrder.preventedMatchId()).isEqualTo(3L);
        assertThat(listener.receivedOrder.executionId()).isEqualTo(8641984L);
        assertThat(listener.receivedOrder.isTheOrderOnTheBook()).isEqualTo(true);
        assertThat(listener.receivedOrder.isThisTradeTheMakerSide()).isEqualTo(false);
        assertThat(listener.receivedOrder.ignore()).isEqualTo(false);
        assertThat(listener.receivedOrder.orderCreationTime()).isEqualTo(1499405658657L);
        assertThat(listener.receivedOrder.cumulativeQuoteAssetTransactedQty()).isEqualTo("0.00000000");
        assertThat(listener.receivedOrder.lastQuoteAssetTransactedQty()).isEqualTo("0.00000000");
        assertThat(listener.receivedOrder.quoteOrderQty()).isEqualTo("0.00000000");
        assertThat(listener.receivedOrder.workingTime()).isEqualTo(1499405658657L);
        assertThat(listener.receivedOrder.selfTradePreventionMode()).isEqualTo("NONE");
    }
}
