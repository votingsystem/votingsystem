package org.votingsystem.controlcenter.service

import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.DERObject
import org.bouncycastle.asn1.cms.CMSAttributes
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.SignerInformationVerifier
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.operator.DigestCalculator
import org.bouncycastle.tsp.TSPUtil
import org.bouncycastle.tsp.TimeStampToken
import org.bouncycastle.util.encoders.Base64
import org.votingsystem.model.AccessControlVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.HttpHelper

import java.security.MessageDigest
import java.security.cert.X509Certificate

class TimeStampService {
	
	private static final int numMaxAttempts = 3;
	
	def grailsApplication
	def messageSource

	private static final HashMap<Long, SignerInformationVerifier> timeStampVerifiers =
			new HashMap<Long, Set<X509Certificate>>();
			
	public void afterPropertiesSet() throws Exception {}
	
	public ResponseVS validateToken(TimeStampToken timeStampToken, EventVS eventVS, Locale locale) throws Exception {
		log.debug("validateToken - event:${eventVS.id}")
		String msg = null;
		ResponseVS responseVS
		try {
			if(!timeStampToken) {
				msg = messageSource.getMessage('timeStampNullErrorMsg', null, locale)
				log.error("ERROR - validateToken - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_NULL_REQUEST, message:msg)
			}
			SignerInformationVerifier timeStampVerifier = timeStampVerifiers.get(eventVS.accessControlVS.id)
			if(!timeStampVerifier) {
				String timeStampCertURL = "${eventVS.accessControlVS.serverURL}/timeStampVS/cert"
				responseVS = HttpHelper.getInstance().getData(timeStampCertURL, ContentTypeVS.X509_CA)
				if(ResponseVS.SC_OK != responseVS.statusCode) {
					msg = messageSource.getMessage('timeStampCertErrorMsg', [timeStampCertURL].toArray(), locale)
					log.error("validateToken - ${msg}")
					return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
				} else {
					X509Certificate timeStampCert = CertUtil.fromPEMToX509Cert(responseVS.messageBytes)
					timeStampVerifier = new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                            ContextVS.PROVIDER).build(timeStampCert)
					timeStampVerifiers.put(eventVS.accessControlVS.id, timeStampVerifier)
				}
			}
			
			responseVS = validateToken(timeStampToken, timeStampVerifier, locale)
			if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
			
			Date timestampDate = timeStampToken.getTimeStampInfo().getGenTime()
			if(!timestampDate.after(eventVS.dateBegin) || !timestampDate.before(eventVS.getDateFinish())) {
				msg = messageSource.getMessage('timestampDateErrorMsg',
					[timestampDate, eventVS.dateBegin, eventVS.getDateFinish()].toArray(), locale)
				log.debug("validateToken - ERROR TIMESTAMP DATE -  - Event '${eventVS.id}' - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:msg, eventVS:eventVS)
			} else return new ResponseVS(statusCode:ResponseVS.SC_OK);
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('timeStampErrorMsg', null, locale)
			log.error ("validateToken - msg:${msg} - Event '${eventVS.id}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
	}


	public ResponseVS validateToken(TimeStampToken  tsToken, SignerInformationVerifier sigVerifier, Locale locale) {
		log.debug("validateToken")
		String msg = null
		try {
			X509CertificateHolder certHolder = sigVerifier.getAssociatedCertificate();
			DigestCalculator calc = sigVerifier.getDigestCalculator(tsToken.certID.getHashAlgorithm());
			OutputStream cOut = calc.getOutputStream();
			cOut.write(certHolder.getEncoded());
			cOut.close();
			if (!Arrays.equals(tsToken.certID.getCertHash(), calc.getDigest())) {
				msg = messageSource.getMessage('hashCertifcatesErrorMsg', null, locale)
				log.error("validate - ERROR - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
			}
			if (tsToken.certID.getIssuerSerial() != null) {
				IssuerAndSerialNumber issuerSerial = certHolder.getIssuerAndSerialNumber();
				if (!tsToken.certID.getIssuerSerial().getSerial().equals(issuerSerial.getSerialNumber())) {
					msg = messageSource.getMessage('issuerSerialErrorMsg', null, locale)
					log.error("validate - ERROR - ${msg}")
					return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
				}
			}
			TSPUtil.validateCertificate(certHolder);
			if (!certHolder.isValidOn(tsToken.tstInfo.getGenTime())) {
				msg = messageSource.getMessage('certificateDateError', null, locale)
				log.error("validate - ERROR - ${msg}");
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
			}
			CMSSignedData tokenCMSSignedData = tsToken.tsToken
			Collection signers = tokenCMSSignedData.getSignerInfos().getSigners();
			SignerInformation tsaSignerInfo = (SignerInformation)signers.iterator().next();

			DERObject validMessageDigest = tsaSignerInfo.getSingleValuedSignedAttribute(
				CMSAttributes.messageDigest, "message-digest");
			ASN1OctetString signedMessageDigest = (ASN1OctetString)validMessageDigest
			byte[] digestToken = signedMessageDigest.getOctets();
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream()
			tsToken.tsaSignerInfo.content.write(baos);

			String algorithmStr = TSPUtil.getDigestAlgName(
				tsToken.tsaSignerInfo.getDigestAlgorithmID().getAlgorithm().toString())
			
			byte[] contentBytes = baos.toByteArray()
			MessageDigest sha = MessageDigest.getInstance(algorithmStr);
			byte[] resultDigest =  sha.digest(contentBytes);
			baos.close();
			
			if(!Arrays.equals(digestToken, resultDigest)) {
				String tokenStr = new String(Base64.encode(tsToken.getEncoded()));
				String resultDigestStr = new String(Base64.encode(resultDigest));
				String digestTokenStr = new String(Base64.encode(digestToken));
				msg = "resultDigestStr '${resultDigestStr} - digestTokenStr '${digestTokenStr}'"
				log.error("validate - ERROR HASH - ${msg}");
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
			}
			return new ResponseVS(statusCode:ResponseVS.SC_OK)
		}catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			log.debug("validate - token issuer: ${tsToken?.getSID()?.getIssuer()}" +
				" - timeStampSignerInfoVerifier: ${timeStampSignerInfoVerifier?.associatedCertificate?.subject}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:messageSource.getMessage('timeStampErrorMsg', null, locale))
		}
	}
		
			
}
