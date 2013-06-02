package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONElement
import org.bouncycastle.util.encoders.Base64;
import com.itextpdf.text.pdf.PdfReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
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
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.bouncycastle.tsp.*;
import org.sistemavotacion.seguridad.TimeStampResponseGenerator
import static org.bouncycastle.tsp.TSPAlgorithms.*

import org.bouncycastle.util.Store;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.seguridad.*
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.sistemavotacion.util.*;
import org.springframework.beans.factory.InitializingBean

//class TimeStampService implements InitializingBean {
class TimeStampService {
	
	def grailsApplication
	def messageSource
	
	private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
	private static final String DEFAULT_TSA_POLICY_OID = "1.2.3";
	private static final Integer ACCURACYMICROS = 500;
	private static final Integer ACCURACYMILLIS = 500;
	private static final Integer ACCURACYSECONDS = 1;
	
	//# Optional. Specify if requests are ordered. Only false is supported.
	private boolean ORDERING = false;
	private static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

	private AtomicLong sernoGenerator
	private org.sistemavotacion.seguridad.TimeStampResponseGenerator timeStampResponseGen;
	private SignerInformationVerifier timeStampSignerInfoVerifier
	private byte[] signingCertBytes

	private static List ACCEPTEDPOLICIES = ["1.2.3", "1.2.4"];
	private static List ACCEPTEDEXTENSIONS = [];
	private static List ACCEPTEDALGORITHMS = ["SHA1","SHA256", "SHA512"];
		
	private static HashMap<String, String> ACCEPTEDALGORITHMSMAP = [
			"GOST3411":TSPAlgorithms.GOST3411,
			"MD5":TSPAlgorithms.MD5,
			"SHA1":TSPAlgorithms.SHA1,
			"SHA224":TSPAlgorithms.SHA224,
			"SHA256":TSPAlgorithms.SHA256,
			"SHA384":TSPAlgorithms.SHA384,
			"SHA512":TSPAlgorithms.SHA512,
			"RIPEMD128":TSPAlgorithms.RIPEMD128,
			"RIPEMD160":TSPAlgorithms.RIPEMD160,
			"RIPEMD256":TSPAlgorithms.RIPEMD256
		];
	
	//@Override
	public void afterPropertiesSet() throws Exception {
		log.debug(" - afterPropertiesSet - afterPropertiesSet - afterPropertiesSet");
		sernoGenerator = new AtomicLong(SelloTiempo.count())
		def rutaAlmacenClaves = getAbsolutePath("${grailsApplication.config.SistemaVotacion.rutaAlmacenClaves}")
		File keyStoreFile = new File(rutaAlmacenClaves);
		String aliasClaves = grailsApplication.config.SistemaVotacion.aliasClavesFirma
		String password = grailsApplication.config.SistemaVotacion.passwordClavesFirma
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
			FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
		PrivateKey signingKey = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray());
		X509Certificate signingCert = keyStore.getCertificate(aliasClaves)
		
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
			
	public String getAbsolutePath(String filePath){
		String prefijo = "${grailsApplication.mainContext.getResource('.')?.getFile()}"
		String sufijo =filePath.startsWith(File.separator)? filePath : File.separator + filePath;
		return "${prefijo}${sufijo}";
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
	
	public Respuesta processRequest(byte[] timeStampRequestBytes,
			Locale locale) throws Exception {
		if(!timeStampRequestBytes) {
			String msg = messageSource.getMessage('timestampRequestNullMsg', null, locale)
			log.debug("processRequest - ${msg}"); 
			return new Respuesta(mensaje:msg,
				codigoEstado:Respuesta.SC_ERROR_PETICION)
		}
		//String timeStampRequestStr = new String(Base64.encode(timeStampRequestBytes));
		//log.debug("processRequest - timeStampRequestStr: ${timeStampRequestStr}")
		
		
		TimeStampRequest timeStampRequest = new TimeStampRequest(timeStampRequestBytes)
		final Date date = DateUtils.getTodayDate();
		long numSerie = getSernoGenerator().incrementAndGet()
		final BigInteger serialNumber = BigInteger.valueOf(numSerie);
		log.debug("processRequest - serialNumber: '${numSerie}'" +
			" - CertReq: ${timeStampRequest.getCertReq()}");	
		final TimeStampResponse timeStampResponse = getTimeStampResponseGen().generate(
				timeStampRequest, serialNumber, date);
		final TimeStampToken token = timeStampResponse.getTimeStampToken();
		
		if(!token) {
			String msg = messageSource.getMessage('timestampRequestNullMsg', null, locale)
			log.debug("processRequest - ${msg}"); 
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje:msg)
		}
		
		SignerInformationVerifier sigVerifier = getTimeStampSignerInfoVerifier()
		token.validate(sigVerifier)
		//validate(token, locale)
		
		//String tokenStr = new String(Base64.encode(token.getEncoded()));
		//log.debug("processRequest - tokenStr: '${tokenStr}'");
		new SelloTiempo(serialNumber:numSerie, tokenBytes:token.getEncoded(), 
			estado:SelloTiempo.Estado.OK,
			timeStampRequestBytes:timeStampRequestBytes).save()
		return new Respuesta(codigoEstado:Respuesta.SC_OK,
			timeStampToken:token.getEncoded())
	}
		
	public Respuesta validateToken(TimeStampToken  tsToken, Locale locale) {
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
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
			}
			if (tsToken.certID.getIssuerSerial() != null) {
				IssuerAndSerialNumber issuerSerial = certHolder.getIssuerAndSerialNumber();
				if (!tsToken.certID.getIssuerSerial().getSerial().equals(issuerSerial.getSerialNumber())) {
					msg = messageSource.getMessage('issuerSerialErrorMsg', null, locale)
					log.error("validate - ERROR - ${msg}")
					return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
				}
			}
			TSPUtil.validateCertificate(certHolder);
			if (!certHolder.isValidOn(tsToken.tstInfo.getGenTime())) {
				msg = messageSource.getMessage('certificateDateError', null, locale)
				log.error("validate - ERROR - ${msg}");
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
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
				//String tokenStr = new String(Base64.encode(tsToken.getEncoded()));
				String resultDigestStr = new String(Base64.encode(resultDigest));
				String digestTokenStr = new String(Base64.encode(digestToken));
				msg = "resultDigestStr '${resultDigestStr} - digestTokenStr '${digestTokenStr}'"
				log.error("validate - ERROR HASH - ${msg}");
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
			}
			return new Respuesta(codigoEstado:Respuesta.SC_OK)
		}catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			log.debug("validate - token issuer: ${tsToken?.getSID()?.getIssuer()}" +
				" - timeStampSignerInfoVerifier: ${timeStampSignerInfoVerifier?.associatedCertificate?.subject}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje:messageSource.getMessage('timeStampErrorMsg', null, locale))
		}
	}
	
	public Respuesta validateToken(TimeStampToken timeStampToken, 
		Evento evento, Locale locale) throws Exception {
		log.debug("validateToken - event: ${evento.id}")
		String msg = null;
		Respuesta respuesta
		try {
			if(!timeStampToken) {
				msg = messageSource.getMessage('timeStampNullErrorMsg', null, locale)
				log.error("ERROR - verifyToken - ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_NULL_REQUEST, mensaje:msg)
			}
			timeStampToken.validate(getTimeStampSignerInfoVerifier())
			Date timestampDate = timeStampToken.getTimeStampInfo().getGenTime()
			if(!timestampDate.after(evento.fechaInicio) &&
				!timestampDate.before(evento.fechaFin)) {
				String dateRangeStr = "[${evento.fechaInicio} - ${evento.fechaFin}]"
				msg = messageSource.getMessage('timestampDateErrorMsg',
					[timestampDate, dateRangeStr].toArray(), locale)
				log.debug("validateToken - ERROR TIMESTAMP DATE - Event '${evento.id}' - ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:msg, evento:evento)
			} else return new Respuesta(codigoEstado:Respuesta.SC_OK);
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('timeStampErrorMsg', null, locale)
			log.error ("validateToken - msg:{msg} - Event '${evento.id}'")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
		}
	}
			
	public Respuesta validate1(TimeStampToken  tsToken, Locale locale) {
		log.debug("validate")
		try {
			tsToken.validate(getTimeStampSignerInfoVerifier())
			return new Respuesta(codigoEstado:Respuesta.SC_OK)
		}catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			log.debug("validate - token issuer: ${tsToken?.getSID()?.getIssuer()}" + 
				" - timeStampSignerInfoVerifier: ${timeStampSignerInfoVerifier?.associatedCertificate?.subject}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:messageSource.getMessage('timeStampErrorMsg', null, locale))
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
		if(!sernoGenerator) afterPropertiesSet()
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
