package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.util.CurrencyOperation;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponseDto {

    private static Logger log = Logger.getLogger(TransactionResponseDto.class.getName());

    private CurrencyOperation operation;
    private String cmsMessagePEM;
    private String currencyChangeCert;

    @JsonIgnore
    private SignedDocument signedDocument;

    public TransactionResponseDto() {}

    public TransactionResponseDto(CurrencyOperation operation, String currencyChangeCert, SignedDocument signedDocument) throws Exception {
        log.severe("========== TODO");
        this.operation = operation;
        this.signedDocument = signedDocument;
        this.currencyChangeCert = currencyChangeCert;
    }

    public CurrencyOperation getOperation() {
        return operation;
    }

    public void setOperation(CurrencyOperation operation) {
        this.operation = operation;
    }

    public String getCurrencyChangeCert() {
        return currencyChangeCert;
    }

}
