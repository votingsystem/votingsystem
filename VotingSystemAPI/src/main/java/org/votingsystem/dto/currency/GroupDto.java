package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.CertificateVSDto;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Group;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupDto {

    private Long id;
    private String name;
    private TypeVS operation;
    private User.State state;
    private User.Type type;
    private String reason;
    private String description;
    private Date dateCreated;
    private UserDto representative;
    private Set<DeviceDto> connectedDevices;
    private Set<CertificateVSDto> certCollection;
    private Long numPendingUsers;
    private Long numActiveUsers;
    private String IBAN;
    private String NIF;
    private String UUID;
    private Set<TagVS> tags = new HashSet<>();

    public GroupDto() {}

    public static GroupDto BASIC(Group group) {
        GroupDto result = new GroupDto();
        result.setId(group.getId());
        result.setName(group.getName());
        return result;
    }

    public static GroupDto DETAILS(Group group, UserDto representative) {
        GroupDto result = BASIC(group);
        result.setIBAN(group.getIBAN());
        result.setDescription(group.getDescription());
        result.setState(group.getState());
        result.setDateCreated(group.getDateCreated());
        result.setRepresentative(representative);
        result.setType(group.getType());
        result.setTags(group.getTagVSSet());
        return result;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<TagVS> getTags() {
        return tags;
    }

    public void setTags(Set<TagVS> tags) {
        this.tags = tags;
    }

    public void validateNewGroupRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_NEW != operation) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_NEW' - operation found: " + operation);
        if(name == null) throw new ValidationExceptionVS("missing param 'groupName'");
        if(description == null) throw new ValidationExceptionVS("missing param 'groupInfo'");
    }

    public void validateCancelRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_CANCEL != operation) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_CANCEL' - operation found: " + operation);
        if(name == null) throw new ValidationExceptionVS("missing param 'name'");
        if(id == null) throw new ValidationExceptionVS("missing param 'id'");
    }

    public void validateEditRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_EDIT != operation) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_EDIT' - operation found: " + operation);
        if(name == null) throw new ValidationExceptionVS("missing param 'name'");
        if(id == null) throw new ValidationExceptionVS("missing param 'id'");
        if(description == null) throw new ValidationExceptionVS("missing param 'info'");
    }

    public void validateSubscriptionRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_SUBSCRIBE != operation) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_SUBSCRIBE' - operation found: " + operation);
        if(name == null) throw new ValidationExceptionVS("missing param 'name'");
        if(id == null) throw new ValidationExceptionVS("missing param 'id'");
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

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public UserDto getRepresentative() {
        return representative;
    }

    public void setRepresentative(UserDto representative) {
        this.representative = representative;
    }

    public Set<DeviceDto> getConnectedDevices() {
        return connectedDevices;
    }

    public void setConnectedDevices(Set<DeviceDto> connectedDevices) {
        this.connectedDevices = connectedDevices;
    }

    public Set<CertificateVSDto> getCertCollection() {
        return certCollection;
    }

    public void setCertCollection(Set<CertificateVSDto> certCollection) {
        this.certCollection = certCollection;
    }

    public Long getNumPendingUsers() {
        return numPendingUsers;
    }

    public void setNumPendingUsers(Long numPendingUsers) {
        this.numPendingUsers = numPendingUsers;
    }

    public Long getNumActiveUsers() {
        return numActiveUsers;
    }

    public void setNumActiveUsers(Long numActiveUsers) {
        this.numActiveUsers = numActiveUsers;
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

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}
