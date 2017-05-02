package org.votingsystem.util;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum CurrencyOperation implements SystemOperation {

    @JsonProperty("CLOSE_SESSION")
    CLOSE_SESSION(null, false),
    @JsonProperty("CURRENCY_WALLET_CHANGE")
    CURRENCY_WALLET_CHANGE(null, false),
    @JsonProperty("CURRENCY_REQUEST")
    CURRENCY_REQUEST("/currency/request", true),
    @JsonProperty("BUNDLE_STATE")
    BUNDLE_STATE("/api/currency/bundleState", false),
    @JsonProperty("GET_METADATA")
    GET_METADATA("/api/metadata", false),
    @JsonProperty("CURRENCY_SEND")
    CURRENCY_SEND("/api/currency/send", true),
    @JsonProperty("MSG_TO_DEVICE")
    MSG_TO_DEVICE("/api/msg/send", false),

    @JsonProperty("CURRENCY_CHANGE")
    CURRENCY_CHANGE(null, true),
    @JsonProperty("GET_TRUSTED_CERTS")
    GET_TRUSTED_CERTS("/api/certs/trusted", false),
    @JsonProperty("GET_CURRENCY_STATUS")
    GET_CURRENCY_STATUS("/api/currency/state", false),
    @JsonProperty("GET_CURRENCY_BUNDLE_STATUS")
    GET_CURRENCY_BUNDLE_STATUS("/api/currency/bundle-state", false),
    @JsonProperty("GET_SESSION_CERTIFICATION")
    GET_SESSION_CERTIFICATION("/api/cert-issuer/mobile-browser-session", false),
    @JsonProperty("GET_USER_ACCOUNTS")
    GET_USER_ACCOUNTS("/api/currency-account/user-accounts", false),
    @JsonProperty("INIT_DEVICE_SESSION")
    INIT_DEVICE_SESSION("/api/session/init-device-session", false),

    @JsonProperty("QR_INFO")
    QR_INFO("/api/currency-qr/info", false),
    @JsonProperty("REGISTER_DEVICE")
    REGISTER_DEVICE("/api/cert-issuer/register-device", false),
    @JsonProperty("TRANSACTION_INFO")
    TRANSACTION_INFO(null, false),
    @JsonProperty("TRANSACTION_FROM_BANK")
    TRANSACTION_FROM_BANK("/api/transaction", true),
    @JsonProperty("TRANSACTION_FROM_USER")
    TRANSACTION_FROM_USER("/api/transaction", true),
    @JsonProperty("VALIDATE_SESSION_CERTIFICATES")
    VALIDATE_SESSION_CERTIFICATES("/api/session/validate-mobile-browser-session-certificates", false);

    private String url;
    private boolean currencyTransaction;

    CurrencyOperation(String url, boolean currencyTransaction) {
        this.url = url;
        this.currencyTransaction = currencyTransaction;
    }

    public String getUrl(String entityId) {
        return entityId + url;
    }

    public Set<CurrencyOperation> getCurrencyTransaction() {
        Set<CurrencyOperation> result = new HashSet<>();
        for(CurrencyOperation currencyOperation : CurrencyOperation.values()) {
            if(currencyOperation.isCurrencyTransaction())
                result.add(currencyOperation);
        }
        return result;
    }

    public boolean isCurrencyTransaction() {
        return currencyTransaction;
    }

}
