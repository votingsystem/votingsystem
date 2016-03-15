package org.votingsystem.web.currency.ejb;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Interval;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.currency.util.LoggerVS;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
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

    private static Logger log = Logger.getLogger(CSRBean.class.getName());

    private BigDecimal currencyMinValue = BigDecimal.ONE;

    @Inject DAOBean dao;
    @Inject CMSBean cmsBean;
    @Inject ConfigVS config;


    public Set<String> signCurrencyRequest (CurrencyRequestDto requestDto) throws ExceptionVS {
        Interval timePeriod = null;
        if(requestDto.getTimeLimited()) timePeriod = DateUtils.getCurrentWeekPeriod();
        else {
            Date dateFrom = DateUtils.resetCalendar().getTime();
            Date dateTo = DateUtils.addDays(dateFrom, 365).getTime(); //one year
            timePeriod = new Interval(dateFrom, dateTo);
        }
        Certificate authorityCertificate = cmsBean.getServerCertificate();
        Set<Currency> issuedCurrencySet = new HashSet<>();
        Set<String> issuedCertSet = new HashSet<>();
        try {
            for(CurrencyDto currencyDto : requestDto.getCurrencyDtoMap().values()) {
                X509Certificate x509AnonymousCert = cmsBean.signCSR(
                        currencyDto.getCsrPKCS10(), null, timePeriod.getDateFrom(), timePeriod.getDateTo());
                Currency currency = Currency.FROM_CERT(x509AnonymousCert, requestDto.getTagVS(), authorityCertificate);
                currency.setType(Currency.Type.REQUEST);
                issuedCurrencySet.add(dao.persist(currency));
                issuedCertSet.add(new String(PEMUtils.getPEMEncoded(x509AnonymousCert)));
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

    public Currency signCurrencyRequest(PKCS10CertificationRequest pkcs10Req, Currency.Type type,
                                        CurrencyBatch currencyBatch) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                pkcs10Req, ContextVS.CURRENCY_OID);
        TagVS tagVS = config.getTag(certExtensionDto.getTag());
        if(currencyMinValue.compareTo(certExtensionDto.getAmount()) > 0) throw new ExceptionVS(messages.get("currencyMinValueError",
                currencyMinValue.toString(), certExtensionDto.getAmount().toString()));
        Interval timePeriod = null;
        if(certExtensionDto.getTimeLimited()) timePeriod = DateUtils.getCurrentWeekPeriod();
        else {
            Date dateFrom = DateUtils.resetCalendar().getTime();
            Date dateTo = DateUtils.addDays(dateFrom, 365).getTime(); //one year
            timePeriod = new Interval(dateFrom, dateTo);
        }
        Certificate authorityCertificate = cmsBean.getServerCertificate();
        try {
            X509Certificate x509AnonymousCert = cmsBean.signCSR(
                    pkcs10Req, null, timePeriod.getDateFrom(), timePeriod.getDateTo());
            Currency currency = Currency.FROM_CERT(x509AnonymousCert, tagVS, authorityCertificate);
            currency.setType(type);
            currency = dao.persist(currency);
            LoggerVS.logCurrencyIssued(currency);
            return currency;
        } catch(Exception ex) {
            throw new ExceptionVS(messages.get("currencyRequestDataError"));
        }
    }


}
