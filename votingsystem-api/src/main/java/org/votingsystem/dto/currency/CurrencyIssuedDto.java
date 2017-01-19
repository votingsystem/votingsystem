package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyIssuedDto {

    private List<TagDto> okList;
    private List<TagDto> expendedList;
    private List<TagDto> lapsedList;
    private List<TagDto> errorList;


    public CurrencyIssuedDto() {}

    public CurrencyIssuedDto(List<TagDto> okList, List<TagDto> expendedList, List<TagDto> lapsedList,
                     List<TagDto> errorList) {
        this.okList = okList;
        this.expendedList = expendedList;
        this.lapsedList = lapsedList;
        this.errorList = errorList;
    }

    public List<TagDto> getOkList() {
        return okList;
    }

    public void setOkList(List<TagDto> okList) {
        this.okList = okList;
    }

    public List<TagDto> getExpendedList() {
        return expendedList;
    }

    public void setExpendedList(List<TagDto> expendedList) {
        this.expendedList = expendedList;
    }

    public List<TagDto> getLapsedList() {
        return lapsedList;
    }

    public void setLapsedList(List<TagDto> lapsedList) {
        this.lapsedList = lapsedList;
    }

    public List<TagDto> getErrorList() {
        return errorList;
    }

    public void setErrorList(List<TagDto> errorList) {
        this.errorList = errorList;
    }
}
