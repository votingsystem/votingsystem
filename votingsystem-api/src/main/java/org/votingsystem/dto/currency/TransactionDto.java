package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Sets;
import org.votingsystem.dto.UserDto;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Tag;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Messages;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDto {

    private static Logger log = Logger.getLogger(TransactionDto.class.getName());

    private CurrencyOperation operation;
    private Long id;
    private Long userId;
    private UserDto fromUser;
    private UserDto toUser;
    private ZonedDateTime validTo;
    private ZonedDateTime dateCreated;
    private String subject;
    private String description;
    private CurrencyCode currencyCode;
    private String fromUserName;
    private String toUserName;
    private String fromUserIBAN;
    private String bankIBAN;
    private String signedDocumentBase64;
    private String UUID;
    private BigDecimal amount;
    private Boolean timeLimited = Boolean.FALSE;
    private Integer numReceptors;
    private Transaction.Type type;
    private Set<String> tags;
    private Set<String> toUserIBAN = null;
    private Long numChildTransactions;

    private User.Type userToType;
    private List<Transaction.Type> paymentOptions;
    private TransactionDetailsDto details;

    @JsonIgnore
    private List<User> toUserList;
    @JsonIgnore
    private User signer;
    @JsonIgnore
    private User receptor;
    @JsonIgnore
    private SignedDocument signedDocument;


    public TransactionDto() {}

    public TransactionDto(Transaction transaction) {
        this.id = transaction.getId();
        if(transaction.getFromUser() != null) {
            this.fromUser = UserDto.BASIC(transaction.getFromUser());
        }
        fromUserIBAN = transaction.getFromUserIBAN();
        fromUserName = transaction.getFromUserName();
        if(transaction.getToUser() != null) {
            this.toUser = UserDto.BASIC(transaction.getToUser());
        }
        this.validTo = ZonedDateTime.of(transaction.getValidTo(), ZoneId.systemDefault());
        this.dateCreated = ZonedDateTime.of(transaction.getDateCreated(), ZoneId.systemDefault());
        this.subject = transaction.getSubject();
        this.amount = transaction.getAmount();
        this.type = transaction.getType();
        this.currencyCode = transaction.getCurrencyCode();
        this.timeLimited = transaction.getIsTimeLimited();
    }

    public static TransactionDto PAYMENT_REQUEST(String toUser, User.Type userToType, BigDecimal amount,
            CurrencyCode currencyCode, String toUserIBAN, String subject, String tagName) {
        TransactionDto dto = new TransactionDto();
        dto.setOperation(CurrencyOperation.TRANSACTION_INFO);
        dto.setUserToType(userToType);
        dto.setToUserName(toUser);
        dto.setAmount(amount);
        dto.setCurrencyCode(currencyCode);
        dto.setSubject(subject);
        dto.setToUserIBAN(Sets.newHashSet(toUserIBAN));
        dto.setTags(Sets.newHashSet(tagName));
        dto.setDateCreated(ZonedDateTime.now());
        dto.setUUID(java.util.UUID.randomUUID().toString());
        return dto;
    }

    public static TransactionDto CURRENCY_SEND(String toUser, String subject, BigDecimal amount,
                                   CurrencyCode currencyCode, String toUserIBAN, boolean isTimeLimited, String tag) {
        TransactionDto dto = new TransactionDto();
        dto.setOperation(CurrencyOperation.CURRENCY_SEND);
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

    public static TransactionDto BASIC(String toUser, User.Type userToType, BigDecimal amount,
                                       CurrencyCode currencyCode, String toUserIBAN, String subject, String tag) {
        TransactionDto dto = new TransactionDto();
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

    public void validate() throws ValidationException {
        if(operation == null)
            throw new ValidationException("missing param 'operation'");
        type = Transaction.Type.valueOf(operation.toString());
        if(amount == null)
            throw new ValidationException("missing param 'amount'");
        if(getCurrencyCode() == null)
            throw new ValidationException("missing param 'currencyCode'");
        if(subject == null)
            throw new ValidationException("missing param 'subject'");
        if(timeLimited)
            validTo =  DateUtils.getWeekPeriod(LocalDateTime.now()).getDateTo();
        if (tags.size() != 1) { //for now transactions can only have one tag associated
            throw new ValidationException("invalid number of tags:" + tags.size());
        }
    }

    @JsonIgnore
    public Transaction getTransaction(Tag tag) throws Exception {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setFromUserName(fromUserName);
        transaction.setUserId(getUserId());
        transaction.setType(type);
        if(toUser != null) {
            transaction.setToUser(toUser.getUser());
        }
        transaction.setFromUserIBAN(fromUserIBAN);
        if(fromUser != null) {
            transaction.setFromUser(fromUser.getUser());
        }
        transaction.setTag(tag);
        transaction.setIsTimeLimited(timeLimited);
        transaction.setSubject(subject);
        transaction.setCurrencyCode(currencyCode);
        transaction.setDateCreated(dateCreated.toLocalDateTime());
        transaction.setValidTo(validTo.toLocalDateTime());
        transaction.setAmount(amount);
        return transaction;
    }

    @JsonIgnore
    public Transaction getTransaction(User fromUser, User toUser, Map<CurrencyAccount, BigDecimal> accountFromMovements,
                                  Tag tag) throws Exception {
        Transaction transaction = new Transaction();
        transaction.setFromUser(fromUser);
        transaction.setFromUserIBAN(fromUser.getIBAN());
        transaction.setToUser(toUser);
        if(toUser != null)
            transaction.setToUserIBAN(toUser.getIBAN());
        transaction.setType(getType());
        transaction.setAccountFromMovements(accountFromMovements);
        transaction.setAmount(amount);
        transaction.setCurrencyCode(currencyCode);
        transaction.setSubject(subject);
        transaction.setValidTo(validTo.toLocalDateTime());
        transaction.setState(Transaction.State.OK);
        transaction.setTag(tag);
        return transaction;
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

    public ZonedDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(ZonedDateTime validTo) {
        this.validTo = validTo;
    }

    public ZonedDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(ZonedDateTime dateCreated) {
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Transaction.Type getType() {
        if(type == null && operation != null) type = Transaction.Type.valueOf(operation.toString());
        return type;
    }

    public void setType(Transaction.Type type) {
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

    public CurrencyOperation getOperation() {
        return operation;
    }

    public void setOperation(CurrencyOperation operation) {
        this.operation = operation;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
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

    @JsonIgnore
    public String getTagName() {
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

    public TransactionDto getGroupChild(String receptorNIF, BigDecimal receptorPart, Integer numReceptors,
                                        String contextURL) {
        TransactionDto dto =  new TransactionDto();
        dto.setOperation(operation);
        dto.setToUserName(receptorNIF);
        dto.setAmount(receptorPart);
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

    public User.Type getUserToType() {
        return userToType;
    }

    public void setUserToType(User.Type userToType) {
        this.userToType = userToType;
    }

    public List<Transaction.Type> getPaymentOptions() {
        return paymentOptions;
    }

    public void setPaymentOptions(List<Transaction.Type> paymentOptions) {
        this.paymentOptions = paymentOptions;
    }

    public String validateReceipt(SignedDocument signedDocument, boolean isIncome) throws Exception {
        TransactionDto dto = signedDocument.getSignedContent(TransactionDto.class);
        switch(dto.getOperation()) {
            case TRANSACTION_FROM_USER:
                return validateFromUserReceipt(signedDocument, isIncome);
            case CURRENCY_SEND:
                return validateCurrencySendReceipt(signedDocument, isIncome);
            case CURRENCY_CHANGE:
                return validateCurrencyChangeReceipt(signedDocument, isIncome);
            default: throw new ValidationException("unknown operation: " + dto.getOperation());
        }
    }

    private String validateCurrencySendReceipt(SignedDocument signedDocument, boolean isIncome) throws Exception {
        CurrencyBatchDto receiptDto = signedDocument.getSignedContent(CurrencyBatchDto.class);
        if(CurrencyOperation.CURRENCY_SEND != receiptDto.getOperation()) throw new ValidationException("ERROR - expected type: " +
                CurrencyOperation.CURRENCY_SEND + " - found: " + receiptDto.getOperation());
        if(type == Transaction.Type.TRANSACTION_INFO) {
            if(!paymentOptions.contains(Transaction.Type.CURRENCY_SEND)) throw new ValidationException(
                    "unexpected type: " + receiptDto.getOperation());
        }
        Set<String> receptorsSet = Sets.newHashSet(receiptDto.getToUserIBAN());
        if(!toUserIBAN.equals(receptorsSet)) throw new ValidationException(
                "expected toUserIBAN " + toUserIBAN + " found " + receiptDto.getToUserIBAN());
        if(amount.compareTo(receiptDto.getBatchAmount()) != 0) throw new ValidationException(
                "expected amount " + amount + " amount " + receiptDto.getBatchAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationException(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getBatchUUID())) throw new ValidationException(
                "expected UUID " + UUID + " found " + receiptDto.getBatchUUID());
        String action = isIncome ? Messages.currentInstance().get("income_lbl"): Messages.currentInstance().get("expense_lbl");
        return Messages.currentInstance().get("currency_send_receipt_ok_msg", action, receiptDto.getBatchAmount() + " " +
                receiptDto.getCurrencyCode(), receiptDto.getTag());
    }

    private String validateFromUserReceipt(SignedDocument signedDocument, boolean isIncome) throws Exception {
        TransactionDto receiptDto = signedDocument.getSignedContent(TransactionDto.class);
        if(type == Transaction.Type.TRANSACTION_INFO) {
            if(!paymentOptions.contains(receiptDto.getType())) throw new ValidationException("unexpected type " +
                    receiptDto.getType());
        } else if(type != receiptDto.getType()) throw new ValidationException("expected type " + type + " found " +
                receiptDto.getType());
        if(userToType != receiptDto.getUserToType()) throw new ValidationException("expected userToType " + userToType +
                " found " + receiptDto.getUserToType());
        if(!new HashSet<>(toUserIBAN).equals(new HashSet<>(receiptDto.getToUserIBAN())) ||
                toUserIBAN.size() != receiptDto.getToUserIBAN().size()) throw new ValidationException(
                "expected toUserIBAN " + toUserIBAN + " found " + receiptDto.getToUserIBAN());
        if(!subject.equals(receiptDto.getSubject())) throw new ValidationException("expected subject " + subject +
                " found " + receiptDto.getSubject());
        if(!toUserName.equals(receiptDto.getToUserName())) throw new ValidationException(
                "expected toUserName " + toUserName + " found " + receiptDto.getToUserName());
        if(amount.compareTo(receiptDto.getAmount()) != 0) throw new ValidationException(
                "expected amount " + amount + " amount " + receiptDto.getAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationException(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getUUID())) throw new ValidationException(
                "expected UUID " + UUID + " found " + receiptDto.getUUID());
        if(details != null && !details.equals(receiptDto.getDetails())) throw new ValidationException(
                "expected details " + details + " found " + receiptDto.getDetails());
        String action = isIncome ? Messages.currentInstance().get("income_lbl"): Messages.currentInstance().get("expense_lbl");
        return Messages.currentInstance().get("from_user_receipt_ok_msg", action, receiptDto.getAmount() + " " +
                receiptDto.getCurrencyCode(), receiptDto.getTagName());
    }

    private String validateCurrencyChangeReceipt(SignedDocument signedDocument, boolean isIncome) throws Exception {
        log.severe("=========== TODO");
        CurrencyBatchDto receiptDto = signedDocument.getSignedContent(CurrencyBatchDto.class);
        if(CurrencyOperation.CURRENCY_CHANGE != receiptDto.getOperation()) throw new ValidationException("ERROR - expected type: " +
                CurrencyOperation.CURRENCY_CHANGE + " - found: " + receiptDto.getOperation());
        if(type == Transaction.Type.TRANSACTION_INFO) {
            if(!paymentOptions.contains(Transaction.Type.CURRENCY_CHANGE)) throw new ValidationException(
                    "unexpected type: " + receiptDto.getOperation());
        }
        if(amount.compareTo(receiptDto.getBatchAmount()) != 0) throw new ValidationException(
                "expected amount " + amount + " amount " + receiptDto.getBatchAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationException(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getBatchUUID())) throw new ValidationException(
                "expected UUID " + UUID + " found " + receiptDto.getBatchUUID());
        String action = isIncome ? Messages.currentInstance().get("income_lbl"):
                Messages.currentInstance().get("expense_lbl");

        String result = Messages.currentInstance().get("currency_change_receipt_ok_msg", action,
                receiptDto.getBatchAmount() + " " + receiptDto.getCurrencyCode(), receiptDto.getTag());
        if(receiptDto.getTimeLimited()) {
            result = result + " - " + Messages.currentInstance().get("time_remaining_lbl");
        }
        return result;
    }

    public TransactionDetailsDto getDetails() {
        return details;
    }

    public void setDetails(TransactionDetailsDto details) {
        this.details = details;
    }

    public Boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public String getSignedDocumentBase64() {
        return signedDocumentBase64;
    }

    public void setSignedDocumentBase64(String signedDocumentBase64) {
        this.signedDocumentBase64 = signedDocumentBase64;
    }

    public SignedDocument getSignedDocument() {
        return signedDocument;
    }

    public TransactionDto setSignedDocument(SignedDocument signedDocument) {
        this.signedDocument = signedDocument;
        return this;
    }
}
