package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.TagVSDto;

import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyIssuedDto {

    private List<TagVSDto> okList;
    private List<TagVSDto> expendedList;
    private List<TagVSDto> lapsedList;
    private List<TagVSDto> errorList;


    public CurrencyIssuedDto() {}

    public CurrencyIssuedDto(List<TagVSDto> okList, List<TagVSDto> expendedList, List<TagVSDto> lapsedList,
                             List<TagVSDto> errorList) {
        this.okList = okList;
        this.expendedList = expendedList;
        this.lapsedList = lapsedList;
        this.errorList = errorList;
    }

    public List<TagVSDto> getOkList() {
        return okList;
    }

    public void setOkList(List<TagVSDto> okList) {
        this.okList = okList;
    }

    public List<TagVSDto> getExpendedList() {
        return expendedList;
    }

    public void setExpendedList(List<TagVSDto> expendedList) {
        this.expendedList = expendedList;
    }

    public List<TagVSDto> getLapsedList() {
        return lapsedList;
    }

    public void setLapsedList(List<TagVSDto> lapsedList) {
        this.lapsedList = lapsedList;
    }

    public List<TagVSDto> getErrorList() {
        return errorList;
    }

    public void setErrorList(List<TagVSDto> errorList) {
        this.errorList = errorList;
    }
}
