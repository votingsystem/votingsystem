package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import org.votingsystem.throwable.ExceptionBase;
import org.votingsystem.util.CountryEurope;

import java.io.Serializable;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@JacksonXmlRootElement(localName = "Address")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(AddressDto.class.getName());

    @JacksonXmlProperty(localName = "Id", isAttribute = true)
    private Long id;
    @JacksonXmlProperty(localName = "PostalCode", isAttribute = true)
    private String postalCode;
    @JacksonXmlProperty(localName = "Country", isAttribute = true)
    private CountryEurope country;
    private String metaInf;
    private String province;
    private String city;
    @JacksonXmlText
    private String address;

    public AddressDto() {}

    public AddressDto(String address, String postalCode) {
        if(address != null) {
            this.address = Base64.getEncoder().encodeToString(address.getBytes());
        }
        this.postalCode = postalCode;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @JsonIgnore
    public String getDecodedAddress() {
        String result = null;
        try {
            if(address != null) {
                byte[] decodeAddressBytes = Base64.getDecoder().decode(address.getBytes());
                result = new String(decodeAddressBytes, "UTF-8");
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return result;
    }

    public void setAddress(String address) {
        this.address = address;
    }

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getMetaInf() {
		return metaInf;
	}

	public void setMetaInf(String metaInf) {
		this.metaInf = metaInf;
	}

    public CountryEurope getCountry() {
        return country;
    }

    public void setCountry(CountryEurope country) {
        this.country = country;
    }

    public String getAddress() {
        return address;
    }

    public void checkAddress(AddressDto address) throws ExceptionBase {
        if(address.getDecodedAddress() != null) if(!address.getDecodedAddress().equals(this.address)) throw new ExceptionBase(
                "expected address " + address.getDecodedAddress() + " found " + this.address);
        if(address.getPostalCode() != null) if(!address.getPostalCode().equals(postalCode))
                throw new ExceptionBase("expected postalCode " + address.getDecodedAddress() +
                " found " + postalCode);
        if(address.getProvince() != null) if(!address.getProvince().equals(province))
                throw new ExceptionBase("expected province " + address.getProvince() +
                " found " + province);
        if(address.getCity() != null) if(!address.getCity().equals(city))
            throw new ExceptionBase("expected city " + address.getCity() + " found " + city);
        if(address.getCountry() != null) if(!address.getCountry().equals(country))
            throw new ExceptionBase("expected country " + address.getCountry() + " found " + country);
    }

}
