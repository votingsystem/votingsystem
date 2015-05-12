package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.web.currency.util.LoggerVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class CSRBean {

    private static Logger log = Logger.getLogger(CSRBean.class.getSimpleName());

    private BigDecimal currencyMinValue = BigDecimal.ONE;

    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject ConfigVS config;


    public Set<String> signCurrencyRequest (CurrencyRequestDto requestDto) throws ExceptionVS {
        TimePeriod timePeriod = null;
        if(requestDto.getTimeLimited()) timePeriod = DateUtils.getCurrentWeekPeriod();
        else {
            Date dateFrom = DateUtils.resetCalendar().getTime();
            Date dateTo = DateUtils.addDays(dateFrom, 365).getTime(); //one year
            timePeriod = new TimePeriod(dateFrom, dateTo);
        }
        CertificateVS authorityCertificateVS = signatureBean.getServerCertificateVS();
        Set<Currency> issuedCurrencySet = new HashSet<>();
        Set<String> issuedCertSet = new HashSet<>();
        try {
            for(CurrencyDto currencyDto : requestDto.getCurrencyDtoMap().values()) {
                X509Certificate x509AnonymousCert = signatureBean.signCSR(
                        currencyDto.getCsr(), null, timePeriod.getDateFrom(), timePeriod.getDateTo());
                Currency currency = currencyDto.loadCertData(x509AnonymousCert, timePeriod, requestDto.getTagVS(),
                        authorityCertificateVS);
                issuedCurrencySet.add(dao.persist(currency));
                issuedCertSet.add(new String(CertUtils.getPEMEncoded(x509AnonymousCert)));
                LoggerVS.logCurrencyIssued(currency);
            }
            return issuedCertSet;
        } catch(Exception ex) {
            for(Currency currency : issuedCurrencySet) {
                if(currency.getId() != null) {
                    dao.merge(currency.setState(Currency.State.ERROR).setReason(ex.getMessage()));
                }
            }
            throw new ExceptionVS("currencyRequestDataError: " + ex.getMessage());
        }
    }

    public String signCurrencyRequest(CurrencyDto currencyDto, TagVS tagVS) throws ExceptionVS {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        if(currencyMinValue.compareTo(currencyDto.getAmount()) > 0) throw new ExceptionVS(messages.get("currencyMinValueError",
                currencyMinValue.toString(), currencyDto.getAmount().toString()));
        TimePeriod timePeriod = null;
        Currency currency = null;
        if(currencyDto.isTimeLimited()) timePeriod = DateUtils.getCurrentWeekPeriod();
        else {
            Date dateFrom = DateUtils.resetCalendar().getTime();
            Date dateTo = DateUtils.addDays(dateFrom, 365).getTime(); //one year
            timePeriod = new TimePeriod(dateFrom, dateTo);
        }
        CertificateVS authorityCertificateVS = signatureBean.getServerCertificateVS();
        try {
            X509Certificate x509AnonymousCert = signatureBean.signCSR(
                    currencyDto.getCsr(), null, timePeriod.getDateFrom(), timePeriod.getDateTo());
            currency = currencyDto.loadCertData(x509AnonymousCert, timePeriod, tagVS, authorityCertificateVS);
            currency = dao.persist(currency);
            LoggerVS.logCurrencyIssued(currency);
            return new String(CertUtils.getPEMEncoded(x509AnonymousCert));
        } catch(Exception ex) {
            throw new ExceptionVS(messages.get("currencyRequestDataError"));
        }
    }

}
