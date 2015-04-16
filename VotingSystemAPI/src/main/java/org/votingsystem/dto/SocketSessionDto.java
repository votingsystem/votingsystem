package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import java.util.Base64;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SocketSessionDto {

    private TypeVS operation;
    private String deviceFromId;
    private String smimeMessage;
    private String locale;
    private String UUID;

    public SocketSessionDto() {}

    public static SocketSessionDto INIT_SESSION_REQUEST(String deviceFromId) {
        SocketSessionDto socketSessionDto = new SocketSessionDto();
        socketSessionDto.setUUID(java.util.UUID.randomUUID().toString());
        socketSessionDto.setOperation(TypeVS.INIT_VALIDATED_SESSION);
        socketSessionDto.setDeviceFromId(deviceFromId);
        return socketSessionDto;
    }

    public SocketSessionDto getInitConnectionMsg(SMIMEMessage smimeMessage) throws Exception {
        setLocale(ContextVS.getInstance().getLocale().getLanguage());
        setSmimeMessage(Base64.getEncoder().encodeToString(smimeMessage.getBytes()));
        return this;
    }


    public SocketSessionDto clone() {
        SocketSessionDto dto = new SocketSessionDto();
        dto.setOperation(operation);
        dto.setDeviceFromId(deviceFromId);
        dto.setSmimeMessage(smimeMessage);
        dto.setLocale(locale);
        dto.setUUID(UUID);
        return dto;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getDeviceFromId() {
        return deviceFromId;
    }

    public void setDeviceFromId(String deviceFromId) {
        this.deviceFromId = deviceFromId;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(String smimeMessage) {
        this.smimeMessage = smimeMessage;
    }
}
