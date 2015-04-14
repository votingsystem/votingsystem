package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.currency.TransactionVS;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionVSDto {

    private Long id;
    private UserVSDto fromUserVS;
    private UserVSDto toUserVS;
    private Date validTo;
    private Date dateCreated;
    private String subject;
    private String description;
    private String currency;
    private String receipt;
    private String messageSMIME;
    private String messageSMIMEURL;
    private BigDecimal amount;
    private TransactionVS.Type type;
    private Set<String> tags;
    private Long numChildTransactions;

    public TransactionVSDto() {}

    public TransactionVSDto(TransactionVS transactionVS, String contextURL) {
        this.setId(transactionVS.getId());
        if(transactionVS.getFromUserVS() != null) {
            this.setFromUserVS(UserVSDto.BASIC(transactionVS.getFromUserVS()));
            if(transactionVS.getFromUserIBAN() != null) {
                Map senderMap = new HashMap<>();
                senderMap.put("fromUserIBAN", transactionVS.getFromUserIBAN());
                senderMap.put("fromUser", transactionVS.getFromUser());
                this.getFromUserVS().setSender(senderMap);
            }
        }
        if(transactionVS.getToUserVS() != null) {
            this.setToUserVS(UserVSDto.BASIC(transactionVS.getToUserVS()));
        }
        this.setValidTo(transactionVS.getValidTo());
        this.setDateCreated(transactionVS.getDateCreated());
        this.setSubject(transactionVS.getSubject());
        this.setAmount(transactionVS.getAmount());
        this.setType(transactionVS.getType());
        this.setCurrency(transactionVS.getCurrencyCode());
        if(transactionVS.getMessageSMIME() != null) {
            setMessageSMIMEURL(contextURL + "/rest/messageSMIME/id/" + transactionVS.getMessageSMIME().getId());
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserVSDto getFromUserVS() {
        return fromUserVS;
    }

    public void setFromUserVS(UserVSDto fromUserVS) {
        this.fromUserVS = fromUserVS;
    }

    public UserVSDto getToUserVS() {
        return toUserVS;
    }

    public void setToUserVS(UserVSDto toUserVS) {
        this.toUserVS = toUserVS;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMessageSMIMEURL() {
        return messageSMIMEURL;
    }

    public void setMessageSMIMEURL(String messageSMIMEURL) {
        this.messageSMIMEURL = messageSMIMEURL;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionVS.Type getType() {
        return type;
    }

    public void setType(TransactionVS.Type type) {
        this.type = type;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Long getNumChildTransactions() {
        return numChildTransactions;
    }

    public void setNumChildTransactions(Long numChildTransactions) {
        this.numChildTransactions = numChildTransactions;
    }

    public String getMessageSMIME() {
        return messageSMIME;
    }

    public void setMessageSMIME(String messageSMIME) {
        this.messageSMIME = messageSMIME;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }
}
