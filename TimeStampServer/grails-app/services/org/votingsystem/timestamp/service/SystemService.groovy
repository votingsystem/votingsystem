package org.votingsystem.timestamp.service

import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.DERObject
import org.bouncycastle.asn1.cms.CMSAttributes
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.SignerInformationVerifier
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.operator.DigestCalculator
import org.bouncycastle.tsp.*
import org.bouncycastle.util.Store
import org.votingsystem.model.ContextVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.signature.util.SignatureData
import org.votingsystem.util.FileUtils
import org.votingsystem.util.MetaInfMsg
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import static org.springframework.context.i18n.LocaleContextHolder.getLocale

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class SystemService {

	def grailsApplication
	def messageSource

    private SignatureData signingData;
	private SignerInformationVerifier timeStampSignerInfoVerifier
	private byte[] signingCertPEMBytes
    private byte[] signingCertChainPEMBytes

	public Map init() {
		log.debug("init");
        try {
            File keyStoreFile = grailsApplication.mainContext.getResource(
                    grailsApplication.config.vs.keyStorePath).getFile()
            String keyAlias = grailsApplication.config.vs.signKeyAlias
            String password = grailsApplication.config.vs.signKeyPassword
            KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
                    FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
            PrivateKey signingKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
            X509Certificate signingCert = keyStore.getCertificate(keyAlias)
            signingCertPEMBytes = CertUtils.getPEMEncoded (signingCert)
            timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                    ContextVS.PROVIDER).build(signingCert);
            X509CertificateHolder certHolder = timeStampSignerInfoVerifier.getAssociatedCertificate();
            TSPUtil.validateCertificate(certHolder);
            Certificate[] chain = keyStore.getCertificateChain(keyAlias);
            signingCertChainPEMBytes = CertUtils.getPEMEncoded (Arrays.asList(chain))
            Store certs = new JcaCertStore(Arrays.asList(chain));
            signingData = new SignatureData(signingCert, signingKey, certs);
            return [signingCertPEMBytes: signingCertPEMBytes,
                    signingCertChainPEMBytes: signingCertChainPEMBytes,
                    timeStampSignerInfoVerifier:timeStampSignerInfoVerifier]
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
        }
	}


    public SignatureData getSigningData () {
        return signingData;
    }
	
	public byte[] getSigningCertPEMBytes() {
		if(!signingCertPEMBytes) signingCertPEMBytes = init()?.signingCertPEMBytes
		return signingCertPEMBytes
	}

    public byte[] getSigningCertChainPEMBytes() {
        if(!signingCertChainPEMBytes) signingCertChainPEMBytes = init()?.signingCertChainPEMBytes
        return signingCertChainPEMBytes
    }

    public void validateToken(TimeStampToken tsToken) throws ExceptionVS {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        if(tsToken == null) throw new ExceptionVS(messageSource.getMessage('documentWithoutTimeStampErrorMsg', null,
                locale), MetaInfMsg.getErrorMsg(methodName, 'timestampMissing'))
        SignerInformationVerifier sigVerifier = getTimeStampSignerInfoVerifier()
        X509CertificateHolder certHolder = sigVerifier.getAssociatedCertificate();
        DigestCalculator calc = sigVerifier.getDigestCalculator(tsToken.certID.getHashAlgorithm());
        OutputStream cOut = calc.getOutputStream();
        cOut.write(certHolder.getEncoded());
        cOut.close();
        if (!Arrays.equals(tsToken.certID.getCertHash(), calc.getDigest())) {
            throw new ExceptionVS(messageSource.getMessage('certHashErrorMsg', null, locale))
        }
        if (tsToken.certID.getIssuerSerial() != null) {
            IssuerAndSerialNumber issuerSerial = certHolder.getIssuerAndSerialNumber();
            if (!tsToken.certID.getIssuerSerial().getSerial().equals(issuerSerial.getSerialNumber())) {
                throw new ExceptionVS(messageSource.getMessage('issuerSerialErrorMsg', null, locale))
            }
        }
        if (!certHolder.isValidOn(tsToken.tstInfo.getGenTime())) {
            throw new ExceptionVS(messageSource.getMessage('certificateDateError', null, locale))
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

	
}
