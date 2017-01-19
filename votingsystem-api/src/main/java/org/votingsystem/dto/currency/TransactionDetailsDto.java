package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.model.Address;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDetailsDto {

    private Integer numItems;
    private BigDecimal itemPrice;
    private BigDecimal discount;
    private Date dateDelivery;
    private Address deliveryAddress;

    public boolean equals(TransactionDetailsDto details) {
        if(numItems.intValue() != details.numItems) return false;
        if(itemPrice.compareTo(details.getItemPrice()) != 0) return false;
        if(discount.compareTo(details.getDiscount()) != 0) return false;
        if(dateDelivery.compareTo(details.getDateDelivery()) != 0) return false;
        return true;
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

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(Address deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    @Override public String toString() {
        return MessageFormat.format("[numItems: {0} - itemPrice: {1} - discount: {2} - dateDelivery: {3} - deliveryAddress: {4}]",
                numItems, itemPrice, discount, dateDelivery, deliveryAddress);
    }
}
