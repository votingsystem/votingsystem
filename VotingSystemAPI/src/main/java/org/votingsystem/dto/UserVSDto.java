package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.BankVS;
import org.votingsystem.model.currency.GroupVS;

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
    private DeviceVSDto deviceVS;
    private Set<CertificateVSDto> certCollection = new HashSet<>();
    private String firstName;
    private String lastName;
    private String metaInf;
    private String country;
    private String IBAN;
    private String NIF;
    private UserVSDto representative;//this is for groups

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
        result.setState(userVS.getState());
        return result;
    }

    public static UserVSDto COMPLETE(UserVS userVS) throws Exception {
        UserVSDto userVSDto = BASIC(userVS);
        if(userVS.getCertificate() != null) {
            Set<CertificateVSDto> certCollection = new HashSet<>();
            certCollection.add(new CertificateVSDto(userVS.getCertificate()));
            userVSDto.setCertCollection(certCollection);
        }
        DeviceVSDto deviceVS = new DeviceVSDto();
        deviceVS.setPhone(userVS.getPhone());
        deviceVS.setEmail(userVS.getEmail());
        userVSDto.setDeviceVS(deviceVS);
        return userVSDto;
    }

    public static UserVSDto DEVICES(UserVS userVS, Set<DeviceVSDto> connectedDevices,
                    List<CertificateVS> certificateVSList) throws Exception {
        UserVSDto userVSDto = BASIC(userVS);
        userVSDto.setReason(userVS.getReason());
        userVSDto.setDescription(userVS.getDescription());
        userVSDto.setConnectedDevices(connectedDevices);
        if(certificateVSList != null) {
            Set<CertificateVSDto> certCollection = new HashSet<>();
            for(CertificateVS certificateVS : certificateVSList) {
                certCollection.add(new CertificateVSDto(certificateVS.getX509Cert()));
            }
            userVSDto.setCertCollection(certCollection);
        }
        return userVSDto;
    }

    @JsonIgnore
    public UserVS getUserVS() throws Exception {
        UserVS userVS = null;
        switch (type) {
            case BANKVS:
                userVS = new BankVS();
                break;
            case GROUP:
                userVS = new GroupVS();
                if(representative != null) ((GroupVS)userVS).setRepresentative(representative.getUserVS());
                break;
            default:
                userVS = new UserVS();
        }
        if(!certCollection.isEmpty()) {
            userVS.setCertificate(certCollection.iterator().next().getX509Cert());
        }
        userVS.setId(id);
        userVS.setName(name);
        userVS.setFirstName(firstName);
        userVS.setLastName(lastName);
        userVS.setIBAN(IBAN);
        userVS.setNif(NIF);
        userVS.setType(type);
        userVS.setState(state);
        userVS.setReason(reason);
        userVS.setDescription(description);
        return userVS;
    }

    @JsonIgnore
    public Set<DeviceVS> getDevices() throws Exception {
        Set<DeviceVS> result = null;
        if (connectedDevices != null) {
            result = new HashSet<>();
            for(DeviceVSDto deviceVSDto : connectedDevices) {
                result.add(deviceVSDto.getDeviceVS());
            }
        }
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

    public DeviceVSDto getDeviceVS() {
        return deviceVS;
    }

    public void setDeviceVS(DeviceVSDto deviceVS) {
        this.deviceVS = deviceVS;
    }

    public UserVSDto getRepresentative() {
        return representative;
    }

    public void setRepresentative(UserVSDto representative) {
        this.representative = representative;
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
}
