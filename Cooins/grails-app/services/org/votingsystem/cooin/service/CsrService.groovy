package org.votingsystem.cooin.service

import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.CertificateVS
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.cooin.model.Cooin
import org.votingsystem.cooin.model.CooinRequestBatch
import org.votingsystem.cooin.util.LoggerVS

import java.security.cert.X509Certificate

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

class CsrService {

    private LinkGenerator grailsLinkGenerator
	def grailsApplication
	def messageSource
    def signatureVSService

    public synchronized CooinRequestBatch signCooinBatchRequest (CooinRequestBatch cooinBatchRequest){
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        DateUtils.TimePeriod timePeriod = null
        if(cooinBatchRequest.isTimeLimited) timePeriod = DateUtils.getCurrentWeekPeriod()
        else {
            Date dateFrom = Calendar.getInstance().getTime()
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
            cancelCooins(cooinBatchRequest.cooinsMap.values(), ex.getMessage())
            throw new ExceptionVS(messageSource.getMessage('cooinRequestDataError', null,
                    locale), ex)
        }
    }

    private void cancelCooins(Collection<Cooin> issuedCooinList, String reason) {
        for(Cooin cooin : issuedCooinList) {
            if(cooin.getId() != null) {
                cooin.setState(Cooin.State.CANCELLED).setReason(reason).save()
            }
        }
    }

}