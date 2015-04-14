package org.votingsystem.dto.currency;

import java.math.BigDecimal;


public class TransactionVSPartDto {

    private Long messageSMIMEParentId;
    private Integer numReceptors;
    private String toUser;
    private BigDecimal toUserAmount;

    public TransactionVSPartDto() {}

    public TransactionVSPartDto(Long messageSMIMEReqId, String toUserNif, int numReceptors, BigDecimal userPart) {
        this.messageSMIMEParentId = messageSMIMEReqId;
        this.toUser = toUserNif;
        this.numReceptors = numReceptors;
        this.toUserAmount = userPart;
    }



    public Long getMessageSMIMEParentId() {
        return messageSMIMEParentId;
    }

    public void setMessageSMIMEParentId(Long messageSMIMEParentId) {
        this.messageSMIMEParentId = messageSMIMEParentId;
    }

    public Integer getNumReceptors() {
        return numReceptors;
    }

    public void setNumReceptors(Integer numReceptors) {
        this.numReceptors = numReceptors;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public BigDecimal getToUserAmount() {
        return toUserAmount;
    }

    public void setToUserAmount(BigDecimal toUserAmount) {
        this.toUserAmount = toUserAmount;
    }
}
