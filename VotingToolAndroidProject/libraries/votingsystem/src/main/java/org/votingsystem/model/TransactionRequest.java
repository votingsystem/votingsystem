package org.votingsystem.model;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.lib.R;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionRequest {

    private TypeVS type;
    private UserVS.Type userToType;
    private TransactionVS.Type transactionType;
    private String IBAN;
    private String subject;
    private String toUser;
    private BigDecimal amount;
    private String currency;
    private UserVS fromUser;
    private String tagVS;
    private String infoURL;
    private String UUID;
    private Date date;
    private List<Payment> paymentOptions;
    private Payment paymentMethod;
    private List<String> coinCsrList;
    //details
    private Integer numItems;
    private BigDecimal itemPrice;
    private BigDecimal discount;
    private Date dateDelivery;
    private AddressVS deliveryAddressVS;

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

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public Payment getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Payment paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getConfirmMessage(Context context) {
        return context.getString(R.string.transaction_request_confirm_msg,
                paymentMethod.getDescription(context), amount.toString() + " " + currency, toUser);
    }

    public String getTagVS() {
        return tagVS;
    }

    public void setTagVS(String tagVS) {
        this.tagVS = tagVS;
    }

    public String getPaymentConfirmURL() {
        return infoURL + "/" + "payment";
    }

    public void checkRequest(TransactionRequest request) throws ExceptionVS {
        if(request.getType() != type) throw new ExceptionVS(
                "expected type " + request.getType().toString() + " found " + type.toString());
        if(request.getIBAN() != null) if(!request.getIBAN().equals(IBAN)) throw new ExceptionVS(
                "expected IBAN " + request.getIBAN() + " found " + IBAN);
        if(request.getSubject() != null) if(!request.getSubject().equals(subject)) throw new ExceptionVS(
                "expected subject " + request.getSubject() + " found " + subject);
        if(request.getToUser() != null) if(!request.getToUser().equals(toUser)) throw new ExceptionVS(
                "expected toUser " + request.getToUser() + " found " + toUser);
        if(request.getAmount().compareTo(amount) != 0) throw new ExceptionVS(
                "expected amount " + request.getAmount().toString() + " amount " + amount.toString());
        if(!request.getCurrency().equals(currency)) throw new ExceptionVS(
                "expected currency " + request.getCurrency() + " found " + currency);
        if(request.getUUID() != null) if(!request.getUUID().equals(UUID)) throw new ExceptionVS(
                "expected UUID " + request.getUUID() + " found " + UUID);
        if(request.getNumItems() != null) if(request.getNumItems() != numItems)  throw new ExceptionVS(
                "expected numItems " + request.getNumItems() + " found " + numItems);
        if(request.getItemPrice() != null) if(request.getItemPrice().compareTo(itemPrice) != 0)
                throw new ExceptionVS("expected itemPrice " +
                request.getItemPrice().toString() + " found " + itemPrice.toString());
        if(request.getDiscount() != null) if(request.getDiscount().compareTo(discount) != 0)
                throw new ExceptionVS("expected discount " +
                request.getDiscount().toString() + " found " + discount.toString());
        if(request.getDateDelivery() != null) if(request.getDateDelivery().compareTo(dateDelivery) != 0)
                throw new ExceptionVS("expected dateDelivery " +
                request.getDateDelivery().toString() + " found " + dateDelivery.toString());
        if(request.getDeliveryAddressVS() != null) request.getDeliveryAddressVS().checkAddress(deliveryAddressVS);
    }

    public static TransactionRequest parse(JSONObject jsonObject) throws ParseException, JSONException {
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setType(TypeVS.valueOf(jsonObject.getString("typeVS")));
        if(jsonObject.has("IBAN")) transactionRequest.setIBAN(jsonObject.getString("IBAN"));
        if(jsonObject.has("userToType")) transactionRequest.setUserToType(
                UserVS.Type.valueOf(jsonObject.getString("userToType")));
        if(jsonObject.has("subject")) transactionRequest.setSubject(jsonObject.getString("subject"));
        if(jsonObject.has("toUser")) transactionRequest.setToUser(jsonObject.getString("toUser"));
        if(jsonObject.has("amount")) transactionRequest.setAmount(new BigDecimal(jsonObject.getString("amount")));
        if(jsonObject.has("currency")) transactionRequest.setCurrency(jsonObject.getString("currency"));
        if(jsonObject.has("tagVS")) transactionRequest.setTagVS(jsonObject.getString("tagVS"));
        if(jsonObject.has("paymentMethod")) transactionRequest.setPaymentMethod(Payment.valueOf(
                jsonObject.getString("paymentMethod")));
        if(jsonObject.has("infoURL")) transactionRequest.setInfoURL(jsonObject.getString("infoURL"));
        if(jsonObject.has("UUID")) transactionRequest.setUUID(jsonObject.getString("UUID"));
        if(jsonObject.has("details")) {
            JSONObject detailsJSON = jsonObject.getJSONObject("details");
            if(detailsJSON.has("numItems")) transactionRequest.setNumItems(detailsJSON.getInt("numItems"));
            if(detailsJSON.has("itemPrice")) transactionRequest.setItemPrice(
                    new BigDecimal(detailsJSON.getString("itemPrice")));
            if(detailsJSON.has("discount")) transactionRequest.setDiscount(
                    new BigDecimal(detailsJSON.getString("discount")));
            if(detailsJSON.has("dateDelivery")) transactionRequest.setDateDelivery(DateUtils.getDateFromString(
                    jsonObject.getString("dateDelivery")));
            if(detailsJSON.has("deliveryAddress")) transactionRequest.setDeliveryAddressVS(
                    AddressVS.parse(detailsJSON.getJSONObject("deliveryAddress")));
        }
        return transactionRequest;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("typeVS" , type.toString());
        result.put("userToType" , getUserToType().toString());
        result.put("IBAN" , IBAN);
        result.put("subject" , subject);
        result.put("toUser" , toUser);
        result.put("currency" , currency);
        result.put("amount" , amount);
        if(paymentMethod != null )result.put("paymentMethod" , paymentMethod.toString());
        result.put("tagVS" , tagVS);
        result.put("infoURL" , infoURL);
        result.put("UUID" , UUID);
        if(paymentOptions != null) {
            List<String> paymentOptionsList = new ArrayList<>();
            for(Payment payment : paymentOptions) paymentOptionsList.add(payment.toString());
            result.put("paymentOptions" , paymentOptions);
        }
        return result;
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

    public JSONObject getCooinServerTransaction(String fromUserIBAN) throws Exception {
        Map mapToSend = new HashMap();
        mapToSend.put("operation", TypeVS.FROM_USERVS.toString());
        mapToSend.put("fromUserIBAN", fromUserIBAN);
        mapToSend.put("subject", subject);
        mapToSend.put("toUser", toUser);
        mapToSend.put("toUserIBAN", Arrays.asList(IBAN));
        mapToSend.put("tags", Arrays.asList(tagVS));
        mapToSend.put("amount", amount.toString());
        mapToSend.put("currencyCode", currency);
        Map details = new HashMap();
        details.put("UUID", UUID);
        mapToSend.put("details", details);
        mapToSend.put("UUID", java.util.UUID.randomUUID().toString());
        return new JSONObject(mapToSend);
    }


    public String getInfoURL() {
        return infoURL;
    }

    public void setInfoURL(String infoURL) {
        this.infoURL = infoURL;
    }
}