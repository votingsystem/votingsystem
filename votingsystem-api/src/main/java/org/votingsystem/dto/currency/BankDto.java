package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.throwable.ValidationException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "Bank")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BankDto {

    @JacksonXmlProperty(localName = "EntityId", isAttribute = true)
    private String entityId;
    private String IBAN;
    private String UUID;
    @JacksonXmlProperty(localName = "X509Certificate")
    private String x509Certificate;
    @JacksonXmlProperty(localName = "Info")
    private String info;

    public BankDto() { }

    public BankDto(String IBAN, String info, String x509Certificate, String entityId) {
        this.IBAN= IBAN;
        this.info = info;
        this.x509Certificate = x509Certificate;
        this.entityId = entityId;
    }

    public void validatePublishRequest(String entityId) throws ValidationException {
        if(entityId == null)
            throw new ValidationException("missing param 'entityId'");
        if(IBAN == null)
            throw new ValidationException("missing param 'IBAN'");
        if(info == null)
            throw new ValidationException("missing param 'info'");
        if(x509Certificate == null)
            throw new ValidationException("missing param 'x509Certificate'");
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(String x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }
}