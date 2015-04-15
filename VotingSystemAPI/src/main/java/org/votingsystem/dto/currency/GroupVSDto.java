package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.votingsystem.dto.CertificateVSDto;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupVSDto {

    private Long id;
    private String name;
    private String info;
    private TypeVS operation;
    private UserVS.State state;
    private UserVS.Type type;
    private String reason;
    private String description;
    private Date dateCreated;
    private UserVSDto representative;
    private Set<DeviceVSDto> connectedDevices;
    private Set<CertificateVSDto> certCollection;
    private Long numPendingUsers;
    private Long numActiveUsers;
    private String IBAN;
    private String NIF;
    private String UUID;
    private Set<TagVS> tags = new HashSet<>();

    public GroupVSDto() {}

    public static GroupVSDto BASIC(GroupVS groupVS) {
        GroupVSDto result = new GroupVSDto();
        result.setId(groupVS.getId());
        result.setName(groupVS.getName());
        return result;
    }

    public static GroupVSDto DETAILS(GroupVS groupVS, UserVSDto representative) {
        GroupVSDto result = BASIC(groupVS);
        result.setIBAN(groupVS.getIBAN());
        result.setDescription(groupVS.getDescription());
        result.setState(groupVS.getState());
        result.setDateCreated(groupVS.getDateCreated());
        result.setRepresentative(representative);
        result.setType(groupVS.getType());
        result.setTags(groupVS.getTagVSSet());

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
        if(TypeVS.CURRENCY_GROUP_NEW != getOperation()) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_NEW' - operation found: " + getOperation());
        if(getName() == null) throw new ValidationExceptionVS("missing param 'groupvsName'");
        if(getInfo() == null) throw new ValidationExceptionVS("missing param 'groupvsInfo'");
    }

    public void validateCancelRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_CANCEL != getOperation()) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_CANCEL' - operation found: " + getOperation());
        if(getName() == null) throw new ValidationExceptionVS("missing param 'name'");
        if(getId() == null) throw new ValidationExceptionVS("missing param 'id'");
    }

    public void validateEditRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_NEW != getOperation()) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_NEW' - operation found: " + getOperation());
        if(getName() == null) throw new ValidationExceptionVS("missing param 'name'");
        if(getId() == null) throw new ValidationExceptionVS("missing param 'id'");
        if(getInfo() == null) throw new ValidationExceptionVS("missing param 'info'");
    }

    public void validateSubscriptionRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_SUBSCRIBE != getOperation()) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_SUBSCRIBE' - operation found: " + getOperation());
        if(getName() == null) throw new ValidationExceptionVS("missing param 'name'");
        if(getId() == null) throw new ValidationExceptionVS("missing param 'id'");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
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

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public UserVSDto getRepresentative() {
        return representative;
    }

    public void setRepresentative(UserVSDto representative) {
        this.representative = representative;
    }

    public Set<DeviceVSDto> getConnectedDevices() {
        return connectedDevices;
    }

    public void setConnectedDevices(Set<DeviceVSDto> connectedDevices) {
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
