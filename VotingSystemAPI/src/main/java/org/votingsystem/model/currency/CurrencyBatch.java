package org.votingsystem.model.currency;

import org.votingsystem.model.BatchRequest;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("CurrencyBatch")
public class CurrencyBatch extends BatchRequest implements Serializable {

    private static Logger log = java.util.logging.Logger.getLogger(CurrencyBatch.class.getSimpleName());

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUserVS") private UserVS toUserVS;
    @Column(name="batchAmount") private BigDecimal batchAmount;
    @Column(name="leftOver") private BigDecimal leftOver;
    @Column(name="currencyCode") private String currencyCode;
    @ManyToOne(fetch= FetchType.EAGER)
    @JoinColumn(name="tagVS", nullable=false) private TagVS tagVS;
    @Column(name="timeLimited") private Boolean timeLimited = Boolean.FALSE;
    @OneToOne private MessageSMIME messageSMIME;
    @Column(name="batchUUID") private String batchUUID;
    @Column(name="subject") private String subject;

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

    public BigDecimal getLeftOver() {
        return leftOver;
    }

    public void setLeftOver(BigDecimal leftOver) {
        this.leftOver = leftOver;
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
}
