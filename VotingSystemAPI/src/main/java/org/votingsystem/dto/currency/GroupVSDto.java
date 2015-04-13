package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;

import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupVSDto {

    private String name;
    private String info;
    private TypeVS operation;
    private Long id;
    private Set<TagVS> tagSet = new HashSet<>();

    public GroupVSDto() {}

    public static GroupVSDto BASIC(GroupVS groupVS) {
        GroupVSDto result = new GroupVSDto();
        result.setId(groupVS.getId());
        result.setName(groupVS.getName());
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

    public Set<TagVS> getTagSet() {
        return tagSet;
    }

    public void setTagSet(Set<TagVS> tagSet) {
        this.tagSet = tagSet;
    }

    public void validateNewGroupRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_NEW != operation) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_NEW' - operation found: " + operation);
        if(name == null) throw new ValidationExceptionVS("missing param 'groupvsName'");
        if(id == null) throw new ValidationExceptionVS("missing param 'id'");
        if(info == null) throw new ValidationExceptionVS("missing param 'groupvsInfo'");
    }

    public void validateCancelRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_CANCEL != operation) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_CANCEL' - operation found: " + operation);
        if(name == null) throw new ValidationExceptionVS("missing param 'name'");
        if(id == null) throw new ValidationExceptionVS("missing param 'id'");
    }

    public void validateEditRequest() throws ValidationExceptionVS {
        if(TypeVS.CURRENCY_GROUP_NEW != operation) throw new ValidationExceptionVS(
                "operation expected: 'CURRENCY_GROUP_NEW' - operation found: " + operation);
        if(name == null) throw new ValidationExceptionVS("missing param 'name'");
        if(id == null) throw new ValidationExceptionVS("missing param 'id'");
        if(info == null) throw new ValidationExceptionVS("missing param 'info'");
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

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
