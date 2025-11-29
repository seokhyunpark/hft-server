package io.github.seokhyunpark.hft.exchange.stream;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.seokhyunpark.hft.exchange.dto.PartialBookDepth;
import io.github.seokhyunpark.hft.exchange.listener.MarketEventListener;

public class MarketDataStreamTest {
    static class TestListener implements MarketEventListener {
        PartialBookDepth receivedData;

        @Override
        public void onPartialBookDepthReceived(PartialBookDepth partialBookDepth) {
            this.receivedData = partialBookDepth;
        }
    }

    @Test
    @DisplayName("메시지가 오면 리스너에게 데이터를 넘겨줘야 한다.")
    public void onMessageSuccess() throws URISyntaxException {
        URI uri = new URI("wss//test.com");
        TestListener listener = new TestListener();

        MarketDataStream stream = new MarketDataStream(uri, listener);

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
                  ]
                }
                """;

        stream.onMessage(data);
        assertThat(listener.receivedData).isNotNull();
        assertThat(listener.receivedData.lastUpdateId()).isEqualTo(37530297893L);
    }

    @Test
    @DisplayName("잘못된 메시지가 오면 리스너에게 아무것도 넘기지 말아야 한다.")
    public void onMessageFail() throws URISyntaxException {
        URI uri = new URI("wss//test.com");
        TestListener listener = new TestListener();

        MarketDataStream stream = new MarketDataStream(uri, listener);

        String data = "{Invalid Data}";

        stream.onMessage(data);
        assertThat(listener.receivedData).isNull();
    }
}
