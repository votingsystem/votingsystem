package org.votingsystem.android.util;

import org.votingsystem.model.TypeVS;
import org.votingsystem.util.ExceptionVS;

import java.math.BigDecimal;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRMessageVS {

    private String currencyCode;
    private String tag;
    private Long id;
    private BigDecimal amount;
    private Integer numTickets;
    private String qrMessage;
    private TypeVS operation;
    private String URL;
    private String UUID;

    public QRMessageVS(String qrMessage) throws ExceptionVS {
        if(qrMessage.toLowerCase().contains("http://") ||
                qrMessage.toLowerCase().contains("https://")) {
            this.URL = qrMessage;
        } else this.setUUID(getUUID());
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getNumTickets() {
        return numTickets;
    }

    public void setNumTickets(int numTickets) {
        this.numTickets = numTickets;
    }

    public String getQrMessage() {
        return qrMessage;
    }

    public void setQrMessage(String qrMessage) {
        this.qrMessage = qrMessage;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}