package org.votingsystem.currency.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.TagVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;

import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupVSDto {

    private String groupvsName;
    private String groupvsInfo;
    private TypeVS operation;
    private Long id;
    private Set<TagVS> tagSet = new HashSet<>();

    public GroupVSDto() {}

    public String getGroupvsName() {
        return groupvsName;
    }

    public void setGroupvsName(String groupvsName) {
        this.groupvsName = groupvsName;
    }

    public String getGroupvsInfo() {
        return groupvsInfo;
    }

    public void setGroupvsInfo(String groupvsInfo) {
        this.groupvsInfo = groupvsInfo;
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

    public Set<TagVS> getTagSet() {
        return tagSet;
    }

    public void setTagSet(Set<TagVS> tagSet) {
        this.tagSet = tagSet;
    }

    public void validateNewGroupRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_NEW != operation) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_NEW' - operation found: " + operation);
        if(groupvsName == null) throw new ValidationExceptionVS("missing param 'groupvsName'");
        if(id == null) throw new ValidationExceptionVS("missing param 'id'");
        if(groupvsInfo == null) throw new ValidationExceptionVS("missing param 'groupvsInfo'");
    }

    public void validateCancelRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_CANCEL != operation) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_CANCEL' - operation found: " + operation);
        if(groupvsName == null) throw new ValidationExceptionVS("missing param 'groupvsName'");
        if(id == null) throw new ValidationExceptionVS("missing param 'id'");
    }

    public void validateEditRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_NEW != operation) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_NEW' - operation found: " + operation);
        if(groupvsName == null) throw new ValidationExceptionVS("missing param 'groupvsName'");
        if(id == null) throw new ValidationExceptionVS("missing param 'id'");
        if(groupvsInfo == null) throw new ValidationExceptionVS("missing param 'groupvsInfo'");
    }

    public void validateSubscriptionRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_SUBSCRIBE != operation) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_SUBSCRIBE' - operation found: " + operation);
        if(groupvsName == null) throw new ValidationExceptionVS("missing param 'groupvsName'");
        if(id == null) throw new ValidationExceptionVS("missing param 'id'");
    }

}
