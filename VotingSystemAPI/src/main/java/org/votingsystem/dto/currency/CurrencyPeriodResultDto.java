package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.util.TimePeriod;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyPeriodResultDto {

    private TimePeriod timePeriod;
    private Map<String, Map<String, IncomesDto>> leftOverMap;
    private Map<String, Map<String, IncomesDto>> changeMap;
    private Map<String, Map<String, IncomesDto>> requestMap;

    private Set<String> leftOverSet;
    private Set<String> changeSet;
    private Set<String> requestSet;

    public CurrencyPeriodResultDto() { }


    public TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(TimePeriod timePeriod) {
        this.timePeriod = timePeriod;
    }

    public Map<String, Map<String, IncomesDto>> getLeftOverMap() {
        return leftOverMap;
    }

    public void setLeftOverMap(Map<String, Map<String, IncomesDto>> leftOverMap) {
        this.leftOverMap = leftOverMap;
    }

    public Map<String, Map<String, IncomesDto>> getChangeMap() {
        return changeMap;
    }

    public void setChangeMap(Map<String, Map<String, IncomesDto>> changeMap) {
        this.changeMap = changeMap;
    }

    public Map<String, Map<String, IncomesDto>> getRequestMap() {
        return requestMap;
    }

    public void setRequestMap(Map<String, Map<String, IncomesDto>> requestMap) {
        this.requestMap = requestMap;
    }

    public Set<String> getLeftOverSet() {
        return leftOverSet;
    }

    public void setLeftOverSet(Set<String> leftOverSet) {
        this.leftOverSet = leftOverSet;
    }

    public Set<String> getChangeSet() {
        return changeSet;
    }

    public void setChangeSet(Set<String> changeSet) {
        this.changeSet = changeSet;
    }

    public Set<String> getRequestSet() {
        return requestSet;
    }

    public void setRequestSet(Set<String> requestSet) {
        this.requestSet = requestSet;
    }

}