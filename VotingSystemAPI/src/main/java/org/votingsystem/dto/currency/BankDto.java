package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.TypeVS;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankDto {

    private TypeVS operation;
    private String info;
    private String certChainPEM;
    private String IBAN;
    private String UUID;

    public BankDto() { }

    public static BankDto NEW(String IBAN, String info, String certChainPEM) {
        BankDto bankDto = new BankDto();
        bankDto.setOperation(TypeVS.BANK_NEW);
        bankDto.setIBAN(IBAN);
        bankDto.setInfo(info);
        bankDto.setCertChainPEM(certChainPEM);
        bankDto.setUUID(java.util.UUID.randomUUID().toString());
        return bankDto;
    }

    public void validatePublishRequest() throws ValidationException {
        if(operation == null) throw new ValidationException("missing param 'operation'");
        if(TypeVS.BANK_NEW != operation) throw new ValidationException(
                "Operation expected: 'BANK_NEW' - operation found: " + operation.toString());
        if(IBAN == null) throw new ValidationException("missing param 'IBAN'");
        if(info == null) throw new ValidationException("missing param 'info'");
        if(certChainPEM == null) throw new ValidationException("missing param 'certChainPEM'");
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
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
