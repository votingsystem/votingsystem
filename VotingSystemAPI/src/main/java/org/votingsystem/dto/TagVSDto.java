package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.CurrencyCode;
import org.votingsystem.model.TagVS;

import java.math.BigDecimal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagVSDto {

    public static final String WILDTAG = "WILDTAG";

    private Long id;
    private String name;
    private CurrencyCode currencyCode;
    private BigDecimal amount;

    public TagVSDto() {}

    public TagVSDto(TagVS tagVS) {
        this.id = tagVS.getId();
        this.name = tagVS.getName();
    }

    public TagVSDto(BigDecimal amount, CurrencyCode currencyCode, TagVS tagVS) {
        this.id = tagVS.getId();
        this.name = tagVS.getName();
        this.amount = amount;
        this.currencyCode = currencyCode;
    }

    public static TagVSDto CURRENCY_DATA(BigDecimal amount, CurrencyCode currencyCode, TagVS tagVS) {
        TagVSDto tagVSDto = new TagVSDto();
        tagVSDto.setName(tagVS.getName());
        tagVSDto.setAmount(amount);
        tagVSDto.setCurrencyCode(currencyCode);
        return tagVSDto;
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

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

}
