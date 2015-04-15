package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.currency.SubscriptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;

import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionVSDto {

    private Long id;
    private Long groupvsId;
    private String groupvsName;
    private String groupvsInfo;
    private String userVSName;
    private String userVSNIF;
    private String subscriptionSMIME_URL;
    private String activationSMIME_URL;
    private String cancellationSMIME_URL;
    private String reason;
    private TypeVS operation;
    private GroupVSDto groupvs;
    private UserVSDto uservs;
    private SubscriptionVS.State state;
    private Date dateCreated;
    private Date dateCancelled;
    private Date dateActivated;


    public SubscriptionVSDto() {}

    public static SubscriptionVSDto DETAILED(SubscriptionVS subscriptionVS, String restURL) {
        SubscriptionVSDto result = new SubscriptionVSDto();
        result.setId(subscriptionVS.getId());
        result.setState(subscriptionVS.getState());
        result.setSubscriptionSMIME_URL(restURL + "/messageSMIME/id/" + subscriptionVS.getSubscriptionSMIME().getId());
        if(subscriptionVS.getActivationSMIME() != null) result.setActivationSMIME_URL(restURL + "/messageSMIME/id/" +
                subscriptionVS.getActivationSMIME().getId());
        if(subscriptionVS.getCancellationSMIME() != null) result.setActivationSMIME_URL(restURL + "/messageSMIME/id/" +
                subscriptionVS.getCancellationSMIME().getId());
        result.setDateActivated(subscriptionVS.getDateActivated());
        result.setDateCreated(subscriptionVS.getDateCreated());
        result.setDateCancelled(subscriptionVS.getDateCancelled());
        result.setGroupvs(GroupVSDto.BASIC(subscriptionVS.getGroupVS()));
        result.setUservs(UserVSDto.BASIC(subscriptionVS.getUserVS()));
        return result;
    }

    private void validate() throws ValidationExceptionVS {
        if(operation == null) throw new ValidationExceptionVS("missing param 'operation'");
        if(groupvsId == null) throw new ValidationExceptionVS("missing param 'groupvsId'");
        if(groupvsName == null) throw new ValidationExceptionVS("missing param 'groupvsName'");
        if(userVSName== null) throw new ValidationExceptionVS("missing param 'userVSName'");
        if(userVSNIF == null) throw new ValidationExceptionVS("missing param 'userVSNIF'");
    }

    public void validateActivationRequest() throws ValidationExceptionVS {
        validate();
        if(TypeVS.CURRENCY_GROUP_USER_ACTIVATE != operation) throw new ValidationExceptionVS(
                "Operation expected: 'CURRENCY_GROUP_USER_ACTIVATE' - operation found: " + operation);
    }

    public void loadActivationRequest() {
        operation = TypeVS.CURRENCY_GROUP_USER_ACTIVATE;
        groupvsId = groupvs.getId();
        groupvsName = groupvs.getName();
        userVSName = uservs.getName();
        userVSNIF = uservs.getNIF();
    }

    public void validateDeActivationRequest() throws ValidationExceptionVS {
        validate();
        if(TypeVS.CURRENCY_GROUP_USER_DEACTIVATE != operation) throw new ValidationExceptionVS(
                "Operation expected: 'CURRENCY_GROUP_USER_DEACTIVATE' - operation found: " + operation);
    }

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

    public String getUserVSName() {
        return userVSName;
    }

    public void setUserVSName(String userVSName) {
        this.userVSName = userVSName;
    }

    public String getUserVSNIF() {
        return userVSNIF;
    }

    public void setUserVSNIF(String userVSNIF) {
        this.userVSNIF = userVSNIF;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public SubscriptionVS.State getState() {
        return state;
    }

    public void setState(SubscriptionVS.State state) {
        this.state = state;
    }

    public String getSubscriptionSMIME_URL() {
        return subscriptionSMIME_URL;
    }

    public void setSubscriptionSMIME_URL(String subscriptionSMIME_URL) {
        this.subscriptionSMIME_URL = subscriptionSMIME_URL;
    }

    public String getActivationSMIME_URL() {
        return activationSMIME_URL;
    }

    public void setActivationSMIME_URL(String activationSMIME_URL) {
        this.activationSMIME_URL = activationSMIME_URL;
    }

    public String getCancellationSMIME_URL() {
        return cancellationSMIME_URL;
    }

    public void setCancellationSMIME_URL(String cancellationSMIME_URL) {
        this.cancellationSMIME_URL = cancellationSMIME_URL;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateActivated() {
        return dateActivated;
    }

    public void setDateActivated(Date dateActivated) {
        this.dateActivated = dateActivated;
    }

    public Date getDateCancelled() {
        return dateCancelled;
    }

    public void setDateCancelled(Date dateCancelled) {
        this.dateCancelled = dateCancelled;
    }

    public GroupVSDto getGroupvs() {
        return groupvs;
    }

    public void setGroupvs(GroupVSDto groupvs) {
        this.groupvs = groupvs;
    }

    public UserVSDto getUservs() {
        return uservs;
    }

    public void setUservs(UserVSDto uservs) {
        this.uservs = uservs;
    }

    public Long getGroupvsId() {
        return groupvsId;
    }

    public void setGroupvsId(Long groupvsId) {
        this.groupvsId = groupvsId;
    }
}
