package org.votingsystem.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Interval {

    public enum Lapse {YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND}

    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private Boolean currentWeekPeriod;

    public Interval(){}

    public Interval(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        this.dateFrom = ZonedDateTime.from(dateFrom);
        this.dateTo = ZonedDateTime.from(dateTo);
    }

    public ZonedDateTime getDateFrom() {
        return dateFrom;
    }

    public ZonedDateTime getDateTo() {
        return dateTo;
    }

    public static Interval parse(Map dataMap) throws ParseException {
        ZonedDateTime dateFrom = DateUtils.getZonedDateTime((String) dataMap.get("dateFrom"));
        ZonedDateTime dateTo = DateUtils.getZonedDateTime((String) dataMap.get("dateTo"));
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

    public boolean inRange(LocalDateTime localDateToCheck) {
        ZonedDateTime dateToCheck = ZonedDateTime.of(localDateToCheck, ZoneId.systemDefault());
        return dateToCheck.compareTo(dateFrom) >= 0 && dateToCheck.compareTo(dateTo) <= 0;
    }

    public boolean inRange(ZonedDateTime dateToCheck) {
        return dateToCheck.compareTo(dateFrom) >= 0 && dateToCheck.compareTo(dateTo) <= 0;
    }

    @Override public String toString() {
        return "Period from [" + DateUtils.getDateStr(dateFrom) + " - " + DateUtils.getDateStr(dateTo) + "]";
    }

}