package org.votingsystem.currency;

import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationType;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyUtils {

    public static void checkDto(Currency currency, CurrencyDto currencyDto) throws ValidationException{
        if(currency.getAmount().compareTo(currencyDto.getAmount()) != 0)
            throw new ValidationException(MessageFormat.format("Expected currency amount ''{0}'' found ''{1}''",
                    currency.getAmount(), currencyDto.getAmount()));
        if(currency.getCurrencyCode() != currencyDto.getCurrencyCode())
            throw new ValidationException(MessageFormat.format("Expected currency code ''{0}'' found ''{1}''",
                    currency.getCurrencyCode(), currencyDto.getCurrencyCode()));
        if(currency.getCurrencyEntity().equals(currencyDto.getCurrencyEntity()))
            throw new ValidationException(MessageFormat.format("Expected currency entity ''{0}'' found ''{1}''",
                    currency.getCurrencyEntity(), currencyDto.getCurrencyEntity()));
    }

    public static CurrencyTransactionRequest buildCurrencyTransactionRequest(String subject, String toUserIBAN,
            BigDecimal batchAmount, CurrencyCode currencyCode, List<Currency> currencyList,
            String currencyEntity, String timestampEntityId) throws Exception {
        CurrencyBatchDto batchDto = new CurrencyBatchDto(subject, toUserIBAN, batchAmount, currencyCode);
        batchDto.setBatchUUID(UUID.randomUUID().toString());
        CurrencyTransactionRequest currencyTransactionRequest = new CurrencyTransactionRequest(batchDto);
        BigDecimal accumulated = BigDecimal.ZERO;
        for (org.votingsystem.model.currency.Currency currency : currencyList) {
            accumulated = accumulated.add(currency.getAmount());
        }
        if(batchAmount.compareTo(accumulated) > 0) {
            throw new ValidationException(MessageFormat.format("''{0}'' batchAmount exceeds currency sum ''{1}''",
                    batchAmount, accumulated));
        } else if(batchAmount.compareTo(accumulated) != 0){
            batchDto.setLeftOver(accumulated.subtract(batchAmount));
            Currency leftOverCurrency = Currency.build(currencyEntity, batchDto.getLeftOver(), currencyCode);
            currencyTransactionRequest.setLeftOverCurrency(leftOverCurrency);
            batchDto.setLeftOverCSR(new String(leftOverCurrency.getCertificationRequest().getCsrPEM()));
        }
        Set<String> currencySet = new HashSet<>();
        for (org.votingsystem.model.currency.Currency currency : currencyList) {
            byte[] contentToSign = JSON.getMapper().writeValueAsBytes(batchDto.buildBatchItem(currency));
            byte[] contentSigned = currency.getCertificationRequest().signPKCS7WithTimeStamp(contentToSign,
                    OperationType.TIMESTAMP_REQUEST.getUrl(timestampEntityId));
            currency.setContent(contentSigned);
            currencySet.add(new String(contentSigned));
        }
        batchDto.setCurrencySet(currencySet);
        return currencyTransactionRequest;
    }


    public Map<String, Currency> getRevocationHashMap(List<Currency> currencyList) throws ValidationException {
        Map<String, Currency> result = new HashMap<>();
        for(Currency currency : currencyList) {
            result.put(currency.getRevocationHash(), currency);
        }
        return result;
    }

}
