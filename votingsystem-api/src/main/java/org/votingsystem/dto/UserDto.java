package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.util.IdDocument;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    @JacksonXmlProperty(localName = "State", isAttribute = true)
    private User.State state;
    @JacksonXmlProperty(localName = "Type", isAttribute = true)
    private User.Type type;
    @JacksonXmlProperty(localName = "ConnectedDevices")
    private Set<DeviceDto> connectedDevices;
    @JacksonXmlProperty(localName = "Device")
    private DeviceDto device;
    private Set<CertificateDto> certCollection = new HashSet<>();
    @JacksonXmlProperty(localName = "GivenName")
    private String givenName;
    @JacksonXmlProperty(localName = "SurName")
    private String surName;
    @JacksonXmlProperty(localName = "Phone")
    private String phone;
    @JacksonXmlProperty(localName = "Email")
    private String email;
    @JacksonXmlProperty(localName = "Company")
    private String company;
    @JacksonXmlProperty(localName = "Country")
    private String country;
    @JacksonXmlProperty(localName = "IBAN")
    private String IBAN;
    //document id
    @JacksonXmlProperty(localName = "NumId")
    private String numId;
    @JacksonXmlProperty(localName = "DocumentType")
    private IdDocument documentType;
    @JacksonXmlProperty(localName = "UUID")
    private String UUID;


    public UserDto() {}

    public UserDto(User user) {
        this.givenName = user.getName();
        this.surName = user.getSurname();
        this.numId = user.getNumId();
        this.documentType = user.getDocumentType();
        this.type = User.Type.USER;
        this.phone = user.getPhone();
        this.email = user.getEmail();
    }

    public UserDto(String company, String givenName, String surName, String email, String phone, User.Type type){
        this.company = company;
        this.givenName = givenName;
        this.surName = surName;
        this.email = email;
        this.phone = phone;
        this.type = type;
    }

    public static UserDto BASIC(User user) {
        UserDto result = new UserDto();
        result.setId(user.getId());
        result.setGivenName(user.getName());
        result.setSurName(user.getSurname());
        result.setIBAN(user.getIBAN());
        result.setNumId(user.getNumId());
        result.setDocumentType(user.getDocumentType());
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
        userDto.setConnectedDevices(connectedDevices);
        if(certificateList != null) {
            Set<CertificateDto> certCollection = new HashSet<>();
            for(Certificate certificate : certificateList) {
                certCollection.add(new CertificateDto(certificate.getX509Certificate()));
            }
            userDto.setCertCollection(certCollection);
        }
        return userDto;
    }

    @JsonIgnore
    public User getUser() throws Exception {
        User user = new User();
        if(!certCollection.isEmpty()) {
            user.setX509Certificate(certCollection.iterator().next().getX509Cert());
        }
        user.setId(id);
        user.setName(givenName);
        user.setSurname(surName);
        user.setIBAN(IBAN);
        user.setNumIdAndType(numId, documentType);
        user.setType(type);
        user.setState(state);
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

    public String getNumId() {
        return numId;
    }

    public void setNumId(String numId) {
        this.numId = numId;
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

    public Set<DeviceDto> getConnectedDevices() {
        return connectedDevices;
    }

    public void setConnectedDevices(Set<DeviceDto> connectedDevices) {
        this.connectedDevices = connectedDevices;
    }

    public Set<CertificateDto> getCertCollection() {
        return certCollection;
    }

    public void setCertCollection(Set<CertificateDto> certCollection) {
        this.certCollection = certCollection;
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

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public IdDocument getDocumentType() {
        return documentType;
    }

    public void setDocumentType(IdDocument documentType) {
        this.documentType = documentType;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public DeviceDto getDevice() {
        return device;
    }

    public void setDevice(DeviceDto device) {
        this.device = device;
    }
}
