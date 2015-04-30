package org.votingsystem.web.currency.ejb;

import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyRequestBatch;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.currency.util.LoggerVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class CSRBean {

    private static Logger log = Logger.getLogger(CSRBean.class.getSimpleName());

    private BigDecimal currencyMinValue = BigDecimal.ONE;

    private MessagesVS messages = MessagesVS.getCurrentInstance();
    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject ConfigVS config;


    public CurrencyRequestBatch signCurrencyBatchRequest (CurrencyRequestBatch currencyBatchRequest) throws ExceptionVS {
        TimePeriod timePeriod = null;
        if(currencyBatchRequest.getIsTimeLimited()) timePeriod = DateUtils.getCurrentWeekPeriod();
        else {
            Date dateFrom = DateUtils.resetCalendar().getTime();
            Date dateTo = DateUtils.addDays(dateFrom, 365).getTime(); //one year
            timePeriod = new TimePeriod(dateFrom, dateTo);
        }
        Collection<Currency> currencyCollection = currencyBatchRequest.getCurrencyMap().values();
        CertificateVS authorityCertificateVS = signatureBean.getServerCertificateVS();
        try {
            for(Currency currency : currencyCollection) {
                X509Certificate x509AnonymousCert = signatureBean.signCSR(
                        currency.getCsr(), null, timePeriod.getDateFrom(), timePeriod.getDateTo());
                dao.persist(currency.loadCertData(x509AnonymousCert, timePeriod, authorityCertificateVS));
                LoggerVS.logCurrencyIssued(currency.getId(), currency.getCurrencyCode(), currency.getAmount(), currency.getTag(),
                        currencyBatchRequest.getIsTimeLimited(), timePeriod.getDateFrom(), timePeriod.getDateTo());
            }
            return currencyBatchRequest;
        } catch(Exception ex) {
            for(Currency currency : currencyBatchRequest.getCurrencyMap().values()) {
                if(currency.getId() != null) {
                    dao.merge(currency.setState(Currency.State.ERROR).setReason(ex.getMessage()));
                }
            }
            throw new ExceptionVS("currencyRequestDataError");
        }
    }

    public Currency signCurrencyRequest(Currency currency) throws ExceptionVS {
        if(currencyMinValue.compareTo(currency.getAmount()) > 0) throw new ExceptionVS(messages.get("currencyMinValueError",
                currencyMinValue.toString(), currency.getAmount().toString()));
        TimePeriod timePeriod = null;
        if(currency.getIsTimeLimited()) timePeriod = DateUtils.getCurrentWeekPeriod();
        else {
            Date dateFrom = DateUtils.resetCalendar().getTime();
            Date dateTo = DateUtils.addDays(dateFrom, 365).getTime(); //one year
            timePeriod = new TimePeriod(dateFrom, dateTo);
        }
        CertificateVS authorityCertificateVS = signatureBean.getServerCertificateVS();
        try {
            X509Certificate x509AnonymousCert = signatureBean.signCSR(
                    currency.getCsr(), null, timePeriod.getDateFrom(), timePeriod.getDateTo());
            dao.persist(currency.loadCertData(x509AnonymousCert, timePeriod, authorityCertificateVS));
            LoggerVS.logCurrencyIssued(currency.getId(), currency.getCurrencyCode(), currency.getAmount(), currency.getTag(),
                    currency.getIsTimeLimited(), timePeriod.getDateFrom(), timePeriod.getDateTo());
        } catch(Exception ex) {
            throw new ExceptionVS(messages.get("currencyRequestDataError"));
        }
        return currency;
    }
}
