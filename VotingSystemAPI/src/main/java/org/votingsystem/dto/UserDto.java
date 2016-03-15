package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.Group;
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
public class UserDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private TypeVS operation;
    private User.State state;
    private User.Type type;
    private String name;
    private String reason;
    private String message;
    private String description;
    private Set<DeviceDto> connectedDevices;
    private DeviceDto device;
    private Set<CertificateDto> certCollection = new HashSet<>();
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String metaInf;
    private String country;
    private String IBAN;
    private String NIF;
    private UserDto representative;//this is for groups

    private Long numRepresentations ;
    private String URL;
    private String representativeMessageURL;
    private String imageURL;
    private String UUID;
    private String base64Image;



    public UserDto() {}

    public UserDto(User user) {
        this.name = user.getName();
        this.NIF = user.getNif();
        this.type = User.Type.USER;
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.phone = user.getPhone();
        this.email = user.getEmail();
    }

    public static UserDto BASIC(User user) {
        UserDto result = new UserDto();
        result.setId(user.getId());
        result.setName(user.getName());
        result.setFirstName(user.getFirstName());
        result.setLastName(user.getLastName());
        result.setIBAN(user.getIBAN());
        result.setNIF(user.getNif());
        result.setType(user.getType());
        result.setState(user.getState());
        return result;
    }

    public static UserDto COMPLETE(User user) throws Exception {
        UserDto userDto = BASIC(user);
        if(user.getX509Certificate() != null) {
            Set<CertificateDto> certCollection = new HashSet<>();
            certCollection.add(new CertificateDto(user.getX509Certificate()));
            userDto.setCertCollection(certCollection);
        }
        DeviceDto device = new DeviceDto();
        device.setPhone(user.getPhone());
        device.setEmail(user.getEmail());
        userDto.setDevice(device);
        return userDto;
    }

    public static UserDto DEVICES(User user, Set<DeviceDto> connectedDevices,
                                  List<Certificate> certificateList) throws Exception {
        UserDto userDto = BASIC(user);
        userDto.setReason(user.getReason());
        userDto.setDescription(user.getDescription());
        userDto.setConnectedDevices(connectedDevices);
        if(certificateList != null) {
            Set<CertificateDto> certCollection = new HashSet<>();
            for(Certificate certificate : certificateList) {
                certCollection.add(new CertificateDto(certificate.getX509Cert()));
            }
            userDto.setCertCollection(certCollection);
        }
        return userDto;
    }

    public static UserDto REPRESENTATIVE(User user, Long cmsActivationId, Long numRepresentations,
                                         String contextURL) {
        UserDto userDto = BASIC(user);
        userDto.type = User.Type.REPRESENTATIVE;
        userDto.numRepresentations = numRepresentations;
        userDto.URL = format("{0}/rest/representative/id/{1}", contextURL, user.getId());
        userDto.representativeMessageURL = format("{0}/rest/cmsMessage/id/{1}", contextURL, cmsActivationId);
        userDto.imageURL = format("{0}/rest/representative/id/{1}/image", contextURL, user.getId());
        userDto.description = user.getDescription();
        return userDto;
    }

    @JsonIgnore
    public User getUser() throws Exception {
        User user = null;
        switch (type) {
            case BANK:
                user = new Bank();
                break;
            case GROUP:
                user = new Group();
                if(representative != null) ((Group) user).setRepresentative(representative.getUser());
                break;
            default:
                user = new User();
        }
        if(!certCollection.isEmpty()) {
            user.setX509Certificate(certCollection.iterator().next().getX509Cert());
        }
        user.setId(id);
        user.setName(name);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setIBAN(IBAN);
        user.setNif(NIF);
        user.setType(type);
        user.setState(state);
        user.setReason(reason);
        user.setDescription(description);
        return user;
    }

    @JsonIgnore
    public Set<Device> getDevices() throws Exception {
        Set<Device> result = null;
        if (connectedDevices != null) {
            result = new HashSet<>();
            for(DeviceDto deviceDto : connectedDevices) {
                result.add(deviceDto.getDevice());
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

    public User.State getState() {
        return state;
    }

    public void setState(User.State state) {
        this.state = state;
    }

    public User.Type getType() {
        return type;
    }

    public void setType(User.Type type) {
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

    public Set<DeviceDto> getConnectedDevices() {
        return connectedDevices;
    }

    public void setConnectedDevices(Set<DeviceDto> connectedDevices) {
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

    public Set<CertificateDto> getCertCollection() {
        return certCollection;
    }

    public void setCertCollection(Set<CertificateDto> certCollection) {
        this.certCollection = certCollection;
    }

    public DeviceDto getDevice() {
        return device;
    }

    public UserDto setDevice(DeviceDto device) {
        this.device = device;
        return this;
    }

    public UserDto getRepresentative() {
        return representative;
    }

    public void setRepresentative(UserDto representative) {
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
