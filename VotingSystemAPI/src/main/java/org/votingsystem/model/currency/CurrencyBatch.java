package org.votingsystem.model.currency;

import org.votingsystem.model.Batch;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("CurrencyBatch")
public class CurrencyBatch extends Batch implements Serializable {

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUser") private User toUser;
    @Column(name="batchAmount") private BigDecimal batchAmount;
    @OneToOne
    @JoinColumn(name="leftOver") private Currency leftOver;
    @OneToOne
    @JoinColumn(name="currencyChange") private Currency currencyChange;
    @Column(name="currencyCode") private String currencyCode;
    @ManyToOne(fetch= FetchType.EAGER)
    @JoinColumn(name="tagVS", nullable=false) private TagVS tagVS;
    @Column(name="timeLimited") private Boolean timeLimited = Boolean.FALSE;
    @OneToOne private CMSMessage cmsMessage;
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

    public User getToUser() {
        return toUser;
    }

    public CurrencyBatch setToUser(User toUser) {
        this.toUser = toUser;
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

    public CMSMessage getCmsMessage() {
        return cmsMessage;
    }

    public CurrencyBatch setCmsMessage(CMSMessage cmsMessage) {
        this.cmsMessage = cmsMessage;
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
