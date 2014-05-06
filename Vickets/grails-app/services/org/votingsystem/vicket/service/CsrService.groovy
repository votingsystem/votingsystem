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
import org.votingsystem.model.CurrencyVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.vicket.Vicket
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.StringUtils

import java.security.cert.X509Certificate

class CsrService {

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

    public synchronized ResponseVS signVicket (byte[] csrPEMBytes, String vicketAmount,
           CurrencyVS vicketCurrency, Locale locale) {
        PKCS10CertificationRequest csr = CertUtil.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        String serverURL = grailsApplication.config.grails.serverURL
        try {
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

            if(!vicketAmount.equals(amount) || !vicketCurrency.toString().equals(currency)) throw new ExceptionVS(
                    messageSource.getMessage('csrVicketValueErrorMsg',
                    ["${vicketAmount} ${vicketCurrency.toString()}", "${amount} ${currency}"].toArray(), locale))
            if (!serverURL.equals(vicketProviderURL))  throw new ExceptionVS(messageSource.getMessage(
                    "serverMismatchErrorMsg", [serverURL, vicketProviderURL].toArray(), locale));
            if (!hashCertVSBase64) throw new ExceptionVS(messageSource.getMessage("csrMissingHashCertVSErrorMsg",
                    [serverURL, vicketProviderURL].toArray(), locale));
            //HexBinaryAdapter hexConverter = new HexBinaryAdapter();
            //String hashCertVSBase64 = new String(hexConverter.unmarshal(certAttributeJSON.hashCertVS));
            Calendar calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Date certValidFrom = calendar.getTime()
            calendar.add(Calendar.DAY_OF_YEAR, 7);
            Date certValidTo = DateUtils.getMonday(calendar).getTime()
            X509Certificate issuedCert = signatureVSService.signCSR(csr, null, certValidFrom, certValidTo)
            if (!issuedCert)  throw new ExceptionVS(messageSource.getMessage('csrSigningErrorMsg', null, locale))
            else {
                Vicket vicket = new Vicket(serialNumber:issuedCert.getSerialNumber().longValue(),
                        content:issuedCert.getEncoded(), state:Vicket.State.OK, hashCertVS:hashCertVSBase64,
                        vicketProviderURL: vicketProviderURL, amount:new BigDecimal(amount), currency:vicketCurrency,
                        authorityCertificateVS:signatureVSService.getServerCertificateVS(),
                        validFrom:certValidFrom, validTo: certValidTo).save()
                log.debug("signVicket - expended Vicket '${vicket.id}'")
                byte[] issuedCertPEMBytes = CertUtil.getPEMEncoded(issuedCert);
                Map data = [vicketAmount:vicketAmount, vicketCurrency:vicketCurrency, vicket: vicket]
                return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_REQUEST,
                        data:data, messageBytes:issuedCertPEMBytes)
            }
        } catch(ExceptionVS ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage(), type:TypeVS.ERROR)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR,
                    message:messageSource.getMessage('vicketRequestDataError', null, locale))
        }
    }

    public synchronized ResponseVS signVicketBatchRequest (byte[] vicketBatchRequest, BigDecimal expectedAmount,
            CurrencyVS expectedCurrency, Locale locale){
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
                CurrencyVS vicketCurrency = CurrencyVS.valueOf(it.currency)
                if(vicketCurrency != expectedCurrency) throw new ExceptionVS(messageSource.getMessage(
                        'vicketBatchRequestCurrencyErrorMsg', [expectedCurrency.toString(),
                        vicketCurrency.toString()].toArray(), locale));
                String vicketAmount = it.vicketValue
                batchAmount = batchAmount.add(new BigDecimal(vicketAmount))
                responseVS = signVicket(csr.getBytes(), vicketAmount, vicketCurrency, locale)
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    issuedVicketList.add(responseVS.data.vicket)
                    issuedVicketCertList.add(new String(responseVS.getMessageBytes(), "UTF-8"))
                } else throw new ExceptionVS(responseVS.getMessage())
            }
            if(expectedAmount.compareTo(batchAmount) != 0) throw new ExceptionVS(messageSource.getMessage(
                    'vicketBatchRequestAmountErrorMsg', ["${expectedAmount.toString()} ${expectedCurrency}",
                    "${batchAmount.toString()} ${expectedCurrency}"], locale))
            return new ResponseVS(statusCode: ResponseVS.SC_OK, data:issuedVicketCertList, type:TypeVS.VICKET_REQUEST);
        } catch(ExceptionVS ex) {
            log.error(ex.getMessage(), ex);
            cancelVickets(issuedVicketList, ex.getMessage())
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage(),
                    type:TypeVS.VICKET_REQUEST_ERROR)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.VICKET_REQUEST_ERROR,
                    message:messageSource.getMessage('vicketRequestDataError', null, locale))
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
