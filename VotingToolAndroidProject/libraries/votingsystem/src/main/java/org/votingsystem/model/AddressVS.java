package org.votingsystem.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class AddressVS implements Serializable {

    public enum Type {CERTIFICATION_OFFICE}

    public static final long serialVersionUID = 1L;
    
    private Long id;
    private String name;
    private String metaInf;
    private String postalCode;
    private String province;
    private Country country;
    private String city;
    public Date dateCreated;
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

    public void checkAddress(AddressVS address) throws ExceptionVS {
        if(address.getName() != null) if(!address.getName().equals(name)) throw new ExceptionVS(
                "expected name " + address.getName() + " found " + name);
        if(address.getPostalCode() != null) if(!address.getPostalCode().equals(postalCode))
                throw new ExceptionVS("expected postalCode " + address.getName() +
                " found " + postalCode);
        if(address.getProvince() != null) if(!address.getProvince().equals(province))
                throw new ExceptionVS("expected province " + address.getProvince() +
                " found " + province);
        if(address.getCity() != null) if(!address.getCity().equals(city))
            throw new ExceptionVS("expected city " + address.getCity() + " found " + city);
        if(address.getCountry() != null) if(!address.getCountry().equals(country))
            throw new ExceptionVS("expected country " + address.getCountry() + " found " + country);
    }

    public static AddressVS parse(JSONObject jsonObject) throws ParseException, JSONException {
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

    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();
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
