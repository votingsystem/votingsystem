package org.votingsystem.model.currency;

import org.votingsystem.model.BatchVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("CurrencyBatch")
public class CurrencyBatch extends BatchVS implements Serializable {

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUserVS") private UserVS toUserVS;
    @Column(name="batchAmount") private BigDecimal batchAmount;
    @OneToOne
    @JoinColumn(name="leftOver") private Currency leftOver;
    @OneToOne
    @JoinColumn(name="currencyChange") private Currency currencyChange;
    @Column(name="currencyCode") private String currencyCode;
    @ManyToOne(fetch= FetchType.EAGER)
    @JoinColumn(name="tagVS", nullable=false) private TagVS tagVS;
    @Column(name="timeLimited") private Boolean timeLimited = Boolean.FALSE;
    @OneToOne private MessageSMIME messageSMIME;
    @Column(name="batchUUID") private String batchUUID;
    @Column(name="subject") private String subject;

    @Transient private Set<Currency> validatedCurrencySet;

    public CurrencyBatch() {}

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public TagVS getTagVS() {
        return tagVS;
    }

    public void setTagVS(TagVS tagVS) {
        this.tagVS = tagVS;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public UserVS getToUserVS() {
        return toUserVS;
    }

    public CurrencyBatch setToUserVS(UserVS toUserVS) {
        this.toUserVS = toUserVS;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public MessageSMIME getMessageSMIME() {
        return messageSMIME;
    }

    public CurrencyBatch setMessageSMIME(MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
        return this;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public Currency getLeftOver() {
        return leftOver;
    }

    public void setLeftOver(Currency leftOver) {
        this.leftOver = leftOver;
    }

    public Currency getCurrencyChange() {
        return currencyChange;
    }

    public void setCurrencyChange(Currency currencyChange) {
        this.currencyChange = currencyChange;
    }

    public Set<Currency> getValidatedCurrencySet() {
        return validatedCurrencySet;
    }

    public void setValidatedCurrencySet(Set<Currency> validatedCurrencySet) {
        this.validatedCurrencySet = validatedCurrencySet;
    }
}
