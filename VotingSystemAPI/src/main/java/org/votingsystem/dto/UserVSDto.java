package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.UserVS;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserVSDto {

    private Long id;
    private UserVS.State state;
    private UserVS.Type type;
    private String name;
    private String reason;
    private String description;
    private Set<DeviceVSDto> connectedDevices;
    private Set<CertificateVSDto> certCollection;
    private String firstName;
    private String lastName;
    private String IBAN;
    private String NIF;
    private Map sender;

    public UserVSDto() {}

    public static UserVSDto BASIC(UserVS userVS) {
        UserVSDto result = new UserVSDto();
        result.setId(userVS.getId());
        result.setName(userVS.getName());
        result.setFirstName(userVS.getFirstName());
        result.setLastName(userVS.getLastName());
        result.setIBAN(userVS.getIBAN());
        result.setNIF(userVS.getNif());
        result.setType(userVS.getType());
        return result;
    }

    public static UserVSDto DEVICES(UserVS userVS, Set<DeviceVSDto> connectedDevices,
                    List<CertificateVS> certificateVSList) throws Exception {
        UserVSDto userVSDto = BASIC(userVS);
        userVSDto.setState(userVS.getState());
        userVSDto.setType(userVS.getType());
        userVSDto.setReason(userVS.getReason());
        userVSDto.setDescription(userVS.getDescription());
        userVSDto.setConnectedDevices(connectedDevices);
        Set<CertificateVSDto> certCollection = new HashSet<>();
        if(certificateVSList != null) {
            for(CertificateVS certificateVS : certificateVSList) {
                certCollection.add(new CertificateVSDto(certificateVS.getX509Cert()));
            }
            userVSDto.setCertCollection(certCollection);
        }
        return userVSDto;
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

    public UserVS.State getState() {
        return state;
    }

    public void setState(UserVS.State state) {
        this.state = state;
    }

    public UserVS.Type getType() {
        return type;
    }

    public void setType(UserVS.Type type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<DeviceVSDto> getConnectedDevices() {
        return connectedDevices;
    }

    public void setConnectedDevices(Set<DeviceVSDto> connectedDevices) {
        this.connectedDevices = connectedDevices;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Set<CertificateVSDto> getCertCollection() {
        return certCollection;
    }

    public void setCertCollection(Set<CertificateVSDto> certCollection) {
        this.certCollection = certCollection;
    }

    public Map getSender() {
        return sender;
    }

    public void setSender(Map sender) {
        this.sender = sender;
    }
}
