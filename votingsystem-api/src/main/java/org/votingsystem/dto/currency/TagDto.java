package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.model.currency.Tag;

import java.math.BigDecimal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagDto {

    public static final String WILDTAG = "WILDTAG";

    private Long id;
    private String name;
    private CurrencyCode currencyCode;
    private BigDecimal amount;

    public TagDto() {}

    public TagDto(Tag tag) {
        this.id = tag.getId();
        this.name = tag.getName();
    }

    public TagDto(BigDecimal amount, CurrencyCode currencyCode, Tag tag) {
        this.id = tag.getId();
        this.name = tag.getName();
        this.amount = amount;
        this.currencyCode = currencyCode;
    }

    public static TagDto CURRENCY_DATA(BigDecimal amount, CurrencyCode currencyCode, Tag tag) {
        TagDto tagDto = new TagDto();
        tagDto.setName(tag.getName());
        tagDto.setAmount(amount);
        tagDto.setCurrencyCode(currencyCode);
        return tagDto;
    }

    public static TagDto FROM_TAG(Tag tag) {
        if(tag == null)
            return null;
        else
            return new TagDto(tag);
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