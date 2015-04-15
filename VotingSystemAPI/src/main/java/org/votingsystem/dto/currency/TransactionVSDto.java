package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;

import java.math.BigDecimal;
import java.util.*;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionVSDto {

    private TypeVS operation;
    private Long id;
    private UserVSDto fromUserVS;
    private UserVSDto toUserVS;
    private Date validTo;
    private Date dateCreated;
    private String subject;
    private String description;
    private String currencyCode;
    private String fromUser;
    private String fromUserIBAN;
    private String receipt;
    private String messageSMIME;
    private String messageSMIMEURL;
    private String UUID;
    private BigDecimal amount;
    private Boolean isTimeLimited = Boolean.FALSE;
    private Integer numReceptors;
    private TransactionVS.Type type;
    private Set<String> tags;
    private List<String> toUserIBAN = new ArrayList<>();
    private Long numChildTransactions;

    private TransactionVS.Type transactionType;
    @JsonIgnore private List<UserVS> toUserVSList;
    @JsonIgnore private GroupVS groupVS;
    @JsonIgnore private UserVS signer;
    @JsonIgnore private UserVS receptor;
    @JsonIgnore private TagVS tag;
    @JsonIgnore private MessageSMIME transactionVSSMIME;


    public TransactionVSDto() {}


    public void validate() throws ValidationExceptionVS {
        if(operation == null) throw new ValidationExceptionVS("missing param 'operation'");
        transactionType = TransactionVS.Type.valueOf(operation.toString());
        if(amount == null) throw new ValidationExceptionVS("missing param 'amount'");
        if(getCurrencyCode() == null) throw new ValidationExceptionVS("missing param 'currencyCode'");
        if(subject == null) throw new ValidationExceptionVS("missing param 'subject'");
        if(isTimeLimited) validTo = DateUtils.getCurrentWeekPeriod().getDateTo();
        if(tags.size() != 1) { //for now transactions can only have one tag associated
            throw new ValidationExceptionVS("invalid number of tags:" + tags.size());
        }
    }


    public TransactionVSDto(TransactionVS transactionVS, String contextURL) {
        this.setId(transactionVS.getId());
        if(transactionVS.getFromUserVS() != null) {
            this.setFromUserVS(UserVSDto.BASIC(transactionVS.getFromUserVS()));
            if(transactionVS.getFromUserIBAN() != null) {
                Map senderMap = new HashMap<>();
                senderMap.put("fromUserIBAN", transactionVS.getFromUserIBAN());
                senderMap.put("fromUser", transactionVS.getFromUser());
                this.getFromUserVS().setSender(senderMap);
            }
        }
        if(transactionVS.getToUserVS() != null) {
            this.setToUserVS(UserVSDto.BASIC(transactionVS.getToUserVS()));
        }
        this.setValidTo(transactionVS.getValidTo());
        this.setDateCreated(transactionVS.getDateCreated());
        this.setSubject(transactionVS.getSubject());
        this.setAmount(transactionVS.getAmount());
        this.setType(transactionVS.getType());
        this.setCurrencyCode(transactionVS.getCurrencyCode());
        if(transactionVS.getMessageSMIME() != null) {
            setMessageSMIMEURL(contextURL + "/rest/messageSMIME/id/" + transactionVS.getMessageSMIME().getId());
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserVSDto getFromUserVS() {
        return fromUserVS;
    }

    public void setFromUserVS(UserVSDto fromUserVS) {
        this.fromUserVS = fromUserVS;
    }

    public UserVSDto getToUserVS() {
        return toUserVS;
    }

    public void setToUserVS(UserVSDto toUserVS) {
        this.toUserVS = toUserVS;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getMessageSMIMEURL() {
        return messageSMIMEURL;
    }

    public void setMessageSMIMEURL(String messageSMIMEURL) {
        this.messageSMIMEURL = messageSMIMEURL;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionVS.Type getType() {
        return type;
    }

    public void setType(TransactionVS.Type type) {
        this.type = type;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Long getNumChildTransactions() {
        return numChildTransactions;
    }

    public void setNumChildTransactions(Long numChildTransactions) {
        this.numChildTransactions = numChildTransactions;
    }

    public String getMessageSMIME() {
        return messageSMIME;
    }

    public void setMessageSMIME(String messageSMIME) {
        this.messageSMIME = messageSMIME;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String getFromUserIBAN() {
        return fromUserIBAN;
    }

    public void setFromUserIBAN(String fromUserIBAN) {
        this.fromUserIBAN = fromUserIBAN;
    }

    public Boolean isTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }

    public Integer getNumReceptors() {
        return numReceptors;
    }

    public void setNumReceptors(Integer numReceptors) {
        this.numReceptors = numReceptors;
    }

    public TransactionVS.Type getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionVS.Type transactionType) {
        this.transactionType = transactionType;
    }

    public List<UserVS> getToUserVSList() {
        return toUserVSList;
    }

    public void setToUserVSList(List<UserVS> toUserVSList) {
        this.toUserVSList = toUserVSList;
        this.numReceptors = toUserVSList.size();
    }

    public GroupVS getGroupVS() {
        return groupVS;
    }

    public void setGroupVS(GroupVS groupVS) {
        this.groupVS = groupVS;
    }

    public TagVS getTag() {
        return tag;
    }

    public void setTag(TagVS tag) {
        this.tag = tag;
    }

    public List<String> getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(List<String> toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public UserVS getSigner() {
        return signer;
    }

    public void setSigner(UserVS signer) {
        this.signer = signer;
    }

    public UserVS getReceptor() {
        return receptor;
    }

    public void setReceptor(UserVS receptor) {
        this.receptor = receptor;
    }

    public MessageSMIME getTransactionVSSMIME() {
        return transactionVSSMIME;
    }

    public void setTransactionVSSMIME(MessageSMIME transactionVSSMIME) {
        this.transactionVSSMIME = transactionVSSMIME;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}
