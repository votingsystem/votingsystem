package org.votingsystem.accesscontrol.service

import org.votingsystem.accesscontrol.model.*

import grails.converters.JSON

import org.codehaus.groovy.grails.web.json.JSONElement
import org.bouncycastle.util.encoders.Base64;

import com.itextpdf.text.pdf.PdfReader;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.*
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;

import org.bouncycastle.asn1.*;
import org.bouncycastle.cert.CertUtils;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.bouncycastle.tsp.*;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.TimeStampResponseGenerator

import static org.bouncycastle.tsp.TSPAlgorithms.*

import org.bouncycastle.util.Store;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.asn1.cmp.PKIFailureInfo
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.votingsystem.util.DateUtils;
import org.votingsystem.signature.util.*
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.votingsystem.util.*;
import org.springframework.beans.factory.InitializingBean

//class TimeStampService implements InitializingBean {
class TimeStampService {
	
	def grailsApplication
	def messageSource
	
	private static final int numMaxAttempts = 3;
	
	private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
	private static final String DEFAULT_TSA_POLICY_OID = "1.2.3";
	private static final Integer ACCURACYMICROS = 500;
	private static final Integer ACCURACYMILLIS = 500;
	private static final Integer ACCURACYSECONDS = 1;
	
	//# Optional. Specify if requests are ordered. Only false is supported.
	private boolean ORDERING = false;
	private static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

	private AtomicLong sernoGenerator
	private org.votingsystem.signature.util.TimeStampResponseGenerator timeStampResponseGen;
	private SignerInformationVerifier timeStampSignerInfoVerifier
	private byte[] signingCertBytes
	private String tsaName
	
	
	private static List ACCEPTEDPOLICIES = ["1.2.3", "1.2.4"];
	private static List ACCEPTEDEXTENSIONS = [];
	private static List ACCEPTEDALGORITHMS = ["SHA1","SHA256", "SHA512"];
	
	
	private transient SecureRandom random;
	/** Random generator algorithm. */
	private static String algorithm = "SHA1PRNG";
	private X509Certificate signingCert;
	/** number of bytes serial number to generate, default 8 */
	private int noOctets = 8;
	
	private static HashMap<String, String> ACCEPTEDALGORITHMSMAP = [
			"SHA1":TSPAlgorithms.SHA1,
			"SHA224":TSPAlgorithms.SHA224,
			"SHA256":TSPAlgorithms.SHA256,
			"SHA384":TSPAlgorithms.SHA384,
			"SHA512":TSPAlgorithms.SHA512
		];
	
	//@Override
	public void afterPropertiesSet() throws Exception {
		log.debug(" - afterPropertiesSet - afterPropertiesSet - afterPropertiesSet");
		File keyStoreFile = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.keyStorePath).getFile()
		String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
		String password = grailsApplication.config.VotingSystem.signKeysPassword
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
			FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
		PrivateKey signingKey = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray());
		signingCert = keyStore.getCertificate(aliasClaves)
		
		signingCertBytes = CertUtil.fromX509CertToPEM (signingCert)
		
		timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().
			setProvider(BC).build(signingCert);
		
		Certificate[] chain = keyStore.getCertificateChain(aliasClaves);
		Store certs = new JcaCertStore(Arrays.asList(chain));
		JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(
			new JcaDigestCalculatorProviderBuilder().setProvider(BC).build());
		TimeStampTokenGenerator timeStampTokenGen = new TimeStampTokenGenerator(infoGeneratorBuilder.build(
			new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC).build(signingKey), signingCert),
			new ASN1ObjectIdentifier(DEFAULT_TSA_POLICY_OID));
		timeStampTokenGen.setAccuracyMicros(ACCURACYMICROS);
		timeStampTokenGen.setAccuracyMillis(ACCURACYMILLIS);
		timeStampTokenGen.setAccuracySeconds(ACCURACYSECONDS);
		timeStampTokenGen.setOrdering(ORDERING);
		timeStampTokenGen.addCertificates(certs);
		timeStampResponseGen = new TimeStampResponseGenerator(timeStampTokenGen,
				getAcceptedAlgorithms(), getAcceptedPolicies(),
				getAcceptedExtensions())
	}
	
	private TimeStampResponseGenerator getTimeStampResponseGen() {
		if(!timeStampResponseGen) afterPropertiesSet()
		return timeStampResponseGen
	}
	
	
	public byte[] getSigningCert() {
		log.debug("getSigningCerts");
		if(!signingCertBytes) afterPropertiesSet()
		return signingCertBytes
	}
	
	/*Method to Tests*/
	void inicializarTest() {
		log.debug("inicializarTest");
		String authorityDN = "O=Sistema de Votación, C=ES";
		KeyPair authorityKP = TSPTestUtil.makeKeyPair();
		X509Certificate authorityCert = TSPTestUtil.makeCACertificate(authorityKP,
				authorityDN, authorityKP, authorityDN);
		String signingDN = "CN=, E=jgzornoza@gmail.com, O=Sistema de Votación, C=ES";
		KeyPair signingKP = TSPTestUtil.makeKeyPair();
		X509Certificate signingCert = TSPTestUtil.makeCertificate(signingKP, signingDN, authorityKP, authorityDN);
		PrivateKey signingKey = signingKP.getPrivate();
		List certList = new ArrayList();
		certList.add(signingCert);
		certList.add(authorityCert);
		Store certs = new JcaCertStore(certList);
		JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(
			new JcaDigestCalculatorProviderBuilder().setProvider(BC).build());
		TimeStampTokenGenerator timeStampTokenGen = new TimeStampTokenGenerator(infoGeneratorBuilder.build(
			new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC).build(signingKey), signingCert),
			new ASN1ObjectIdentifier(DEFAULT_TSA_POLICY_OID));
		timeStampTokenGen.setAccuracyMicros(ACCURACYMICROS);
		timeStampTokenGen.setAccuracyMillis(ACCURACYMILLIS);
		timeStampTokenGen.setAccuracySeconds(ACCURACYSECONDS);
		timeStampTokenGen.setOrdering(ORDERING);
		timeStampTokenGen.addCertificates(certs);
		timeStampResponseGen = new TimeStampResponseGenerator(timeStampTokenGen,
			getAcceptedAlgorithms(), getAcceptedPolicies(),
			getAcceptedExtensions())
	}	
	
	public ResponseVS processRequest(byte[] timeStampRequestBytes,
			Locale locale) throws Exception {	
		if(!timeStampRequestBytes) {
			String msg = messageSource.getMessage('timestampRequestNullMsg', null, locale)
			log.debug("processRequest - ${msg}"); 
			return new ResponseVS(message:msg,
				statusCode:ResponseVS.SC_ERROR_PETICION)
		}
		TimeStampRequest timeStampRequest = new TimeStampRequest(timeStampRequestBytes)
		final Date date = DateUtils.getTodayDate();
		//long numSerie = getSernoGenerator().incrementAndGet()
		//final BigInteger serialNumber = BigInteger.valueOf(numSerie);
		final BigInteger serialNumber = getSerno(noOctets)
		long numSerie = serialNumber.longValue()
		
		log.debug("processRequest - serialNumber: '${serialNumber}'" +
			" - CertReq: ${timeStampRequest.getCertReq()}");	
		
		final TimeStampToken token = null;
		synchronized(this) {
			final TimeStampResponse timeStampResponse = getTimeStampResponseGen().generate(
				timeStampRequest, serialNumber, date);
			token = timeStampResponse.getTimeStampToken();
			PKIFailureInfo failureInfo = timeStampResponse.getFailInfo();
			if (failureInfo != null) {
				log.debug("timeStampResponse Status: " + timeStampResponse.getStatus())
				log.error("timeStampResponse Failure info: ${failureInfo.intValue()}");
			}
		}
		
		//String timeStampRequestStr = new String(Base64.encode(timeStampRequestBytes));
		//log.debug("timeStampRequestStr: ${timeStampRequestStr}")
		//String digestStr = new String(Base64.encode(timeStampRequest.getMessageImprintDigest()));
		//log.debug("timeStampRequest MessageImprintDigest: ${digestStr}")
		//log.debug("timeStampRequest MessageImprintAlgOID: ${timeStampRequest.getMessageImprintAlgOID()}")
		
		if(!token) {
			log.error("processRequest - error:'${timeStampResponse.getStatusString()}'")
			String msg = messageSource.getMessage('timestampRequestNullMsg', null, locale)
			log.debug("processRequest - ${msg}"); 
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
				message:msg)
		}
		
		SignerInformationVerifier sigVerifier = getTimeStampSignerInfoVerifier()
		AtomicBoolean done = new AtomicBoolean(false);
		int numAttemp = 0;
		while(!done.get()) {
			log.debug(" ------ validating token");
			try {
				token.validate(sigVerifier)
				//validate(token, locale)
				done.set(true)
			} catch(Exception ex) {
				if(numAttemp < numMaxAttempts) {
					++numAttemp;
				} else {
					File errorFile = new File("${grailsApplication.config.VotingSystem.errorsBaseDir}/timeStampError_${System.currentTimeMillis()}")
					errorFile.setBytes(Base64.encode(token.getEncoded()))
					log.error(" ------ Exceeded max num attemps - ${ex.getMessage()}", ex);
					throw ex
				}
			}
		}
		//String tokenStr = new String(Base64.encode(token.getEncoded()));
		//log.debug("processRequest - tokenStr: '${tokenStr}'");
		new SelloTiempo(serialNumber:numSerie, tokenBytes:token.getEncoded(), 
			estado:SelloTiempo.Estado.OK,
			timeStampRequestBytes:timeStampRequestBytes).save()
		return new ResponseVS(statusCode:ResponseVS.SC_OK,
			messageBytes:token.getEncoded())
	}
			
	public BigInteger getSerno(int maxLength) {
		if (random == null) {
			try {
				random = SecureRandom.getInstance(algorithm);
			} catch (NoSuchAlgorithmException ex) {
				log.error(ex.getMessage(), ex)
			}
		}
		random.setSeed(new Date().getTime());
		final byte[] sernobytes = new byte[maxLength];
		random.nextBytes(sernobytes);
		BigInteger serno = new BigInteger(sernobytes).abs();
		return serno;
	}
		
	public ResponseVS validateToken(TimeStampToken  tsToken, Locale locale) {
		log.debug("validateToken")
		String msg = null
		try {
			SignerInformationVerifier sigVerifier = getTimeStampSignerInfoVerifier()
			X509CertificateHolder certHolder = sigVerifier.getAssociatedCertificate();
			DigestCalculator calc = sigVerifier.getDigestCalculator(tsToken.certID.getHashAlgorithm());
			OutputStream cOut = calc.getOutputStream();
			cOut.write(certHolder.getEncoded());
			cOut.close();
			if (!Arrays.equals(tsToken.certID.getCertHash(), calc.getDigest())) {
				msg = messageSource.getMessage('hashCertifcatesErrorMsg', null, locale)
				log.error("validate - ERROR - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION, message:msg)
			}
			if (tsToken.certID.getIssuerSerial() != null) {
				IssuerAndSerialNumber issuerSerial = certHolder.getIssuerAndSerialNumber();
				if (!tsToken.certID.getIssuerSerial().getSerial().equals(issuerSerial.getSerialNumber())) {
					msg = messageSource.getMessage('issuerSerialErrorMsg', null, locale)
					log.error("validate - ERROR - ${msg}")
					return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION, message:msg)
				}
			}
			TSPUtil.validateCertificate(certHolder);
			if (!certHolder.isValidOn(tsToken.tstInfo.getGenTime())) {
				msg = messageSource.getMessage('certificateDateError', null, locale)
				log.error("validate - ERROR - ${msg}");
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION, message:msg)
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
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION, message:msg)
			}
			return new ResponseVS(statusCode:ResponseVS.SC_OK)
		}catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			log.debug("validate - token issuer: ${tsToken?.getSID()?.getIssuer()}" +
				" - timeStampSignerInfoVerifier: ${timeStampSignerInfoVerifier?.associatedCertificate?.subject}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
				message:messageSource.getMessage('timeStampErrorMsg', null, locale))
		}
	}
	
	public ResponseVS validateToken(TimeStampToken timeStampToken, 
		Evento evento, Locale locale) throws Exception {
		log.debug("validateToken - event: ${evento.id}")
		String msg = null;
		ResponseVS respuesta
		try {
			if(!timeStampToken) {
				msg = messageSource.getMessage('timeStampNullErrorMsg', null, locale)
				log.error("ERROR - verifyToken - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_NULL_REQUEST, message:msg)
			}
			respuesta = validateToken(timeStampToken, locale)
			if(ResponseVS.SC_OK != respuesta.statusCode) return respuesta
			
			//timeStampToken.validate(getTimeStampSignerInfoVerifier()) -> Cert validation
			// errors problems with multiple requests
			Date timestampDate = timeStampToken.getTimeStampInfo().getGenTime()
			if(!timestampDate.after(evento.fechaInicio) ||
				!timestampDate.before(evento.fechaFin)) {
				msg = messageSource.getMessage('timestampDateErrorMsg',
					[timestampDate, evento.fechaInicio, evento.fechaFin].toArray(), locale)
				log.debug("validateToken - ERROR TIMESTAMP DATE - Event '${evento.id}' - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
					message:msg, eventVS:evento)
			} else return new ResponseVS(statusCode:ResponseVS.SC_OK);
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('timeStampErrorMsg', null, locale)
			log.error ("validateToken - msg:{msg} - Event '${evento.id}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION, message:msg)
		}
	}
	
	public SignerInformationVerifier getTimeStampSignerInfoVerifier(){
		if(!timeStampSignerInfoVerifier) afterPropertiesSet()
		return timeStampSignerInfoVerifier
	}
	
			
	public byte[] getTimeStampRequest(byte[] digest)
			throws TSPException, IOException, Exception  {
		log.debug("getTimeStampRequest")
		TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
		//reqgen.setReqPolicy(m_sPolicyOID);
		TimeStampRequest timeStampRequest = reqgen.generate(TSPAlgorithms.SHA256, digest);
		return timeStampRequest.getEncoded();
	}
		
	private Set<String> getAcceptedAlgorithms() {
		Set<String> acceptedAlgorithms = ACCEPTEDALGORITHMS?.collect {ACCEPTEDALGORITHMSMAP.get(it)}
		log.debug(" -- acceptedAlgorithms: " + acceptedAlgorithms?.toArray())
		return acceptedAlgorithms;
	}

	private AtomicLong getSernoGenerator() {
		if(!sernoGenerator) {
			sernoGenerator = new AtomicLong(SelloTiempo.count())
		}
		return sernoGenerator
	}

	private Set<String> getAcceptedPolicies() {
		Set<String> acceptedPolicies = ACCEPTEDPOLICIES?.collect {return it}
		log.debug("-- acceptedPolicies: " + acceptedPolicies?.toArray())
		return acceptedPolicies;
	}

	private Set<String> getAcceptedExtensions() {
		Set<String> acceptedExtensions = ACCEPTEDEXTENSIONS?.collect {return it}
		log.debug("-- getAcceptedExtensions: " + acceptedExtensions?.toArray())
		return acceptedExtensions;
	}
	
}
