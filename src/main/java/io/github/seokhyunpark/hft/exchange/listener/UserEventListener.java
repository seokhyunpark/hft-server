package io.github.seokhyunpark.hft.exchange.listener;

import io.github.seokhyunpark.hft.exchange.dto.stream.AccountUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.BalanceUpdate;
import io.github.seokhyunpark.hft.exchange.dto.stream.OrderUpdate;

public interface UserEventListener {
    void onAccountUpdateReceived(AccountUpdate accountUpdate);

    void onBalanceUpdateReceived(BalanceUpdate balanceUpdate);

    void onOrderUpdateReceived(OrderUpdate orderUpdate);
}
