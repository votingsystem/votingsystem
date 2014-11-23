package org.votingsystem.model;

import org.json.JSONObject;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.DateUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.mail.Header;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AnonymousDelegationVS extends ReceiptContainer {

    private static final long serialVersionUID = 1L;

    public static final String TAG = AnonymousDelegationVS.class.getSimpleName();

    private Long localId = -1L;
    private transient SMIMEMessage delegationReceipt;
    private transient byte[] delegationReceiptBytes;
    private String originHashCertVS;
    private String hashCertVSBase64;
    private String representativeNif;
    private String representativeName;
    private Integer weeksOperationActive;
    private String serverURL;
    private CertificationRequestVS certificationRequest;
    private UserVS representative;
    private Date dateFrom;
    private Date dateTo;

    public SMIMEMessage getCancelVoteReceipt() {
        if(delegationReceipt == null && delegationReceiptBytes != null) {
            try {
                delegationReceipt = new SMIMEMessage(new ByteArrayInputStream(delegationReceiptBytes));
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return delegationReceipt;
    }

    public AnonymousDelegationVS(Integer weeksOperationActive, Date dateFrom, Date dateTo,
                 String serverURL) throws Exception {
        this.setWeeksOperationActive(weeksOperationActive);
        this.serverURL = serverURL;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        originHashCertVS = UUID.randomUUID().toString();
        hashCertVSBase64 = CMSUtils.getHashBase64(getOriginHashCertVS(),
                ContextVS.VOTING_DATA_DIGEST);
        certificationRequest = CertificationRequestVS.getAnonymousDelegationRequest(
                ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM,
                ContextVS.PROVIDER, serverURL, hashCertVSBase64, weeksOperationActive.toString());
        setTypeVS(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION);
    }

    @Override public String getSubject() {
        return null;
    }

    @Override public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    @Override public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    @Override public Long getLocalId() {
        return localId;
    }

    @Override public void setLocalId(Long localId) {
        this.localId = localId;
    }

    @Override public SMIMEMessage getReceipt() {
        return delegationReceipt;
    }

    @Override public String getMessageId() {
        String result = null;
        try {
            SMIMEMessage receipt = getReceipt();
            String[] headers = receipt.getHeader("Message-ID");
            if(headers != null && headers.length > 0) return headers[0];
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return result;
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

    public void setDelegationReceipt(SMIMEMessage delegationReceipt) {
        this.delegationReceipt = delegationReceipt;
    }

    public String getRepresentativeNif() {
        return representativeNif;
    }

    public void setRepresentativeNif(String representativeNif) {
        this.representativeNif = representativeNif;
    }

    public String getRepresentativeName() {
        return representativeName;
    }

    public void setRepresentativeName(String representativeName) {
        this.representativeName = representativeName;
    }

    public Integer getWeeksOperationActive() {
        return weeksOperationActive;
    }

    public void setWeeksOperationActive(Integer weeksOperationActive) {
        this.weeksOperationActive = weeksOperationActive;
    }

    public UserVS getRepresentative() {
        return representative;
    }

    public void setRepresentative(UserVS representative) {
        this.representative = representative;
    }

    public JSONObject getRequest() {
        Map result = new HashMap();
        result.put("weeksOperationActive", getWeeksOperationActive());
        result.put("dateFrom", DateUtils.getDateStr(dateFrom));
        result.put("dateTo", DateUtils.getDateStr(dateTo));
        result.put("accessControlURL", serverURL);
        result.put("operation", TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST.toString());
        result.put("UUID", UUID.randomUUID().toString());
        return new JSONObject(result);
    }

    public JSONObject getCancellationRequest() {
        Map result = new HashMap();
        result.put("weeksOperationActive", getWeeksOperationActive());
        result.put("dateFrom", DateUtils.getDateStr(dateFrom));
        result.put("dateTo", DateUtils.getDateStr(dateTo));
        result.put("accessControlURL", serverURL);
        result.put("operation", TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED);
        result.put("hashCertVSBase64", hashCertVSBase64);
        result.put("originHashCertVSBase64", originHashCertVS);
        result.put("UUID", UUID.randomUUID().toString());
        return new JSONObject(result);
    }

    public JSONObject getDelegation(String representativeNif, String representativeName) {
        Map result = new HashMap();
        result.put("representativeNif", representativeNif);
        result.put("representativeName", representativeName);
        result.put("weeksOperationActive", getWeeksOperationActive());
        result.put("dateFrom", DateUtils.getDateStr(dateFrom));
        result.put("dateTo", DateUtils.getDateStr(dateTo));
        result.put("accessControlURL", serverURL);
        result.put("operation", TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION.toString());
        result.put("UUID", UUID.randomUUID().toString());
        return new JSONObject(result);
    }

    public static AnonymousDelegationVS parse(JSONObject jsonObject) throws Exception {
        Date dateFrom = DateUtils.getDateFromString(jsonObject.getString("dateFrom"));
        Date dateTo = DateUtils.getDateFromString(jsonObject.getString("dateTo"));
        AnonymousDelegationVS result = new AnonymousDelegationVS(jsonObject.getInt("weeksOperationActive"),
                dateFrom, dateTo, jsonObject.getString("accessControlURL"));
        if(jsonObject.has("representativeNif"))
            result.setRepresentativeNif(jsonObject.getString("representativeNif"));
        if(jsonObject.has("representativeName"))
            result.setRepresentativeName(jsonObject.getString("representativeName"));
        return result;
    }

}