package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.BankVS;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.util.TypeVS;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserVSDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private TypeVS operation;
    private UserVS.State state;
    private UserVS.Type type;
    private String name;
    private String reason;
    private String message;
    private String description;
    private Set<DeviceVSDto> connectedDevices;
    private DeviceVSDto deviceVS;
    private Set<CertificateVSDto> certCollection = new HashSet<>();
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String metaInf;
    private String country;
    private String IBAN;
    private String NIF;
    private UserVSDto representative;//this is for groups

    private Long numRepresentations ;
    private String URL;
    private String representativeMessageURL;
    private String imageURL;
    private String UUID;
    private String base64Image;



    public UserVSDto() {}

    public UserVSDto(UserVS userVS) {
        this.name = userVS.getName();
        this.NIF = userVS.getNif();
        this.type = UserVS.Type.USER;
        this.firstName = userVS.getFirstName();
        this.lastName = userVS.getLastName();
        this.phone = userVS.getPhone();
        this.email = userVS.getEmail();
    }

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

    public static UserVSDto REPRESENTATIVE(UserVS userVS, Long cmsActivationId, Long numRepresentations,
                                           String contextURL) {
        UserVSDto userVSDto = BASIC(userVS);
        userVSDto.type = UserVS.Type.REPRESENTATIVE;
        userVSDto.numRepresentations = numRepresentations;
        userVSDto.URL = format("{0}/rest/representative/id/{1}", contextURL, userVS.getId());
        userVSDto.representativeMessageURL = format("{0}/rest/messageCMS/id/{1}", contextURL, cmsActivationId);
        userVSDto.imageURL = format("{0}/rest/representative/id/{1}/image", contextURL, userVS.getId());
        userVSDto.description = userVS.getDescription();
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

    public UserVSDto setDeviceVS(DeviceVSDto deviceVS) {
        this.deviceVS = deviceVS;
        return this;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRepresentativeMessageURL() {
        return representativeMessageURL;
    }

    public void setRepresentativeMessageURL(String representativeMessageURL) {
        this.representativeMessageURL = representativeMessageURL;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public Long getNumRepresentations() {
        return numRepresentations;
    }

    public void setNumRepresentations(Long numRepresentations) {
        this.numRepresentations = numRepresentations;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
