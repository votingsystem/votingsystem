package org.votingsystem.model;


import org.votingsystem.signature.smime.SMIMEMessageWrapper;

import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public interface ReceiptContainer {

    public enum State {ACTIVE, CANCELLED}

    public String getSubject();
    public TypeVS getType();
    public Date getValidFrom();
    public Date getValidTo();
    public Long getLocalId();
    public void setLocalId(Long localId);
    public SMIMEMessageWrapper getReceipt();

}
