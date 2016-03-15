package org.votingsystem.model.currency;

import org.votingsystem.model.Batch;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.TagVS;

import javax.persistence.*;
import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("CurrencyRequestBatch")
public class CurrencyRequestBatch extends Batch implements Serializable  {

    public static final long serialVersionUID = 1L;

    @OneToOne private CMSMessage cmsMessage;
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tagVS", nullable=false) private TagVS tagVS;
    @Column(name="timeLimited", nullable=false) private Boolean timeLimited;

    public CurrencyRequestBatch() {}

    public CMSMessage getCmsMessage() {
        return cmsMessage;
    }

    public void setCmsMessage(CMSMessage cmsMessage) {
        this.cmsMessage = cmsMessage;
    }

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

}