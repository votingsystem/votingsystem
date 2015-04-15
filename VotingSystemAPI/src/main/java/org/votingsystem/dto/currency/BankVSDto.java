package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankVSDto {

    private TypeVS operation;
    private String info;
    private String certChainPEM;
    private String IBAN;
    private String UUID;

    public BankVSDto() { }

    public static BankVSDto NEW(String IBAN, String info, String certChainPEM) {
        BankVSDto bankVSDto = new BankVSDto();
        bankVSDto.setOperation(TypeVS.BANKVS_NEW);
        bankVSDto.setIBAN(IBAN);
        bankVSDto.setInfo(info);
        bankVSDto.setCertChainPEM(certChainPEM);
        bankVSDto.setUUID(java.util.UUID.randomUUID().toString());
        return bankVSDto;
    }

    public void validatePublishRequest() throws ValidationExceptionVS {
        if(operation == null) throw new ValidationExceptionVS("missing param 'operation'");
        if(TypeVS.BANKVS_NEW != operation) throw new ValidationExceptionVS(
                "Operation expected: 'BANKVS_NEW' - operation found: " + operation.toString());
        if(IBAN == null) throw new ValidationExceptionVS("missing param 'IBAN'");
        if(info == null) throw new ValidationExceptionVS("missing param 'info'");
        if(certChainPEM == null) throw new ValidationExceptionVS("missing param 'certChainPEM'");
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
