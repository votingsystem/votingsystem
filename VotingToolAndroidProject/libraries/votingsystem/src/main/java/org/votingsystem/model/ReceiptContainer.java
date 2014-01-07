package org.votingsystem.model;


import java.util.Date;

public interface ReceiptContainer {

    public enum State {ACTIVE, CANCELLED}

    public String getSubject();
    public TypeVS getType();
    public Date getValidFrom();
    public Date getValidTo();
    public Long getLocalId();
    public void setLocalId(Long localId);

}
