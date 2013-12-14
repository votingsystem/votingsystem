package org.votingsystem.accesscontrol.service

import org.bouncycastle.asn1.ASN1Set
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo
import org.bouncycastle.jce.PKCS10CertificationRequest
import org.votingsystem.signature.util.PKCS10WrapperClient
import org.votingsystem.util.StringUtils
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.KeyStoreVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserRequestCsrVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VoteRequestCsrVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.util.FileUtils

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate

class CsrService {

    LinkGenerator grailsLinkGenerator
	def grailsApplication
	def subscriptionVSService
	def messageSource
	
	public synchronized ResponseVS signCertVoteVS (byte[] csr, EventVS eventVS, UserVS userVS, Locale locale) {
		log.debug("signCertVoteVS - eventVS: ${eventVS?.id}");
		ResponseVS responseVS = validateCSRVote(csr, eventVS, locale)
		if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
		PublicKey requestPublicKey = responseVS.data.publicKey
		KeyStoreVS keyStoreVS = eventVS.getKeyStoreVS()
		//TODO ==== vote keystore -- this is for developement
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreVS.bytes,
			grailsApplication.config.VotingSystem.signKeysPassword.toCharArray());
		PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(keyStoreVS.keyAlias,
			grailsApplication.config.VotingSystem.signKeysPassword.toCharArray());
		X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(keyStoreVS.keyAlias);
		String representativeURL = null
		if(userVS?.type == UserVS.Type.REPRESENTATIVE) {
            representativeURL = "OU=RepresentativeURL:${grailsLinkGenerator.link(controller:"representative", absolute:true)}/${userVS?.id}"
        }
		X509Certificate issuedCert = CertUtil.signCSR(csr, representativeURL, privateKeySigner, certSigner,
                eventVS.dateBegin, eventVS.dateFinish)
		if (!issuedCert) {
            String msg = messageSource.getMessage('csrVoteRequestErrorMsg', null, locale)
            log.error(msg)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR, message:msg)
		} else {
			VoteRequestCsrVS requestCSR = new VoteRequestCsrVS(serialNumber:issuedCert.getSerialNumber().longValue(),
				    content:csr, eventVSElection:eventVS, state:VoteRequestCsrVS.State.OK,
                    hashCertVoteBase64:responseVS.data.hashCertVoteBase64)
			requestCSR.save()
			CertificateVS certificate = new CertificateVS(serialNumber:issuedCert.getSerialNumber().longValue(),
				    content:issuedCert.getEncoded(), eventVSElection:eventVS, voteRequestCsrVS:requestCSR,
                    type:CertificateVS.Type.VOTEVS, state:CertificateVS.State.OK,
				    hashCertVoteBase64:responseVS.data.hashCertVoteBase64)
            if(userVS?.type == UserVS.Type.REPRESENTATIVE) certificate.setUserVS(userVS)
			certificate.save()
			byte[] issuedCertPEMBytes = CertUtil.getPEMEncoded(issuedCert);
			Map data = [requestPublicKey:requestPublicKey, issuedCert:issuedCertPEMBytes]
			return new ResponseVS(statusCode:ResponseVS.SC_OK, data:data)
		}
	}
		
	public X509Certificate getVoteCert(byte[] pemCertCollection) throws Exception {
		Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(pemCertCollection);
		for (X509Certificate certificate : certificates) {
			if (certificate.subjectDN.toString().contains("OU=hashCertVoteHex:")) {
				return certificate
			}
		}
		return null
	}
	
	
	public X509Certificate getUserCert(byte[] pemCertCollection) throws Exception {
		Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(pemCertCollection);
		for (X509Certificate certificate : certificates) {
			if (certificate.subjectDN.toString().contains("deviceId=")) {
				return certificate;
			}
		}
		return null
	}
		
    private ResponseVS validateCSRVote(byte[] csrPEMBytes, EventVSElection eventVS, Locale locale) {
        PKCS10CertificationRequest csr = CertUtil.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
		if(!csr) {
			String msg = messageSource.getMessage('csrVoteRequestErrorMsg', null, locale)
			log.error("- validateCSRVote - ERROR  ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ACCESS_REQUEST_ERROR)
		}
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects()
        String accessControlURL
        String eventId
        String hashCertVoteHex
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case PKCS10WrapperClient.ACCESS_CONTROL_URL_TAG:
                    accessControlURL = ((DERUTF8String)attribute.getObject()).getString()
                    break;
                case PKCS10WrapperClient.EVENT_ID_TAG:
                    eventId = ((DERUTF8String)attribute.getObject()).getString()
                    break;
                case PKCS10WrapperClient.HASH_CERT_VOTE_TAG:
                    hashCertVoteHex = ((DERUTF8String)attribute.getObject()).getString()
                    break;
            }
        }
        log.debug("validateCSRVote - accessControlURL: ${accessControlURL} - eventId: ${eventId} - " +
                "hashCertVoteHex: ${hashCertVoteHex}")
        if (!eventId.equals(String.valueOf(eventVS.getId()))) {
            String msg = messageSource.getMessage('csrRequestError', null, locale)
            log.error("- validateCSRVote - ERROR - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,message:msg, type:TypeVS.ACCESS_REQUEST_ERROR)
        }
        accessControlURL = StringUtils.checkURL(accessControlURL)
        String serverURL = grailsApplication.config.grails.serverURL
        if (!serverURL.equals(accessControlURL)) {
            String msg = messageSource.getMessage('accessControlURLError',[serverURL,accessControlURL].toArray(),locale)
            log.error("- validateCSRVote - ERROR - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ACCESS_REQUEST_ERROR)
        }
        try {
            HexBinaryAdapter hexConverter = new HexBinaryAdapter();
            String hashCertVoteBase64 = new String(hexConverter.unmarshal(hashCertVoteHex));
            VoteRequestCsrVS csrVote = VoteRequestCsrVS.findWhere(hashCertVoteBase64:hashCertVoteBase64)
            if (csrVote) {
                String msg = messageSource.getMessage('hashCertVoteRepeated', [hashCertVoteBase64].toArray(), locale)
                log.error("- validateCSRVote - ERROR - repeated cert votes: ${csrVote.id} - ${msg}")
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                        type:TypeVS.ACCESS_REQUEST_ERROR)
            } else return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.ACCESS_REQUEST,
                    data:[publicKey:csr.getPublicKey(), hashCertVoteBase64:hashCertVoteBase64])
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage(),
                    type:TypeVS.ACCESS_REQUEST_ERROR)
        }

        /*String eventId;
        String accessControlURL;
        String hashCertVoteHex;
        String hashCertVoteBase64;
        String subjectDN = info.getSubject().toString();
        log.debug("validateCSRVote - subject: " + subjectDN)
        if(subjectDN.split("OU=eventId:").length > 1) {
            eventId = subjectDN.split("OU=eventId:")[1].split(",")[0];
            if (!eventId.equals(String.valueOf(eventVS.getId()))) {
                String msg = messageSource.getMessage('csrRequestError', null, locale)
                log.error("- validateCSRVote - ERROR - ${msg}")
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                        message:msg, type:TypeVS.ACCESS_REQUEST_ERROR)
            }
        }
        if(subjectDN.split("CN=accessControlURL:").length > 1) {
            String parte = subjectDN.split("CN=accessControlURL:")[1];
            if (parte.split(",").length > 1) {
                accessControlURL = parte.split(",")[0];
            } else accessControlURL = parte;
            accessControlURL = org.votingsystem.util.StringUtils.checkURL(accessControlURL)
            String serverURL = grailsApplication.config.grails.serverURL
            if (!serverURL.equals(accessControlURL)) {
                String msg = messageSource.getMessage(
                    'accessControlURLError', [serverURL, accessControlURL].toArray(), locale)
                log.error("- validateCSRVote - ERROR - ${msg}")
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:msg, type:TypeVS.ACCESS_REQUEST_ERROR)
            }
        }
        if (subjectDN.split("OU=hashCertVoteHex:").length > 1) {
            try {
                hashCertVoteHex = subjectDN.split("OU=hashCertVoteHex:")[1].split(",")[0];
                HexBinaryAdapter hexConverter = new HexBinaryAdapter();
                hashCertVoteBase64 = new String(hexConverter.unmarshal(hashCertVoteHex));
                log.debug("hashCertVoteBase64: ${hashCertVoteBase64}")
                VoteRequestCsrVS requestCSR = VoteRequestCsrVS.findWhere(
                    hashCertVoteBase64:hashCertVoteBase64)
                if (solicitudCSR) {
                    String msg = messageSource.getMessage(
                        'hashCertVoteRepeated', [hashCertVoteBase64].toArray(), locale)
                    log.error("- validateCSRVote - ERROR - solicitudCSR previa: ${solicitudCSR.id} - ${msg}")
                    return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                        message:msg, type:TypeVS.ACCESS_REQUEST_ERROR)
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex)
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:ex.getMessage(), type:TypeVS.ACCESS_REQUEST_ERROR)
            }
        }
		return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.ACCESS_REQUEST,
			data:[publicKey:csr.getPublicKey(), hashCertVoteBase64:hashCertVoteBase64])*/
    }

    /*  C=ES,
        ST=State or Province,
        L=locality name,
        O=organization name,
        OU=org unit,
        CN=common name,
        emailAddress=user@votingsystem.org,
        serialNumber=1234,
        mobilePhone=555555555,
        deviceId=4321,
        SN=surname,
        GN=given name,
        GN=name given */
	public ResponseVS saveUserCSR(byte[] csrPEMBytes, Locale locale) {
		PKCS10CertificationRequest csr = CertUtil.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
		CertificationRequestInfo info = csr.getCertificationRequestInfo();
		String nif;
		String email;
		String phone;
		String deviceId;
		String subjectDN = info.getSubject().toString();
		log.debug("saveUserCSR - subject: " + subjectDN)
		if(subjectDN.split("emailAddress=").length > 1) {
			email = subjectDN.split("emailAddress=")[1].split(",")[0];
		}
		if(subjectDN.split("SERIALNUMBER=").length > 1) {
			nif = subjectDN.split("SERIALNUMBER=")[1];
			if (nif.split(",").length > 1)  nif = nif.split(",")[0];
		}
		if (subjectDN.split("mobilePhone=").length > 1) {
			phone = subjectDN.split("mobilePhone=")[1].split(",")[0];
		}
		if (subjectDN.split("deviceId=").length > 1) {
			deviceId = subjectDN.split("deviceId=")[1].split(",")[0];
			log.debug("deviceId: ${deviceId}")
		} else log.debug("deviceId not found")
		ResponseVS responseVS = subscriptionVSService.checkDevice(nif, phone, email, deviceId, locale)
		if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS;
		UserRequestCsrVS requestCSR
		def solicitudesPrevias = UserRequestCsrVS.findAllByDeviceVSAndUserVSAndState(
			responseVS.data, responseVS.userVS, UserRequestCsrVS.State.PENDING)
		solicitudesPrevias.each {eventVSItem ->
			eventVSItem.state = UserRequestCsrVS.State.CANCELLED
			eventVSItem.save();
		}
		UserRequestCsrVS.withTransaction {
			requestCSR = new UserRequestCsrVS(state:UserRequestCsrVS.State.PENDING,
				content:csrPEMBytes, userVS:responseVS.userVS,deviceVS:responseVS.data).save()
		}
		if(requestCSR) return new ResponseVS(statusCode:ResponseVS.SC_OK, message:requestCSR.id)
		else return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST)
	}
	
	public synchronized ResponseVS signCertUserVS (UserRequestCsrVS requestCSR, Locale locale) {
		log.debug("signCertUserVS");
		File keyStoreFile = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.keyStorePath).getFile()
		String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
		String password = grailsApplication.config.VotingSystem.signKeysPassword
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
			FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
		PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray());
		X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(aliasClaves);
		
		//log.debug("signCertUserVS - certSigner:${certSigner}");

		Date today = Calendar.getInstance().getTime();
		Calendar today_plus_year = Calendar.getInstance();
		today_plus_year.add(Calendar.YEAR, 1);
		X509Certificate issuedCert = CertUtil.signCSR(requestCSR.content, null, privateKeySigner,
				certSigner, today, today_plus_year.getTime())
		if (!issuedCert) {
			return new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    messageSource.getMessage("csrValidationErrorMsg", null, locale))
		} else {
			requestCSR.state = UserRequestCsrVS.State.OK
			requestCSR.serialNumber = issuedCert.getSerialNumber().longValue()
			requestCSR.save()
			CertificateVS certificate = new CertificateVS(serialNumber:issuedCert.getSerialNumber()?.longValue(),
				content:issuedCert.getEncoded(), userVS:requestCSR.userVS, state:CertificateVS.State.OK,
				userRequestCsrVS:requestCSR, type:CertificateVS.Type.USER)
			certificate.save()
			return new ResponseVS(statusCode:ResponseVS.SC_OK)
		}
	}


}
