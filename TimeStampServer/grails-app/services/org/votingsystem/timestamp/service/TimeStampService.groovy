package org.votingsystem.timestamp.service

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.DERObject
import org.bouncycastle.asn1.cmp.PKIFailureInfo
import org.bouncycastle.asn1.cms.CMSAttributes
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.SignerInformationVerifier
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.operator.DigestCalculator
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.bouncycastle.tsp.*
import org.bouncycastle.util.Store
import org.springframework.context.i18n.LocaleContextHolder
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.TimeStampVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.signature.util.TimeStampResponseGenerator
import org.votingsystem.signature.util.KeyGeneratorVS
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.MetaInfMsg

import javax.security.auth.x500.X500PrivateCredential
import java.security.*
import java.security.cert.Certificate
import java.security.cert.X509Certificate

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
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

	private org.votingsystem.signature.util.TimeStampResponseGenerator timeStampResponseGen;
	private SignerInformationVerifier timeStampSignerInfoVerifier
	private byte[] signingCertPEMBytes
    private byte[] signingCertChainPEMBytes
	private String tsaName

	private static List ACCEPTEDPOLICIES = ["1.2.3", "1.2.4"];
	private static List ACCEPTEDEXTENSIONS = [];
	private static List ACCEPTEDALGORITHMS = ["SHA1","SHA256", "SHA512"];

	private X509Certificate signingCert;
	
	private static HashMap<String, String> ACCEPTEDALGORITHMSMAP = [
			"SHA1":TSPAlgorithms.SHA1,
			"SHA224":TSPAlgorithms.SHA224,
			"SHA256":TSPAlgorithms.SHA256,
			"SHA384":TSPAlgorithms.SHA384,
			"SHA512":TSPAlgorithms.SHA512
		];

	private synchronized Map init() {
		log.debug("init");
        try {
            File keyStoreFile = grailsApplication.mainContext.getResource(
                    grailsApplication.config.VotingSystem.keyStorePath).getFile()
            String keyAlias = grailsApplication.config.VotingSystem.signKeysAlias
            String password = grailsApplication.config.VotingSystem.signKeysPassword
            KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
                    FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
            PrivateKey signingKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
            signingCert = keyStore.getCertificate(keyAlias)
            signingCertPEMBytes = CertUtils.getPEMEncoded (signingCert)
            timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                    ContextVS.PROVIDER).build(signingCert);
            X509CertificateHolder certHolder = timeStampSignerInfoVerifier.getAssociatedCertificate();
            TSPUtil.validateCertificate(certHolder);
            Certificate[] chain = keyStore.getCertificateChain(keyAlias);
            signingCertChainPEMBytes = CertUtils.getPEMEncoded (Arrays.asList(chain))
            Store certs = new JcaCertStore(Arrays.asList(chain));
            JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder().setProvider(ContextVS.PROVIDER).build());
            TimeStampTokenGenerator timeStampTokenGen = new TimeStampTokenGenerator(infoGeneratorBuilder.build(
                    new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(ContextVS.PROVIDER).build(signingKey),
                    signingCert), new ASN1ObjectIdentifier(DEFAULT_TSA_POLICY_OID));
            timeStampTokenGen.setAccuracyMicros(ACCURACYMICROS);
            timeStampTokenGen.setAccuracyMillis(ACCURACYMILLIS);
            timeStampTokenGen.setAccuracySeconds(ACCURACYSECONDS);
            timeStampTokenGen.setOrdering(ORDERING);
            timeStampTokenGen.addCertificates(certs);
            timeStampResponseGen = new TimeStampResponseGenerator(timeStampTokenGen, getAcceptedAlgorithms(),
                    getAcceptedPolicies(), getAcceptedExtensions())
            return [timeStampResponseGen:timeStampResponseGen, signingCertPEMBytes: signingCertPEMBytes,
                    signingCertChainPEMBytes: signingCertChainPEMBytes,
                    timeStampSignerInfoVerifier:timeStampSignerInfoVerifier]
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
        }
	}
	
	private TimeStampResponseGenerator getTimeStampResponseGen() {
		if(!timeStampResponseGen) timeStampResponseGen = init()?.timeStampResponseGen
		return timeStampResponseGen
	}
	
	public byte[] getSigningCertPEMBytes() {
		if(!signingCertPEMBytes) signingCertPEMBytes = init()?.signingCertPEMBytes
		return signingCertPEMBytes
	}

    public byte[] getSigningCertChainPEMBytes() {
        if(!signingCertChainPEMBytes) signingCertChainPEMBytes = init()?.signingCertChainPEMBytes
        return signingCertChainPEMBytes
    }
	
	/*Method to Tests*/
	void initTest() {
		log.debug("initTest");
        long CERT_VALID_FROM = System.currentTimeMillis();
        long ROOT_KEYSTORE_PERIOD = 20000000000L;
        long USER_KEYSTORE_PERIOD = 20000000000L;
        String ROOT_ALIAS = "rootAlias";
        String END_ENTITY_ALIAS = "endEntityAlias";
        String PASSWORD = "PemPass";
        String strSubjectDN = "O=Sistema de Votación, C=ES";

        KeyStore rootCAKeyStore = KeyStoreUtil.createRootKeyStore (CERT_VALID_FROM, ROOT_KEYSTORE_PERIOD,
                PASSWORD.toCharArray(), ROOT_ALIAS, strSubjectDN);
        X509Certificate rootCACert = (X509Certificate)rootCAKeyStore.getCertificate(ROOT_ALIAS);
        PrivateKey rootCAPrivateKey = (PrivateKey)rootCAKeyStore.getKey(ROOT_ALIAS,PASSWORD.toCharArray());
        X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(rootCACert, rootCAPrivateKey,  ROOT_ALIAS);

        KeyStore timeStampingKeyStore = KeyStoreUtil.createTimeStampingKeyStore(CERT_VALID_FROM, ROOT_KEYSTORE_PERIOD,
                PASSWORD.toCharArray(),  ROOT_ALIAS, rootCAPrivateCredential, "O=TimeSTamping Cert, C=ES")

        PrivateKey signingKey = (PrivateKey)timeStampingKeyStore.getKey(ROOT_ALIAS, PASSWORD.toCharArray());
        Certificate[] chain = timeStampingKeyStore.getCertificateChain(END_ENTITY_ALIAS);
        X509Certificate signingCert = (X509Certificate)chain[0]

		List certList = new ArrayList();
		certList.add(signingCert);
        certList.add(rootCACert);
		Store certs = new JcaCertStore(certList);
		JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(
			new JcaDigestCalculatorProviderBuilder().setProvider(ContextVS.PROVIDER).build());
		TimeStampTokenGenerator timeStampTokenGen = new TimeStampTokenGenerator(infoGeneratorBuilder.build(
			    new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(ContextVS.PROVIDER).build(signingKey),
                signingCert), new ASN1ObjectIdentifier(DEFAULT_TSA_POLICY_OID));
		timeStampTokenGen.setAccuracyMicros(ACCURACYMICROS);
		timeStampTokenGen.setAccuracyMillis(ACCURACYMILLIS);
		timeStampTokenGen.setAccuracySeconds(ACCURACYSECONDS);
		timeStampTokenGen.setOrdering(ORDERING);
		timeStampTokenGen.addCertificates(certs);
		timeStampResponseGen = new TimeStampResponseGenerator(timeStampTokenGen,
			getAcceptedAlgorithms(), getAcceptedPolicies(), getAcceptedExtensions())
	}	
	
	public ResponseVS processRequest(byte[] timeStampRequestBytes, Date date, Locale locale) throws Exception {
		if(!timeStampRequestBytes) {
			String msg = messageSource.getMessage('timestampRequestNullMsg', null, locale)
			log.debug("processRequest - ${msg}"); 
			return new ResponseVS(message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
		}
		TimeStampRequest timeStampRequest = new TimeStampRequest(timeStampRequestBytes)
		final BigInteger serialNumber = KeyGeneratorVS.INSTANCE.getSerno()
		log.debug("processRequest - serialNumber: '${serialNumber}' - CertReq: ${timeStampRequest.getCertReq()}");
		final TimeStampToken token = null;
		synchronized(this) {
			TimeStampResponse timeStampResponse =getTimeStampResponseGen().generate(timeStampRequest,serialNumber,date);
			token = timeStampResponse.getTimeStampToken();
			PKIFailureInfo failureInfo = timeStampResponse.getFailInfo();
			if (failureInfo != null) {
				log.error("timeStampResponse Status: " + timeStampResponse.getStatus())
				log.error("timeStampResponse Failure info: ${failureInfo.intValue()}");
                log.error("timeStampResponse error: ${timeStampResponse.getStatusString()}");
                return new ResponseVS(ResponseVS.SC_ERROR, messageSource.getMessage('timestampGenErrorMsg', null, locale))
			}
		}
        TimeStampVS timeStampVS = new TimeStampVS(serialNumber:serialNumber.longValue(), tokenBytes:token.getEncoded(),
                state:TimeStampVS.State.OK, timeStampRequestBytes:timeStampRequestBytes).save()
		return new ResponseVS(statusCode:ResponseVS.SC_OK, messageBytes:token.getEncoded(),
                contentType:ContentTypeVS.TIMESTAMP_RESPONSE)
	}

    public void validateToken(TimeStampToken tsToken) throws ExceptionVS {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        if(tsToken == null) throw new ExceptionVS(messageSource.getMessage('documentWithoutTimeStampErrorMsg', null,
                LocaleContextHolder.locale), MetaInfMsg.getErrorMsg(methodName, 'timestampMissing'))
        SignerInformationVerifier sigVerifier = getTimeStampSignerInfoVerifier()
        if(!sigVerifier)throw new ExceptionVS("TimeStamp service not initialized")
        X509CertificateHolder certHolder = sigVerifier.getAssociatedCertificate();
        DigestCalculator calc = sigVerifier.getDigestCalculator(tsToken.certID.getHashAlgorithm());
        OutputStream cOut = calc.getOutputStream();
        cOut.write(certHolder.getEncoded());
        cOut.close();
        if (!Arrays.equals(tsToken.certID.getCertHash(), calc.getDigest())) {
            throw new ExceptionVS(messageSource.getMessage('certHashErrorMsg', null, LocaleContextHolder.locale))
        }
        if (tsToken.certID.getIssuerSerial() != null) {
            IssuerAndSerialNumber issuerSerial = certHolder.getIssuerAndSerialNumber();
            if (!tsToken.certID.getIssuerSerial().getSerial().equals(issuerSerial.getSerialNumber())) {
                throw new ExceptionVS(messageSource.getMessage('issuerSerialErrorMsg', null, LocaleContextHolder.locale))
            }
        }
        if (!certHolder.isValidOn(tsToken.tstInfo.getGenTime())) {
            throw new ExceptionVS(messageSource.getMessage('certificateDateError', null, LocaleContextHolder.locale))
        }
        CMSSignedData tokenCMSSignedData = tsToken.tsToken
        Collection signers = tokenCMSSignedData.getSignerInfos().getSigners();
        SignerInformation tsaSignerInfo = (SignerInformation)signers.iterator().next();

        DERObject validMessageDigest = tsaSignerInfo.getSingleValuedSignedAttribute(
                CMSAttributes.messageDigest, "message-digest");
        ASN1OctetString signedMessageDigest = (ASN1OctetString)validMessageDigest
        byte[] digestToken = signedMessageDigest.getOctets();

        String algorithmStr = TSPUtil.getDigestAlgName(
                tsToken.tsaSignerInfo.getDigestAlgorithmID().getAlgorithm().toString())

        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        tsToken.tsaSignerInfo.content.write(baos);
        byte[] contentBytes = baos.toByteArray()
        MessageDigest sha = MessageDigest.getInstance(algorithmStr);
        byte[] resultDigest =  sha.digest(contentBytes);
        baos.close();
        if(!Arrays.equals(digestToken, resultDigest)) {
            String tokenStr = Base64.getEncoder().encodeToString(tsToken.getEncoded());
            String resultDigestStr = Base64.getEncoder().encodeToString(resultDigest);
            String digestTokenStr = Base64.getEncoder().encodeToString(digestToken);
            throw new ExceptionVS("algorithmStr: '${algorithmStr} 'resultDigestStr '${resultDigestStr} - digestTokenStr '${digestTokenStr}'")
        }
    }
	
	public SignerInformationVerifier getTimeStampSignerInfoVerifier(){
		if(!timeStampSignerInfoVerifier) timeStampSignerInfoVerifier = init()?.timeStampSignerInfoVerifier
		return timeStampSignerInfoVerifier
	}
			
	public byte[] getTimeStampRequest(byte[] digest) throws TSPException, IOException, Exception  {
		log.debug("getTimeStampRequest")
		TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
		//reqgen.setReqPolicy(m_sPolicyOID);
		TimeStampRequest timeStampRequest = reqgen.generate(TSPAlgorithms.SHA256, digest);
		return timeStampRequest.getEncoded();
	}
		
	private Set<String> getAcceptedAlgorithms() {
		Set<String> acceptedAlgorithms = ACCEPTEDALGORITHMS?.collect {ACCEPTEDALGORITHMSMAP.get(it)}
		log.debug("getAcceptedAlgorithms: " + acceptedAlgorithms?.toArray())
		return acceptedAlgorithms;
	}

	private Set<String> getAcceptedPolicies() {
		Set<String> acceptedPolicies = ACCEPTEDPOLICIES?.collect {return it}
		log.debug("getAcceptedPolicies: " + acceptedPolicies?.toArray())
		return acceptedPolicies;
	}

	private Set<String> getAcceptedExtensions() {
		Set<String> acceptedExtensions = ACCEPTEDEXTENSIONS?.collect {return it}
		log.debug("getAcceptedExtensions: " + acceptedExtensions?.toArray())
		return acceptedExtensions;
	}
	
}
