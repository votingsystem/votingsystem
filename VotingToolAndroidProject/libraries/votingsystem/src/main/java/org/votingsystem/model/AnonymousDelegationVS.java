package org.votingsystem.model;

import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertificationRequestVS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.UUID;

import javax.mail.Header;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class AnonymousDelegationVS implements java.io.Serializable, ReceiptContainer {

    private static final long serialVersionUID = 1L;

    public static final String TAG = "AnonymousDelegationVS";

    private String originHashCertVS;
    private String hashCertVSBase64;
    private String weeksOperationActive;
    private String serverURL;
    private CertificationRequestVS certificationRequest;
    private Header header;
    private TypeVS type;
    private transient SMIMEMessageWrapper delegationReceipt;
    private transient byte[] delegationReceiptBytes;

    public SMIMEMessageWrapper getCancelVoteReceipt() {
        if(delegationReceipt == null && delegationReceiptBytes != null) {
            try {
                delegationReceipt = new SMIMEMessageWrapper(null,
                        new ByteArrayInputStream(delegationReceiptBytes), null);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return delegationReceipt;
    }

    public AnonymousDelegationVS(String weeksOperationActive, String serverURL) throws Exception {
        this.weeksOperationActive = weeksOperationActive;
        this.serverURL = serverURL;
        originHashCertVS = UUID.randomUUID().toString();
        hashCertVSBase64 = CMSUtils.getHashBase64(getOriginHashCertVS(),
                ContextVS.VOTING_DATA_DIGEST);
        certificationRequest = CertificationRequestVS.getAnonymousDelegationRequest(
                ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM,
                ContextVS.PROVIDER, serverURL, hashCertVSBase64, weeksOperationActive);
    }

    @Override public String getSubject() {
        return null;
    }

    @Override public TypeVS getType() {
        return TypeVS.ANONYMOUS_DELEGATION;
    }

    public void setType(TypeVS type) {
        this.type = type;
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

    public String getHashCertVS() {
        return hashCertVSBase64;
    }

    public CertificationRequestVS getCertificationRequest() {
        return certificationRequest;
    }

    public Header getHeader() {
        return header;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(delegationReceipt != null) s.writeObject(delegationReceipt.getBytes());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        delegationReceiptBytes = (byte[]) s.readObject();
    }

    public void setDelegationReceipt(SMIMEMessageWrapper delegationReceipt) {
        this.delegationReceipt = delegationReceipt;
    }
}