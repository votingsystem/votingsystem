package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.AddressVS;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionVSDetailsDto {

    private Integer numItems;
    private BigDecimal itemPrice;
    private BigDecimal discount;
    private Date dateDelivery;
    private AddressVS deliveryAddressVS;

    public boolean equals(TransactionVSDetailsDto details) {
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

    public AddressVS getDeliveryAddressVS() {
        return deliveryAddressVS;
    }

    public void setDeliveryAddressVS(AddressVS deliveryAddressVS) {
        this.deliveryAddressVS = deliveryAddressVS;
    }

    @Override public String toString() {
        return MessageFormat.format("[numItems: {0} - itemPrice: {1} - discount: {2} - dateDelivery: {3} - deliveryAddressVS: {4}]",
                numItems, itemPrice, discount, dateDelivery, deliveryAddressVS);
    }
}
