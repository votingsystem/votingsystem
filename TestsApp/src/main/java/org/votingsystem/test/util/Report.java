package org.votingsystem.test.util;

import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.ExceptionVS;

import java.math.BigDecimal;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Report {

    private Integer numTotal = 0;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private UserVS.Type type;
    private TransactionVS.Source source;
    private String currencyCode;
    private Map<String, Map<String, BigDecimal>> currencyMap = null;


    public static class Builder {
        private final UserVS.Type type;
        private final String currencyCode;

        private Integer numTotal = 0;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private TransactionVS.Source source;
        private Map<String, Map<String, BigDecimal>> currencyMap;

        public Builder(UserVS.Type type, String currencyCode) {
            this.type = type;
            this.currencyCode = currencyCode;
        }

        public Builder numTotal(Integer numTotal) {
            this.numTotal = numTotal;
            return this;
        }

        public Builder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder currencyMap(Map<String, Map<String, BigDecimal>> currencyMap) {
            this.currencyMap = currencyMap;
            return this;
        }

        public Builder source(TransactionVS.Source source) {
            this.source = source;
            return this;
        }

        public Report build() {
            return new Report(this);
        }
    }

    public Report(Builder builder) {
        numTotal = builder.numTotal;
        totalAmount = builder.totalAmount;
        type = builder.type;
        source = builder.source;
        currencyCode = builder.currencyCode;
        setCurrencyMap(builder.currencyMap);
    }

    public Map<String, Map<String, BigDecimal>> getCurrencyMap() {
        return currencyMap;
    }

    public void setCurrencyMap(Map<String, Map<String, BigDecimal>> currencyMap) {
        this.currencyMap = currencyMap;
    }

    public Report sum(Report report) throws ExceptionVS {
        if(report.getType() != type) throw new ExceptionVS("Expected:" + type.toString() +
                " found: " + report.getType().toString());
        if(report.source != source) throw new ExceptionVS("Expected: " + source.toString() + " - found: " +
                report.getSource().toString());
        Map newCurrencyMap = TransactionVSUtils.sumCurrencyMap(currencyMap , report.currencyMap);
        return new Builder(report.getType(), report.getCurrencyCode()).numTotal(numTotal + report.numTotal).totalAmount(
                totalAmount.add(report.totalAmount)).currencyMap(newCurrencyMap).build();
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public UserVS.Type getType() {
        return type;
    }

    public void setType(UserVS.Type type) {
        this.type = type;
    }

    public TransactionVS.Source getSource() {
        return source;
    }

    public void setSource(TransactionVS.Source source) {
        this.source = source;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}