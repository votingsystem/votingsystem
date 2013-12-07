package org.votingsystem.accesscontrol.service

import org.bouncycastle.asn1.pkcs.CertificationRequestInfo
import org.bouncycastle.jce.PKCS10CertificationRequest
import org.bouncycastle.openssl.PEMReader
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
		X509Certificate issuedCert = signCSR(csr, representativeURL, privateKeySigner, certSigner, eventVS.dateBegin,
                    eventVS.dateFinish)
		if (!issuedCert) {
            String msg = "signCertVoteVS - issuedCert null"
            log.error(msg)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR, message:msg)
		} else {
			VoteRequestCsrVS solicitudCSR = new VoteRequestCsrVS(serialNumber:issuedCert.getSerialNumber().longValue(),
				    content:csr, eventVSElection:eventVS, state:VoteRequestCsrVS.State.OK,
                    hashCertVoteBase64:responseVS.data.hashCertVoteBase64)
			solicitudCSR.save()
			CertificateVS certificate = new CertificateVS(serialNumber:issuedCert.getSerialNumber().longValue(),
				    content:issuedCert.getEncoded(), eventVSElection:eventVS, voteRequestCsrVS:solicitudCSR,
                    type:CertificateVS.Type.VOTEVS, state:CertificateVS.State.OK,
				    hashCertVoteBase64:responseVS.data.hashCertVoteBase64)
            if(userVS?.type == UserVS.Type.REPRESENTATIVE) certificate.setUserVS(userVS)
			certificate.save()
			byte[] issuedCertPEMBytes = CertUtil.getPEMEncoded(issuedCert);
			Map data = [requestPublicKey:requestPublicKey, issuedCert:issuedCertPEMBytes]
			return new ResponseVS(statusCode:ResponseVS.SC_OK, data:data)
		}
	}
	
	/**
	 * Genera un certificado V3
	 */
	public X509Certificate signCSR(byte[] csrPEMBytes, String organizationalUnit,
			PrivateKey caKey, X509Certificate caCert, Date dateBegin, Date dateFinish)
			throws Exception {
		PKCS10CertificationRequest csr = fromPEMToPKCS10CertificationRequest(csrPEMBytes);
		String strSubjectDN = csr.getCertificationRequestInfo().getSubject().toString();
		if (!csr.verify() || strSubjectDN == null) {
			log.error("signCSR - ERROR VERIFYING");
			return null;
		}
		if(organizationalUnit != null) {
			strSubjectDN = organizationalUnit + "," + strSubjectDN;
		}
		//log.debug(" - strSubjectDN: " + strSubjectDN);
		X509Certificate issuedCert = CertUtil.generateV3EndEntityCertFromCsr(
				csr, caKey, caCert, dateBegin, dateFinish, "" + strSubjectDN);
		//byte[] issuedCertPemBytes = CertUtil.getPEMEncoded(issuedCert);
		//byte[] caCertPemBytes = CertUtil.getPEMEncoded(caCert);
		//byte[] resultCsr = new byte[issuedCertPemBytes.length + caCertPemBytes.length];
		//System.arraycopy(issuedCertPemBytes, 0, resultCsr, 0, issuedCertPemBytes.length);
		//System.arraycopy(caCertPemBytes, 0, resultCsr, issuedCertPemBytes.length, caCertPemBytes.length);
		//return resultCsr;
		return issuedCert;
	}
		
	public X509Certificate getVoteCert(byte[] csrFirmada) throws Exception {
		Collection<X509Certificate> certificates =
			CertUtil.fromPEMToX509CertCollection(csrFirmada);
		X509Certificate userCert
		for (X509Certificate certificate : certificates) {
			if (certificate.subjectDN.toString().contains("OU=hashCertVoteHex:")) {
				userCert = certificate
			}
		}
		return userCert
	}
	
	
	public X509Certificate getUserCert(byte[] csrFirmada) throws Exception {
		Collection<X509Certificate> certificates =
			CertUtil.fromPEMToX509CertCollection(csrFirmada);
		X509Certificate userCert
		for (X509Certificate certificate : certificates) {
			if (certificate.subjectDN.toString().contains("OU=deviceId:")) {
				userCert = certificate
			}
		}
		return userCert
	}
		
    private ResponseVS validateCSRVote(byte[] csrPEMBytes, EventVSElection eventVS, Locale locale) {
        PKCS10CertificationRequest csr = fromPEMToPKCS10CertificationRequest(csrPEMBytes);
		if(!csr) {
			String msg = messageSource.getMessage('csrRequestErrorMsg', null, locale)
			log.error("- validateCSRVote - ERROR  ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ACCESS_REQUEST_ERROR)
		}
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
		String eventId;
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
				VoteRequestCsrVS solicitudCSR = VoteRequestCsrVS.findWhere(
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
			data:[publicKey:csr.getPublicKey(), hashCertVoteBase64:hashCertVoteBase64])
    }
	
	public ResponseVS saveUserCSR(byte[] csrPEMBytes, Locale locale) {
		PKCS10CertificationRequest csr = fromPEMToPKCS10CertificationRequest(csrPEMBytes);
		CertificationRequestInfo info = csr.getCertificationRequestInfo();
		String nif;
		String email;
		String phone;
		String deviceId;
		String subjectDN = info.getSubject().toString();
		log.debug("saveUserCSR - subject: " + subjectDN)
		if(subjectDN.split("OU=email:").length > 1) {
			email = subjectDN.split("OU=email:")[1].split(",")[0];
		}
		if(subjectDN.split("CN=nif:").length > 1) {
			nif = subjectDN.split("CN=nif:")[1];
			if (nif.split(",").length > 1) {
				nif = nif.split(",")[0];
			}
		} else if(subjectDN.split("SERIALNUMBER=").length > 1) {
			nif = subjectDN.split("SERIALNUMBER=")[1];
			if (nif.split(",").length > 1) {
				nif = nif.split(",")[0];
			}
		}
		if (subjectDN.split("OU=phone:").length > 1) {
			phone = subjectDN.split("OU=phone:")[1].split(",")[0];
		}
		if (subjectDN.split("OU=deviceId:").length > 1) {
			deviceId = subjectDN.split("OU=deviceId:")[1].split(",")[0];
			log.debug("Con deviceId: ${deviceId}")
		} else log.debug("Sin deviceId")
		ResponseVS responseVS = subscriptionVSService.checkDevice(nif, phone, email, deviceId, locale)
		if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS;
		UserRequestCsrVS solicitudCSR
		def solicitudesPrevias = UserRequestCsrVS.findAllByDeviceVSAndUserVSAndState(
			responseVS.data, responseVS.userVS, UserRequestCsrVS.State.PENDING)
		solicitudesPrevias.each {eventVSItem ->
			eventVSItem.state = UserRequestCsrVS.State.CANCELLED
			eventVSItem.save();
		}
		UserRequestCsrVS.withTransaction {
			solicitudCSR = new UserRequestCsrVS(state:UserRequestCsrVS.State.PENDING,
				content:csrPEMBytes, userVS:responseVS.userVS,deviceVS:responseVS.data).save()
		}
		if(solicitudCSR) return new ResponseVS(statusCode:ResponseVS.SC_OK, message:solicitudCSR.id)
		else return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST)
	}
	
	public synchronized ResponseVS signCertUserVS (UserRequestCsrVS solicitudCSR, Locale locale) {
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
		X509Certificate issuedCert = signCSR(
				solicitudCSR.content, null, privateKeySigner,
				certSigner, today, today_plus_year.getTime())
		if (!issuedCert) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:TypeVS.ERROR_VALIDANDO_CSR.toString())
		} else {
			solicitudCSR.state = UserRequestCsrVS.State.OK
			solicitudCSR.serialNumber = issuedCert.getSerialNumber().longValue()
			solicitudCSR.save()
			CertificateVS certificate = new CertificateVS(serialNumber:issuedCert.getSerialNumber()?.longValue(),
				content:issuedCert.getEncoded(), userVS:solicitudCSR.userVS, state:CertificateVS.State.OK,
				userRequestCsrVS:solicitudCSR, type:CertificateVS.Type.USER)
			certificate.save()
			return new ResponseVS(statusCode:ResponseVS.SC_OK)
		}
	}
	
	
	PKCS10CertificationRequest fromPEMToPKCS10CertificationRequest (
		byte[] csrBytes) throws Exception {
		PEMReader pemReader = new PEMReader(new InputStreamReader(
			new ByteArrayInputStream(csrBytes)));
		PKCS10CertificationRequest result = (PKCS10CertificationRequest)pemReader.readObject()
		pemReader.close();
		return result;
	}

}
