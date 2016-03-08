package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.util.TypeVS;

import java.util.Base64;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionResponseDto {

    private TypeVS operation;
    private String cmsMessagePEM;
    private String currencyChangeCert;

    @JsonIgnore private CMSSignedMessage cms;

    public TransactionResponseDto() {}

    public TransactionResponseDto(TypeVS operation, String currencyChangeCert, CMSSignedMessage cmsMessage) throws Exception {
        this.operation = operation;
        this.cmsMessagePEM = cmsMessage.toPEMStr();
        this.currencyChangeCert = currencyChangeCert;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getCurrencyChangeCert() {
        return currencyChangeCert;
    }

    @JsonIgnore
    public CMSSignedMessage getCMS() throws Exception {
        if(cms == null && cmsMessagePEM != null) cms = CMSSignedMessage.FROM_PEM(cmsMessagePEM);
        return cms;
    }

    public void setCMS(CMSSignedMessage cms) {
        this.cms = cms;
    }

    public String getCmsMessagePEM() {
        return cmsMessagePEM;
    }

    public void setCmsMessagePEM(String cmsMessagePEM) {
        this.cmsMessagePEM = cmsMessagePEM;
    }
}
