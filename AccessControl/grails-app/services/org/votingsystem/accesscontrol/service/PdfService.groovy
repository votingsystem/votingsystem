package org.votingsystem.accesscontrol.service

import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.*
import org.bouncycastle.tsp.TimeStampToken
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.PDFDocumentVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.FileUtils

import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate

class PdfService {

	def grailsApplication
    def timeStampService
	def signatureVSService
	def messageSource
	def subscriptionVSService

	private PrivateKey key;
	private Certificate[] chain;

	private synchronized void init() throws Exception {
		log.debug "init - init - init"
		File keyStoreFile = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.keyStorePath).getFile()
		String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
		String password = grailsApplication.config.VotingSystem.signKeysPassword
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
			FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
		key = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray());
		chain = keyStore.getCertificateChain(aliasClaves);
		log.debug "aliasClaves: ${aliasClaves} - chain.length:${chain.length}"
	}
	
	public ResponseVS checkSignature (byte[] signedPDF, Locale locale) {
		log.debug "checkSignature - signedPDF.length: ${signedPDF.length}"
		ResponseVS responseVS = null;
		PdfReader reader = new PdfReader(signedPDF);
		AcroFields acroFields = reader.getAcroFields();
		ArrayList<String> names = acroFields.getSignatureNames();
        String msg = null;
        responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
			message:messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale));
		for (String name : names) {
			log.debug("checkSignature - Signature name: " + name + " - covers whole document:" +
				acroFields.signatureCoversWholeDocument(name));
			PdfPKCS7 pk = acroFields.verifySignature(name, "BC");
			log.debug("checkSignature - Hash verified -> ${pk.verify()}");
			if(!pk.verify()) {
				log.debug("checkSignature - VERIFICATION FAILED!!!");
				responseVS = new ResponseVS (statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:messageSource.getMessage('pdfSignedDocumentError', null, locale))
			}
			X509Certificate signingCert = pk.getSigningCertificate();
			UserVS userVS = UserVS.getUserVS(signingCert);
			log.debug("checkSignature - Signing cert Subject:" + PdfPKCS7.getSubjectFields(pk.getSigningCertificate()));
			//Calendar signDate = pk.getSignDate();
			X509Certificate[] pkc = (X509Certificate[])pk.getSignCertificateChain();
			TimeStampToken timeStampToken = pk.getTimeStampToken();
            if(timeStampToken != null) {
                ResponseVS timestampValidationResp = timeStampService.validateToken(timeStampToken, locale)
                log.debug("checkSignature - timestampValidationResp - " +
                        "statusCode:${timestampValidationResp.statusCode} - message:${timestampValidationResp.message}")
                if(ResponseVS.SC_OK != timestampValidationResp.statusCode) {
                    log.error("checkSignature - TIMESTAMP ERROR - ${timestampValidationResp.message}")
                    return timestampValidationResp
                }
            } else {
                msg = messageSource.getMessage('documentWithoutTimeStampErrorMsg', null, locale)
                log.error("ERROR - checkSignature - ${msg}")
                return new ResponseVS(message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST)
            }
            Calendar signDate = Calendar.getInstance();
            signDate.setTime(timeStampToken.getTimeStampInfo().getGenTime())
			KeyStore keyStore = signatureVSService.getTrustedCertsKeyStore()
			Object[] fails = PdfPKCS7.verifyCertificates(pkc, keyStore, null, signDate);
			if(fails != null) {
				log.debug("checkSignature - fails - Cert '${signingCert.getSerialNumber()?.longValue()}' has fails: ${fails[1]}" );
				for(X509Certificate cert:pkc) {
					String notAfter = DateUtils.getStringFromDate(cert.getNotAfter())
					String notBefore = DateUtils.getStringFromDate(cert.getNotBefore())
					log.debug("checkSignature - fails - Cert: ${cert.getSubjectDN()} - NotBefore: ${notBefore} - NotAfter: ${notAfter}")
				}
                msg = messageSource.getMessage('pdfSignedCertsErrorMsg', null, locale)
				return new ResponseVS (statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
			}
			CertificateVS certificate = CertificateVS.findWhere(serialNumber:signingCert.getSerialNumber()?.longValue())
			if (!certificate) {
				String subject = PdfPKCS7.getSubjectFields(pk.getSigningCertificate())
				CertificateVS certificateCA
				for(X509Certificate x509Certificate : pkc) {
					log.debug("checkSignature - checking document cert '${x509Certificate?.getSerialNumber()?.longValue()}'")
					if(signingCert.getSerialNumber()?.longValue() !=
						x509Certificate.getSerialNumber()?.longValue()) {
						log.debug("checkSignature - CA: '${x509Certificate?.getSerialNumber()?.longValue()}' - ${x509Certificate.getSubjectDN().toString()}")
						certificateCA = signatureVSService.getCACertificate(x509Certificate.getSerialNumber()?.longValue())
						//log.debug("checkSignature - CA id: ${certificateCA?.id}")
						userVS.setCertificateCA(certificateCA);
					}
				}
				ResponseVS userValidationResponseVS = subscriptionVSService.checkUser(userVS, locale);
				if(ResponseVS.SC_OK != userValidationResponseVS.statusCode) return userValidationResponseVS;
				userVS = userValidationResponseVS.userVS;
				certificate = (CertificateVS)userValidationResponseVS.data;
			} else userVS = certificate.userVS;

            userVS.setTimeStampToken(timeStampToken)
            PDFDocumentVS pdfDocumentVS = new PDFDocumentVS(pdf:signedPDF, userVS:userVS, timeStampToken:timeStampToken,
				signDate:timeStampToken?.getTimeStampInfo()?.getGenTime(), state:PDFDocumentVS.State.VALIDATED)
			PDFDocumentVS.withTransaction { pdfDocumentVS.save() }
			responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, data:pdfDocumentVS);
			/*BasicOCSPResp ocsp = pk.getOcsp();
			if (ocsp != null) {
				// Get a trusted certificate (could have come from a certificate store)
				InputStream inStream = new FileInputStream("responder.cer");
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				X509Certificate cert = (X509Certificate)cf.generateCertificate(inStream);
				inStream.close();
				log.debug("OCSP signature verifies: " + ocsp.verify(cert.getPublicKey(), "BC"));
				log.debug("OCSP revocation refers to this certificate: " + pk.isRevocationValid());
			}*/
		}
		log.debug "checkSignature - DOCUMENT OK"
		return responseVS;
	}

	public ResponseVS signDocument(PdfReader reader, String reason, String location,
                             PDFDocumentVS pdfDocumentVS) throws Exception {
		ResponseVS responseVS
		try {
			File file = File.createTempFile("serverSignedPDF", ".pdf")
			file.deleteOnExit();
			FileOutputStream outputStream = new FileOutputStream(file)
			PdfStamper stp = PdfStamper.createSignature(reader, outputStream, '\0' as char, null, true);
			PdfSignatureAppearance signatureAppearance = stp.getSignatureAppearance();
			signatureAppearance.setCrypto(getPrivateKey(), getServerCertChain(), null, PdfSignatureAppearance.WINCER_SIGNED);
			signatureAppearance.setReason(reason);
			signatureAppearance.setLocation(location);
			signatureAppearance.setVisibleSignature(new Rectangle(330, 40, 580, 140), 1, null);
			log.debug("signDocument - stp.hasSignature: " + stp.hasSignature)
			if (stp != null) stp.close();
			pdfDocumentVS.pdf = file.getBytes()
			pdfDocumentVS.save()
			responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, file:file)
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage())
		}
		return responseVS
	}
	
	public ResponseVS signDocumentAndBlock(PdfReader reader, String reason,
			String location, PDFDocumentVS pdfDocumentVS) throws Exception {
		ResponseVS responseVS
		try {
			File file = File.createTempFile("serverSignedPDF", ".pdf")
			file.deleteOnExit();
			FileOutputStream outputStream = new FileOutputStream(file)
			PdfStamper stp = PdfStamper.createSignature(reader, outputStream, '\0' as char, null, true);
			stp.setEncryption(null, null,PdfWriter.ALLOW_PRINTING, false);
			PdfSignatureAppearance signatureAppearance = stp.getSignatureAppearance();
			signatureAppearance.setCrypto(getPrivateKey(), getServerCertChain(), null, PdfSignatureAppearance.WINCER_SIGNED);
			signatureAppearance.setReason(reason);
			signatureAppearance.setLocation(location);
			signatureAppearance.setVisibleSignature(new Rectangle(330, 40, 580, 140), 1, null);
			log.debug("signDocumentAndBlock - stp.hasSignature: " + stp.hasSignature)
			if (stp != null) stp.close();
			pdfDocumentVS.pdf = file.getBytes()
			pdfDocumentVS.save()
			responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, file:file)
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage())
		}
		return responseVS
	}
	
	public static void concatenate2PDF(PdfReader reader1, PdfReader reader2,
		FileOutputStream outputStream) throws Exception {
		//Document document = new Document();
		//document.addHeader(null, null);
		PdfCopyFields copy = new PdfCopyFields(outputStream);
		copy.addDocument(reader1);
		copy.addDocument(reader2);
		copy.close();
	}


    private Certificate[] getServerCertChain() {
        if(chain == null) init();
        return chain;
    }

    private PrivateKey getPrivateKey() {
        if(key == null) init();
        return key;
    }

}