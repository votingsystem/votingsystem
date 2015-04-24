package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyBatchResponseDto {

    private String leftOverCoin;
    private String receipt;


    public CurrencyBatchResponseDto() {};

    public CurrencyBatchResponseDto(String receipt, String leftOverCoin) {
        this.receipt = receipt;
        this.leftOverCoin = leftOverCoin;
    }

    public String getLeftOverCoin() {
        return leftOverCoin;
    }

    public void setLeftOverCoin(String leftOverCoin) {
        this.leftOverCoin = leftOverCoin;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }
}
