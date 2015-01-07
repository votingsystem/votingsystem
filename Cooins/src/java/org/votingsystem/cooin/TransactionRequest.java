package org.votingsystem.cooin;


import net.sf.json.JSONObject;
import org.votingsystem.model.AddressVS;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import static java.util.stream.Collectors.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionRequest {

    public enum Type {PAYMENT, PAYMENT_REQUEST, DELIVERY_WITHOUT_PAYMENT, DELIVERY_WITH_PAYMENT, REQUEST_FORM}

    private Type type;
    private String IBAN;
    private String subject;
    private String toUser;
    private BigDecimal amount;
    private String currency;
    private String UUID;
    private Date date;
    private List<Payment> paymentOptions;
    private List<String> coinCsrList;
    //details
    private Integer numItems;
    private BigDecimal itemPrice;
    private BigDecimal discount;
    private Date dateDelivery;
    private AddressVS deliveryAddressVS;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
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

    JSONObject toJSON() {
        JSONObject result = new JSONObject();
        result.put("type" , type.toString());
        result.put("IBAN" , IBAN);
        result.put("subject" , subject);
        result.put("toUser" , toUser);
        result.put("currency" , currency);
        result.put("amount" , amount);
        result.put("UUID" , currency);
        List<String> paymentOptionsList = paymentOptions.stream().map(option -> option.toString()).collect(toList());
        result.put("paymentOptions" , paymentOptions);
        return result;
    }

}