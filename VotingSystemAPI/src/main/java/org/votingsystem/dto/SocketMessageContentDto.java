package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import java.net.InetAddress;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SocketMessageContentDto {

    private TypeVS operation;
    private Integer statusCode;
    private String subject;
    private String locale = ContextVS.getInstance().getLocale().getLanguage();;
    private String message;
    private String from;
    private String deviceFromName;
    private String deviceToName;
    private Long deviceFromId;
    private String sessionId;
    private String textToSign;
    private String toUser;
    private String hashCertVS;
    private String cmsMessage;
    private Set<CurrencyDto> currencyList;
    private String URL;


    public SocketMessageContentDto() {}

    public SocketMessageContentDto(TypeVS operation, Integer statusCode, String message, String URL) {
        this.operation = operation;
        this.statusCode = statusCode;
        this.message = message;
        this.URL = URL;
    }

    public static SocketMessageContentDto getSignRequest(String toUser, String textToSign, String subject) throws Exception {
        SocketMessageContentDto messageContentDto =  new SocketMessageContentDto();
        messageContentDto.setOperation(TypeVS.MESSAGEVS_SIGN);
        messageContentDto.setDeviceFromName(InetAddress.getLocalHost().getHostName());
        messageContentDto.setToUser(toUser);
        messageContentDto.setTextToSign(textToSign);
        messageContentDto.setSubject(subject);
        messageContentDto.setLocale(ContextVS.getInstance().getLocale().getLanguage());
        return messageContentDto;
    }

    public static SocketMessageContentDto getCurrencyWalletChangeRequest(List<Currency> currencyList) throws Exception {
        SocketMessageContentDto messageContentDto = new SocketMessageContentDto();
        messageContentDto.setOperation(TypeVS.CURRENCY_WALLET_CHANGE);
        messageContentDto.setDeviceFromName(InetAddress.getLocalHost().getHostName());
        messageContentDto.setDeviceFromId(ContextVS.getInstance().getConnectedDevice().getId());
        messageContentDto.setLocale(ContextVS.getInstance().getLocale().getLanguage());
        messageContentDto.setCurrencyList(CurrencyDto.serializeCollection(currencyList));
        return messageContentDto;
    }

    public static SocketMessageContentDto getMessageVSToDevice(
            UserVS userVS, String toUser, String message) throws Exception {
        SocketMessageContentDto messageContentDto = new SocketMessageContentDto();
        messageContentDto.setOperation(TypeVS.MESSAGEVS);
        messageContentDto.setFrom(userVS.getFullName());
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

    public String getTextToSign() {
        return textToSign;
    }

    public void setTextToSign(String textToSign) {
        this.textToSign = textToSign;
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
        byte[] cmsMessageBytes = Base64.getDecoder().decode(cmsMessage.getBytes());
        return new CMSSignedMessage(cmsMessageBytes);
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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
