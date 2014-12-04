package org.votingsystem.util;

import org.votingsystem.model.TypeVS;
import java.math.BigDecimal;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRMessageVS {

    private TypeVS operation;
    private String URL;
    private String currencyCode;
    private String tag;
    private Long id;
    private BigDecimal amount;
    private Integer numTickets;
    private String qrMessage;

    //http://cooins:8086/Cooins/QR/test?cht=qr&chs=200x200&chl=qrMessage
    //qrMessage -> operation=TRANSACTION;id=1;URL=https://cooins:8086/Cooins;amount=100_eur_WILDTAG;
    public QRMessageVS(String qrMessage) throws ExceptionVS {
        this.qrMessage = qrMessage;
        String[] messageParts = qrMessage.split(";");
        String[] partContent = messageParts[0].split("="); //0 -> operation
        if(partContent.length > 1) {
            if(!"operation".equals(partContent[0])) throw new ExceptionVS("part 0 of the qrMessage must be 'operation'");
            operation = TypeVS.valueOf(partContent[1]);
        }
        partContent = messageParts[1].split("="); //1 -> id
        if(partContent.length > 1) {
            if(!"id".equals(partContent[0])) throw new ExceptionVS("part 1 of the qrMessage must be 'id'");
            id = Long.valueOf(partContent[1]);
        }
        partContent = messageParts[2].split("="); //2 -> URL
        if(partContent.length > 1) {
            if(!"URL".equals(partContent[0])) throw new ExceptionVS("part 2 of the qrMessage must be 'URL'");
            URL = partContent[1];
        }
        partContent = messageParts[3].split("="); //3 -> amount
        if(partContent.length > 1) {
            if(!"amount".equals(partContent[0])) throw new ExceptionVS("part 3 of the qrMessage must be 'amount'");
            String[] subParts = partContent[1].split("_");
            if(subParts.length != 3)  throw new ExceptionVS("expected 'amount_currencyCode_tag' found '" + partContent[1] + "'");
            if(subParts[0].contains("x")) {
                setNumTickets(Integer.valueOf(subParts[0].split("x")[0]));
                amount = new BigDecimal(subParts[0].split("x")[1]);
            } else amount = new BigDecimal(subParts[0]);
            currencyCode = subParts[1].toUpperCase();
            tag = subParts[2];
        }
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

    public Integer getNumTickets() {
        return numTickets;
    }

    public void setNumTickets(Integer numTickets) {
        this.numTickets = numTickets;
    }

    public String getQrMessage() {
        return qrMessage;
    }

    public void setQrMessage(String qrMessage) {
        this.qrMessage = qrMessage;
    }
}
