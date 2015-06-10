package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.TypeVS;

import java.util.Base64;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionResponseDto {

    private TypeVS operation;
    private String smimeMessage;
    private String currencyChangeCert;

    @JsonIgnore private SMIMEMessage smime;

    public TransactionResponseDto() {}

    public TransactionResponseDto(TypeVS operation, String currencyChangeCert, SMIMEMessage smimeMessage) throws Exception {
        this.operation = operation;
        this.smimeMessage = Base64.getEncoder().encodeToString(smimeMessage.getBytes());
        this.currencyChangeCert = currencyChangeCert;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(String smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    public String getCurrencyChangeCert() {
        return currencyChangeCert;
    }

    public void setCurrencyChangeCert(String currencyChangeCert) {
        this.currencyChangeCert = currencyChangeCert;
    }

    @JsonIgnore
    public SMIMEMessage getSmime() throws Exception {
        if(smime == null && smimeMessage != null) smime = new SMIMEMessage(Base64.getDecoder().decode(smimeMessage));
        return smime;
    }

    public void setSmime(SMIMEMessage smime) {
        this.smime = smime;
    }
}
