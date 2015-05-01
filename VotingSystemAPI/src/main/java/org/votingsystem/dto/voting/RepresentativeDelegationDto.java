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
public class RepresentativeDelegationDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private TypeVS operation;
    private String originHashCertVS;
    private String hashCertVSBase64;
    private Integer weeksOperationActive;
    private String serverURL;
    private UserVSDto representative;
    private Date dateFrom;
    private Date dateTo;
    private String UUID;

    @JsonIgnore private transient SMIMEMessage receipt;
    @JsonIgnore private CertificationRequestVS certificationRequest;

    public RepresentativeDelegationDto() {}

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

    @JsonIgnore
    public String getMessageId() {
        if(receipt == null) return null;
        String result = null;
        try {
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

    @JsonIgnore
    public CertificationRequestVS getCertificationRequest() {
        return certificationRequest;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(receipt != null) s.writeObject(receipt.getBytes());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws Exception {
        s.defaultReadObject();
        byte[] receiptBytes = (byte[]) s.readObject();
        if(receiptBytes != null) receipt = new SMIMEMessage(receiptBytes);
    }

    public void setDelegationReceipt(SMIMEMessage receipt, X509Certificate serverCert) throws Exception {
        Collection matches = receipt.checkSignerCert(serverCert);
        if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
        this.receipt = receipt;
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

    @JsonIgnore
    public RepresentativeDelegationDto getRequest(TypeVS operation) {
        RepresentativeDelegationDto requestDto = new RepresentativeDelegationDto();
        requestDto.setOperation(operation);
        requestDto.setWeeksOperationActive(weeksOperationActive);
        requestDto.setRepresentative(representative);
        requestDto.setServerURL(serverURL);
        requestDto.setHashCertVSBase64(hashCertVSBase64);
        if(dateTo != null) requestDto.setDateTo(new Date(dateTo.getTime()));
        if(dateFrom != null) requestDto.setDateFrom(new Date(dateFrom.getTime()));
        requestDto.setUUID(java.util.UUID.randomUUID().toString());
        return requestDto;
    }

    @JsonIgnore
    public RepresentativeDelegationDto getCancelationRequest() {
        RepresentativeDelegationDto cancelationDto = getRequest(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION);
        cancelationDto.setOriginHashCertVS(originHashCertVS);
        return cancelationDto;
    }

    @JsonIgnore
    public RepresentativeDelegationDto getAnonymousCertRequest() throws NoSuchAlgorithmException, IOException,
            NoSuchProviderException, InvalidKeyException, SignatureException {
        originHashCertVS = java.util.UUID.randomUUID().toString();
        hashCertVSBase64 = CMSUtils.getHashBase64(originHashCertVS, ContextVS.VOTING_DATA_DIGEST);
        dateFrom = DateUtils.getMonday(DateUtils.addDays(7)).getTime();//Next week Monday
        dateTo = DateUtils.addDays(dateFrom, weeksOperationActive * 7).getTime();
        certificationRequest = CertificationRequestVS.getAnonymousDelegationRequest(
                ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM,
                ContextVS.PROVIDER, serverURL, hashCertVSBase64, weeksOperationActive, dateFrom, dateTo);
        RepresentativeDelegationDto requestDto = getRequest(TypeVS.ANONYMOUS_SELECTION_CERT_REQUEST);
        return requestDto;
    }

    @JsonIgnore
    public RepresentativeDelegationDto getDelegation(){
        RepresentativeDelegationDto delegationDto = getRequest(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION);
        return delegationDto;
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

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    @JsonIgnore
    public SMIMEMessage getReceipt() {
        return receipt;
    }

    public void setReceipt(SMIMEMessage receipt) {
        this.receipt = receipt;
    }
}