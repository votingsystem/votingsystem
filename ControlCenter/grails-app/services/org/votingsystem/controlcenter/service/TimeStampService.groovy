package org.votingsystem.controlcenter.service

import org.votingsystem.controlcenter.model.*
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.DERObject
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.*;

import grails.converters.JSON
import java.security.cert.X509Certificate;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean

import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.SignerInformationVerifier
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.operator.DigestCalculator
import org.bouncycastle.tsp.TimeStampToken
import org.codehaus.groovy.grails.web.json.JSONElement

import java.security.MessageDigest
import java.security.cert.X509Certificate;

import org.bouncycastle.tsp.TSPUtil
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.asn1.cms.CMSAttributes;


class TimeStampService {
	
	private static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
	
	private static final int numMaxAttempts = 3;
	
	def grailsApplication
	def messageSource
	def httpService

	private static final HashMap<Long, SignerInformationVerifier> timeStampVerifiers =
			new HashMap<Long, Set<X509Certificate>>();
			
	public void afterPropertiesSet() throws Exception {}
	
	public ResponseVS validateToken(TimeStampToken timeStampToken, 
		EventoVotacion evento, Locale locale) throws Exception {
		log.debug("validateToken - event:${evento.id}")
		String msg = null;
		ResponseVS respuesta
		try {
			if(!timeStampToken) {
				msg = messageSource.getMessage('timeStampNullErrorMsg', null, locale)
				log.error("ERROR - validateToken - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_NULL_REQUEST, message:msg)
			}
			ControlAcceso accessControl = evento.controlAcceso
			SignerInformationVerifier timeStampVerifier = timeStampVerifiers.get(accessControl.id)
			if(!timeStampVerifier) {
				String accessControlURL = accessControl.serverURL
				while(accessControlURL.endsWith("/")) {
					accessControlURL = accessControlURL.substring(0, accessControlURL.length() - 1)
				}
				String timeStampCertURL = "${accessControlURL}/timeStamp/cert"
				respuesta = httpService.getInfo(timeStampCertURL, null)
				if(ResponseVS.SC_OK != respuesta.statusCode) {
					msg = messageSource.getMessage('timeStampCertErrorMsg', [timeStampCertURL].toArray(), locale)
					log.error("validateToken - ${msg}")
					return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
				} else {
					X509Certificate timeStampCert = CertUtil.fromPEMToX509Cert(respuesta.messageBytes)
					timeStampVerifier = new JcaSimpleSignerInfoVerifierBuilder().
						setProvider(BC).build(timeStampCert)
					timeStampVerifiers.put(accessControl.id, timeStampVerifier)
				}
			}
			
			respuesta = validateToken(timeStampToken, timeStampVerifier, locale)
			if(ResponseVS.SC_OK != respuesta.statusCode) return respuesta
			
			Date timestampDate = timeStampToken.getTimeStampInfo().getGenTime()
			if(!timestampDate.after(evento.fechaInicio) ||
				!timestampDate.before(evento.getDateFinish())) {
				msg = messageSource.getMessage('timestampDateErrorMsg',
					[timestampDate, evento.fechaInicio, evento.getDateFinish()].toArray(), locale)
				log.debug("validateToken - ERROR TIMESTAMP DATE -  - Event '${evento.id}' - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:msg, evento:evento)
			} else return new ResponseVS(statusCode:ResponseVS.SC_OK);
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('timeStampErrorMsg', null, locale)
			log.error ("validateToken - msg:${msg} - Event '${evento.id}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
	}


	public ResponseVS validateToken(TimeStampToken  tsToken, 
			SignerInformationVerifier sigVerifier, Locale locale) {
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
