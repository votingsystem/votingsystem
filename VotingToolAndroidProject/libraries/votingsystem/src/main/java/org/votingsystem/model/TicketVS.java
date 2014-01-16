package org.votingsystem.model;

import org.votingsystem.signature.smime.CMSUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TicketVS implements java.io.Serializable, ReceiptContainer {

    private static final long serialVersionUID = 1L;

    public static final String TAG = "TicketVS";

    private String originHashCertVS;
    private String hashCertVSBase64;
    private BigDecimal amount;
    private TypeVS typeVS;

    public TicketVS(BigDecimal amount, TypeVS typeVS) {
        this.setAmount(amount);
        this.setTypeVS(typeVS);
        try {
            setOriginHashCertVS(UUID.randomUUID().toString());
            setHashCertVSBase64(CMSUtils.getHashBase64(getOriginHashCertVS(), ContextVS.VOTING_DATA_DIGEST));
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public String getSubject() {
        return null;
    }

    @Override public TypeVS getType() {
        return null;
    }

    @Override public Date getValidFrom() {
        return null;
    }

    @Override public Date getValidTo() {
        return null;
    }

    @Override public Long getLocalId() {
        return null;
    }

    @Override public void setLocalId(Long localId) {

    }


    public String getOriginHashCertVS() {
        return originHashCertVS;
    }

    public void setOriginHashCertVS(String originHashCertVS) {
        this.originHashCertVS = originHashCertVS;
    }

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public void setHashCertVSBase64(String hashCertVSBase64) {
        this.hashCertVSBase64 = hashCertVSBase64;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }
}