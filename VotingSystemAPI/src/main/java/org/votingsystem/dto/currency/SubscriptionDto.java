package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.UserDto;
import org.votingsystem.model.currency.Subscription;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.TypeVS;

import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionDto {

    private Long id;
    private Long groupId;
    private String groupName;
    private String groupInfo;
    private String userName;
    private String userNIF;
    private String subscriptionCMS_URL;
    private String activationCMS_URL;
    private String cancellationCMS_URL;
    private String reason;
    private String UUID;
    private TypeVS operation;
    private GroupDto group;
    private UserDto user;
    private Subscription.State state;
    private Date dateCreated;
    private Date dateCancelled;
    private Date dateActivated;


    public SubscriptionDto() {}

    public static SubscriptionDto DETAILED(Subscription subscription, String contextURL) {
        SubscriptionDto result = new SubscriptionDto();
        result.setId(subscription.getId());
        result.setState(subscription.getState());
        result.setSubscriptionCMS_URL(contextURL + "/rest/cmsMessage/id/" + subscription.getSubscriptionCMS().getId());
        if(subscription.getActivationCMS() != null) result.setActivationCMS_URL(contextURL + "/rest/cmsMessage/id/" +
                subscription.getActivationCMS().getId());
        if(subscription.getCancellationCMS() != null) result.setActivationCMS_URL(contextURL + "/rest/cmsMessage/id/" +
                subscription.getCancellationCMS().getId());
        result.setDateActivated(subscription.getDateActivated());
        result.setDateCreated(subscription.getDateCreated());
        result.setDateCancelled(subscription.getDateCancelled());
        result.setGroup(GroupDto.BASIC(subscription.getGroup()));
        result.setUser(UserDto.BASIC(subscription.getUser()));
        return result;
    }

    private void validate() throws ValidationException {
        if(operation == null) throw new ValidationException("missing param 'operation'");
        if(groupId == null) throw new ValidationException("missing param 'groupId'");
        if(groupName == null) throw new ValidationException("missing param 'groupName'");
        if(userName == null) throw new ValidationException("missing param 'userName'");
        if(userNIF == null) throw new ValidationException("missing param 'userNIF'");
    }

    public void validateActivationRequest() throws ValidationException {
        validate();
        if(TypeVS.CURRENCY_GROUP_USER_ACTIVATE != operation) throw new ValidationException(
                "Operation expected: 'CURRENCY_GROUP_USER_ACTIVATE' - operation found: " + operation);
    }

    public void loadActivationRequest() {
        operation = TypeVS.CURRENCY_GROUP_USER_ACTIVATE;
        groupId = group.getId();
        groupName = group.getName();
        userName = user.getName();
        userNIF = user.getNIF();
        UUID = java.util.UUID.randomUUID().toString();
    }

    public void validateDeActivationRequest() throws ValidationException {
        validate();
        if(TypeVS.CURRENCY_GROUP_USER_DEACTIVATE != operation) throw new ValidationException(
                "Operation expected: 'CURRENCY_GROUP_USER_DEACTIVATE' - operation found: " + operation);
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupInfo() {
        return groupInfo;
    }

    public void setGroupInfo(String groupInfo) {
        this.groupInfo = groupInfo;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserNIF() {
        return userNIF;
    }

    public void setUserNIF(String userNIF) {
        this.userNIF = userNIF;
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

    public Subscription.State getState() {
        return state;
    }

    public void setState(Subscription.State state) {
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

    public GroupDto getGroup() {
        return group;
    }

    public void setGroup(GroupDto group) {
        this.group = group;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}
