package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.bouncycastle.operator.OperatorCreationException;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.UserDto;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.CertificationRequestVS;

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
    private String originHashAnonymousDelegation;
    private String hashAnonymousDelegation;
    private String anonymousDelegationRequestBase64ContentDigest;
    private Integer weeksOperationActive;
    private String serverURL;
    private UserDto representative;
    private Date dateFrom;
    private Date dateTo;
    private String UUID;

    @JsonIgnore private transient CMSSignedMessage receipt;
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
            if(receipt != null) s.writeObject(receipt.toASN1Structure().getEncoded());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws Exception {
        s.defaultReadObject();
        byte[] receiptBytes = (byte[]) s.readObject();
        if(receiptBytes != null) receipt = new CMSSignedMessage(receiptBytes);
    }

    public void setDelegationReceipt(CMSSignedMessage receipt, X509Certificate serverCert) throws Exception {
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

    public UserDto getRepresentative() {
        return representative;
    }

    public void setRepresentative(UserDto representative) {
        this.representative = representative;
    }

    @JsonIgnore
    public RepresentativeDelegationDto getRequest(TypeVS operation) {
        RepresentativeDelegationDto requestDto = new RepresentativeDelegationDto();
        requestDto.setOperation(operation);
        requestDto.setWeeksOperationActive(weeksOperationActive);
        requestDto.setServerURL(serverURL);
        if(dateTo != null) requestDto.setDateTo(new Date(dateTo.getTime()));
        if(dateFrom != null) requestDto.setDateFrom(new Date(dateFrom.getTime()));
        requestDto.setUUID(java.util.UUID.randomUUID().toString());
        return requestDto;
    }

    @JsonIgnore
    public RepresentativeDelegationDto getAnonymousCancelationRequest() {
        RepresentativeDelegationDto cancelationDto = getRequest(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION);
        cancelationDto.setOriginHashAnonymousDelegation(originHashAnonymousDelegation);
        cancelationDto.setHashAnonymousDelegation(hashAnonymousDelegation);
        return cancelationDto;
    }

    @JsonIgnore
    public RepresentativeDelegationDto getAnonymousRepresentationDocumentCancelationRequest() {
        RepresentativeDelegationDto cancelationDto = getRequest(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION);
        cancelationDto.setOriginHashCertVS(originHashCertVS);
        cancelationDto.setHashCertVSBase64(hashCertVSBase64);
        return cancelationDto;
    }

    @JsonIgnore
    public RepresentativeDelegationDto getAnonymousCertRequest() throws NoSuchAlgorithmException, IOException,
            NoSuchProviderException, InvalidKeyException, SignatureException, OperatorCreationException {
        originHashCertVS = java.util.UUID.randomUUID().toString();
        hashCertVSBase64 = StringUtils.getHashBase64(originHashCertVS, ContextVS.DATA_DIGEST_ALGORITHM);
        originHashAnonymousDelegation = java.util.UUID.randomUUID().toString();
        hashAnonymousDelegation = StringUtils.getHashBase64(originHashAnonymousDelegation, ContextVS.DATA_DIGEST_ALGORITHM);
        dateFrom = DateUtils.getMonday(DateUtils.addDays(7)).getTime();//Next week Monday
        dateTo = DateUtils.addDays(dateFrom, weeksOperationActive * 7).getTime();
        certificationRequest = CertificationRequestVS.getAnonymousDelegationRequest(ContextVS.SIGNATURE_ALGORITHM,
                ContextVS.PROVIDER, serverURL, hashCertVSBase64, weeksOperationActive, dateFrom, dateTo);
        RepresentativeDelegationDto requestDto = getRequest(TypeVS.ANONYMOUS_SELECTION_CERT_REQUEST);
        requestDto.setHashAnonymousDelegation(hashAnonymousDelegation);
        return requestDto;
    }

    @JsonIgnore
    public RepresentativeDelegationDto getDelegation(){
        RepresentativeDelegationDto delegationDto = getRequest(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION);
        delegationDto.setRepresentative(representative);
        delegationDto.getRepresentative().setDescription(null);
        delegationDto.getRepresentative().setCertCollection(null);
        delegationDto.setHashCertVSBase64(hashCertVSBase64);
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
    public CMSSignedMessage getReceipt() {
        return receipt;
    }

    public void setReceipt(CMSSignedMessage receipt) {
        this.receipt = receipt;
    }

    public String getOriginHashAnonymousDelegation() {
        return originHashAnonymousDelegation;
    }

    public void setOriginHashAnonymousDelegation(String originHashAnonymousDelegation) {
        this.originHashAnonymousDelegation = originHashAnonymousDelegation;
    }

    public String getHashAnonymousDelegation() {
        return hashAnonymousDelegation;
    }

    public void setHashAnonymousDelegation(String hashAnonymousDelegation) {
        this.hashAnonymousDelegation = hashAnonymousDelegation;
    }

    public String getAnonymousDelegationRequestBase64ContentDigest() {
        return anonymousDelegationRequestBase64ContentDigest;
    }

    public void setAnonymousDelegationRequestBase64ContentDigest(String anonymousDelegationRequestBase64ContentDigest) {
        this.anonymousDelegationRequestBase64ContentDigest = anonymousDelegationRequestBase64ContentDigest;
    }
}