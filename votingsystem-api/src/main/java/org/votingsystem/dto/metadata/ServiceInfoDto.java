package org.votingsystem.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "service")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceInfoDto {

    @JacksonXmlProperty(localName = "Name", isAttribute = true)
    private String name;
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "url")
    private Set<ServiceURLDto> serviceURLSet;

    public ServiceInfoDto() {}

    public ServiceInfoDto(String name, Set<ServiceURLDto> serviceURLSet) {
        this.name = name;
        this.serviceURLSet = serviceURLSet;
    }

    public String getName() {
        return name;
    }

    public Set<ServiceURLDto> getServiceURLSet() {
        return serviceURLSet;
    }

}
