package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.TagVS;
import java.math.BigDecimal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagVSDto {

    private Long id;
    private String name;
    private String currencyCode;
    private BigDecimal amount;

    public TagVSDto() {}

    public TagVSDto(TagVS tagVS) {
        this.id = tagVS.getId();
        this.name = tagVS.getName();
    }

    public TagVSDto(BigDecimal amount, String currencyCode, TagVS tagVS) {
        this.id = tagVS.getId();
        this.name = tagVS.getName();
        this.amount = amount;
        this.currencyCode = currencyCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

}
