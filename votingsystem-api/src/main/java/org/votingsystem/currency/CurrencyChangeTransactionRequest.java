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
public class CurrencyChangeTransactionRequest {

    private CurrencyBatchDto batchDto;
    private CurrencyBatch currencyBatch;
    private Set<Currency> currencySet;
    private PKCS10CertificationRequest currencyChangeCSR;
    private PKCS10CertificationRequest leftOverCsr;

    public CurrencyChangeTransactionRequest(){}

    public CurrencyChangeTransactionRequest(CurrencyBatchDto batchDto){
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

    public void setCurrencyBatch(CurrencyBatch currencyBatch) {
        this.currencyBatch = currencyBatch;
    }

    public Set<Currency> getCurrencySet() {
        return currencySet;
    }

    public CurrencyChangeTransactionRequest setCurrencySet(Set<Currency> currencySet) {
        this.currencySet = currencySet;
        return this;
    }

    public PKCS10CertificationRequest getCurrencyChangeCSR() {
        return currencyChangeCSR;
    }

    public CurrencyChangeTransactionRequest setCurrencyChangeCSR(PKCS10CertificationRequest currencyChangeCSR) {
        this.currencyChangeCSR = currencyChangeCSR;
        return this;
    }

    public static CurrencyChangeTransactionRequest build(CurrencyBatchDto batchDto) throws Exception {
        if(batchDto.getOperation() != CurrencyOperation.CURRENCY_CHANGE)
            throw new ValidationException(MessageFormat.format("Expected batch type 'CURRENCY_CHANGE' found ''{1}''",
                    batchDto.getOperation()));
        CurrencyChangeTransactionRequest transactionRequest = new CurrencyChangeTransactionRequest(batchDto);
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
        PKCS10CertificationRequest currencyChangeCSR = PEMUtils.fromPEMToPKCS10CertificationRequest(
                batchDto.getCurrencyChangeCSR().getBytes());
        certExtensionDto = CertificateUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                currencyChangeCSR, Constants.CURRENCY_OID);
        if(certExtensionDto.getAmount().compareTo(batchDto.getBatchAmount()) != 0) throw new ValidationException(
                "currencyChange 'amount' mismatch - request: " + batchDto.getBatchAmount() +
                        " - csr: " + certExtensionDto.getAmount());
        if(!certExtensionDto.getCurrencyCode().equals(batchDto.getCurrencyCode())) throw new ValidationException(
                "currencyChange 'currencyCode' mismatch - request: " + batchDto.getCurrencyCode() +
                        " - csr: " + certExtensionDto.getCurrencyCode());
        CurrencyBatch currencyBatch = new CurrencyBatch(batchDto);
        transactionRequest.setCurrencyChangeCSR(currencyChangeCSR).setCurrencySet(currencySet).setCurrencyBatch(currencyBatch);
        return transactionRequest;
    }

    public PKCS10CertificationRequest getLeftOverCsr() {
        return leftOverCsr;
    }

    public void setLeftOverCsr(PKCS10CertificationRequest leftOverCsr) {
        this.leftOverCsr = leftOverCsr;
    }
}