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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnonymousDelegationDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private TypeVS operation;
    private String originHashCertVS;
    private String hashCertVSBase64;
    private Integer weeksOperationActive;
    private String serverURL;
    private UserVSDto representative;
    private Date dateFrom;
    private Date dateTo;
    private transient SMIMEMessage delegationReceipt;
    private String UUID;

    @JsonIgnore private CertificationRequestVS certificationRequest;

    public AnonymousDelegationDto() {}

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

    private void readObject(ObjectInputStream s) throws Exception {
        s.defaultReadObject();
        byte[] delegationReceiptBytes = (byte[]) s.readObject();
        if(delegationReceiptBytes != null) delegationReceipt = new SMIMEMessage(delegationReceiptBytes);
    }

    public void setDelegationReceipt(SMIMEMessage delegationReceipt, X509Certificate serverCert) throws Exception {
        Collection matches = delegationReceipt.checkSignerCert(serverCert);
        if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
        this.delegationReceipt = delegationReceipt;
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

    public AnonymousDelegationDto getRequest(TypeVS operation) {
        AnonymousDelegationDto requestDto = new AnonymousDelegationDto();
        requestDto.setOperation(operation);
        requestDto.setWeeksOperationActive(weeksOperationActive);
        requestDto.setRepresentative(representative);
        requestDto.setServerURL(serverURL);
        requestDto.setHashCertVSBase64(hashCertVSBase64);
        requestDto.setDateTo(new Date(dateTo.getTime()));
        requestDto.setDateFrom(new Date(dateFrom.getTime()));
        requestDto.setUUID(java.util.UUID.randomUUID().toString());
        return requestDto;
    }

    public AnonymousDelegationDto getCancelationRequest() {
        AnonymousDelegationDto cancelationDto = getRequest(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION);
        cancelationDto.setOriginHashCertVS(originHashCertVS);
        return cancelationDto;
    }


    public AnonymousDelegationDto getAnonymousCertRequest() throws NoSuchAlgorithmException, IOException,
            NoSuchProviderException, InvalidKeyException, SignatureException {
        originHashCertVS = java.util.UUID.randomUUID().toString();
        hashCertVSBase64 = CMSUtils.getHashBase64(originHashCertVS, ContextVS.VOTING_DATA_DIGEST);
        dateFrom = DateUtils.getMonday(DateUtils.addDays(7)).getTime();//Next week Monday
        dateTo = DateUtils.addDays(dateFrom, weeksOperationActive * 7).getTime();
        certificationRequest = CertificationRequestVS.getAnonymousDelegationRequest(
                ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM,
                ContextVS.PROVIDER, serverURL, hashCertVSBase64, weeksOperationActive, dateFrom, dateTo);
        AnonymousDelegationDto requestDto = getRequest(TypeVS.ANONYMOUS_SELECTION_CERT_REQUEST);
        return requestDto;
    }

    public AnonymousDelegationDto getDelegation(){
        AnonymousDelegationDto delegationDto = getRequest(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION);
        return delegationDto;
    }

    public SMIMEMessage getDelegationReceipt() {
        return delegationReceipt;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
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

    public void setDelegationReceipt(SMIMEMessage delegationReceipt) {
        this.delegationReceipt = delegationReceipt;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

}