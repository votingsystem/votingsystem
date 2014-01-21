package org.votingsystem.model;

import org.votingsystem.signature.smime.SMIMEMessageWrapper;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public abstract class ReceiptContainer implements Serializable {

    public enum State {ACTIVE, CANCELLED}


    public abstract String getSubject();

    public abstract TypeVS getType() ;

    public abstract Date getValidFrom();

    public abstract Date getValidTo();

    public abstract Long getLocalId();

    public abstract void setLocalId(Long localId);

    public abstract SMIMEMessageWrapper getReceipt() throws Exception;

    public abstract String getMessageId();

}
