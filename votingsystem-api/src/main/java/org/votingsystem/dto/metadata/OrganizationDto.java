package org.votingsystem.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "Organization")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationDto {

    @JacksonXmlProperty(localName = "OrganizationName")
    private String organizationName;
    @JacksonXmlProperty(localName = "OrganizationUnit")
    private String organizationUnit;
    @JacksonXmlProperty(localName = "OrganizationURL")
    private String organizationURL;

    public OrganizationDto(){}

    public OrganizationDto(String organizationName, String organizationUnit, String organizationURL) {
        this.organizationName = organizationName;
        this.organizationUnit = organizationUnit;
        this.organizationURL = organizationURL;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getOrganizationURL() {
        return organizationURL;
    }

    public String getOrganizationUnit() {
        return organizationUnit;
    }
}
