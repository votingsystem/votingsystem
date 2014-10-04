package org.votingsystem.vicket.service

import grails.converters.JSON
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo
import org.bouncycastle.jce.PKCS10CertificationRequest
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.springframework.context.i18n.LocaleContextHolder
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.StringUtils
import org.votingsystem.vicket.model.Vicket
import org.votingsystem.vicket.model.VicketBatch
import org.votingsystem.vicket.util.LoggerVS

import java.security.cert.X509Certificate

class CsrService {

    private static final CLASS_NAME = CsrService.class.getSimpleName()

    private LinkGenerator grailsLinkGenerator
	def grailsApplication
	def messageSource
    def signatureVSService

    public synchronized VicketBatch signVicketBatchRequest (VicketBatch vicketBatchRequest){
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        DateUtils.TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod()
        Collection<Vicket> vicketCollection = vicketBatchRequest.getVicketsMap().values()
        CertificateVS authorityCertificateVS = signatureVSService.getServerCertificateVS()
        try {
            for(Vicket vicket:vicketCollection) {
                X509Certificate x509AnonymousCert = signatureVSService.signCSR(
                        vicket.getCsr(), null, timePeriod.getDateFrom(), timePeriod.getDateTo())
                vicket.loadCertData(x509AnonymousCert, timePeriod, authorityCertificateVS).save()
                LoggerVS.logVicketIssued(vicket.id, vicket.currencyCode, vicket.amount, vicket.tag,
                        timePeriod.getDateFrom(), timePeriod.getDateTo())
            }
            return vicketBatchRequest;
        } catch(Exception ex) {
            cancelVickets(vicketBatchRequest.vicketsMap.values(), ex.getMessage())
            throw new ExceptionVS(messageSource.getMessage('vicketRequestDataError', null,
                    LocaleContextHolder.locale), ex)
        }
    }

    private void cancelVickets(Collection<Vicket> issuedVicketList, String reason) {
        for(Vicket vicket : issuedVicketList) {
            if(vicket.getId() != null) {
                vicket.state = Vicket.State.CANCELLED
                vicket.reason = reason
                vicket.save()
            }
        }
    }

}