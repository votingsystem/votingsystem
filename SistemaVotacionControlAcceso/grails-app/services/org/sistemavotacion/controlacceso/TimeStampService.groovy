package org.sistemavotacion.controlacceso


import org.sistemavotacion.controlacceso.modelo.*
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONElement
import org.bouncycastle.util.encoders.Base64;

import com.itextpdf.text.pdf.PdfReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.*
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import org.bouncycastle.asn1.*;
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

	private SernoGenerator sernoGenerator
	private org.sistemavotacion.seguridad.TimeStampResponseGenerator timeStampResponseGen;

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
		sernoGenerator = SernoGenerator.instance(messageSource);
		def rutaAlmacenClaves = getAbsolutePath("${grailsApplication.config.SistemaVotacion.rutaAlmacenClaves}")
		File keyStoreFile = new File(rutaAlmacenClaves);
		String aliasClaves = grailsApplication.config.SistemaVotacion.aliasClavesFirma
		String password = grailsApplication.config.SistemaVotacion.passwordClavesFirma
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
			FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
		PrivateKey signingKey = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray());
		X509Certificate signingCert = keyStore.getCertificate(aliasClaves)
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
			
	public String getAbsolutePath(String filePath){
		String prefijo = "${grailsApplication.mainContext.getResource('.')?.getFile()}"
		String sufijo =filePath.startsWith(File.separator)? filePath : File.separator + filePath;
		return "${prefijo}${sufijo}";
	}
	
	/*Method to Tests*/
	void inicializarTest() {
		log.debug("TimeStampService - inicializarTest");
		sernoGenerator = SernoGenerator.instance(messageSource);
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
		log.debug("processRequest")
		if(!timeStampRequestBytes) return
		try {
			String timeStampRequestStr = new String(Base64.encode(timeStampRequestBytes));
			TimeStampRequest timeStampRequest = new TimeStampRequest(timeStampRequestBytes)
			final Date date = DateUtils.getTodayDate();
			final BigInteger serialNumber = sernoGenerator.getSerialNumber();
			log.debug("processRequest - serialNumber: '${serialNumber.longValue()}'" );
			final TimeStampResponse timeStampResponse = timeStampResponseGen.generate(
					timeStampRequest, serialNumber, date);
			//timeStampResponse.validate(timeStampRequest)
			final TimeStampToken token = timeStampResponse.getTimeStampToken();
			//String tokenStr = new String(Base64.encode(token.getEncoded()));
			//log.debug("processRequest - tokenStr: '${tokenStr}'");
			if(token) {
				new SelloTiempo(serialNumber:serialNumber.longValue(),
					tokenBytes:token.getEncoded(), estado:SelloTiempo.Estado.OK,
					timeStampRequestBytes:timeStampRequestBytes).save()
				return new Respuesta(codigoEstado:Respuesta.SC_OK,
					timeStampToken:token.getEncoded())
			} else {
				new SelloTiempo(serialNumber:serialNumber?.longValue(),
					reason:timeStampResponse?.getStatusString(),
					estado:SelloTiempo.Estado.ERRORES,
					timeStampRequestBytes:timeStampRequestBytes).save()
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:messageSource.getMessage('error.timeStampGeneration', null, locale))
			}
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje:messageSource.getMessage('error.timeStampGeneration', null, locale))
		}
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
	
	private static class MyChecker
	extends PKIXCertPathChecker
 {
	 private static int count;

	 public void init(boolean forward)
	 throws CertPathValidatorException
	 {
		 //To change body of implemented methods use File | Settings | File Templates.
	 }

	 public boolean isForwardCheckingSupported()
	 {
		 return true;
	 }

	 public Set getSupportedExtensions()
	 {
		 return null;
	 }

	 public void check(Certificate cert, Collection unresolvedCritExts)
	 throws CertPathValidatorException
	 {
		 count++;
	 }

	 public int getCount()
	 {
		return count;
	 }
 }
}
