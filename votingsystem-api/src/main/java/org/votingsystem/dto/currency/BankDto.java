package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyOperation;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BankDto {

    private CurrencyOperation operation;
    private String info;
    private String certChainPEM;
    private String IBAN;
    private String UUID;

    public BankDto() { }

    public BankDto(String IBAN, String info, String certChainPEM) {
        this.setOperation(CurrencyOperation.BANK_NEW);
        this.setIBAN(IBAN);
        this.setInfo(info);
        this.setCertChainPEM(certChainPEM);
        this.setUUID(java.util.UUID.randomUUID().toString());
    }

    public void validatePublishRequest() throws ValidationException {
        if(CurrencyOperation.BANK_NEW != operation)
            throw new ValidationException("Operation expected: 'BANK_NEW' - found: " + operation);
        if(IBAN == null)
            throw new ValidationException("missing param 'IBAN'");
        if(info == null)
            throw new ValidationException("missing param 'info'");
        if(certChainPEM == null)
            throw new ValidationException("missing param 'certChainPEM'");
    }

    public CurrencyOperation getOperation() {
        return operation;
    }

    public void setOperation(CurrencyOperation operation) {
        this.operation = operation;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getCertChainPEM() {
        return certChainPEM;
    }

    public void setCertChainPEM(String certChainPEM) {
        this.certChainPEM = certChainPEM;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

}
