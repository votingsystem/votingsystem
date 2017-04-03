package org.votingsystem.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.http.SystemEntityType;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "Metadata")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataDto {

    @JacksonXmlProperty(localName = "Operation")
    private OperationTypeDto operation;

    @JacksonXmlProperty(localName = "Language", isAttribute = true)
    private String language;

    @JacksonXmlProperty(localName = "TimeZone", isAttribute = true)
    private String timeZone;

    @JacksonXmlProperty(localName = "ValidUntil", isAttribute = true)
    private String validUntil;

    @JacksonXmlProperty(localName = "Entity")
    private SystemEntityDto entity;

    @JacksonXmlElementWrapper(useWrapping = true, localName = "Keys")
    @JacksonXmlProperty(localName = "Key")
    private Set<KeyDto> keyDescriptorSet;

    @JacksonXmlProperty(localName = "TrustedEntities")
    private TrustedEntitiesDto trustedEntities;


    public MetadataDto() {}

    public MetadataDto(SystemEntityType entityType, String entityID, Set<KeyDto> keyDescriptorSet) {
        this.entity= new SystemEntityDto(entityID, entityType);
        this.keyDescriptorSet = keyDescriptorSet;
    }

    public Set<KeyDto> getKeyDescriptorSet() {
        return keyDescriptorSet;
    }

    public void setKeyDescriptorSet(Set<KeyDto> keyDescriptorSet) {
        this.keyDescriptorSet = keyDescriptorSet;
    }

    @JsonIgnore
    public LocalDateTime getValidUntilDate() {
        if(validUntil == null)
            return null;
        return ZonedDateTime.parse(validUntil).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }


    public MetadataDto setOrganization(OrganizationDto organization) {
        this.entity.setOrganization(organization);
        return this;
    }


    public MetadataDto setContactPerson(UserDto contactPerson) {
        this.entity.setContactPerson(contactPerson);
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public MetadataDto setLanguage(String language) {
        this.language = language;
        return this;
    }

    public MetadataDto setValidUntil(String validUntil) {
        this.validUntil = validUntil;
        return this;
    }

    public SystemEntityDto getEntity() {
        return entity;
    }

    public MetadataDto setEntity(SystemEntityDto entity) {
        this.entity = entity;
        return this;
    }

    public MetadataDto setLocation(LocationDto location) {
        this.entity.setLocation(location);
        return this;
    }

    public String getTimeZone() {
        return ZoneId.systemDefault().toString();
    }

    public TrustedEntitiesDto getTrustedEntities() {
        return trustedEntities;
    }

    public void setTrustedEntities(TrustedEntitiesDto trustedEntities) {
        this.trustedEntities = trustedEntities;
    }

    public Set<KeyDto> getKeys(KeyDto.Use keyUse) {
        Set<KeyDto> result = new HashSet<>();
        for(KeyDto keyDto : keyDescriptorSet) {
            if(keyDto.getUse() == keyUse)
                result.add(keyDto);
        }
        return result;
    }

    public OperationTypeDto getOperation() {
        return operation;
    }

    public MetadataDto setOperation(OperationTypeDto operation) {
        this.operation = operation;
        return this;
    }

}
