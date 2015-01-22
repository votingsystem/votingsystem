package org.votingsystem.model;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.lib.R;
import org.votingsystem.signature.smime.SMIMEMessage;
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
    private String toUserName;
    private BigDecimal amount;
    private String currencyCode;
    private UserVS fromUser;
    private String tagVS;
    private String infoURL;
    private Boolean isTimeLimited;
    private String UUID;
    private String batchUUID;
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
    private SMIMEMessage smimeMessage;

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

    public Payment getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Payment paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getConfirmMessage(Context context) {
        return context.getString(R.string.transaction_request_confirm_msg,
                paymentMethod.getDescription(context), amount.toString() + " " + currencyCode, toUserName);
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
        if(request.getToUserName() != null) if(!request.getToUserName().equals(toUserName)) throw new ExceptionVS(
                "expected toUserName " + request.getToUserName() + " found " + toUserName);
        if(request.getAmount().compareTo(amount) != 0) throw new ExceptionVS(
                "expected amount " + request.getAmount().toString() + " amount " + amount.toString());
        if(!request.getCurrencyCode().equals(currencyCode)) throw new ExceptionVS(
                "expected currencyCode " + request.getCurrencyCode() + " found " + currencyCode);
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
        if(jsonObject.has("toUserName")) transactionRequest.setToUserName(jsonObject.getString("toUserName"));
        if(jsonObject.has("amount")) transactionRequest.setAmount(new BigDecimal(jsonObject.getString("amount")));
        if(jsonObject.has("currencyCode")) transactionRequest.setCurrencyCode(jsonObject.getString("currencyCode"));
        if(jsonObject.has("tagVS")) transactionRequest.setTagVS(jsonObject.getString("tagVS"));
        if(jsonObject.has("paymentMethod")) transactionRequest.setPaymentMethod(Payment.valueOf(
                jsonObject.getString("paymentMethod")));
        if(jsonObject.has("infoURL")) transactionRequest.setInfoURL(jsonObject.getString("infoURL"));
        if(jsonObject.has("date")) transactionRequest.setDate(
                DateUtils.getDateFromString(jsonObject.getString("date")));
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

    public JSONObject getAnonymousSignedTransaction(Boolean isTimeLimited) throws Exception {
        this.isTimeLimited = isTimeLimited;
        Map mapToSend = new HashMap();
        mapToSend.put("operation", TypeVS.COOIN_SEND.toString());
        mapToSend.put("typeVS", type.toString());
        mapToSend.put("paymentMethod" , paymentMethod.toString());
        mapToSend.put("userToType", userToType.toString());
        mapToSend.put("subject", subject);
        mapToSend.put("toUserName", toUserName);
        mapToSend.put("toUserIBAN", IBAN);
        mapToSend.put("tag", tagVS);
        mapToSend.put("batchAmount", amount.toString());
        mapToSend.put("currencyCode", currencyCode);
        mapToSend.put("isTimeLimited", isTimeLimited);
        Map details = new HashMap();
        details.put("UUID", UUID);//this UUID is for the transaction trigger
        mapToSend.put("details", details);
        batchUUID = java.util.UUID.randomUUID().toString();
        mapToSend.put("batchUUID", batchUUID);
        return new JSONObject(mapToSend);
    }

    public void setAnonymousSignedTransactionReceipt(SMIMEMessage smimeMessage,
            List<Cooin> cooinList) throws JSONException, ExceptionVS {
        JSONObject messageJSON = new  JSONObject(smimeMessage.getSignedContent());
        if(TypeVS.valueOf(messageJSON.getString("operation")) != TypeVS.FROM_USERVS) throw
                new ExceptionVS("Expected operation: " + TypeVS.FROM_USERVS + " - found: " +
                messageJSON.getString("operation"));
        if(Payment.valueOf(messageJSON.getString("paymentMethod")) != paymentMethod) throw
                new ExceptionVS("Expected paymentMethod: " + paymentMethod + " - found: " +
                messageJSON.getString("paymentMethod"));
        if(subject != null && !subject.equals(messageJSON.getString("subject"))) throw
                new ExceptionVS("Expected subject: " + subject + " - found: " +
                messageJSON.getString("subject"));
        if(isTimeLimited != messageJSON.getBoolean("isTimeLimited")) throw new ExceptionVS(
                "Expected isTimeLimited: " + isTimeLimited + " - found: " +
                messageJSON.getString("isTimeLimited"));
        if(!IBAN.equals(messageJSON.getString("toUserIBAN"))) throw new ExceptionVS(
                "Expected toUserIBAN: " + IBAN + " - found: " + messageJSON.getString("toUserIBAN"));
        BigDecimal batchAmount = new BigDecimal(messageJSON.getString("batchAmount"));
        if(batchAmount.compareTo(amount) != 0) throw new ExceptionVS(
                "Expected amount: " + amount.toString() + " - found: " + batchAmount.toString());
        if(!currencyCode.equals(messageJSON.getString("currencyCode"))) throw new ExceptionVS(
                "Expected currencyCode: " + currencyCode + " - found: " +
                messageJSON.getString("currencyCode"));
        JSONArray arrayCertVSCooins = messageJSON.getJSONArray("hashCertVSCooins");
        if(arrayCertVSCooins.length() != cooinList.size()) throw new ExceptionVS(
                "Expected num. cooins: " + cooinList.size() + " - found: " +
                arrayCertVSCooins.length());
        List<String> certVSCooinList = new ArrayList<>();
        for(Cooin cooin: cooinList) {
            certVSCooinList.add(cooin.getHashCertVS());
        }
        for(int i = 0; i < arrayCertVSCooins.length(); i++) {
            if(!certVSCooinList.contains(arrayCertVSCooins.getString(i))) throw new ExceptionVS(
                "Expected unknown hashCertVS: " +arrayCertVSCooins.getString(i));
        }
        if(!tagVS.equals(messageJSON.getString("tag"))) throw new ExceptionVS(
                "Expected tag: " + tagVS + " - found: " + messageJSON.getString("tag"));
        if(!batchUUID.equals(messageJSON.getString("batchUUID"))) throw new ExceptionVS(
                "Expected batchUUID: " + batchUUID + " - found: " + messageJSON.getString("batchUUID"));
        this.smimeMessage = smimeMessage;
    }

    public JSONObject getSignedTransaction(String fromUserIBAN) throws Exception {
        Map mapToSend = new HashMap();
        mapToSend.put("operation", TypeVS.FROM_USERVS.toString());
        mapToSend.put("typeVS", type.toString());
        mapToSend.put("userToType", userToType.toString());
        mapToSend.put("fromUserIBAN", fromUserIBAN);
        mapToSend.put("subject", subject);
        mapToSend.put("toUserName", toUserName);
        mapToSend.put("toUserIBAN", Arrays.asList(IBAN));
        mapToSend.put("tags", Arrays.asList(tagVS));
        mapToSend.put("amount", amount.toString());
        mapToSend.put("currencyCode", currencyCode);
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

    public SMIMEMessage getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(SMIMEMessage smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    public Boolean getIsTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }
}