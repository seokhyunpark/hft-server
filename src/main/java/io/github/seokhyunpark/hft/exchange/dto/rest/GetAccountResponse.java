package io.github.seokhyunpark.hft.exchange.dto.rest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GetAccountResponse(
        // https://developers.binance.com/docs/binance-spot-api-docs/rest-api/account-endpoints#account-information-user_data
        @JsonProperty("makerCommission") long makerCommission,
        @JsonProperty("takerCommission") long takerCommission,
        @JsonProperty("buyerCommission") long buyerCommission,
        @JsonProperty("sellerCommission") long sellerCommission,
        @JsonProperty("commissionRates") CommissionRates commissionRates,
        @JsonProperty("canTrade") boolean canTrade,
        @JsonProperty("canWithdraw") boolean canWithdraw,
        @JsonProperty("canDeposit") boolean canDeposit,
        @JsonProperty("brokered") boolean brokered,
        @JsonProperty("requireSelfTradePrevention") boolean requireSelfTradePrevention,
        @JsonProperty("preventSor") boolean preventSor,
        @JsonProperty("updateTime") long updateTime,
        @JsonProperty("accountType") String accountType,
        @JsonProperty("balances") List<Balance> balances,
        @JsonProperty("permissions") List<String> permissions,
        @JsonProperty("uid") long uid
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommissionRates(
            @JsonProperty("maker") String maker,
            @JsonProperty("taker") String taker,
            @JsonProperty("buyer") String buyer,
            @JsonProperty("seller") String seller
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Balance(
            @JsonProperty("asset") String asset,
            @JsonProperty("free") String free,
            @JsonProperty("locked") String locked
    ) {}
}
