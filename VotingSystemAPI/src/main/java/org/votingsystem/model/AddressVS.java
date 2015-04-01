package org.votingsystem.model;

import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.Country;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="AddressVS")
public class AddressVS extends EntityVS implements Serializable {

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
    @Column(name="dateCreated", length=23, insertable=true)
    public Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23, insertable=true)
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

    public void checkAddress(AddressVS address) throws ValidationExceptionVS {
        if(address.getName() != null) if(!address.getName().equals(name)) throw new ValidationExceptionVS(
                "expected name " + address.getName() + " found " + name);
        if(address.getPostalCode() != null) if(!address.getPostalCode().equals(postalCode))
                throw new ValidationExceptionVS("expected postalCode " + address.getName() +
                " found " + postalCode);
        if(address.getProvince() != null) if(!address.getProvince().equals(province))
                throw new ValidationExceptionVS("expected province " + address.getProvince() +
                " found " + province);
        if(address.getCity() != null) if(!address.getCity().equals(city))
            throw new ValidationExceptionVS("expected city " + address.getCity() + " found " + city);
        if(address.getCountry() != null) if(!address.getCountry().equals(country))
            throw new ValidationExceptionVS("expected country " + address.getCountry() + " found " + country);
    }

    public static AddressVS parse(Map dataMap) throws ParseException {
        AddressVS result = new AddressVS();
        if(dataMap.containsKey("id")) result.setId(((Integer) dataMap.get("id")).longValue());
        if(dataMap.containsKey("name")) result.setName((String) dataMap.get("name"));
        if(dataMap.containsKey("metaInf")) result.setMetaInf((String) dataMap.get("metaInf"));
        if(dataMap.containsKey("postalCode")) result.setPostalCode((String) dataMap.get("postalCode"));
        if(dataMap.containsKey("province")) result.setProvince((String) dataMap.get("province"));
        if(dataMap.containsKey("city")) result.setCity((String) dataMap.get("city"));
        if(dataMap.containsKey("dateCreated")) result.setDateCreated(
                DateUtils.getDateFromString((String) dataMap.get("dateCreated")));
        return result;
    }

    public Map toMap() {
        Map result = new HashMap<>();
        if(id != null) result.put("id", id);
        if(name != null) result.put("name", name);
        if(metaInf != null) result.put("metaInf", metaInf);
        if(postalCode != null) result.put("postalCode", postalCode);
        if(province != null) result.put("province", province);
        if(city != null) result.put("city", city);
        if(dateCreated != null) result.put("dateCreated", dateCreated);
        if(country != null) result.put("country", country.toString());
        return result;
    }

}
