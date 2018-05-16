package org.votingsystem.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "TrustedEntities")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrustedEntitiesDto {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Entity")
    private Set<EntityDto> entities;

    public TrustedEntitiesDto() {}


    public Set<EntityDto> getEntities() {
        return entities;
    }

    public void setEntities(Set<EntityDto> entities) {
        this.entities = entities;
    }

    public static TrustedEntitiesDto loadTrustedEntities(String trustedEntitiesFilePath) throws IOException {
        File trustedServicesFile = new File(trustedEntitiesFilePath);
        return new XmlMapper().readValue(FileUtils.getBytesFromFile(trustedServicesFile),
                TrustedEntitiesDto.class);
    }

    public static class EntityDto {

        @JacksonXmlProperty(localName = "Id", isAttribute = true)
        private String id;
        @JacksonXmlProperty(localName = "Type", isAttribute = true)
        private SystemEntityType type;
        @JacksonXmlProperty(localName = "CountryCode", isAttribute = true)
        private String countryCode;
        @JacksonXmlText
        private String description;

        public EntityDto() {}


        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public SystemEntityType getType() {
            return type;
        }

        public void setType(SystemEntityType type) {
            this.type = type;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

    }
}

