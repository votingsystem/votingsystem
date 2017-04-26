package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.collect.Sets;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.Messages;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDto {

    private static Logger log = Logger.getLogger(TransactionDto.class.getName());

    @JacksonXmlProperty(localName = "Operation")
    private OperationTypeDto operation;
    private Long id;
    private Long userId;
    private UserDto fromUser;
    private UserDto toUser;
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
    private String toUserIBAN = null;

    private List<CurrencyOperation> paymentOptions;
    private TransactionDetailsDto details;

    @JsonIgnore
    private User signer;
    @JsonIgnore
    private User receptor;
    @JsonIgnore
    private SignedDocument signedDocument;


    public TransactionDto() {}

    public TransactionDto(OperationTypeDto operation) {
        this.operation = operation;
    }

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
        this.dateCreated = ZonedDateTime.of(transaction.getDateCreated(), ZoneId.systemDefault());
        this.subject = transaction.getSubject();
        this.amount = transaction.getAmount();
        this.operation = new OperationTypeDto(transaction.getType(), null);
        this.currencyCode = transaction.getCurrencyCode();
    }

    public static TransactionDto PAYMENT_REQUEST(String toUser, BigDecimal amount,
            CurrencyCode currencyCode, String toUserIBAN, String subject, String entityId) {
        TransactionDto dto = new TransactionDto();
        dto.setOperation(new OperationTypeDto(CurrencyOperation.TRANSACTION_INFO, entityId));
        dto.setToUserName(toUser);
        dto.setAmount(amount);
        dto.setCurrencyCode(currencyCode);
        dto.setSubject(subject);
        dto.setToUserIBAN(toUserIBAN);
        dto.setDateCreated(ZonedDateTime.now());
        dto.setUUID(java.util.UUID.randomUUID().toString());
        return dto;
    }

    public static TransactionDto CURRENCY_SEND(String toUser, String subject, BigDecimal amount,
                       CurrencyCode currencyCode, String toUserIBAN, String entityId) {
        TransactionDto dto = new TransactionDto();
        dto.setOperation(new OperationTypeDto(CurrencyOperation.CURRENCY_SEND, entityId));
        dto.setSubject(subject);
        dto.setToUserName(toUser);
        dto.setAmount(amount);
        dto.setToUserIBAN(toUserIBAN);
        dto.setCurrencyCode(currencyCode);
        dto.setUUID(java.util.UUID.randomUUID().toString());
        return dto;
    }

    public static TransactionDto BASIC(String toUser, BigDecimal amount,
                                       CurrencyCode currencyCode, String toUserIBAN, String subject) {
        TransactionDto dto = new TransactionDto();
        dto.setToUserName(toUser);
        dto.setAmount(amount);
        dto.setCurrencyCode(currencyCode);
        dto.setSubject(subject);
        dto.setToUserIBAN(toUserIBAN);
        dto.setUUID(java.util.UUID.randomUUID().toString());
        return dto;
    }

    public void validate() throws ValidationException {
        if(operation == null)
            throw new ValidationException("missing param 'operation'");
        if(amount == null)
            throw new ValidationException("missing param 'amount'");
        if(currencyCode == null)
            throw new ValidationException("missing param 'currencyCode'");
        if(subject == null)
            throw new ValidationException("missing param 'subject'");
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

    public ZonedDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(ZonedDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getSubject() {
        return subject;
    }

    public TransactionDto setSubject(String subject) {
        this.subject = subject;
        return this;
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

    public TransactionDto setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public OperationTypeDto getOperation() {
        return operation;
    }

    public TransactionDto setOperation(OperationTypeDto operation) {
        this.operation = operation;
        return this;
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

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(String toUserIBAN) {
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

    public List<CurrencyOperation> getPaymentOptions() {
        return paymentOptions;
    }

    public void setPaymentOptions(List<CurrencyOperation> paymentOptions) {
        this.paymentOptions = paymentOptions;
    }

    public String validateReceipt(SignedDocument signedDocument, boolean isIncome) throws Exception {
        TransactionDto dto = signedDocument.getSignedContent(TransactionDto.class);
        switch(dto.getOperation().getCurrencyOperationType()) {
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
                receiptDto.getCurrencyCode());
    }

    private String validateFromUserReceipt(SignedDocument signedDocument, boolean isIncome) throws Exception {
        TransactionDto receiptDto = signedDocument.getSignedContent(TransactionDto.class);
        if(operation.getCurrencyOperationType() != receiptDto.getOperation().getCurrencyOperationType())
            throw new ValidationException("expected type " + operation.getCurrencyOperationType() + " found " +
                    receiptDto.getOperation().getCurrencyOperationType());
        if(!toUserIBAN.equals(receiptDto.getToUserIBAN())) throw new ValidationException(
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
                receiptDto.getCurrencyCode());
    }

    private String validateCurrencyChangeReceipt(SignedDocument signedDocument, boolean isIncome) throws Exception {
        log.severe("=========== TODO");
        CurrencyBatchDto receiptDto = signedDocument.getSignedContent(CurrencyBatchDto.class);
        if(CurrencyOperation.CURRENCY_CHANGE != receiptDto.getOperation()) throw new ValidationException("ERROR - expected type: " +
                CurrencyOperation.CURRENCY_CHANGE + " - found: " + receiptDto.getOperation());
        if(amount.compareTo(receiptDto.getBatchAmount()) != 0) throw new ValidationException(
                "expected amount " + amount + " amount " + receiptDto.getBatchAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationException(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getBatchUUID())) throw new ValidationException(
                "expected UUID " + UUID + " found " + receiptDto.getBatchUUID());
        String action = isIncome ? Messages.currentInstance().get("income_lbl"):
                Messages.currentInstance().get("expense_lbl");

        String result = Messages.currentInstance().get("currency_change_receipt_ok_msg", action,
                receiptDto.getBatchAmount() + " " + receiptDto.getCurrencyCode());
        return result;
    }

    public TransactionDetailsDto getDetails() {
        return details;
    }

    public void setDetails(TransactionDetailsDto details) {
        this.details = details;
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
        this.signer = signedDocument.getFirstSignature().getSigner();
        return this;
    }

}
