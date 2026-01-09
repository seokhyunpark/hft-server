package io.github.seokhyunpark.hft.trading.core;

import org.springframework.stereotype.Component;

import io.github.seokhyunpark.hft.exchange.dto.stream.AccountUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.BalanceUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.OrderUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.PartialBookDepth;
import io.github.seokhyunpark.hft.exchange.listener.MarketEventListener;
import io.github.seokhyunpark.hft.exchange.listener.UserEventListener;

@Component
public class TradingCore implements MarketEventListener, UserEventListener {

    @Override
    public void onPartialBookDepthReceived(PartialBookDepth partialBookDepth) {

    }

    @Override
    public void onAccountUpdateReceived(AccountUpdate accountUpdate) {

    }

    @Override
    public void onBalanceUpdateReceived(BalanceUpdate balanceUpdate) {

    }

    @Override
    public void onOrderUpdateReceived(OrderUpdate orderUpdate) {

    }
}
