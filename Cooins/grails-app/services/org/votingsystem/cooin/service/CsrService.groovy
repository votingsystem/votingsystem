package org.votingsystem.cooin.service

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.cooin.model.Cooin
import org.votingsystem.cooin.model.CooinRequestBatch
import org.votingsystem.cooin.util.LoggerVS
import org.votingsystem.model.CertificateVS
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.util.DateUtils
import java.security.cert.X509Certificate
import static org.springframework.context.i18n.LocaleContextHolder.getLocale

@Transactional
class CsrService {

    private LinkGenerator grailsLinkGenerator
	private BigDecimal cooinMinValue = new BigDecimal(1)
    def grailsApplication
	def messageSource
    def signatureVSService


    public synchronized CooinRequestBatch signCooinBatchRequest (CooinRequestBatch cooinBatchRequest) throws ExceptionVS {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        DateUtils.TimePeriod timePeriod = null
        if(cooinBatchRequest.isTimeLimited) timePeriod = DateUtils.getCurrentWeekPeriod()
        else {
            Date dateFrom = DateUtils.resetCalendar().getTime()
            Date dateTo = DateUtils.addDays(dateFrom, 365).getTime() //one year
            timePeriod = new DateUtils.TimePeriod(dateFrom, dateTo)
        }
        Collection<Cooin> cooinCollection = cooinBatchRequest.getCooinsMap().values()
        CertificateVS authorityCertificateVS = signatureVSService.getServerCertificateVS()
        try {
            for(Cooin cooin:cooinCollection) {
                X509Certificate x509AnonymousCert = signatureVSService.signCSR(
                        cooin.getCsr(), null, timePeriod.getDateFrom(), timePeriod.getDateTo())
                cooin.loadCertData(x509AnonymousCert, timePeriod, authorityCertificateVS).save()
                LoggerVS.logCooinIssued(cooin.id, cooin.currencyCode, cooin.amount, cooin.tag,
                        cooinBatchRequest.isTimeLimited, timePeriod.getDateFrom(), timePeriod.getDateTo())
            }
            return cooinBatchRequest;
        } catch(Exception ex) {
            for(Cooin cooin : cooinBatchRequest.cooinsMap.values()) {
                if(cooin.getId() != null) {
                    cooin.setState(Cooin.State.ERROR).setReason(ex.getMessage()).save()
                }
            }
            throw new ExceptionVS(messageSource.getMessage('cooinRequestDataError', null, locale), ex)
        }
    }

    public synchronized Cooin signCooinRequest (Cooin cooin) throws ExceptionVS {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        if(cooinMinValue.compareTo(cooin.amount) > 0) throw new ExceptionVS(messageSource.getMessage(
                'cooinMinValueError', [cooinMinValue.toString(), cooin.amount.toString()].toArray(), locale))
        DateUtils.TimePeriod timePeriod = null
        if(cooin.isTimeLimited) timePeriod = DateUtils.getCurrentWeekPeriod()
        else {
            Date dateFrom = DateUtils.resetCalendar().getTime()
            Date dateTo = DateUtils.addDays(dateFrom, 365).getTime() //one year
            timePeriod = new DateUtils.TimePeriod(dateFrom, dateTo)
        }
        CertificateVS authorityCertificateVS = signatureVSService.getServerCertificateVS()
        try {
            X509Certificate x509AnonymousCert = signatureVSService.signCSR(
                    cooin.getCsr(), null, timePeriod.getDateFrom(), timePeriod.getDateTo())
            cooin.loadCertData(x509AnonymousCert, timePeriod, authorityCertificateVS).save()
            LoggerVS.logCooinIssued(cooin.id, cooin.currencyCode, cooin.amount, cooin.tag,
                    cooin.isTimeLimited, timePeriod.getDateFrom(), timePeriod.getDateTo())
        } catch(Exception ex) {
            throw new ExceptionVS(messageSource.getMessage('cooinRequestDataError', null, locale), ex)
        }
        return cooin
    }
}