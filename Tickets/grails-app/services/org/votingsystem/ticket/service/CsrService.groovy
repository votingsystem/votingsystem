package org.votingsystem.ticket.service

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
import org.votingsystem.model.ticket.TicketVS
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
        // OU=DigitalCurrency,OU=CURRENCY:euro,OU=AMOUNT:10,CN=ticketProviderURL:http://tickets:8083/Tickets
        String currency
        String amount
        String ticketProviderURL
        if (subjectDN.contains("CURRENCY:")) currency = subjectDN.split("CURRENCY:")[1].split(",")[0];
        if (subjectDN.contains("AMOUNT:")) amount = subjectDN.split("AMOUNT:")[1].split(",")[0];
        if (subjectDN.contains("ticketProviderURL:")) ticketProviderURL = subjectDN.split("ticketProviderURL:")[1].split(",")[0];
        return [ticketProviderURL:ticketProviderURL,amount:amount,currency:currency]
    }

    public synchronized ResponseVS signTicket (byte[] csrPEMBytes, String ticketAmount,
           CurrencyVS ticketCurrency, Locale locale) {
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
                    case ContextVS.TICKET_TAG:
                        String certAttributeJSONStr = ((DERUTF8String)attribute.getObject()).getString()
                        certAttributeJSON = JSON.parse(certAttributeJSONStr)
                        break;
                }
            }
            Map subjectDataMap = TicketVS.checkSubject(info.subject.toString());
            // X500Principal subject = new X500Principal("CN=ticketProviderURL:" + ticketProviderURL +"AMOUNT=" + amount + "CURRENCY=" + currency + ", OU=DigitalCurrency");
            if(!certAttributeJSON) throw new ExceptionVS(messageSource.getMessage(
                    'csrMissingDERTaggedObjectErrorMsg', null, locale))
            String ticketProviderURL = StringUtils.checkURL(certAttributeJSON.ticketProviderURL)
            String hashCertVSBase64 = certAttributeJSON.hashCertVS
            String amount = certAttributeJSON.amount
            String currency = certAttributeJSON.currency
            if(!currency.equals(subjectDataMap.get("currency")) || !amount.equals(subjectDataMap.get("amount")) ||
                    !ticketProviderURL.equals(subjectDataMap.get("ticketProviderURL"))) throw new ExceptionVS(
                    messageSource.getMessage('csrTicketSubjectDNErrorMsg',
                            ["${ticketProviderURL} ${amount} ${currency}",
                            "${subjectDataMap.get("ticketProviderURL")} ${subjectDataMap.get("amount")} ${subjectDataMap.get("currency")}"].toArray(), locale))

            if(!ticketAmount.equals(amount) || !ticketCurrency.toString().equals(currency)) throw new ExceptionVS(
                    messageSource.getMessage('csrTicketValueErrorMsg',
                    ["${ticketAmount} ${ticketCurrency.toString()}", "${amount} ${currency}"].toArray(), locale))
            if (!serverURL.equals(ticketProviderURL))  throw new ExceptionVS(messageSource.getMessage(
                    "serverMismatchErrorMsg", [serverURL, ticketProviderURL].toArray(), locale));
            if (!hashCertVSBase64) throw new ExceptionVS(messageSource.getMessage("csrMissingHashCertVSErrorMsg",
                    [serverURL, ticketProviderURL].toArray(), locale));
            //HexBinaryAdapter hexConverter = new HexBinaryAdapter();
            //String hashCertVSBase64 = new String(hexConverter.unmarshal(certAttributeJSON.hashCertVS));
            Calendar calendar = Calendar.getInstance()
            Date certValidFrom = calendar.getTime()
            calendar.add(Calendar.DAY_OF_YEAR, 7);
            Date certValidTo = DateUtils.getMonday(calendar).getTime()
            X509Certificate issuedCert = signatureVSService.signCSR(csr, null, certValidFrom, certValidTo)
            if (!issuedCert)  throw new ExceptionVS(messageSource.getMessage('csrSigningErrorMsg', null, locale))
            else {
                TicketVS ticketVS = new TicketVS(serialNumber:issuedCert.getSerialNumber().longValue(),
                        content:issuedCert.getEncoded(), state:TicketVS.State.OK, hashCertVS:hashCertVSBase64,
                        ticketProviderURL: ticketProviderURL, amount:new BigDecimal(amount), currency:ticketCurrency,
                        authorityCertificateVS:signatureVSService.getServerCertificateVS(),
                        validFrom:certValidFrom, validTo: certValidTo).save()
                log.debug("signTicket - expended TicketVS '${ticketVS.id}'")
                byte[] issuedCertPEMBytes = CertUtil.getPEMEncoded(issuedCert);
                Map data = [ticketAmount:ticketAmount, ticketCurrency:ticketCurrency, ticketVS: ticketVS]
                return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.TICKET_REQUEST,
                        data:data, messageBytes:issuedCertPEMBytes)
            }
        } catch(ExceptionVS ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage(), type:TypeVS.ERROR)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR,
                    message:messageSource.getMessage('ticketWithdrawalDataError', null, locale))
        }
    }

    public synchronized ResponseVS signTicketBatchRequest (byte[] ticketBatchRequest, BigDecimal expectedAmount,
            CurrencyVS expectedCurrency, Locale locale){
        ResponseVS responseVS = null;
        String msg = null;
        List<TicketVS> issuedTicketList = new ArrayList<TicketVS>()
        try {
            JSONObject dataRequestJSON = JSON.parse(new String(ticketBatchRequest, "UTF-8"))
            JSONArray ticketsArray = dataRequestJSON.ticketCSR
            List<String> issuedTicketCertList = new ArrayList<String>()
            BigDecimal batchAmount = new BigDecimal(0)
            ticketsArray.each {
                String csr = it.csr
                CurrencyVS ticketCurrency = CurrencyVS.valueOf(it.currency)
                if(ticketCurrency != expectedCurrency) throw new ExceptionVS(messageSource.getMessage(
                        'ticketBatchRequestCurrencyErrorMsg', [expectedCurrency.toString(),
                        ticketCurrency.toString()].toArray(), locale));
                String ticketAmount = it.ticketValue
                batchAmount = batchAmount.add(new BigDecimal(ticketAmount))
                responseVS = signTicket(csr.getBytes(), ticketAmount, ticketCurrency, locale)
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    issuedTicketList.add(responseVS.data.ticketVS)
                    issuedTicketCertList.add(new String(responseVS.getMessageBytes(), "UTF-8"))
                } else throw new ExceptionVS(responseVS.getMessage())
            }
            if(expectedAmount.compareTo(batchAmount) != 0) throw new ExceptionVS(messageSource.getMessage(
                    'ticketBatchRequestAmountErrorMsg', ["${expectedAmount.toString()} ${expectedCurrency}",
                    "${batchAmount.toString()} ${expectedCurrency}"], locale))
            return new ResponseVS(statusCode: ResponseVS.SC_OK, data:issuedTicketCertList, type:TypeVS.TICKET_REQUEST);
        } catch(ExceptionVS ex) {
            log.error(ex.getMessage(), ex);
            cancelTickets(issuedTicketList, ex.getMessage())
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage(),
                    type:TypeVS.TICKET_REQUEST_ERROR)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.TICKET_REQUEST_ERROR,
                    message:messageSource.getMessage('ticketWithdrawalDataError', null, locale))
        }
    }

    private void cancelTickets(List<TicketVS> issuedTicketList, String reason) {
        for(TicketVS ticketVS : issuedTicketList) {
            ticketVS.state = TicketVS.State.CANCELLED
            ticketVS.reason = reason
            ticketVS.save()
        }
    }

}
