package org.votingsystem.web.currency.util;

import org.votingsystem.model.AddressVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Payment;
import org.votingsystem.util.TypeVS;

import javax.servlet.AsyncContext;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionRequest {

    private TypeVS type;
    private TransactionVS.Type transactionType;
    private UserVS.Type userToType;
    private String IBAN;
    private String subject;
    private String toUserName;
    private BigDecimal amount;
    private String currencyCode;
    private UserVS fromUser;
    private String tagVS;
    private String infoURL;
    private String UUID;
    private Date date;
    private List<Payment> paymentOptions;
    private Payment paymentMethod;
    private List<String> coinCsrList;
    private AsyncContext asyncContext;
    //details
    private Integer numItems;
    private BigDecimal itemPrice;
    private BigDecimal discount;
    private Date dateDelivery;
    private AddressVS deliveryAddressVS;

    public TransactionRequest() {}

    public TransactionRequest(TypeVS type, UserVS.Type userToType, String subject, String toUserName,
          BigDecimal amount, String currencyCode, String tagVS, Date date, String IBAN, String UUID) {
        this.type = type;
        this.userToType = userToType;
        this.subject = subject;
        this.toUserName = toUserName;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.tagVS = tagVS;
        this.date = date;
        this.IBAN = IBAN;
        this.UUID = UUID;
    }


    public TypeVS getType() {
        return type;
    }

    public void setType(TypeVS type) {
        this.type = type;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public List<Payment> getPaymentOptions() {
        return paymentOptions;
    }

    public void setPaymentOptions(List<Payment> paymentOptions) {
        this.paymentOptions = paymentOptions;
    }

    public List<String> getCoinCsrList() {
        return coinCsrList;
    }

    public void setCoinCsrList(List<String> coinCsrList) {
        this.coinCsrList = coinCsrList;
    }

    public Integer getNumItems() {
        return numItems;
    }

    public void setNumItems(Integer numItems) {
        this.numItems = numItems;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public Date getDateDelivery() {
        return dateDelivery;
    }

    public void setDateDelivery(Date dateDelivery) {
        this.dateDelivery = dateDelivery;
    }

    public AddressVS getDeliveryAddressVS() {
        return deliveryAddressVS;
    }

    public void setDeliveryAddressVS(AddressVS deliveryAddressVS) {
        this.deliveryAddressVS = deliveryAddressVS;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public UserVS getFromUser() {
        return fromUser;
    }

    public void setFromUser(UserVS fromUser) {
        this.fromUser = fromUser;
    }
    public AsyncContext getAsyncContext() {
        asyncContext.getResponse().setCharacterEncoding("UTF-8");
        return asyncContext;
    }

    public void setAsyncContext(AsyncContext asyncContext) {
        this.asyncContext = asyncContext;
    }

    public Payment getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Payment paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public UserVS.Type getUserToType() {
        return userToType;
    }

    public void setUserToType(UserVS.Type userToType) {
        this.userToType = userToType;
    }

    public TransactionVS.Type getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionVS.Type transactionType) {
        this.transactionType = transactionType;
    }

    public String getTagVS() {
        return tagVS;
    }

    public void setTagVS(String tagVS) {
        this.tagVS = tagVS;
    }

    public String getInfoURL() {
        return infoURL;
    }

    public void setInfoURL(String infoURL) {
        this.infoURL = infoURL;
    }

    public void checkRequest(TransactionRequest request) throws ValidationExceptionVS {
        if(request.getUserToType() == null) throw new ValidationExceptionVS("missing user type");
        if(request.getType() != type) throw new ValidationExceptionVS(
                "expected type " + request.getType().toString() + " found " + type.toString());
        if(request.getIBAN() != null && !request.getIBAN().equals(IBAN)) throw new ValidationExceptionVS(
                "expected IBAN " + request.getIBAN() + " found " + IBAN);
        if(request.getSubject() != null) if(!request.getSubject().equals(subject)) throw new ValidationExceptionVS(
                "expected subject " + request.getSubject() + " found " + subject);
        if(request.getToUserName() != null) if(!request.getToUserName().equals(toUserName)) throw new ValidationExceptionVS(
                "expected toUserName " + request.getToUserName() + " found " + toUserName);
        if(request.getAmount().compareTo(amount) != 0) throw new ValidationExceptionVS(
                "expected amount " + request.getAmount().toString() + " amount " + amount.toString());
        if(!request.getCurrencyCode().equals(currencyCode)) throw new ValidationExceptionVS(
                "expected currencyCode " + request.getCurrencyCode() + " found " + currencyCode);
        if(request.getUUID() != null) if(!request.getUUID().equals(UUID)) throw new ValidationExceptionVS(
                "expected UUID " + request.getUUID() + " found " + UUID);
        if(request.getNumItems() != null) if(request.getNumItems() != numItems)  throw new ValidationExceptionVS(
                "expected numItems " + request.getNumItems() + " found " + numItems);
        if(request.getItemPrice() != null) if(request.getItemPrice().compareTo(itemPrice) != 0)
                throw new ValidationExceptionVS("expected itemPrice " +
                request.getItemPrice().toString() + " found " + itemPrice.toString());
        if(request.getDiscount() != null) if(request.getDiscount().compareTo(discount) != 0)
                throw new ValidationExceptionVS("expected discount " +
                request.getDiscount().toString() + " found " + discount.toString());
        if(request.getDateDelivery() != null) if(request.getDateDelivery().compareTo(dateDelivery) != 0)
                throw new ValidationExceptionVS("expected dateDelivery " +
                request.getDateDelivery().toString() + " found " + dateDelivery.toString());
        if(request.getDeliveryAddressVS() != null) request.getDeliveryAddressVS().checkAddress(deliveryAddressVS);
    }

    public static TransactionRequest parse(Map dataMap) throws ParseException {
        TransactionRequest transactionRequest = new TransactionRequest();
        if(dataMap.containsKey("typeVS")) transactionRequest.setType(TypeVS.valueOf((String) dataMap.get("typeVS")));
        if(dataMap.containsKey("IBAN")) transactionRequest.setIBAN((String) dataMap.get("IBAN"));
        if(dataMap.containsKey("toUserIBAN")) {
            if(dataMap.get("toUserIBAN") instanceof List) transactionRequest.setIBAN((String) ((List)dataMap.get(
                    "toUserIBAN")).get(0));
            else if (dataMap.get("toUserIBAN") instanceof String) transactionRequest.setIBAN((String) dataMap.get(
                    "toUserIBAN"));
        }
        if(dataMap.containsKey("userToType")) transactionRequest.setUserToType(
                UserVS.Type.valueOf((String) dataMap.get("userToType")));
        if(dataMap.containsKey("subject")) transactionRequest.setSubject((String) dataMap.get("subject"));
        if(dataMap.containsKey("toUserName")) transactionRequest.setToUserName((String) dataMap.get("toUserName"));
        if(dataMap.containsKey("amount")) transactionRequest.setAmount(new BigDecimal((String) dataMap.get("amount")));
        if (dataMap.containsKey("currencyCode")) transactionRequest.setCurrencyCode((String) dataMap.get("currencyCode"));
        if(dataMap.containsKey("tagVS")) transactionRequest.setTagVS((String) dataMap.get("tagVS"));
        if(dataMap.containsKey("paymentMethod")) transactionRequest.setPaymentMethod(Payment.valueOf(
                (String) dataMap.get("paymentMethod")));
        if(dataMap.containsKey("infoURL")) transactionRequest.setInfoURL((String) dataMap.get("infoURL"));
        if(dataMap.containsKey("date")) transactionRequest.setDate(
                DateUtils.getDateFromString((String) dataMap.get("date")));
        if(dataMap.containsKey("UUID")) transactionRequest.setUUID((String) dataMap.get("UUID"));
        if(dataMap.containsKey("details")) {
            Map detailsJSON = (Map) dataMap.get("details");
            if(detailsJSON.containsKey("UUID")) transactionRequest.setUUID((String) detailsJSON.get("UUID"));
            if(detailsJSON.containsKey("numItems")) transactionRequest.setNumItems((Integer) detailsJSON.get("numItems"));
            if(detailsJSON.containsKey("itemPrice")) transactionRequest.setItemPrice(
                    new BigDecimal((String) detailsJSON.get("itemPrice")));
            if(detailsJSON.containsKey("discount")) transactionRequest.setDiscount(
                    new BigDecimal((String) detailsJSON.get("discount")));
            if(detailsJSON.containsKey("dateDelivery")) transactionRequest.setDateDelivery(DateUtils.getDateFromString(
                    (String) dataMap.get("dateDelivery")));
            if(detailsJSON.containsKey("deliveryAddress")) transactionRequest.setDeliveryAddressVS(
                    AddressVS.parse((Map)detailsJSON.get("deliveryAddress")));
        }
        return transactionRequest;
    }

    public Map getDataMap() {
        Map result = new HashMap<>();
        result.put("typeVS" , type.toString());
        result.put("userToType", userToType.toString());
        result.put("IBAN" , IBAN);
        result.put("subject" , subject);
        result.put("toUserName" , toUserName);
        result.put("currencyCode" , currencyCode);
        result.put("amount" , amount);
        if(paymentMethod != null )result.put("paymentMethod" , paymentMethod.toString());
        result.put("tagVS" , tagVS);
        result.put("infoURL" , infoURL);
        if(date != null) result.put("date" , DateUtils.getDateStr(date));
        result.put("UUID" , UUID);
        if(paymentOptions != null) {
            List<String> paymentOptionsList = new ArrayList<>();
            for(Payment paymentOption : paymentOptions) {
                paymentOptionsList.add(paymentOption.toString());
            }
            //Problems with spring-loaded:
            //List<String> paymentOptionsList = paymentOptions.stream().map(option -> option.toString()).collect(toList());
            result.put("paymentOptions" , paymentOptionsList);
        }
        return result;
    }

}