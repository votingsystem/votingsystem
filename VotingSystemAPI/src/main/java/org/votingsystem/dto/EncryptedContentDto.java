package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.PEMUtils;

import java.net.InetAddress;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptedContentDto {

    private TypeVS operation;
    private String operationCode;
    private TypeVS step;
    private Integer statusCode;
    private String subject;
    private String locale = ContextVS.getInstance().getLocale().getLanguage();;
    private String message;
    private String from;
    private String deviceFromName;
    private String deviceToName;
    private Long deviceFromId;
    private String toUser;
    private String hashCertVS;
    private String cmsMessage;
    private String x509CertificatePEM;
    private String publicKeyPEM;
    private boolean timeLimited;
    private Set<CurrencyDto> currencyList;
    private String URL;
    private String UUID;


    public EncryptedContentDto() {}

    public EncryptedContentDto(TypeVS operation, Integer statusCode, String message, String URL) {
        this.operation = operation;
        this.statusCode = statusCode;
        this.message = message;
        this.URL = URL;
    }

    public static EncryptedContentDto getCurrencyWalletChangeRequest(List<Currency> currencyList) throws Exception {
        EncryptedContentDto messageContentDto = new EncryptedContentDto();
        messageContentDto.setOperation(TypeVS.CURRENCY_WALLET_CHANGE);
        messageContentDto.setDeviceFromName(InetAddress.getLocalHost().getHostName());
        messageContentDto.setDeviceFromId(ContextVS.getInstance().getConnectedDevice().getId());
        messageContentDto.setLocale(ContextVS.getInstance().getLocale().getLanguage());
        messageContentDto.setCurrencyList(CurrencyDto.serializeCollection(currencyList));
        return messageContentDto;
    }

    public static EncryptedContentDto getMessageVSToDevice(
            User user, String toUser, String message) throws Exception {
        EncryptedContentDto messageContentDto = new EncryptedContentDto();
        messageContentDto.setOperation(TypeVS.MESSAGEVS);
        messageContentDto.setFrom(user.getFullName());
        messageContentDto.setDeviceFromName(InetAddress.getLocalHost().getHostName());
        messageContentDto.setDeviceFromId(ContextVS.getInstance().getConnectedDevice().getId());
        messageContentDto.setToUser(toUser);
        messageContentDto.setMessage(message);
        return messageContentDto;
    }


    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getDeviceFromName() {
        return deviceFromName;
    }

    public void setDeviceFromName(String deviceFromName) {
        this.deviceFromName = deviceFromName;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public Long getDeviceFromId() {
        return deviceFromId;
    }

    public void setDeviceFromId(Long deviceFromId) {
        this.deviceFromId = deviceFromId;
    }

    public Set<CurrencyDto> getCurrencyList() {
        return currencyList;
    }

    public void setCurrencyList(Set<CurrencyDto> currencyList) {
        this.currencyList = currencyList;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getCmsMessage() {
        return cmsMessage;
    }

    public void setCMSMessage(String cmsMessage) {
        this.cmsMessage = cmsMessage;
    }

    @JsonIgnore
    CMSSignedMessage getCMS () throws Exception {
        return CMSSignedMessage.FROM_PEM(cmsMessage);
    }

    public String getDeviceToName() {
        return deviceToName;
    }

    public void setDeviceToName(String deviceToName) {
        this.deviceToName = deviceToName;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public X509Certificate getX509Certificate() throws Exception {
        return PEMUtils.fromPEMToX509Cert(x509CertificatePEM.getBytes())    ;
    }

    public String getX509CertificatePEM() {
        return x509CertificatePEM;
    }

    public void setX509CertificatePEM(String x509CertificatePEM) {
        this.x509CertificatePEM = x509CertificatePEM;
    }

    public boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public PublicKey getPublicKey() throws Exception {
        return PEMUtils.fromPEMToRSAPublicKey(publicKeyPEM);
    }

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public void setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
    }

    public TypeVS getStep() {
        return step;
    }

    public void setStep(TypeVS step) {
        this.step = step;
    }
}
