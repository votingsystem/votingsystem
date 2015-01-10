package org.votingsystem.cooin;

import net.sf.json.JSONObject;
import org.votingsystem.model.AddressVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;

import javax.servlet.AsyncContext;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import static java.util.stream.Collectors.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionRequest {

    private TypeVS type;
    private String IBAN;
    private String subject;
    private String toUser;
    private BigDecimal amount;
    private String currency;
    private UserVS fromUser;
    private String UUID;
    private Date date;
    private List<Payment> paymentOptions;
    private Payment paymentSelected;
    private List<String> coinCsrList;
    private AsyncContext asyncContext;
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
    public AsyncContext getAsyncContext() {
        return asyncContext;
    }

    public void setAsyncContext(AsyncContext asyncContext) {
        this.asyncContext = asyncContext;
    }

    public Payment getPaymentSelected() {
        return paymentSelected;
    }

    public void setPaymentSelected(Payment paymentSelected) {
        this.paymentSelected = paymentSelected;
    }

    public void checkRequest(TransactionRequest request) throws ValidationExceptionVS {
        if(request.getType() != type) throw new ValidationExceptionVS(
                TransactionRequest.class, "expected type " + request.getType().toString() + " found " + type.toString());
        if(request.getIBAN() != null) if(!request.getIBAN().equals(IBAN)) throw new ValidationExceptionVS(
                TransactionRequest.class, "expected IBAN " + request.getIBAN() + " found " + IBAN);
        if(request.getSubject() != null) if(!request.getSubject().equals(subject)) throw new ValidationExceptionVS(
                TransactionRequest.class, "expected subject " + request.getSubject() + " found " + subject);
        if(request.getToUser() != null) if(!request.getToUser().equals(toUser)) throw new ValidationExceptionVS(
                TransactionRequest.class, "expected toUser " + request.getToUser() + " found " + toUser);
        if(request.getAmount().compareTo(amount) != 0) throw new ValidationExceptionVS(TransactionRequest.class,
                "expected amount " + request.getAmount().toString() + " amount " + amount.toString());
        if(!request.getCurrency().equals(currency)) throw new ValidationExceptionVS(
                TransactionRequest.class, "expected currency " + request.getCurrency() + " found " + currency);
        if(request.getUUID() != null) if(!request.getUUID().equals(UUID)) throw new ValidationExceptionVS(
                TransactionRequest.class, "expected UUID " + request.getUUID() + " found " + UUID);
        if(request.getNumItems() != null) if(request.getNumItems() != numItems)  throw new ValidationExceptionVS(
                TransactionRequest.class, "expected numItems " + request.getNumItems() + " found " + numItems);
        if(request.getItemPrice() != null) if(request.getItemPrice().compareTo(itemPrice) != 0)
                throw new ValidationExceptionVS(TransactionRequest.class, "expected itemPrice " +
                request.getItemPrice().toString() + " found " + itemPrice.toString());
        if(request.getDiscount() != null) if(request.getDiscount().compareTo(discount) != 0)
                throw new ValidationExceptionVS(TransactionRequest.class, "expected discount " +
                request.getDiscount().toString() + " found " + discount.toString());
        if(request.getDateDelivery() != null) if(request.getDateDelivery().compareTo(dateDelivery) != 0)
                throw new ValidationExceptionVS(TransactionRequest.class, "expected dateDelivery " +
                request.getDateDelivery().toString() + " found " + dateDelivery.toString());
        if(request.getDeliveryAddressVS() != null) request.getDeliveryAddressVS().checkAddress(deliveryAddressVS);
    }

    public static TransactionRequest parse(JSONObject jsonObject) throws ParseException {
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setType(TypeVS.valueOf(jsonObject.getString("typeVS")));
        if(jsonObject.has("IBAN")) transactionRequest.setIBAN(jsonObject.getString("IBAN"));
        if(jsonObject.has("subject")) transactionRequest.setSubject(jsonObject.getString("subject"));
        if(jsonObject.has("toUser")) transactionRequest.setToUser(jsonObject.getString("toUser"));
        if(jsonObject.has("amount")) transactionRequest.setAmount(new BigDecimal(jsonObject.getString("amount")));
        if(jsonObject.has("currency")) transactionRequest.setCurrency(jsonObject.getString("currency"));
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

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        result.put("typeVS" , type.toString());
        result.put("IBAN" , IBAN);
        result.put("subject" , subject);
        result.put("toUser" , toUser);
        result.put("currency" , currency);
        result.put("amount" , amount);
        result.put("UUID" , currency);
        if(paymentOptions != null) {
            List<String> paymentOptionsList = paymentOptions.stream().map(option -> option.toString()).collect(toList());
            result.put("paymentOptions" , paymentOptionsList);
        }
        return result;
    }

}