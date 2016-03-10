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
    private String subscriptionCMS_URL;
    private String activationCMS_URL;
    private String cancellationCMS_URL;
    private String reason;
    private String UUID;
    private TypeVS operation;
    private GroupVSDto groupvs;
    private UserVSDto uservs;
    private SubscriptionVS.State state;
    private Date dateCreated;
    private Date dateCancelled;
    private Date dateActivated;


    public SubscriptionVSDto() {}

    public static SubscriptionVSDto DETAILED(SubscriptionVS subscriptionVS, String contextURL) {
        SubscriptionVSDto result = new SubscriptionVSDto();
        result.setId(subscriptionVS.getId());
        result.setState(subscriptionVS.getState());
        result.setSubscriptionCMS_URL(contextURL + "/rest/cmsMessage/id/" + subscriptionVS.getSubscriptionCMS().getId());
        if(subscriptionVS.getActivationCMS() != null) result.setActivationCMS_URL(contextURL + "/rest/cmsMessage/id/" +
                subscriptionVS.getActivationCMS().getId());
        if(subscriptionVS.getCancellationCMS() != null) result.setActivationCMS_URL(contextURL + "/rest/cmsMessage/id/" +
                subscriptionVS.getCancellationCMS().getId());
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
        UUID = java.util.UUID.randomUUID().toString();
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

    public String getSubscriptionCMS_URL() {
        return subscriptionCMS_URL;
    }

    public void setSubscriptionCMS_URL(String subscriptionCMS_URL) {
        this.subscriptionCMS_URL = subscriptionCMS_URL;
    }

    public String getActivationCMS_URL() {
        return activationCMS_URL;
    }

    public void setActivationCMS_URL(String activationCMS_URL) {
        this.activationCMS_URL = activationCMS_URL;
    }

    public String getCancellationCMS_URL() {
        return cancellationCMS_URL;
    }

    public void setCancellationCMS_URL(String cancellationCMS_URL) {
        this.cancellationCMS_URL = cancellationCMS_URL;
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

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}
