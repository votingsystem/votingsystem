package org.votingsystem.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.util.Country;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="Address")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address extends EntityVS implements Serializable {

    public enum Type {CERTIFICATION_OFFICE}

    public static final long serialVersionUID = 1L;
    
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Column(name="name")
    private String name;
    @Column(name="metaInf")
    private String metaInf;
    @Column(name = "postalCode", length = 10)
    private String postalCode;
    @Column(name = "province", nullable = false, length = 48)
    private String province;
    @Enumerated(EnumType.STRING)
    @Column(name = "country")
    private Country country;
    @Column(name = "city", nullable = false, length = 48)
    private String city;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated")
    public Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated")
    public Date lastUpdated;
    
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
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

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
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

    public void update(Address address) {
        this.name = address.getName();
        this.city = address.getCity();
        this.province = address.getProvince();
        this.postalCode = address.getPostalCode();
        this.country = address.getCountry();
    }

}
