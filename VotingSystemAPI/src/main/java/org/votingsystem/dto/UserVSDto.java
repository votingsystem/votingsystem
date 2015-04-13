package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.votingsystem.model.UserVS;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserVSDto {

    private Long id;
    @JsonProperty("IBAN") private String IBAN;
    @JsonProperty("NIF") private String NIF;
    private String name;

    public UserVSDto() {}

    public static UserVSDto BASIC(UserVS userVS) {
        UserVSDto result = new UserVSDto();
        result.setId(userVS.getId());
        result.setName(userVS.getName());
        result.setIBAN(userVS.getIBAN());
        result.setNIF(userVS.getNif());
        return result;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

    public String getNIF() {
        return NIF;
    }

    public void setNIF(String NIF) {
        this.NIF = NIF;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
