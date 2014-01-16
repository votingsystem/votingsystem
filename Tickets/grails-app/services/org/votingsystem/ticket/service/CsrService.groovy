package org.votingsystem.ticket.service

import grails.converters.JSON
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo
import org.bouncycastle.jce.PKCS10CertificationRequest
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.StringUtils

import java.security.cert.X509Certificate

class CsrService {

    LinkGenerator grailsLinkGenerator
	def grailsApplication
	def messageSource
    def signatureVSService


    public synchronized ResponseVS signTicket (byte[] csrPEMBytes, Locale locale) {
        PKCS10CertificationRequest csr = CertUtil.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        if(!csr) {
            String msg = messageSource.getMessage('csrRequestErrorMsg', null, locale)
            log.error("signTicket - msg:  ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ERROR)
        }
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects()
        def certAttributeJSON
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.TICKET_OID:
                    String certAttributeJSONStr = ((DERUTF8String)attribute.getObject()).getString()
                    certAttributeJSON = JSON.parse(certAttributeJSONStr)
                    break;
            }
        }
        if(!certAttributeJSON) {
            String msg = messageSource.getMessage('csrRequestErrorMsg', null, locale)
            log.error("signTicket - missing certAttributeJSON")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ERROR)
        }
        String amount = certAttributeJSON.amount
        String serverURL = grailsApplication.config.grails.serverURL
        String ticketProviderURL = StringUtils.checkURL(certAttributeJSON.ticketProviderURL)
        if (!serverURL.equals(ticketProviderURL) || !certAttributeJSON.hashCertVS) {
            String msg = messageSource.getMessage('accessControlURLError',
                    [serverURL, ticketProviderURL].toArray(),locale)
            log.error("- signTicket - ERROR - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ERROR)
        }
        //HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        //String hashCertVSBase64 = new String(hexConverter.unmarshal(certAttributeJSON.hashCertVS));
        String hashCertVSBase64 = certAttributeJSON.hashCertVS
        Date certValidFrom = Calendar.getInstance().getTime()
        Calendar today_plus_day = Calendar.getInstance();
        today_plus_day.add(Calendar.DATE, 1);
        Date certValidTo = today_plus_day.getTime()
        X509Certificate issuedCert = signatureVSService.signCSR(csr, null, certValidFrom, certValidTo)
        if (!issuedCert) {
            String msg = messageSource.getMessage('csrRequestErrorMsg', null, locale)
            log.error("signTicket - error signing cert")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR, message:msg)
        } else {
            CertificateVS certificate = new CertificateVS(serialNumber:issuedCert.getSerialNumber().longValue(),
                    content:issuedCert.getEncoded(), type:CertificateVS.Type.TICKET,
                    state:CertificateVS.State.OK, hashCertVSBase64:hashCertVSBase64, validFrom:certValidFrom,
                    validTo: certValidTo).save()
            log.debug("signTicket - expended CertificateVS '${certificate.id}'")
            byte[] issuedCertPEMBytes = CertUtil.getPEMEncoded(issuedCert);
            Map data = [requestPublicKey:csr.getPublicKey(), amount:amount, ticketProviderURL:ticketProviderURL]
            return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.TICKET_REQUEST,
                    data:data, message:"certificateVS_${certificate.id}" , messageBytes:issuedCertPEMBytes)
        }
    }


}
