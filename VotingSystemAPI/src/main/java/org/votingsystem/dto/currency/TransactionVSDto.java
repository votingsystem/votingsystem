package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.UserDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
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
    private Long userId;
    private UserDto fromUser;
    private UserDto toUser;
    private Date validTo;
    private Date dateCreated;
    private String subject;
    private String description;
    private String currencyCode;
    private String fromUserName;
    private String toUserName;
    private String fromUserIBAN;
    private String receipt;
    private String bankIBAN;
    private String cmsMessagePEM;
    private String cmsMessageURL;
    private String cmsMessageParentURL;
    private String UUID;
    private BigDecimal amount;
    private Boolean timeLimited = Boolean.FALSE;
    private Integer numReceptors;
    private TransactionVS.Type type;
    private Set<String> tags;
    private Set<String> toUserIBAN = null;
    private Long numChildTransactions;

    private User.Type userToType;
    private List<TransactionVS.Type> paymentOptions;
    private TransactionVSDetailsDto details;

    @JsonIgnore private List<User> toUserList;
    @JsonIgnore private User signer;
    @JsonIgnore private User receptor;
    @JsonIgnore private CMSMessage cmsMessage_DB;


    public TransactionVSDto() {}

    public TransactionVSDto(TransactionVS transactionVS) {
        this.setId(transactionVS.getId());
        if(transactionVS.getFromUser() != null) {
            this.setFromUser(UserDto.BASIC(transactionVS.getFromUser()));
        }
        fromUserIBAN = transactionVS.getFromUserIBAN();
        fromUserName = transactionVS.getFromUserName();
        if(transactionVS.getToUser() != null) {
            this.setToUser(UserDto.BASIC(transactionVS.getToUser()));
        }
        this.setValidTo(transactionVS.getValidTo());
        this.setDateCreated(transactionVS.getDateCreated());
        this.setSubject(transactionVS.getSubject());
        this.setAmount(transactionVS.getAmount());
        this.setType(transactionVS.getType());
        this.setCurrencyCode(transactionVS.getCurrencyCode());
        this.setTimeLimited(transactionVS.getIsTimeLimited());
    }

    public TransactionVSDto(TransactionVS transactionVS, String contextURL) {
        this(transactionVS);
        if(transactionVS.getCmsMessage() != null) {
            setCmsMessageURL(contextURL + "/rest/cmsMessage/id/" + transactionVS.getCmsMessage().getId());
        }
    }

    public static TransactionVSDto PAYMENT_REQUEST(String toUser, User.Type userToType, BigDecimal amount,
                                                   String currencyCode, String toUserIBAN, String subject, String tag) {
        TransactionVSDto dto = new TransactionVSDto();
        dto.setOperation(TypeVS.TRANSACTIONVS_INFO);
        dto.setUserToType(userToType);
        dto.setToUserName(toUser);
        dto.setAmount(amount);
        dto.setCurrencyCode(currencyCode);
        dto.setSubject(subject);
        dto.setToUserIBAN(Sets.newHashSet(toUserIBAN));
        dto.setTags(Sets.newHashSet(tag));
        dto.setDateCreated(new Date());
        dto.setUUID(java.util.UUID.randomUUID().toString());
        return dto;
    }

    public static TransactionVSDto CURRENCY_SEND(String toUser, String subject, BigDecimal amount,
                   String currencyCode, String toUserIBAN, boolean isTimeLimited, String tag) {
        TransactionVSDto dto = new TransactionVSDto();
        dto.setOperation(TypeVS.CURRENCY_SEND);
        dto.setSubject(subject);
        dto.setToUserName(toUser);
        dto.setAmount(amount);
        dto.setToUserIBAN(Sets.newHashSet(toUserIBAN));
        dto.setTags(Sets.newHashSet(tag));
        dto.setCurrencyCode(currencyCode);
        dto.setTimeLimited(isTimeLimited);
        dto.setUUID(java.util.UUID.randomUUID().toString());
        return dto;
    }

    public static TransactionVSDto BASIC(String toUser, User.Type userToType, BigDecimal amount,
                                         String currencyCode, String toUserIBAN, String subject, String tag) {
        TransactionVSDto dto = new TransactionVSDto();
        dto.setUserToType(userToType);
        dto.setToUserName(toUser);
        dto.setAmount(amount);
        dto.setCurrencyCode(currencyCode);
        dto.setSubject(subject);
        dto.setToUserIBAN(Sets.newHashSet(toUserIBAN));
        dto.setTags(Sets.newHashSet(tag));
        dto.setUUID(java.util.UUID.randomUUID().toString());
        return dto;
    }

    public void validate() throws ValidationExceptionVS {
        if(operation == null) throw new ValidationExceptionVS("missing param 'operation'");
        type = TransactionVS.Type.valueOf(operation.toString());
        if(amount == null) throw new ValidationExceptionVS("missing param 'amount'");
        if(getCurrencyCode() == null) throw new ValidationExceptionVS("missing param 'currencyCode'");
        if(subject == null) throw new ValidationExceptionVS("missing param 'subject'");
        if(timeLimited) validTo = DateUtils.getCurrentWeekPeriod().getDateTo();
        if (tags.size() != 1) { //for now transactions can only have one tag associated
            throw new ValidationExceptionVS("invalid number of tags:" + tags.size());
        }
    }

    @JsonIgnore
    public TransactionVS getTransactionVS(TagVS tagVS) throws Exception {
        TransactionVS transactionVS = new TransactionVS();
        transactionVS.setId(id);
        transactionVS.setFromUserName(fromUserName);
        transactionVS.setUserId(getUserId());
        transactionVS.setType(type);
        if(toUser != null) {
            transactionVS.setToUser(toUser.getUser());
        }
        transactionVS.setFromUserIBAN(fromUserIBAN);
        if(fromUser != null) {
            transactionVS.setFromUser(fromUser.getUser());
        }
        transactionVS.setTag(tagVS);
        transactionVS.setIsTimeLimited(timeLimited);
        transactionVS.setSubject(subject);
        transactionVS.setCurrencyCode(currencyCode);
        transactionVS.setDateCreated(dateCreated);
        transactionVS.setValidTo(validTo);
        transactionVS.setAmount(amount);
        return transactionVS;
    }

    @JsonIgnore
    public TransactionVS getTransactionVS(User fromUser, User toUser,
                                          Map<CurrencyAccount, BigDecimal> accountFromMovements, TagVS tagVS) throws Exception {
        TransactionVS transactionVS = new TransactionVS();
        transactionVS.setFromUser(fromUser);
        transactionVS.setFromUserIBAN(fromUser.getIBAN());
        transactionVS.setToUser(toUser);
        if(toUser != null) transactionVS.setToUserIBAN(toUser.getIBAN());
        transactionVS.setType(getType());
        transactionVS.setAccountFromMovements(accountFromMovements);
        transactionVS.setAmount(amount);
        transactionVS.setCurrencyCode(currencyCode);
        transactionVS.setSubject(subject);
        transactionVS.setValidTo(validTo);
        transactionVS.setCmsMessage(cmsMessage_DB);
        transactionVS.setState(TransactionVS.State.OK);
        transactionVS.setTag(tagVS);
        return transactionVS;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserDto getFromUser() {
        return fromUser;
    }

    public void setFromUser(UserDto fromUser) {
        this.fromUser = fromUser;
    }

    public UserDto getToUser() {
        return toUser;
    }

    public void setToUser(UserDto toUser) {
        this.toUser = toUser;
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


    public String getCmsMessageURL() {
        return cmsMessageURL;
    }

    public void setCmsMessageURL(String cmsMessageURL) {
        this.cmsMessageURL = cmsMessageURL;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionVS.Type getType() {
        if(type == null && operation != null) type = TransactionVS.Type.valueOf(operation.toString());
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

    public String getCmsMessagePEM() {
        return cmsMessagePEM;
    }

    public void setCmsMessagePEM(String cmsMessagePEM) {
        this.cmsMessagePEM = cmsMessagePEM;
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

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public String getFromUserIBAN() {
        return fromUserIBAN;
    }

    public void setFromUserIBAN(String fromUserIBAN) {
        this.fromUserIBAN = fromUserIBAN;
    }

    public Integer getNumReceptors() {
        return numReceptors;
    }

    public void setNumReceptors(Integer numReceptors) {
        this.numReceptors = numReceptors;
    }

    public List<User> getToUserList() {
        return toUserList;
    }

    public void setToUserList(List<User> toUserList) {
        this.toUserList = toUserList;
        this.numReceptors = toUserList.size();
    }


    @JsonIgnore public String getTagName() {
        if (tags != null && !tags.isEmpty()) return tags.iterator().next();
        return null;
    }

    public Set<String> getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(Set<String> toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public User getSigner() {
        return signer;
    }

    public void setSigner(User signer) {
        this.signer = signer;
    }

    public User getReceptor() {
        return receptor;
    }

    public void setReceptor(User receptor) {
        this.receptor = receptor;
    }

    public CMSMessage getCmsMessage_DB() {
        return cmsMessage_DB;
    }

    public void setCmsMessage_DB(CMSMessage cmsMessage_DB) {
        this.cmsMessage_DB = cmsMessage_DB;
        this.signer = cmsMessage_DB.getUser();
    }

    public String getUUID() {
        return UUID;
    }

    public void loadBankTransaction(String UUID) {
        setUUID(UUID);
        if((toUserIBAN == null || toUserIBAN.isEmpty()) && toUser != null) {
            toUserIBAN = Sets.newHashSet(toUser.getIBAN());
            toUser = null;
        }
    }

    public TransactionVSDto getGroupChild(String receptorNIF, BigDecimal receptorPart, Integer numReceptors,
                                          String contextURL) {
        TransactionVSDto dto =  new TransactionVSDto();
        dto.setOperation(operation);
        dto.setToUserName(receptorNIF);
        dto.setAmount(receptorPart);
        dto.setCmsMessageParentURL(contextURL + "/rest/cmsMessage/id/" + cmsMessage_DB.getId());
        dto.setNumReceptors(numReceptors);
        return dto;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getBankIBAN() {
        return bankIBAN;
    }

    public void setBankIBAN(String bankIBAN) {
        this.bankIBAN = bankIBAN;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getCmsMessageParentURL() {
        return cmsMessageParentURL;
    }

    public void setCmsMessageParentURL(String cmsMessageParentURL) {
        this.cmsMessageParentURL = cmsMessageParentURL;
    }

    public User.Type getUserToType() {
        return userToType;
    }

    public void setUserToType(User.Type userToType) {
        this.userToType = userToType;
    }

    public List<TransactionVS.Type> getPaymentOptions() {
        return paymentOptions;
    }

    public void setPaymentOptions(List<TransactionVS.Type> paymentOptions) {
        this.paymentOptions = paymentOptions;
    }

    public String validateReceipt(CMSSignedMessage cmsMessage, boolean isIncome) throws Exception {
        TransactionVSDto dto = cmsMessage.getSignedContent(TransactionVSDto.class);
        switch(dto.getOperation()) {
            case FROM_USER:
                return validateFromUserReceipt(cmsMessage, isIncome);
            case CURRENCY_SEND:
                return validateCurrencySendReceipt(cmsMessage, isIncome);
            case CURRENCY_CHANGE:
                return validateCurrencyChangeReceipt(cmsMessage, isIncome);
            default: throw new ValidationExceptionVS("unknown operation: " + dto.getOperation());
        }
    }

    private String validateCurrencySendReceipt(CMSSignedMessage cmsMessage, boolean isIncome) throws Exception {
        CurrencyBatchDto receiptDto = cmsMessage.getSignedContent(CurrencyBatchDto.class);
        if(TypeVS.CURRENCY_SEND != receiptDto.getOperation()) throw new ValidationExceptionVS("ERROR - expected type: " +
                TypeVS.CURRENCY_SEND + " - found: " + receiptDto.getOperation());
        if(type == TransactionVS.Type.TRANSACTIONVS_INFO) {
            if(!paymentOptions.contains(TransactionVS.Type.CURRENCY_SEND)) throw new ValidationExceptionVS(
                    "unexpected type: " + receiptDto.getOperation());
        }
        Set<String> receptorsSet = Sets.newHashSet(receiptDto.getToUserIBAN());
        if(!toUserIBAN.equals(receptorsSet)) throw new ValidationExceptionVS(
                "expected toUserIBAN " + toUserIBAN + " found " + receiptDto.getToUserIBAN());
        if(amount.compareTo(receiptDto.getBatchAmount()) != 0) throw new ValidationExceptionVS(
                "expected amount " + amount + " amount " + receiptDto.getBatchAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationExceptionVS(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getBatchUUID())) throw new ValidationExceptionVS(
                "expected UUID " + UUID + " found " + receiptDto.getBatchUUID());
        String action = isIncome ? ContextVS.getMessage("income_lbl"): ContextVS.getMessage("expense_lbl");
        return ContextVS.getMessage("currency_send_receipt_ok_msg", action, receiptDto.getBatchAmount() + " " +
                receiptDto.getCurrencyCode(), receiptDto.getTag());
    }

    private String validateFromUserReceipt(CMSSignedMessage cmsMessage, boolean isIncome) throws Exception {
        TransactionVSDto receiptDto = cmsMessage.getSignedContent(TransactionVSDto.class);
        if(type == TransactionVS.Type.TRANSACTIONVS_INFO) {
            if(!paymentOptions.contains(receiptDto.getType())) throw new ValidationExceptionVS("unexpected type " +
                    receiptDto.getType());
        } else if(type != receiptDto.getType()) throw new ValidationExceptionVS("expected type " + type + " found " +
                receiptDto.getType());
        if(userToType != receiptDto.getUserToType()) throw new ValidationExceptionVS("expected userToType " + userToType +
                " found " + receiptDto.getUserToType());
        if(!new HashSet<>(toUserIBAN).equals(new HashSet<>(receiptDto.getToUserIBAN())) ||
                toUserIBAN.size() != receiptDto.getToUserIBAN().size()) throw new ValidationExceptionVS(
                "expected toUserIBAN " + toUserIBAN + " found " + receiptDto.getToUserIBAN());
        if(!subject.equals(receiptDto.getSubject())) throw new ValidationExceptionVS("expected subject " + subject +
                " found " + receiptDto.getSubject());
        if(!toUserName.equals(receiptDto.getToUserName())) throw new ValidationExceptionVS(
                "expected toUserName " + toUserName + " found " + receiptDto.getToUserName());
        if(amount.compareTo(receiptDto.getAmount()) != 0) throw new ValidationExceptionVS(
                "expected amount " + amount + " amount " + receiptDto.getAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationExceptionVS(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getUUID())) throw new ValidationExceptionVS(
                "expected UUID " + UUID + " found " + receiptDto.getUUID());
        if(details != null && !details.equals(receiptDto.getDetails())) throw new ValidationExceptionVS(
                "expected details " + details + " found " + receiptDto.getDetails());
        String action = isIncome ? ContextVS.getMessage("income_lbl"): ContextVS.getMessage("expense_lbl");
        return ContextVS.getMessage("from_user_receipt_ok_msg", action, receiptDto.getAmount() + " " +
                receiptDto.getCurrencyCode(), receiptDto.getTagName());
    }

    private String validateCurrencyChangeReceipt(CMSSignedMessage cmsMessage, boolean isIncome) throws Exception {
        CurrencyBatchDto receiptDto = cmsMessage.getSignedContent(CurrencyBatchDto.class);
        if(TypeVS.CURRENCY_CHANGE != receiptDto.getOperation()) throw new ValidationExceptionVS("ERROR - expected type: " +
                TypeVS.CURRENCY_CHANGE + " - found: " + receiptDto.getOperation());
        if(type == TransactionVS.Type.TRANSACTIONVS_INFO) {
            if(!paymentOptions.contains(TransactionVS.Type.CURRENCY_CHANGE)) throw new ValidationExceptionVS(
                    "unexpected type: " + receiptDto.getOperation());
        }
        if(amount.compareTo(receiptDto.getBatchAmount()) != 0) throw new ValidationExceptionVS(
                "expected amount " + amount + " amount " + receiptDto.getBatchAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationExceptionVS(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getBatchUUID())) throw new ValidationExceptionVS(
                "expected UUID " + UUID + " found " + receiptDto.getBatchUUID());
        String action = isIncome ? ContextVS.getMessage("income_lbl"): ContextVS.getMessage("expense_lbl");

        String result = ContextVS.getMessage("currency_change_receipt_ok_msg", action, receiptDto.getBatchAmount() + " " +
                receiptDto.getCurrencyCode(), receiptDto.getTag());
        if(receiptDto.getTimeLimited()) {
            result = result + " - " + ContextVS.getMessage("time_remaining_lbl");
        }
        return result;
    }

    public TransactionVSDetailsDto getDetails() {
        return details;
    }

    public void setDetails(TransactionVSDetailsDto details) {
        this.details = details;
    }

    public Boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }
}
