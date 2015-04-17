package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;

import java.io.*;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnonymousDelegationDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TAG = AnonymousDelegationDto.class.getSimpleName();

    private Long localId = -1L;
    private TypeVS operation;
    private transient SMIMEMessage delegationReceipt;
    private transient byte[] delegationReceiptBytes;
    @JsonIgnore private String originHashCertVSHidden;
    private String originHashCertVS;
    private String hashCertVSBase64;
    private String representativeNif;
    private String representativeName;
    private Integer weeksOperationActive;
    private String accessControlURL;
    private String serverURL;
    @JsonIgnore private CertificationRequestVS certificationRequest;
    private UserVSDto representative;
    private Date dateFrom;
    private Date dateTo;
    private String UUID;

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

    public AnonymousDelegationDto(Integer weeksOperationActive, String representativeNif, String representativeName,
                                  String serverURL) throws Exception {
        this.setWeeksOperationActive(weeksOperationActive);
        this.serverURL = serverURL;
        this.dateFrom = DateUtils.getMonday(DateUtils.addDays(7)).getTime();//Next week Monday
        this.dateTo = DateUtils.addDays(dateFrom, weeksOperationActive * 7).getTime();
        this.representativeName = representativeName;
        this.representativeNif = representativeNif;
        originHashCertVSHidden = java.util.UUID.randomUUID().toString();
        hashCertVSBase64 = CMSUtils.getHashBase64(originHashCertVSHidden, ContextVS.VOTING_DATA_DIGEST);
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

    public UserVSDto getRepresentative() {
        return representative;
    }

    public void setRepresentative(UserVSDto representative) {
        this.representative = representative;
    }

    public AnonymousDelegationDto getRequest() {
        operation = TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST;
        accessControlURL = serverURL;
        UUID = java.util.UUID.randomUUID().toString();
        return this;
    }

    public AnonymousDelegationDto getCancellationRequest() {
        operation = TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELED;
        accessControlURL = serverURL;
        representativeNif = representative.getNIF();
        representativeName = representative.getName();
        originHashCertVS = originHashCertVSHidden;
        UUID = java.util.UUID.randomUUID().toString();
        return this;
    }

    public AnonymousDelegationDto getDelegation() {
        operation = TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION;
        accessControlURL = serverURL;
        UUID = java.util.UUID.randomUUID().toString();
        return this;
    }

    public static AnonymousDelegationDto parse(Map dataMap) throws Exception {
        AnonymousDelegationDto result = new AnonymousDelegationDto((Integer)dataMap.get("weeksOperationActive"),
                null, null, (String)dataMap.get("accessControlURL"));
        if(dataMap.containsKey("representativeNif"))
            result.setRepresentativeNif((String) dataMap.get("representativeNif"));
        if(dataMap.containsKey("representativeName"))
            result.setRepresentativeName((String) dataMap.get("representativeName"));
        result.setDateFrom(DateUtils.getDateFromString((String) dataMap.get("dateFrom")));
        result.setDateTo(DateUtils.getDateFromString((String) dataMap.get("dateTo")));
        return result;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getAccessControlURL() {
        return accessControlURL;
    }

    public void setAccessControlURL(String accessControlURL) {
        this.accessControlURL = accessControlURL;
    }
}