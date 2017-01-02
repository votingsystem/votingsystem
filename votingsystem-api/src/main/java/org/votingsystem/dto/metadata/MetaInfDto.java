package org.votingsystem.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaInfDto {

    private String reason;
    private String data;

    public MetaInfDto() { }

    public MetaInfDto(String reason, String data) {
        this.reason = reason;
        this.data = data;
    }

    public String getReason() {
        return reason;
    }

    public MetaInfDto setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public String getData() {
        return data;
    }

    public MetaInfDto setData(String data) {
        this.data = data;
        return this;
    }

    public String toXmlStr() throws JsonProcessingException {
        return new XmlMapper().writeValueAsString(this);
    }

    public static MetaInfDto FROM_REASON(String reason) {
        MetaInfDto result = new MetaInfDto();
        result.setReason(reason);
        return result;
    }

    public static MetaInfDto FROM_SIMPLE_REPORT(String xmlSimpleReport) {
        MetaInfDto result = new MetaInfDto();
        result.setData(xmlSimpleReport);
        return result;
    }

}
