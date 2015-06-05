package org.votingsystem.web.currency.util;

import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.ContextVS;

import javax.ws.rs.container.AsyncResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AsyncRequestShopBundle {

    private TransactionVSDto transactionDto;
    private Map<String, Currency> currencyMap;
    private AsyncResponse asyncResponse;

    public AsyncRequestShopBundle(TransactionVSDto dto, AsyncResponse asyncResponse) {
        this.transactionDto = dto;
        this.asyncResponse = asyncResponse;
    }

    public Map<String, Currency> getCurrencyMap() {
        return currencyMap;
    }

    public void setCurrencyMap(Map<String, Currency> currencyMap) {
        this.currencyMap = currencyMap;
    }

    public AsyncResponse getAsyncResponse() {
        return asyncResponse;
    }

    public void setAsyncResponse(AsyncResponse asyncResponse) {
        this.asyncResponse = asyncResponse;
    }

    public void addHashCertVS (String hashCertVS) {
        if(currencyMap == null) currencyMap = new HashMap<>();
        Currency currency =  new  Currency(
                ContextVS.getInstance().getCurrencyServer().getServerURL(),
                transactionDto.getAmount(), transactionDto.getCurrencyCode(),
                transactionDto.isTimeLimited(), hashCertVS,
                new TagVS(transactionDto.getTagName()));
        currencyMap.put(hashCertVS, currency);
    }

    public TransactionVSDto getTransactionDto() {
        return transactionDto;
    }

    public void setTransactionDto(TransactionVSDto transactionDto) {
        this.transactionDto = transactionDto;
    }
}
