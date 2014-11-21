package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import net.sf.json.JSONObject
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo
import org.bouncycastle.jce.PKCS10CertificationRequest
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.NifUtils

import static org.springframework.context.i18n.LocaleContextHolder.*
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.StringUtils

import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate

class CsrService {

    LinkGenerator grailsLinkGenerator
	def grailsApplication
	def subscriptionVSService
	def messageSource
    def signatureVSService
    def systemService

    public static class CsrResponse {
        PublicKey publicKey
        byte[] issuedCert
        String hashCertVSBase64
        public CsrResponse(PublicKey publicKey, byte[] issuedCert, String hashCertVSBase64) {
            this.publicKey = publicKey
            this.issuedCert = issuedCert
            this.hashCertVSBase64 = hashCertVSBase64
        }
    }

	public synchronized CsrResponse signCertVoteVS (byte[] csrPEMBytes, EventVS eventVS, UserVS userVS) throws Exception {
        CsrResponse csrResponse = validateCSRVote(csrPEMBytes, eventVS)
		KeyStoreVS keyStoreVS = eventVS.getKeyStoreVS()
		//TODO ==== vote keystore -- this is for developement
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreVS.bytes,
			grailsApplication.config.vs.signKeysPassword.toCharArray());
		PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(keyStoreVS.keyAlias,
			grailsApplication.config.vs.signKeysPassword.toCharArray());
		X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(keyStoreVS.keyAlias);
        DERTaggedObject representativeExtension = null
        PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
		if(userVS?.type == UserVS.Type.REPRESENTATIVE) {
            String representativeURL = "${grailsLinkGenerator.link(controller:"representative", absolute:true)}/${userVS?.id}"
            representativeExtension = new DERTaggedObject(ContextVS.REPRESENTATIVE_VOTE_TAG,
                    new DERUTF8String(representativeURL))
        }
		X509Certificate issuedCert = CertUtils.signCSR(csr, null, privateKeySigner, certSigner, eventVS.dateBegin,
                eventVS.dateFinish, representativeExtension)
		if (!issuedCert) throw new ExceptionVS(messageSource.getMessage('csrRequestErrorMsg', null, locale))
        CertificateVS certificate = new CertificateVS(serialNumber:issuedCert.getSerialNumber().longValue(),
                content:issuedCert.getEncoded(), eventVSElection:eventVS,
                type:CertificateVS.Type.VOTEVS, state:CertificateVS.State.OK, hashCertVSBase64:csrResponse.hashCertVSBase64)
        if(userVS?.type == UserVS.Type.REPRESENTATIVE) certificate.setUserVS(userVS)
        certificate.save()
        csrResponse.issuedCert = CertUtils.getPEMEncoded(issuedCert);
        return csrResponse
	}

    private CsrResponse validateCSRVote(byte[] csrPEMBytes, EventVSElection eventVS) throws Exception{
        PKCS10CertificationRequest csr
        try {
            csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        } catch(ex) {
            log.error(ex.getMessage(), ex)
            throw new ExceptionVS(messageSource.getMessage('csrRequestErrorMsg', null, locale))
        }
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects()
        JSONObject certDataJSON
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.VOTE_TAG:
                    certDataJSON = JSON.parse(((DERUTF8String)attribute.getObject()).getString())
                    break;
            }
        }
        String eventId = certDataJSON.eventId
        if (!eventId.equals(String.valueOf(eventVS.getId()))) {
            throw new ExceptionVS(messageSource.getMessage('csrRequestError', null, locale))
        }
        String accessControlURL = StringUtils.checkURL(certDataJSON.accessControlURL)
        String serverURL = grailsApplication.config.grails.serverURL
        if (!serverURL.equals(accessControlURL)) {
            throw new ExceptionVS(messageSource.getMessage('accessControlURLError',[serverURL, accessControlURL].toArray(),
                    locale))
        }
        return new CsrResponse(csr.getPublicKey(), null, certDataJSON.hashCertVS)
    }

    public ResponseVS signCertUserVS(MessageSMIME messageSMIME) throws Exception {
        UserVS userVS = messageSMIME.getUserVS()
        def requestJSON = JSON.parse(messageSMIME.getSMIME().getSignedContent())
        if(!systemService.isUserAdmin(userVS.nif) && !userVS.nif.equals(requestJSON.nif)) throw new ExceptionVS(
                messageSource.getMessage('userWithoutPrivilegesToValidateCSR', [userVS.nif].toArray(), locale))
        String validatedNif = NifUtils.validate(requestJSON.nif)
        List deviceVSList = DeviceVS.where {
            eq ("deviceId", requestJSON.deviceId)
            userVS {eq ("nif",  validatedNif)}
        }.list()
        if(deviceVSList.isEmpty()) throw new ExceptionVS(messageSource.getMessage('deviceNotFoundErrorMsg',[requestJSON.deviceId,
            validatedNif].toArray(), locale))
        DeviceVS deviceVS = deviceVSList.iterator().next()
        UserRequestCsrVS csrRequest = UserRequestCsrVS.findWhere(userVS:deviceVS.userVS,
                state:UserRequestCsrVS.State.PENDING);
        if (!csrRequest) throw new ExceptionVS(messageSource.getMessage('userRequestCsrMissingErrorMsg',
                [validatedNif, requestJSON.deviceId].toArray(), locale))
        ResponseVS responseVS = signCertUserVS(csrRequest)
        if(ResponseVS.SC_OK == responseVS.statusCode) {
            X509Certificate issuedCert = responseVS.data
            deviceVS.userVS.updateCertInfo(issuedCert).save()
            deviceVS.updateCertInfo(issuedCert).save()
        }
        return responseVS
    }

    public ResponseVS<X509Certificate> signCertUserVS(UserRequestCsrVS csrRequest) throws Exception {
        Date dateBegin = Calendar.getInstance().getTime();
        Date dateFinish = DateUtils.addDays(dateBegin, 365).getTime() //one year
        PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrRequest.content);
        X509Certificate issuedCert = signatureVSService.signCSR(csr, null,dateBegin, dateFinish, null)
        if (!issuedCert) throw new ExceptionVS(messageSource.getMessage("csrValidationErrorMsg", null, locale))
        csrRequest.serialNumber = issuedCert.getSerialNumber().longValue()
        csrRequest.setState(UserRequestCsrVS.State.OK).save()
        CertificateVS certificate = new CertificateVS(serialNumber:issuedCert.getSerialNumber()?.longValue(),
                content:issuedCert.getEncoded(), userVS:csrRequest.userVS, state:CertificateVS.State.OK,
                userRequestCsrVS:csrRequest, type:CertificateVS.Type.USER).save()
        log.debug("signCertUserVS - issued new CertificateVS id '${certificate.id}' for UserRequestCsrVS '$csrRequest.id'");
        return new ResponseVS(statusCode:ResponseVS.SC_OK, data:issuedCert)
    }

    public synchronized ResponseVS signAnonymousDelegationCert (byte[] csrPEMBytes) {
        PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        if(!csr) {
            String msg = messageSource.getMessage('csrRequestErrorMsg', null, locale)
            log.error("signAnonymousDelegationCert - msg:  ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ERROR)
        }
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects()
        def certAttributeJSON
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG:
                    String certAttributeJSONStr = ((DERUTF8String)attribute.getObject()).getString()
                    certAttributeJSON = JSON.parse(certAttributeJSONStr)
                    break;
            }
        }
        if(!certAttributeJSON) {
            String msg = messageSource.getMessage('csrRequestErrorMsg', null, locale)
            log.error("signAnonymousDelegationCert - missing certAttributeJSON")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ERROR)
        }
        String serverURL = grailsApplication.config.grails.serverURL
        String accessControlURL = StringUtils.checkURL(certAttributeJSON.accessControlURL)
        if (!serverURL.equals(accessControlURL) || !certAttributeJSON.hashCertVS ) {
            String msg = messageSource.getMessage('accessControlURLError',[serverURL,accessControlURL].toArray(),locale)
            log.error("- signAnonymousDelegationCert - ERROR - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ERROR)
        }
        //HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        //String hashCertVSBase64 = new String(hexConverter.unmarshal(certAttributeJSON.hashCertVS));
        String hashCertVSBase64 = certAttributeJSON.hashCertVS
        Date certValidFrom = DateUtils.getMonday(Calendar.getInstance()).getTime()
        Calendar calendarValidTo = Calendar.getInstance();
        calendarValidTo.add(Calendar.DATE, ContextVS.DAYS_ANONYMOUS_DELEGATION_DURATION);
        X509Certificate issuedCert = signatureVSService.signCSR(csr, null, certValidFrom, calendarValidTo.getTime())
        if (!issuedCert) {
            String msg = messageSource.getMessage('csrRequestErrorMsg', null, locale)
            log.error("signAnonymousDelegationCert - error signing cert")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR, message:msg)
        } else {
            CertificateVS certificate = new CertificateVS(serialNumber:issuedCert.getSerialNumber().longValue(),
                    content:issuedCert.getEncoded(), type:CertificateVS.Type.ANONYMOUS_REPRESENTATIVE_DELEGATION,
                    state:CertificateVS.State.OK, hashCertVSBase64:hashCertVSBase64, validFrom:certValidFrom,
                    validTo: calendarValidTo.getTime())
            certificate.save()
            log.debug("signAnonymousDelegationCert - expended CertificateVS '${certificate.id}'")
            byte[] issuedCertPEMBytes = CertUtils.getPEMEncoded(issuedCert);
            Map data = [requestPublicKey:csr.getPublicKey()]
            return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST,
                    contentType: ContentTypeVS.TEXT_STREAM, data:data, message:"certificateVS_${certificate.id}",
                    messageBytes:issuedCertPEMBytes)
        }
    }

	public X509Certificate getVoteCert(byte[] pemCertCollection) throws Exception {
		Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(pemCertCollection);
		for (X509Certificate certificate : certificates) {
			if (certificate.subjectDN.toString().contains("SERIALNUMBER=hashCertVoteHex:")) {
				return certificate
			}
		}
		return null
	}

    /*  C=ES, ST=State or Province, L=locality name, O=organization name, OU=org unit, CN=common name,
        emailAddress=user@votingsystem.org, SERIALNUMBER=1234, SN=surname, GN=given name, GN=name given */
	public ResponseVS saveUserCSR(byte[] csrPEMBytes) {
		PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
		CertificationRequestInfo info = csr.getCertificationRequestInfo();
        String subjectDN = info.getSubject().toString()
        UserVS userVS = UserVS.getUserVS(subjectDN)
        Enumeration csrAttributes = info.getAttributes().getObjects()
        JSONObject deviceDataJSON
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.DEVICEVS_TAG:
                    deviceDataJSON = JSON.parse(((DERUTF8String)attribute.getObject()).getString())
                    break;
            }
        }
		ResponseVS responseVS = subscriptionVSService.checkDevice(userVS.firstName, userVS.lastName, userVS.nif,
                deviceDataJSON?.mobilePhone, deviceDataJSON?.email,  deviceDataJSON?.deviceId, deviceDataJSON?.deviceType)
		if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS;
		def previousRequest = UserRequestCsrVS.findAllByDeviceVSAndUserVSAndState(
			responseVS.data, responseVS.userVS, UserRequestCsrVS.State.PENDING)
        for(UserRequestCsrVS prevRequest: previousRequest) {
            prevRequest.setState(UserRequestCsrVS.State.CANCELLED).save()
        }
        UserRequestCsrVS requestCSR
		UserRequestCsrVS.withTransaction {
			requestCSR = new UserRequestCsrVS(state:UserRequestCsrVS.State.PENDING,
				content:csrPEMBytes, userVS:responseVS.userVS,deviceVS:responseVS.data).save()
		}
        log.debug("saveUserCSR - UserRequestCsrVS id '$requestCSR.id' - cert subject '$subjectDN'")
		if(requestCSR) return new ResponseVS(statusCode:ResponseVS.SC_OK, message:requestCSR.id)
		else return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST)
	}

}
