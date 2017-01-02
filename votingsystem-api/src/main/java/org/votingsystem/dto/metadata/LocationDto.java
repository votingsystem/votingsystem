package org.votingsystem.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.dto.AddressDto;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "Location")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationDto {

    private static final Logger log = Logger.getLogger(LocationDto.class.getName());

    @JacksonXmlProperty(localName = "Country", isAttribute = true)
    private CountryDto country;
    @JacksonXmlProperty(localName = "City")
    private String city;
    @JacksonXmlProperty(localName = "Address")
    private AddressDto address;

    public LocationDto() {}

    public LocationDto(String city, String address, String postalCode, org.votingsystem.util.Country country,
                       String language) {
        this.country = new CountryDto(country, language);
        this.city = city;
        this.address = new AddressDto(address, postalCode);
    }

    public CountryDto getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public AddressDto getAddress() {
        return address;
    }

}
