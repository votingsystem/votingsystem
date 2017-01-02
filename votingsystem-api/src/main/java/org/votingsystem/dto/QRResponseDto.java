package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "QRResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QRResponseDto {

    @JacksonXmlProperty(localName = "Date", isAttribute = true)
    private ZonedDateTime date;
    @JacksonXmlProperty(localName = "Type", isAttribute = true)
    private OperationType operationType;
    @JacksonXmlProperty(localName = "Base64Data")
    private String base64Data;
    
    public QRResponseDto() {}
    
    public QRResponseDto(OperationType operationType, LocalDateTime localDateTime) {
        this.operationType = operationType;
        date = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
    }
    
    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public QRResponseDto setDateLocalDateTime(LocalDateTime localDateTime) {
        this.date = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
        return this;
    }

    public String getBase64Data() {
        return base64Data;
    }


    public <T> T getDataJson(Class<T> type) throws Exception {
        return JSON.getMapper().readValue(Base64.getDecoder().decode(base64Data), type);
    }

    public <T> T getDataXml(Class<T> type) throws Exception {
        return XML.getMapper().readValue(Base64.getDecoder().decode(base64Data), type);
    }

    @JsonIgnore
    public byte[] getData() {
        return Base64.getDecoder().decode(base64Data);
    }

    public QRResponseDto setBase64Data(String base64Data) {
        this.base64Data = base64Data;
        return this;
    }


    public void setDate(ZonedDateTime date) {
        this.date = date;
    }
}
