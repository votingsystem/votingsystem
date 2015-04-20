package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.currency.Wallet;

import javax.mail.Header;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SocketMessageContentDto {

    private TypeVS operation;
    private Integer statusCode;
    private String subject;
    private String locale;
    private String message;
    private String from;
    private String deviceFromName;
    private Long deviceFromId;
    private String textToSign;
    private String toUser;
    private String smimeMessage;
    private Map<String, String> headers;
    private List<Map> currencyList;
    private String URL;

    public SocketMessageContentDto() {}

    public SocketMessageContentDto(TypeVS operation, Integer statusCode, String message, String URL) {
        this.operation = operation;
        this.statusCode = statusCode;
        this.message = message;
        this.URL = URL;
    }

    public static SocketMessageContentDto getSignRequest(DeviceVS deviceVS, String toUser, String textToSign,
            String subject, Header... headers) throws Exception {
        SocketMessageContentDto messageContentDto =  new SocketMessageContentDto();
        messageContentDto.setOperation(TypeVS.MESSAGEVS_SIGN);
        messageContentDto.setDeviceFromName(InetAddress.getLocalHost().getHostName());
        messageContentDto.setToUser(toUser);
        messageContentDto.setTextToSign(textToSign);
        messageContentDto.setSubject(subject);
        messageContentDto.setLocale(ContextVS.getInstance().getLocale().getLanguage());
        if(headers != null) {
            Map<String, String> messageHeaders = new HashMap<>();
            for(Header header : headers) {
                if (header != null) {
                    messageHeaders.put(header.getName(), header.getValue());
                }
            }
            messageContentDto.setHeaders(messageHeaders);
        }
        return messageContentDto;
    }

    public static SocketMessageContentDto getCurrencyWalletChangeRequest(List<Currency> currencyList) throws Exception {
        SocketMessageContentDto messageContentDto = new SocketMessageContentDto();
        messageContentDto.setOperation(TypeVS.CURRENCY_WALLET_CHANGE);
        messageContentDto.setDeviceFromName(InetAddress.getLocalHost().getHostName());
        messageContentDto.setDeviceFromId(ContextVS.getInstance().getDeviceId());
        messageContentDto.setLocale(ContextVS.getInstance().getLocale().getLanguage());
        messageContentDto.setCurrencyList(Wallet.getCertificationRequestSerialized(currencyList));
        return messageContentDto;
    }

    public static SocketMessageContentDto getMessageVSToDevice(
            UserVS userVS, String toUser, String message) throws Exception {
        SocketMessageContentDto messageContentDto = new SocketMessageContentDto();
        messageContentDto.setOperation(TypeVS.MESSAGEVS);
        messageContentDto.setFrom(userVS.getFullName());
        messageContentDto.setDeviceFromName(InetAddress.getLocalHost().getHostName());
        messageContentDto.setDeviceFromId(ContextVS.getInstance().getDeviceId());
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

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Long getDeviceFromId() {
        return deviceFromId;
    }

    public void setDeviceFromId(Long deviceFromId) {
        this.deviceFromId = deviceFromId;
    }

    public List<Map> getCurrencyList() {
        return currencyList;
    }

    public void setCurrencyList(List<Map> currencyList) {
        this.currencyList = currencyList;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(String smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    @JsonIgnore
    SMIMEMessage getSMIME () throws Exception {
        byte[] smimeMessageBytes = Base64.getDecoder().decode(smimeMessage.getBytes());
        return new SMIMEMessage(new ByteArrayInputStream(smimeMessageBytes));
    }

}
