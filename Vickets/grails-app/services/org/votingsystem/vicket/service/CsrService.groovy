package org.votingsystem.vicket.service

import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.CertificateVS
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.vicket.model.Vicket
import org.votingsystem.vicket.model.VicketRequestBatch
import org.votingsystem.vicket.util.LoggerVS

import java.security.cert.X509Certificate

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

class CsrService {

    private LinkGenerator grailsLinkGenerator
	def grailsApplication
	def messageSource
    def signatureVSService

    public synchronized VicketRequestBatch signVicketBatchRequest (VicketRequestBatch vicketBatchRequest){
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        DateUtils.TimePeriod timePeriod = null
        if(vicketBatchRequest.isTimeLimited) timePeriod = DateUtils.getCurrentWeekPeriod()
        else {
            Date dateFrom = Calendar.getInstance().getTime()
            Date dateTo = DateUtils.addDays(dateFrom, 365) //one year
            timePeriod = new DateUtils.TimePeriod(dateFrom, dateTo)
        }
        Collection<Vicket> vicketCollection = vicketBatchRequest.getVicketsMap().values()
        CertificateVS authorityCertificateVS = signatureVSService.getServerCertificateVS()
        try {
            for(Vicket vicket:vicketCollection) {
                X509Certificate x509AnonymousCert = signatureVSService.signCSR(
                        vicket.getCsr(), null, timePeriod.getDateFrom(), timePeriod.getDateTo())
                vicket.loadCertData(x509AnonymousCert, timePeriod, authorityCertificateVS).save()
                LoggerVS.logVicketIssued(vicket.id, vicket.currencyCode, vicket.amount, vicket.tag,
                        vicketBatchRequest.isTimeLimited, timePeriod.getDateFrom(), timePeriod.getDateTo())
            }
            return vicketBatchRequest;
        } catch(Exception ex) {
            cancelVickets(vicketBatchRequest.vicketsMap.values(), ex.getMessage())
            throw new ExceptionVS(messageSource.getMessage('vicketRequestDataError', null,
                    locale), ex)
        }
    }

    private void cancelVickets(Collection<Vicket> issuedVicketList, String reason) {
        for(Vicket vicket : issuedVicketList) {
            if(vicket.getId() != null) {
                vicket.setState(Vicket.State.CANCELLED).setReason(reason).save()
            }
        }
    }

}