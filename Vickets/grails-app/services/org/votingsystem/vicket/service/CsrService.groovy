package org.votingsystem.vicket.service

import grails.converters.JSON
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo
import org.bouncycastle.jce.PKCS10CertificationRequest
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.StringUtils
import org.votingsystem.vicket.model.Vicket
import java.security.cert.X509Certificate

class CsrService {

    private static final CLASS_NAME = CsrService.class.getSimpleName()

    LinkGenerator grailsLinkGenerator
	def grailsApplication
	def messageSource
    def signatureVSService


    private Map checkSubject(String subjectDN) {
        // OU=DigitalCurrency,OU=CURRENCY:euro,OU=AMOUNT:10,CN=vicketProviderURL:http://vickets/Vickets
        String currency
        String amount
        String vicketProviderURL
        if (subjectDN.contains("CURRENCY:")) currency = subjectDN.split("CURRENCY:")[1].split(",")[0];
        if (subjectDN.contains("AMOUNT:")) amount = subjectDN.split("AMOUNT:")[1].split(",")[0];
        if (subjectDN.contains("vicketProviderURL:")) vicketProviderURL = subjectDN.split("vicketProviderURL:")[1].split(",")[0];
        return [vicketProviderURL:vicketProviderURL,amount:amount,currency:currency]
    }

    public synchronized ResponseVS signVicketCsr (byte[] csrPEMBytes, String vicketAmount,
           Currency vicketCurrency, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        PKCS10CertificationRequest csr = CertUtil.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        String serverURL = grailsApplication.config.grails.serverURL
        if(!csr) throw new ExceptionVS(messageSource.getMessage('csrRequestErrorMsg', null, locale))
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects()
        def certAttributeJSON
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.VICKET_TAG:
                    String certAttributeJSONStr = ((DERUTF8String)attribute.getObject()).getString()
                    certAttributeJSON = JSON.parse(certAttributeJSONStr)
                    break;
            }
        }
        Map subjectDataMap = Vicket.checkSubject(info.subject.toString());
        // X500Principal subject = new X500Principal("CN=vicketProviderURL:" + vicketProviderURL +"AMOUNT=" + amount + "CURRENCY=" + currency + ", OU=DigitalCurrency");
        if(!certAttributeJSON) throw new ExceptionVS(messageSource.getMessage(
                'csrMissingDERTaggedObjectErrorMsg', null, locale))
        String vicketProviderURL = StringUtils.checkURL(certAttributeJSON.vicketProviderURL)
        String hashCertVSBase64 = certAttributeJSON.hashCertVS
        String amount = certAttributeJSON.amount
        String currency = certAttributeJSON.currency
        if(!currency.equals(subjectDataMap.get("currency")) || !amount.equals(subjectDataMap.get("amount")) ||
                !vicketProviderURL.equals(subjectDataMap.get("vicketProviderURL"))) throw new ExceptionVS(
                messageSource.getMessage('csrVicketSubjectDNErrorMsg',
                        ["${vicketProviderURL} ${amount} ${currency}",
                         "${subjectDataMap.get("vicketProviderURL")} ${subjectDataMap.get("amount")} ${subjectDataMap.get("currency")}"].toArray(), locale))

        if(!vicketAmount.equals(amount) || !vicketCurrency.getCurrencyCode().equals(currency)) throw new ExceptionVS(
                messageSource.getMessage('csrVicketValueErrorMsg',
                        ["${vicketAmount} ${vicketCurrency.getCurrencyCode()}", "${amount} ${currency}"].toArray(), locale))
        if (!serverURL.equals(vicketProviderURL))  throw new ExceptionVS(messageSource.getMessage(
                "serverMismatchErrorMsg", [serverURL, vicketProviderURL].toArray(), locale));
        if (!hashCertVSBase64) throw new ExceptionVS(messageSource.getMessage("csrMissingHashCertVSErrorMsg",
                [serverURL, vicketProviderURL].toArray(), locale));
        //HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        //String hashCertVSBase64 = new String(hexConverter.unmarshal(certAttributeJSON.hashCertVS));
        DateUtils.TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod()
        X509Certificate issuedCert = signatureVSService.signCSR(
                csr, null, timePeriod.getDateFrom(), timePeriod.getDateTo())
        if (!issuedCert)  throw new ExceptionVS(messageSource.getMessage('csrSigningErrorMsg', null, locale))
        else {
            Vicket vicket = new Vicket(serialNumber:issuedCert.getSerialNumber().longValue(),
                    content:issuedCert.getEncoded(), state:Vicket.State.OK, hashCertVS:hashCertVSBase64,
                    vicketProviderURL: vicketProviderURL, amount:new BigDecimal(amount),
                    currencyCode:vicketCurrency.getCurrencyCode(),
                    authorityCertificateVS:signatureVSService.getServerCertificateVS(),
                    validFrom:timePeriod.getDateFrom(), validTo: timePeriod.getDateTo()).save()
            log.debug("$methodName - expended Vicket '${vicket.id}'")
            byte[] issuedCertPEMBytes = CertUtil.getPEMEncoded(issuedCert);
            Map data = [vicketAmount:vicketAmount, vicketCurrency:vicketCurrency, vicket: vicket]
            return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_REQUEST,
                    data:data, messageBytes:issuedCertPEMBytes)
        }
    }

    public synchronized ResponseVS signVicketBatchRequest (byte[] vicketBatchRequest, BigDecimal expectedAmount,
            Currency expectedCurrency, Locale locale){
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        ResponseVS responseVS = null;
        String msg = null;
        List<Vicket> issuedVicketList = new ArrayList<Vicket>()
        try {
            JSONObject dataRequestJSON = JSON.parse(new String(vicketBatchRequest, "UTF-8"))
            JSONArray vicketsArray = dataRequestJSON.vicketCSR
            List<String> issuedVicketCertList = new ArrayList<String>()
            BigDecimal batchAmount = new BigDecimal(0)
            vicketsArray.each {
                String csr = it.csr
                Currency vicketCurrency = Currency.getInstance(it.currency).getCurrencyCode()
                if(vicketCurrency != expectedCurrency) throw new ExceptionVS(messageSource.getMessage(
                        'vicketBatchRequestCurrencyErrorMsg', [expectedCurrency.toString(),
                        vicketCurrency.getCurrencyCode()].toArray(), locale));
                String vicketAmount = it.vicketValue
                batchAmount = batchAmount.add(new BigDecimal(vicketAmount))
                responseVS = signVicketCsr(csr.getBytes(), vicketAmount, vicketCurrency, locale)
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    issuedVicketList.add(responseVS.data.vicket)
                    issuedVicketCertList.add(new String(responseVS.getMessageBytes(), "UTF-8"))
                } else throw new ExceptionVS(responseVS.getMessage())
            }
            if(expectedAmount.compareTo(batchAmount) != 0) throw new ExceptionVS(messageSource.getMessage(
                    'vicketBatchRequestAmountErrorMsg', ["${expectedAmount.toString()} ${expectedCurrency}",
                    "${batchAmount.toString()} ${expectedCurrency}"], locale))
            return new ResponseVS(statusCode: ResponseVS.SC_OK, data:issuedVicketCertList, type:TypeVS.VICKET_REQUEST);
        } catch(Exception ex) {
            cancelVickets(issuedVicketList, ex.getMessage())
            throw new ExceptionVS(messageSource.getMessage('vicketRequestDataError', null, locale), ex)
        }
    }

    private void cancelVickets(List<Vicket> issuedVicketList, String reason) {
        for(Vicket vicket : issuedVicketList) {
            vicket.state = Vicket.State.CANCELLED
            vicket.reason = reason
            vicket.save()
        }
    }

}