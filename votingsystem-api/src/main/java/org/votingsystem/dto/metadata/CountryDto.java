package org.votingsystem.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.util.Country;

import java.util.Locale;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "Country")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CountryDto {

    @JacksonXmlProperty(localName = "Code", isAttribute = true)
    private String code;
    @JacksonXmlProperty(localName = "Language", isAttribute = true)
    private String language;
    @JacksonXmlProperty(localName = "DisplayName", isAttribute = true)
    private String displayName;

    public CountryDto() {}

    public CountryDto(Country country, String language) {
        this.language = language;
        this.displayName = country.getDisplayCountry();
        this.code = country.name();
    }

    public String getLanguage() {
        return language;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    @JsonIgnore
    public Locale getLocale() {
        return new Locale(language, code);
    }

}
