package org.votingsystem.util;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import java.io.*;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AnonymousDelegationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TAG = AnonymousDelegationRequest.class.getSimpleName();

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

    public AnonymousDelegationRequest(Integer weeksOperationActive, String representativeNif, String representativeName,
              String serverURL) throws Exception {
        this.setWeeksOperationActive(weeksOperationActive);
        this.serverURL = serverURL;
        this.dateFrom = DateUtils.getMonday(DateUtils.addDays(7)).getTime();//Next week Monday
        this.dateTo = DateUtils.addDays(dateFrom, weeksOperationActive * 7).getTime();
        this.representativeName = representativeName;
        this.representativeNif = representativeNif;
        originHashCertVS = UUID.randomUUID().toString();
        hashCertVSBase64 = CMSUtils.getHashBase64(getOriginHashCertVS(), ContextVS.VOTING_DATA_DIGEST);
        certificationRequest = CertificationRequestVS.getAnonymousDelegationRequest(
                ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM,
                ContextVS.PROVIDER, serverURL, hashCertVSBase64, weeksOperationActive.toString(),
                DateUtils.getDateStr(dateFrom), DateUtils.getDateStr(dateTo));
    }

    public String getSubject() {
        return null;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

    public SMIMEMessage getReceipt() {
        return delegationReceipt;
    }

    public String getMessageId() {
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

    public void setDelegationReceipt(SMIMEMessage delegationReceipt, X509Certificate serverCert) throws Exception {
        Collection matches = delegationReceipt.checkSignerCert(serverCert);
        if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
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
        return (JSONObject) JSONSerializer.toJSON(result);
    }

    public JSONObject getCancellationRequest() {
        Map result = new HashMap();
        result.put("weeksOperationActive", getWeeksOperationActive());
        result.put("dateFrom", DateUtils.getDateStr(dateFrom));
        result.put("dateTo", DateUtils.getDateStr(dateTo));
        result.put("accessControlURL", serverURL);
        result.put("representativeNif", representative.getNif());
        result.put("representativeName", representative.getName());
        result.put("hashCertVSBase64", hashCertVSBase64);
        result.put("originHashCertVSBase64", originHashCertVS);
        result.put("operation", TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED.toString());
        result.put("UUID", UUID.randomUUID().toString());
        return (JSONObject) JSONSerializer.toJSON(result);
    }

    public JSONObject getDelegation() {
        Map result = new HashMap();
        result.put("representativeNif", representativeNif);
        result.put("representativeName", representativeName);
        result.put("weeksOperationActive", getWeeksOperationActive());
        result.put("dateFrom", DateUtils.getDateStr(dateFrom));
        result.put("dateTo", DateUtils.getDateStr(dateTo));
        result.put("accessControlURL", serverURL);
        result.put("operation", TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION.toString());
        result.put("UUID", UUID.randomUUID().toString());
        return (JSONObject) JSONSerializer.toJSON(result);
    }

    public static AnonymousDelegationRequest parse(JSONObject jsonObject) throws Exception {
        AnonymousDelegationRequest result = new AnonymousDelegationRequest(jsonObject.getInt("weeksOperationActive"),
                null, null, jsonObject.getString("accessControlURL"));
        if(jsonObject.has("representativeNif"))
            result.setRepresentativeNif(jsonObject.getString("representativeNif"));
        if(jsonObject.has("representativeName"))
            result.setRepresentativeName(jsonObject.getString("representativeName"));
        result.setDateFrom(DateUtils.getDateFromString(jsonObject.getString("dateFrom")));
        result.setDateTo(DateUtils.getDateFromString(jsonObject.getString("dateTo")));
        return result;
    }

}