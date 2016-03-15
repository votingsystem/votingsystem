package org.votingsystem.web.currency.util;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.Currency;

import javax.ws.rs.container.AsyncResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AsyncRequestShopBundle {

    private TransactionDto transactionDto;
    private Map<String, Currency> currencyMap;
    private AsyncResponse asyncResponse;

    public AsyncRequestShopBundle(TransactionDto dto, AsyncResponse asyncResponse) {
        this.transactionDto = dto;
        this.asyncResponse = asyncResponse;
    }

    public AsyncResponse getAsyncResponse() {
        return asyncResponse;
    }

    public void setAsyncResponse(AsyncResponse asyncResponse) {
        this.asyncResponse = asyncResponse;
    }

    public String addHashCertVS (String currencyServerURL, String hashCertVS) throws Exception {
        if(currencyMap == null) currencyMap = new HashMap<>();
        Currency currency =  new  Currency(currencyServerURL,
                transactionDto.getAmount(), transactionDto.getCurrencyCode(),
                transactionDto.isTimeLimited(), hashCertVS,
                new TagVS(transactionDto.getTagName()));
        currencyMap.put(hashCertVS, currency);
        return new String(currency.getCertificationRequest().getCsrPEM());
    }

    public Currency getCurrency(String hashCertVS) {
        return currencyMap.get(hashCertVS);
    }

    public TransactionDto getTransactionDto() {
        return transactionDto;
    }

    public TransactionDto getTransactionDto(CMSSignedMessage cmsMessage) throws Exception {
        transactionDto.setCmsMessagePEM(cmsMessage.toPEMStr());
        return transactionDto;
    }

    public void setTransactionDto(TransactionDto transactionDto) {
        this.transactionDto = transactionDto;
    }
}
