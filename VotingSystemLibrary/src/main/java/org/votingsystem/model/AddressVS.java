package org.votingsystem.model;

import net.sf.json.JSONObject;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="AddressVS")
public class AddressVS implements Serializable {

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
    @Column(name = "country", nullable = false, length = 48)
    private String country;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void checkAddress(AddressVS address) throws ValidationExceptionVS {
        if(address.getName() != null) if(!address.getName().equals(name)) throw new ValidationExceptionVS(AddressVS.class,
                "expected name " + address.getName() + " found " + name);
        if(address.getPostalCode() != null) if(!address.getPostalCode().equals(postalCode))
                throw new ValidationExceptionVS(AddressVS.class, "expected postalCode " + address.getName() +
                " found " + postalCode);
        if(address.getProvince() != null) if(!address.getProvince().equals(province))
                throw new ValidationExceptionVS(AddressVS.class, "expected province " + address.getProvince() +
                " found " + province);
        if(address.getCity() != null) if(!address.getCity().equals(city))
            throw new ValidationExceptionVS(AddressVS.class, "expected city " + address.getCity() + " found " + city);
        if(address.getCountry() != null) if(!address.getCountry().equals(country))
            throw new ValidationExceptionVS(AddressVS.class, "expected country " + address.getCountry() + " found " + country);
    }

    public static AddressVS parse(JSONObject jsonObject) throws ParseException {
        AddressVS result = new AddressVS();
        if(jsonObject.has("id")) result.setId(jsonObject.getLong("id"));
        if(jsonObject.has("name")) result.setName(jsonObject.getString("name"));
        if(jsonObject.has("metaInf")) result.setMetaInf(jsonObject.getString("metaInf"));
        if(jsonObject.has("postalCode")) result.setPostalCode(jsonObject.getString("postalCode"));
        if(jsonObject.has("province")) result.setProvince(jsonObject.getString("province"));
        if(jsonObject.has("city")) result.setCity(jsonObject.getString("city"));
        if(jsonObject.has("dateCreated")) result.setDateCreated(
                DateUtils.getDateFromString(jsonObject.getString("dateCreated")));
        return result;
    }

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        if(id != null) result.put("id", id);
        if(name != null) result.put("name", name);
        if(metaInf != null) result.put("metaInf", metaInf);
        if(postalCode != null) result.put("postalCode", postalCode);
        if(province != null) result.put("province", province);
        if(city != null) result.put("city", city);
        if(dateCreated != null) result.put("dateCreated", dateCreated);
        return result;
    }

}
