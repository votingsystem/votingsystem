package org.votingsystem.currency;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.CurrencyOperation;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyTransactionRequest {

    private CurrencyBatchDto batchDto;
    private CurrencyBatch currencyBatch;
    private Set<Currency> currencySet;
    private String currencyEntity;
    private Currency leftOverCurrency;
    private PKCS10CertificationRequest leftOverCsr;

    public CurrencyTransactionRequest(){}

    public CurrencyTransactionRequest(CurrencyBatchDto batchDto){
        this.batchDto = batchDto;
    }

    public CurrencyBatchDto getBatchDto() {
        return batchDto;
    }

    public void setBatchDto(CurrencyBatchDto batchDto) {
        this.batchDto = batchDto;
    }

    public CurrencyBatch getCurrencyBatch() {
        return currencyBatch;
    }

    public CurrencyTransactionRequest setCurrencyBatch(CurrencyBatch currencyBatch) {
        this.currencyBatch = currencyBatch;
        return this;
    }

    public Set<Currency> getCurrencySet() {
        return currencySet;
    }

    public CurrencyTransactionRequest setCurrencySet(Set<Currency> currencySet) {
        this.currencySet = currencySet;
        return this;
    }

    public String getCurrencyEntity() {
        return currencyEntity;
    }

    public CurrencyTransactionRequest setCurrencyEntity(String currencyEntity) {
        this.currencyEntity = currencyEntity;
        return this;
    }


    public Currency getLeftOverCurrency() {
        return leftOverCurrency;
    }

    public CurrencyTransactionRequest setLeftOverCurrency(Currency leftOverCurrency) {
        this.leftOverCurrency = leftOverCurrency;
        return this;
    }

    public static CurrencyTransactionRequest build(CurrencyBatchDto batchDto) throws Exception {
        if(batchDto.getOperation() != CurrencyOperation.CURRENCY_SEND)
            throw new ValidationException(MessageFormat.format("Expected batch type 'CURRENCY_SEND' found ''{1}''",
                    batchDto.getOperation()));
        CurrencyTransactionRequest transactionRequest = new CurrencyTransactionRequest(batchDto);
        BigDecimal accumulated = BigDecimal.ZERO;
        LocalDateTime checkDate = LocalDateTime.now();
        Set<Currency> currencySet = new HashSet<>();
        for(String currencyItem : batchDto.getCurrencySet()) {
            try {
                CMSSignedMessage signedMessage = CMSSignedMessage.FROM_PEM(currencyItem.getBytes());
                Currency currency = new Currency(signedMessage);
                CurrencyDto batchItem = currency.getBatchItemDto();
                String currencyData = "currency batch item with revocation hash '" + batchItem.getRevocationHash() + "' ";
                if(!batchDto.getSubject().equals(batchItem.getSubject())) throw new ValidationException(
                        currencyData + "expected subject " + batchDto.getSubject() + " found " + batchItem.getSubject());
                if(batchDto.getToUserIBAN() != null) {
                    if(!batchDto.getToUserIBAN().equals(batchItem.getToUserIBAN())) throw new ValidationException(
                            currencyData + "expected toUserIBAN " + batchDto.getToUserIBAN() + " found " + batchItem.getToUserIBAN());
                }
                if(!batchDto.getCurrencyCode().equals(batchItem.getCurrencyCode())) throw new ValidationException(
                        currencyData + "expected currencyCode " + batchDto.getCurrencyCode() + " found " + batchItem.getCurrencyCode());

                if(checkDate.isAfter(currency.getValidTo())) throw new ValidationException(MessageFormat.format(
                        "currency ''{0}'' expired on: ''{1}''", currency.getRevocationHash(), currency.getValidTo()));
                accumulated = accumulated.add(currency.getAmount());
                currencySet.add(currency);
            } catch(Exception ex) {
                throw new ValidationException("Error with currency : " + ex.getMessage(), ex);
            }
        }
        if(currencySet.isEmpty())
            throw new ValidationException("Empty currency batch");
        CurrencyCertExtensionDto certExtensionDto = null;
        BigDecimal leftOverCalculated = BigDecimal.ZERO;
        PKCS10CertificationRequest leftOverCsr = null;
        if(batchDto.getLeftOverCSR() != null) {
            leftOverCsr = PEMUtils.fromPEMToPKCS10CertificationRequest(batchDto.getLeftOverCSR().getBytes());
            certExtensionDto = CertificateUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                    leftOverCsr, Constants.CURRENCY_OID);
            if(batchDto.getLeftOver().compareTo(certExtensionDto.getAmount()) != 0) throw new ValidationException(
                    "leftOver 'amount' mismatch - request: " + batchDto.getLeftOver() + " - csr: " + certExtensionDto.getAmount());
            if(!certExtensionDto.getCurrencyCode().equals(batchDto.getCurrencyCode())) throw new ValidationException(
                    "leftOver 'currencyCode' mismatch - request: " + batchDto.getCurrencyCode() + " - csr: " +
                            certExtensionDto.getCurrencyCode());
            leftOverCalculated = accumulated.subtract(batchDto.getBatchAmount());
            if(leftOverCalculated.compareTo(batchDto.getLeftOver()) != 0) throw new ValidationException(
                    "leftOverCalculated: " + leftOverCalculated + " - leftOver: " + batchDto.getLeftOver());
        } else if(batchDto.getLeftOver() != null && batchDto.getLeftOver().compareTo(BigDecimal.ZERO) != 0)
            throw new ValidationException("request with leftOver but without CSR");
        BigDecimal expectedAmount = batchDto.getBatchAmount().add(leftOverCalculated);
        if(expectedAmount.compareTo(accumulated) != 0)
            throw new ValidationException(MessageFormat.format("Amount expected ''{0}'' amount in currencies ''{1}''",
                    expectedAmount, accumulated));
        CurrencyBatch currencyBatch = new CurrencyBatch(batchDto);
        transactionRequest.setCurrencySet(currencySet).setCurrencyBatch(currencyBatch).setLeftOverCsr(leftOverCsr);
        return transactionRequest;
    }

    public PKCS10CertificationRequest getLeftOverCsr() {
        return leftOverCsr;
    }

    public void setLeftOverCsr(PKCS10CertificationRequest leftOverCsr) {
        this.leftOverCsr = leftOverCsr;
    }
}
