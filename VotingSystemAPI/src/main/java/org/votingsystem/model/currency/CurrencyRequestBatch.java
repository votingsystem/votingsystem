package org.votingsystem.model.currency;

import org.votingsystem.model.BatchVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;

import javax.persistence.*;
import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("CurrencyRequestBatch")
public class CurrencyRequestBatch extends BatchVS implements Serializable  {

    public static final long serialVersionUID = 1L;

    @OneToOne private MessageSMIME messageSMIME;
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tagVS", nullable=false) private TagVS tagVS;
    @Column(name="timeLimited", nullable=false) private Boolean timeLimited;

    public CurrencyRequestBatch() {}

    public MessageSMIME getMessageSMIME() {
        return messageSMIME;
    }

    public void setMessageSMIME(MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
    }

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

}