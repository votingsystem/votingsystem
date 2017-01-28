package org.votingsystem.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.dto.UserDto;
import org.votingsystem.http.SystemEntityType;

@JacksonXmlRootElement(localName = "entity")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemEntityDto {

    @JacksonXmlProperty(localName = "Type", isAttribute = true)
    private SystemEntityType entityType;

    @JacksonXmlProperty(localName = "Id", isAttribute = true)
    private String id;

    @JacksonXmlProperty(localName = "Organization")
    private OrganizationDto organization;

    @JacksonXmlProperty(localName = "Location")
    private LocationDto location;

    @JacksonXmlProperty(localName = "ContactPerson")
    private UserDto contactPerson;


    public SystemEntityDto() {}

    public SystemEntityDto(String id, SystemEntityType entityType) {
        this.id = id;
        this.entityType = entityType;
    }

    @JsonIgnore
    public SystemEntityType getSystemEntityType() {
        return entityType;
    }

    public String getId() {
        return id;
    }

    public SystemEntityDto setId(String id) {
        this.id = id;
        return this;
    }

    public SystemEntityType getEntityType() {
        return entityType;
    }

    public SystemEntityDto setEntityType(SystemEntityType entityType) {
        this.entityType = entityType;
        return this;
    }

    public OrganizationDto getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationDto organization) {
        this.organization = organization;
    }

    public UserDto getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(UserDto contactPerson) {
        this.contactPerson = contactPerson;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }

    public static SystemEntityDto buildVotingServiceEntity(String entityId) {
        return new SystemEntityDto(entityId, SystemEntityType.VOTING_SERVICE_PROVIDER);
    }

    public static SystemEntityDto buildIdentityServiceEntity(String entityId) {
        return new SystemEntityDto(entityId, SystemEntityType.ID_PROVIDER);
    }
}
