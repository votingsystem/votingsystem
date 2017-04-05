package org.currency.web.util;

import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.Tag;

import javax.ws.rs.container.AsyncResponse;
import java.util.Base64;
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

    public String addRevocationHash(String currencyServerURL, String revocationHash) throws Exception {
        if(currencyMap == null) currencyMap = new HashMap<>();
        Currency currency =  new  Currency(currencyServerURL,
                transactionDto.getAmount(), transactionDto.getCurrencyCode(),
                transactionDto.isTimeLimited(), revocationHash, new Tag(transactionDto.getTagName()));
        currencyMap.put(revocationHash, currency);
        return new String(currency.getCertificationRequest().getCsrPEM());
    }

    public Currency getCurrency(String hashCertVS) {
        return currencyMap.get(hashCertVS);
    }

    public TransactionDto getTransactionDto() {
        return transactionDto;
    }

    public TransactionDto getTransactionDto(byte[] signedDocument) throws Exception {
        transactionDto.setSignedDocumentBase64(Base64.getEncoder().encodeToString(signedDocument));
        return transactionDto;
    }

    public void setTransactionDto(TransactionDto transactionDto) {
        this.transactionDto = transactionDto;
    }

}