package org.votingsystem.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.AddressDto;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.util.CountryEurope;

import javax.persistence.*;
import java.io.Serializable;
import java.text.MessageFormat;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="ADDRESS")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address extends EntityBase implements Serializable {

    public static final long serialVersionUID = 1L;
    
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;
    @Column(name="NAME")
    private String name;
    @Column(name="META_INF")
    private String metaInf;
    @Column(name = "POSTAL_CODE", length = 10)
    private String postalCode;
    @Column(name = "PROVINCE", nullable = false, length = 48)
    private String province;
    @Enumerated(EnumType.STRING)
    @Column(name = "COUNTRY")
    private CountryEurope country;
    @Column(name = "CITY", nullable = false, length = 48)
    private String city;
    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;
    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    public Address(AddressDto addressDto) {
        this.name = addressDto.getDecodedAddress();
        this.postalCode = addressDto.getPostalCode();
        this.metaInf = addressDto.getMetaInf();
        this.province = addressDto.getProvince();
        this.country = addressDto.getCountry();
        this.city = addressDto.getCity();
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean equals(Address address) {
        if(address.getName() != null) if(!address.getName().equals(name)) return false;
        if(address.getPostalCode() != null) if(!address.getPostalCode().equals(postalCode)) return false;
        if(address.getProvince() != null) if(!address.getProvince().equals(province)) return false;
        if(address.getCity() != null) if(!address.getCity().equals(city)) return false;
        if(address.getCountry() != null) if(!address.getCountry().equals(country)) return false;
        return true;
    }

    @Override public String toString() {
        return MessageFormat.format("[name: {0} - postalCode: {1} - province: {2} - city: {3} - country: {4}]",
                name, postalCode, province, city, country);
    }

    public void update(AddressDto address) {
        this.name = address.getDecodedAddress();
        this.city = address.getCity();
        this.province = address.getProvince();
        this.postalCode = address.getPostalCode();
        this.country = address.getCountry();
    }

}
