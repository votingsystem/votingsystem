package org.votingsystem.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Interval {

    public enum Lapse {YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND}

    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private Boolean currentWeekPeriod;

    public Interval(){}

    public Interval(LocalDateTime dateFrom, LocalDateTime dateTo) {
        this.dateFrom = LocalDateTime.from(dateFrom);
        this.dateTo = LocalDateTime.from(dateTo);
    }

    public LocalDateTime getDateFrom() {
        return dateFrom;
    }

    public LocalDateTime getDateTo() {
        return dateTo;
    }

    public static Interval parse(Map dataMap) throws ParseException {
        LocalDateTime dateFrom = DateUtils.getDate((String) dataMap.get("dateFrom"));
        LocalDateTime dateTo = DateUtils.getDate((String) dataMap.get("dateTo"));
        return new Interval(dateFrom, dateTo);
    }

    public Boolean isCurrentWeekPeriod() {
        if(currentWeekPeriod == null) {
            Interval period = DateUtils.getWeekPeriod(LocalDateTime.now());
            currentWeekPeriod = (dateFrom.compareTo(period.getDateFrom()) >=0 &&
                    dateTo.compareTo(period.getDateTo()) <= 0);
        }
        return currentWeekPeriod;
    }

    public void setCurrentWeekPeriod(Boolean currentWeekPeriod) {
        this.currentWeekPeriod = currentWeekPeriod;
    }

    public boolean inRange(LocalDateTime dateToCheck) {
        return dateToCheck.compareTo(dateFrom) >= 0 && dateToCheck.compareTo(dateTo) <= 0;
    }

    @Override public String toString() {
        return "Period from [" + DateUtils.getDateStr(dateFrom) + " - " + DateUtils.getDateStr(dateTo) + "]";
    }
}
