package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.MessageCMS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
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
    private UserVSDto fromUserVS;
    private UserVSDto toUserVS;
    private Date validTo;
    private Date dateCreated;
    private String subject;
    private String description;
    private String currencyCode;
    private String fromUser;
    private String toUser;
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

    private UserVS.Type userToType;
    private List<TransactionVS.Type> paymentOptions;
    private TransactionVSDetailsDto details;

    @JsonIgnore private List<UserVS> toUserVSList;
    @JsonIgnore private UserVS signer;
    @JsonIgnore private UserVS receptor;
    @JsonIgnore private MessageCMS messageCMS_DB;


    public TransactionVSDto() {}

    public TransactionVSDto(TransactionVS transactionVS) {
        this.setId(transactionVS.getId());
        if(transactionVS.getFromUserVS() != null) {
            this.setFromUserVS(UserVSDto.BASIC(transactionVS.getFromUserVS()));
        }
        fromUserIBAN = transactionVS.getFromUserIBAN();
        fromUser = transactionVS.getFromUser();
        if(transactionVS.getToUserVS() != null) {
            this.setToUserVS(UserVSDto.BASIC(transactionVS.getToUserVS()));
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
        if(transactionVS.getMessageCMS() != null) {
            setCmsMessageURL(contextURL + "/rest/cmsMessagePEM/id/" + transactionVS.getMessageCMS().getId());
        }
    }

    public static TransactionVSDto PAYMENT_REQUEST(String toUser, UserVS.Type userToType, BigDecimal amount,
               String currencyCode, String toUserIBAN, String subject, String tag) {
        TransactionVSDto dto = new TransactionVSDto();
        dto.setOperation(TypeVS.TRANSACTIONVS_INFO);
        dto.setUserToType(userToType);
        dto.setToUser(toUser);
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
        dto.setToUser(toUser);
        dto.setAmount(amount);
        dto.setToUserIBAN(Sets.newHashSet(toUserIBAN));
        dto.setTags(Sets.newHashSet(tag));
        dto.setCurrencyCode(currencyCode);
        dto.setTimeLimited(isTimeLimited);
        dto.setUUID(java.util.UUID.randomUUID().toString());
        return dto;
    }

    public static TransactionVSDto BASIC(String toUser, UserVS.Type userToType, BigDecimal amount,
                 String currencyCode, String toUserIBAN, String subject, String tag) {
        TransactionVSDto dto = new TransactionVSDto();
        dto.setUserToType(userToType);
        dto.setToUser(toUser);
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
        transactionVS.setFromUser(fromUser);
        transactionVS.setUserId(getUserId());
        transactionVS.setType(type);
        if(toUserVS != null) {
            transactionVS.setToUserVS(toUserVS.getUserVS());
        }
        transactionVS.setFromUserIBAN(fromUserIBAN);
        if(fromUserVS != null) {
            transactionVS.setFromUserVS(fromUserVS.getUserVS());
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
    public TransactionVS getTransactionVS(UserVS fromUserVS, UserVS toUserVS,
                  Map<CurrencyAccount, BigDecimal> accountFromMovements, TagVS tagVS) throws Exception {
        TransactionVS transactionVS = new TransactionVS();
        transactionVS.setFromUserVS(fromUserVS);
        transactionVS.setFromUserIBAN(fromUserVS.getIBAN());
        transactionVS.setToUserVS(toUserVS);
        if(toUserVS != null) transactionVS.setToUserIBAN(toUserVS.getIBAN());
        transactionVS.setType(getType());
        transactionVS.setAccountFromMovements(accountFromMovements);
        transactionVS.setAmount(amount);
        transactionVS.setCurrencyCode(currencyCode);
        transactionVS.setSubject(subject);
        transactionVS.setValidTo(validTo);
        transactionVS.setMessageCMS(messageCMS_DB);
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


    public String getCMSMessageURL() {
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

    public String getCMSMessagePEM() {
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

    public Integer getNumReceptors() {
        return numReceptors;
    }

    public void setNumReceptors(Integer numReceptors) {
        this.numReceptors = numReceptors;
    }

    public List<UserVS> getToUserVSList() {
        return toUserVSList;
    }

    public void setToUserVSList(List<UserVS> toUserVSList) {
        this.toUserVSList = toUserVSList;
        this.numReceptors = toUserVSList.size();
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

    public MessageCMS getMessageCMS_DB() {
        return messageCMS_DB;
    }

    public void setMessageCMS_DB(MessageCMS messageCMS_DB) {
        this.messageCMS_DB = messageCMS_DB;
        this.signer = messageCMS_DB.getUserVS();
    }

    public String getUUID() {
        return UUID;
    }

    public void loadBankVSTransaction(String UUID) {
        setUUID(UUID);
        if((toUserIBAN == null || toUserIBAN.isEmpty()) && toUserVS != null) {
            toUserIBAN = Sets.newHashSet(toUserVS.getIBAN());
            toUserVS = null;
        }
    }

    public TransactionVSDto getGroupVSChild(String receptorNIF, BigDecimal receptorPart, Integer numReceptors,
                String contextURL) {
        TransactionVSDto dto =  new TransactionVSDto();
        dto.setOperation(operation);
        dto.setToUser(receptorNIF);
        dto.setAmount(receptorPart);
        dto.setCmsMessageParentURL(contextURL + "/rest/cmsMessagePEM/id/" + messageCMS_DB.getId());
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

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getCMSMessageParentURL() {
        return cmsMessageParentURL;
    }

    public void setCmsMessageParentURL(String cmsMessageParentURL) {
        this.cmsMessageParentURL = cmsMessageParentURL;
    }

    public UserVS.Type getUserToType() {
        return userToType;
    }

    public void setUserToType(UserVS.Type userToType) {
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
            case FROM_USERVS:
                return validateFromUserVSReceipt(cmsMessage, isIncome);
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

    private String validateFromUserVSReceipt(CMSSignedMessage cmsMessage, boolean isIncome) throws Exception {
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
        if(!toUser.equals(receiptDto.getToUser())) throw new ValidationExceptionVS(
                "expected toUser " + toUser + " found " + receiptDto.getToUser());
        if(amount.compareTo(receiptDto.getAmount()) != 0) throw new ValidationExceptionVS(
                "expected amount " + amount + " amount " + receiptDto.getAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationExceptionVS(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getUUID())) throw new ValidationExceptionVS(
                "expected UUID " + UUID + " found " + receiptDto.getUUID());
        if(details != null && !details.equals(receiptDto.getDetails())) throw new ValidationExceptionVS(
                "expected details " + details + " found " + receiptDto.getDetails());
        String action = isIncome ? ContextVS.getMessage("income_lbl"): ContextVS.getMessage("expense_lbl");
        return ContextVS.getMessage("from_uservs_receipt_ok_msg", action, receiptDto.getAmount() + " " +
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
