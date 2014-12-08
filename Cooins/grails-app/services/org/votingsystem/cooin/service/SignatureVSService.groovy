package org.votingsystem.cooin.service

import grails.converters.JSON
import grails.transaction.Transactional
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.jce.PKCS10CertificationRequest
import org.springframework.dao.DataAccessException
import org.votingsystem.callable.MessageTimeStamper
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.smime.SMIMESignedGeneratorVS
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.cooin.model.MessageVS
import javax.mail.Header
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.CertPathValidatorException
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import static org.springframework.context.i18n.LocaleContextHolder.getLocale

@Transactional
class  SignatureVSService {

    //static transactional = false
	
	private SMIMESignedGeneratorVS signedMailGenerator;
    private static Set<TrustAnchor> trustAnchors;
    private static Set<TrustAnchor> cooinAnchors;
    private Set<X509Certificate> trustedCerts
	private static HashMap<Long, CertificateVS> trustedCertsHashMap
	private X509Certificate localServerCertSigner;
    private PrivateKey serverPrivateKey;
    private CertificateVS serverCertificateVS
    private Encryptor encryptor;

	def grailsApplication;
	def messageSource
	def subscriptionVSService
	def timeStampService

    @Transactional
	public synchronized Map init() throws Exception {
		log.debug("init")
		File keyStoreFile = grailsApplication.mainContext.getResource(
			grailsApplication.config.vs.keyStorePath).getFile()
		String keyAlias = grailsApplication.config.vs.signKeyAlias
		String password = grailsApplication.config.vs.signKeyPassword
		signedMailGenerator = new SMIMESignedGeneratorVS(FileUtils.getBytesFromFile(keyStoreFile),
			keyAlias, password.toCharArray(), ContextVS.SIGN_MECHANISM);
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
		java.security.cert.Certificate[] chain = keyStore.getCertificateChain(keyAlias);
		byte[] pemCertsArray
        localServerCertSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        cooinAnchors = new HashSet<TrustAnchor>();
        cooinAnchors.add(new TrustAnchor(localServerCertSigner, null));
		for (int i = 0; i < chain.length; i++) {
            checkAuthorityCertDB(chain[i])
			if(!pemCertsArray) pemCertsArray = CertUtils.getPEMEncoded (chain[i])
			else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtils.getPEMEncoded (chain[i]))
		}
        serverCertificateVS = CertificateVS.findBySerialNumber(localServerCertSigner.getSerialNumber().longValue())
        serverPrivateKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray())
		File certChainFile = grailsApplication.mainContext.getResource(
                grailsApplication.config.vs.certChainPath).getFile();
		certChainFile.createNewFile()
		certChainFile.setBytes(pemCertsArray)
        encryptor = new Encryptor(localServerCertSigner, serverPrivateKey);
		initCertAuthorities();
        return [signedMailGenerator:signedMailGenerator, encryptor:encryptor, trustedCertsHashMap:trustedCertsHashMap,
                serverCertificateVS:serverCertificateVS, localServerCertSigner:localServerCertSigner,
                serverPrivateKey:serverPrivateKey];
	}

    @Transactional
    private CertificateVS checkAuthorityCertDB(X509Certificate x509AuthorityCert) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        String certData = "${x509AuthorityCert?.getSubjectDN()} - numSerie:${x509AuthorityCert?.getSerialNumber()?.longValue()}"
        CertificateVS certificateVS = CertificateVS.findWhere(serialNumber:x509AuthorityCert.getSerialNumber().longValue(),
                type:CertificateVS.Type.CERTIFICATE_AUTHORITY)
        if(!certificateVS) {
            certificateVS = new CertificateVS(isRoot:CertUtils.isSelfSigned(x509AuthorityCert),
                    type:CertificateVS.Type.CERTIFICATE_AUTHORITY, state:CertificateVS.State.OK,
                    content:x509AuthorityCert.getEncoded(),
                    serialNumber:x509AuthorityCert.getSerialNumber().longValue(),
                    validFrom:x509AuthorityCert.getNotBefore(),
                    validTo:x509AuthorityCert.getNotAfter()).save()
            log.debug "$methodName - ADDED NEW FILE SYSTEM CA CERT - $certData - certificateVS.id:'${certificateVS?.id}'"
        } else if (CertificateVS.State.OK != certificateVS.state) {
            throw new ExceptionVS("File system athority cert '${x509AuthorityCert?.getSubjectDN()}' " +
                    " - certificateVS.id:'${certificateVS?.id}' state: '${certificateVS.state}'")
        } else if(certificateVS.type != CertificateVS.Type.CERTIFICATE_AUTHORITY) {
            String msg = "Updated from type '${certificateVS.type}'  to type 'CERTIFICATE_AUTHORITY'"
            certificateVS.description = "${certificateVS.description} #### $msg"
            certificateVS.type = CertificateVS.Type.CERTIFICATE_AUTHORITY
            certificateVS.save()
            log.debug "$methodName - $certData - $msg"
        }
        return certificateVS
    }

    public Set<TrustAnchor> getCooinAnchors() {
        return cooinAnchors;
    }

    public X509Certificate getServerCert() {
        if(localServerCertSigner == null) localServerCertSigner = init().localServerCertSigner
        return localServerCertSigner
    }

    public CertificateVS getServerCertificateVS() {
        if(!serverCertificateVS) serverCertificateVS = init().serverCertificateVS
        return serverCertificateVS
    }

    private PrivateKey getServerPrivateKey() {
        if(serverPrivateKey == null) serverPrivateKey = init().serverPrivateKey
        return serverPrivateKey
    }

    private HashMap<Long, CertificateVS> getTrustedCertsHashMap() {
        if(!trustedCertsHashMap) initCertAuthorities().trustedCertsHashMap
        return trustedCertsHashMap
    }

	public boolean isSystemSignedMessage(Set<UserVS> signers) {
        for(UserVS userVS: signers) {
            if(userVS.getCertificate().equals(localServerCertSigner)) return true
        }
		return false
	}

    /**
     * Generate V3 Certificate from CSR
     */
    public X509Certificate signCSR(PKCS10CertificationRequest csr, String organizationalUnit, Date dateBegin,
           Date dateFinish, DERTaggedObject... certExtensions) throws Exception {
        X509Certificate issuedCert = CertUtils.signCSR(csr, organizationalUnit, getServerPrivateKey(),
                getServerCert(), dateBegin, dateFinish, certExtensions)
        return issuedCert
    }

    @Transactional
	public Map initCertAuthorities() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        File directory = grailsApplication.mainContext.getResource(
                grailsApplication.config.vs.certAuthoritiesDirPath).getFile()

        File[] acFiles = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String fileName) {
                return fileName.startsWith("AC_") && fileName.endsWith(".pem");
            }
        });
        for(File caFile:acFiles) {
            X509Certificate fileSystemX509TrustedCert = CertUtils.fromPEMToX509Cert(FileUtils.getBytesFromFile(caFile))
            checkAuthorityCertDB(fileSystemX509TrustedCert)
        }
        List<CertificateVS>  trustedCertsList = CertificateVS.createCriteria().list(offset: 0) {
            eq("state", CertificateVS.State.OK)
            eq("type",	CertificateVS.Type.CERTIFICATE_AUTHORITY)
        }
        trustedCertsHashMap = new HashMap<Long, CertificateVS>();
        trustedCerts = new HashSet<X509Certificate>();
        trustAnchors = new HashSet<TrustAnchor>();
        for (CertificateVS certificateVS : trustedCertsList) {
            X509Certificate x509Cert = certificateVS.getX509Cert()
            trustedCerts.add(x509Cert)
            trustedCertsHashMap.put(x509Cert.getSerialNumber()?.longValue(), certificateVS)
            trustAnchors.add(new TrustAnchor(x509Cert, null));
            String certData = "${x509Cert?.getSubjectDN()} - numSerie:${x509Cert?.getSerialNumber()?.longValue()}"
            log.debug "$methodName - certificateVS.id: '${certificateVS?.id}' - $certData"
        }
        log.debug("$methodName - loaded '${trustedCertsHashMap?.keySet().size()}' authorities")
        return [trustedCertsHashMap: trustedCertsHashMap, trustedCerts:trustedCerts, trustAnchors:trustAnchors]
	}

    public Set<TrustAnchor> getTrustAnchors() {
        if(!trustAnchors) trustAnchors = initCertAuthorities().trustAnchors
        return trustAnchors;
    }

    private Set<X509Certificate> getTrustedCerts() {
        if(!trustedCerts)  trustedCerts = initCertAuthorities().trustedCerts
        return trustedCerts;
    }

	public SMIMEMessage getSMIME (String fromUser,String toUser,String textToSign,String subject, Header header) {
		log.debug "getSMIME - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
		if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		return getSignedMailGenerator().getSMIME(fromUser, toUser, textToSign, subject, header);
	}

    public ResponseVS getSMIMETimeStamped (String fromUser,String toUser,String textToSign,String subject,
            Header... headers) {
        log.debug "getSMIMETimeStamped - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
        if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        SMIMEMessage smimeMessage = getSignedMailGenerator().getSMIME(
                fromUser, toUser, textToSign, subject, headers)
        MessageTimeStamper timeStamper = new MessageTimeStamper(
                smimeMessage, "${grailsApplication.config.vs.urlTimeStampServer}/timeStamp")
        ResponseVS responseVS = timeStamper.call();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        responseVS = new ResponseVS(ResponseVS.SC_OK)
        responseVS.setSMIME(timeStamper.getSMIME())
        return responseVS;
    }
		
	public synchronized SMIMEMessage getSMIMEMultiSigned (
		String fromUser, String toUser,	final SMIMEMessage smimeMessage, String subject) {
		log.debug("getSMIMEMultiSigned - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'");
		return getSignedMailGenerator().getSMIMEMultiSigned(fromUser, toUser, smimeMessage, subject);
	}

    @Transactional
	public ResponseVS validateSMIME(SMIMEMessage smimeMessage) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:smimeMessage.getContentDigestStr())
		if(messageSMIME) {
			String message = messageSource.getMessage('smimeDigestRepeatedErrorMsg', 
				[smimeMessage.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIME - ${message} - messageSMIME.id: ${messageSMIME.id}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:message,
                    metaInf: MetaInfMsg.getErrorMsg(methodName, "hashRepeated"))
		}
		return validateSignersCerts(smimeMessage)
	}

    public CertUtils.CertValidatorResultVS verifyUserCertificate(UserVS userVS) throws Exception {
        CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                getTrustAnchors(), false, [userVS.getCertificate()])
        X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
        userVS.setCertificateCA(getTrustedCertsHashMap().get(certCaResult?.getSerialNumber()?.longValue()))
        log.debug("verifyCertificate - user '${userVS.nif}' cert issuer: " + certCaResult?.getSubjectDN()?.toString() +
                " - CA certificateVS.id : " + userVS.getCertificateCA().getId());
        return validatorResult
    }

    public ResponseVS validateCertificates(List<X509Certificate> certificateList) {
        log.debug("validateCertificates")
        ResponseVS validationResponse = CertUtils.verifyCertificate(getTrustAnchors(), false, certificateList)
        //X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
        return validationResponse
    }

	public ResponseVS validateSignersCerts(SMIMEMessage smimeMessage) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		Set<UserVS> signersVS = smimeMessage.getSigners();
		if(signersVS.isEmpty()) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
			messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale))
		Set<UserVS> checkedSigners = new HashSet<UserVS>()
        UserVS checkedSigner = null
        UserVS anonymousSigner = null
        CertUtils.CertValidatorResultVS validatorResult
        String signerNIF = org.votingsystem.util.NifUtils.validate(smimeMessage.getSigner().getNif())
		for(UserVS userVS: signersVS) {
			try {
                timeStampService.validateToken(userVS.getTimeStampToken())
                validatorResult = verifyUserCertificate(userVS)
                ResponseVS responseVS = null
                if(validatorResult.getChecker().isAnonymousSigner()) {
                    log.debug("validateSignersCerts - is anonymous signer")
                    anonymousSigner = userVS
                    responseVS = new ResponseVS(ResponseVS.SC_OK).setUserVS(anonymousSigner)
                } else {
                    responseVS = subscriptionVSService.checkUser(userVS)
                    if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
                    if(responseVS.userVS.nif.equals(signerNIF)) checkedSigner = responseVS.userVS;
                }
                checkedSigners.add(responseVS.userVS)
			} catch (CertPathValidatorException ex) {
				log.error(ex.getMessage(), ex)
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                        metaInf: MetaInfMsg.getExceptionMsg(methodName, ex),
                        message: messageSource.getMessage('unknownCAErrorMsg', null, locale))
			} catch (DataAccessException ex) {
                log.error(ex.getMessage(), ex)
                return new ResponseVS(message:messageSource.getMessage('paramsErrorMsg', null, locale),
                        statusCode:ResponseVS.SC_ERROR, metaInf: MetaInfMsg.getExceptionMsg(methodName, ex))
            } catch (Exception ex) {
				log.error(ex.getMessage(), ex)
				return new ResponseVS(message:ex.getMessage(), statusCode:ResponseVS.SC_ERROR,
                        metaInf: MetaInfMsg.getExceptionMsg(methodName, ex))
			}
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, smimeMessage:smimeMessage,
                data:[checkedSigners:checkedSigners, checkedSigner:checkedSigner, anonymousSigner:anonymousSigner,
                extensionChecker:validatorResult.getChecker()])
	}

    @Transactional
    private ResponseVS processMessageVS(byte[] messageVSBytes, ContentTypeVS contenType) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        JSONObject messageVSJSON = (JSONObject) JSONSerializer.toJSON(new String(messageVSBytes, "UTF-8"));

        SMIMEMessage smimeSender = new SMIMEMessage(new ByteArrayInputStream(
                Base64.getDecoder().decode(messageVSJSON.smimeMessage.getBytes())))
        ResponseVS responseVS = processSMIMERequest(smimeSender, contenType)
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
        MessageSMIME messageSMIMEReq = responseVS.messageSMIME
        UserVS fromUser = messageSMIMEReq.getUserVS()
        def messageJSON = JSON.parse(messageSMIMEReq.getSMIME()?.getSignedContent())
        String msg = null
        String toUserNIFValidated = org.votingsystem.util.NifUtils.validate(messageVSJSON.toUserNIF)
        if(!fromUser || ! toUserNIFValidated || !messageVSJSON.encryptedDataList || !messageVSJSON.encryptedDataInfo) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName} - ${msg}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR,
                    message:msg, metaInf: MetaInfMsg.getErrorMsg(methodName, "params"))
        }
        UserVS toUser = UserVS.findWhere(nif:toUserNIFValidated)
        messageVSJSON.encryptedDataList.each { dataMap ->
            def dataMapInfo = messageVSJSON.encryptedDataInfo.find { it ->
                it.serialNumber == dataMap.serialNumber
            }
            String encryptedMessageHash = CMSUtils.getHashBase64(dataMap.encryptedData, ContextVS.VOTING_DATA_DIGEST);
            if(!encryptedMessageHash.equals(dataMapInfo.encryptedMessageHashBase64))
                    msg = messageSource.getMessage("messageVSHashErrorMsg",
                    [dataMapInfo.encryptedMessageHashBase64, encryptedMessageHash].toArray(), locale)
        }
        if(msg != null) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR,
                message:msg, metaInf: MetaInfMsg.getErrorMsg(methodName, "params"))

        MessageVS messageVS = new MessageVS(content: messageVSBytes, fromUserVS: fromUser, toUserVS: toUser,
                senderMessageSMIME:messageSMIMEReq, type:TypeVS.MESSAGEVS, state: MessageVS.State.PENDING)

        if (!messageVS.save()) {messageVS.errors.each { log.error("messageVS - error - ${it}")}}

        log.debug("OK - MessageVS from user '${fromUser?.id}' to user '${toUser?.id}'")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, data:[messageVS:messageVS, messageSMIMEReq:messageSMIMEReq])
    }

    @Transactional
    private ResponseVS processSMIMERequest(SMIMEMessage smimeMessageReq, ContentTypeVS contenType) {
        if (smimeMessageReq?.isValidSignature()) {
            log.debug "processSMIMERequest - isValidSignature"
            ResponseVS certValidationResponse = null;
            switch(contenType) {
                //case ContentTypeVS.COOIN: break;
                default:
                    certValidationResponse = validateSMIME(smimeMessageReq);
            }
            TypeVS typeVS = TypeVS.OK;
            if(contenType && ContentTypeVS.COOIN == contenType) typeVS = TypeVS.COOIN;
            MessageSMIME messageSMIME
            if(ResponseVS.SC_OK != certValidationResponse.statusCode) {
                messageSMIME = new MessageSMIME(reason:certValidationResponse.message, type:TypeVS.SIGNATURE_ERROR,
                        metaInf:certValidationResponse.metaInf ,content:smimeMessageReq.getBytes()).save()
                log.error "*** Filter - processSMIMERequest - failed - status: ${certValidationResponse.statusCode}" +
                        " - message: ${certValidationResponse.message}"
                return certValidationResponse
            } else {
                messageSMIME = new MessageSMIME(signers:certValidationResponse.data?.checkedSigners,
                        anonymousSigner:certValidationResponse.data?.anonymousSigner,
                        userVS:certValidationResponse.data?.checkedSigner, smimeMessage:smimeMessageReq,
                        eventVS:certValidationResponse.eventVS, type:typeVS,
                        content:smimeMessageReq.getBytes(), base64ContentDigest:smimeMessageReq.getContentDigestStr()).save()
            }
            return new ResponseVS(statusCode:ResponseVS.SC_OK, messageSMIME: messageSMIME)
        } else if(smimeMessageReq) {
            log.error "**** Filter - processSMIMERequest - signature ERROR - "
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:messageSource.getMessage('signatureErrorMsg', null, locale))
        }
    }

    public byte[] encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert) throws Exception {
        return getEncryptor().encryptToCMS(dataToEncrypt, receiverCert);
    }

    public byte[] encryptToCMS(byte[] dataToEncrypt, PublicKey  receptorPublicKey) throws Exception {
        return getEncryptor().encryptToCMS(dataToEncrypt, receptorPublicKey);
    }

    public byte[] decryptCMS (byte[] encryptedFile) {
        return getEncryptor().decryptCMS(encryptedFile)
    }

    public byte[] encryptMessage(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        return getEncryptor().encryptMessage(bytesToEncrypt, publicKey);
    }

    public byte[] encryptMessage(byte[] bytesToEncrypt, X509Certificate receiverCert) throws Exception {
        return getEncryptor().encryptMessage(bytesToEncrypt, receiverCert);
    }

    public ResponseVS decryptMessage (byte[] encryptedFile) {
        return getEncryptor().decryptMessage(encryptedFile);
    }

    ResponseVS encryptSMIME(byte[] bytesToEncrypt, X509Certificate receiverCert) throws Exception {
        return getEncryptor().encryptSMIME(bytesToEncrypt, receiverCert);
    }

    ResponseVS decryptSMIME(byte[] encryptedMessageBytes) {
        return getEncryptor().decryptSMIME(encryptedMessageBytes);
    }

    private Encryptor getEncryptor() {
        if(encryptor == null) encryptor = init().encryptor
        return encryptor;
    }

	private SMIMESignedGeneratorVS getSignedMailGenerator() {
		if(signedMailGenerator == null) signedMailGenerator = init().signedMailGenerator
		return signedMailGenerator
	}

}