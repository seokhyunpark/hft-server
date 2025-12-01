package io.github.seokhyunpark.hft.exchange.listener;

import io.github.seokhyunpark.hft.exchange.dto.AccountUpdate;
import io.github.seokhyunpark.hft.exchange.dto.BalanceUpdate;
import io.github.seokhyunpark.hft.exchange.dto.OrderUpdate;

public interface UserEventListener {
    void onAccountUpdateReceived(AccountUpdate accountUpdate);

    void onBalanceUpdateReceived(BalanceUpdate balanceUpdate);

    void onOrderUpdateReceived(OrderUpdate orderUpdate);
}
