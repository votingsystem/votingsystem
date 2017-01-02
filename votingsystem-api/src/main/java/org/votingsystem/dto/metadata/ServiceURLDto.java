package org.votingsystem.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import org.votingsystem.http.HttpRequest;

/**
 * Created by jgzornoza on 12/10/16.
 */
@JacksonXmlRootElement(localName = "service")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceURLDto {

    @JacksonXmlProperty(localName = "Name", isAttribute = true)
    private String name;
    @JacksonXmlProperty(localName = "HttpMethod", isAttribute = true)
    private HttpRequest.Method httpMethod;
    @JacksonXmlText
    private String url;

    public ServiceURLDto() {}

    public ServiceURLDto(String name, HttpRequest.Method httpMethod, String url) {
        this.name = name;
        this.httpMethod = httpMethod;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public HttpRequest.Method getHttpMethod() {
        return httpMethod;
    }

    public String getUrl() {
        return url;
    }
}
