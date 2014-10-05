package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.jce.PKCS10CertificationRequest
import org.bouncycastle.util.encoders.Base64
import org.springframework.dao.DataAccessException
import org.votingsystem.callable.MessageTimeStamper
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.signature.util.CertExtensionCheckerVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.vicket.model.MessageVS

import javax.mail.Header
import javax.mail.internet.InternetAddress
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.CertPathValidatorException
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

@Transactional
class  SignatureVSService {

    private static final CLASS_NAME = SignatureVSService.class.getSimpleName()

    //static transactional = false
	
	private SignedMailGenerator signedMailGenerator;
    private static Set<TrustAnchor> trustAnchors;
    private static Set<TrustAnchor> vicketAnchors;
	private KeyStore trustedCertsKeyStore
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
			grailsApplication.config.VotingSystem.keyStorePath).getFile()
		String keyAlias = grailsApplication.config.VotingSystem.signKeysAlias
		String password = grailsApplication.config.VotingSystem.signKeysPassword
		signedMailGenerator = new SignedMailGenerator(FileUtils.getBytesFromFile(keyStoreFile), 
			keyAlias, password.toCharArray(), ContextVS.SIGN_MECHANISM);
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
		java.security.cert.Certificate[] chain = keyStore.getCertificateChain(keyAlias);
		byte[] pemCertsArray
        localServerCertSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        vicketAnchors = new HashSet<TrustAnchor>();
        vicketAnchors.add(new TrustAnchor(localServerCertSigner, null));
		for (int i = 0; i < chain.length; i++) {
            checkAuthorityCertDB(chain[i])
			if(!pemCertsArray) pemCertsArray = CertUtil.getPEMEncoded (chain[i])
			else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtil.getPEMEncoded (chain[i]))
		}
        serverCertificateVS = CertificateVS.findBySerialNumber(localServerCertSigner.getSerialNumber().longValue())
        serverPrivateKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray())
		File certChainFile = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certChainPath).getFile();
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
            certificateVS = new CertificateVS(isRoot:CertUtil.isSelfSigned(x509AuthorityCert),
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

    public Set<TrustAnchor> getVicketAnchors() {
        return vicketAnchors;
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
        X509Certificate issuedCert = CertUtil.signCSR(csr, organizationalUnit, getServerPrivateKey(),
                getServerCert(), dateBegin, dateFinish, certExtensions)
        return issuedCert
    }

    @Transactional
	public Map initCertAuthorities() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        File directory = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certAuthoritiesDirPath).getFile()

        File[] acFiles = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String fileName) {
                return fileName.startsWith("AC_") && fileName.endsWith(".pem");
            }
        });
        for(File caFile:acFiles) {
            X509Certificate fileSystemX509TrustedCert = CertUtil.fromPEMToX509Cert(FileUtils.getBytesFromFile(caFile))
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

	public SMIMEMessage getSMIMEMessage (String fromUser,String toUser,String textToSign,String subject, Header header) {
		log.debug "getSMIMEMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
		if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		return getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, header);
	}

    public ResponseVS getTimestampedSignedMimeMessage (String fromUser,String toUser,String textToSign,String subject,
            Header... headers) {
        log.debug "getTimestampedSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
        if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        SMIMEMessage smimeMessage = getSignedMailGenerator().genMimeMessage(
                fromUser, toUser, textToSign, subject, headers)
        MessageTimeStamper timeStamper = new MessageTimeStamper(
                smimeMessage, "${grailsApplication.config.VotingSystem.urlTimeStampServer}/timeStamp")
        ResponseVS responseVS = timeStamper.call();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        smimeMessage = timeStamper.getSmimeMessage();
        responseVS = new ResponseVS(ResponseVS.SC_OK)
        responseVS.setSmimeMessage(smimeMessage)
        return responseVS;
    }
		
	public synchronized SMIMEMessage getMultiSignedMimeMessage (
		String fromUser, String toUser,	final SMIMEMessage smimeMessage, String subject) {
		log.debug("getMultiSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'");
		if(fromUser) {
			fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setFrom(new InternetAddress(fromUser))
		} 
		if(toUser) {
			toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setHeader("To", toUser)
		}
		SMIMEMessage multiSignedMessage = getSignedMailGenerator().genMultiSignedMessage(smimeMessage, subject);
		return multiSignedMessage
	}

    @Transactional
	public ResponseVS validateSMIME(SMIMEMessage smimeMessage, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:smimeMessage.getContentDigestStr())
		if(messageSMIME) {
			String message = messageSource.getMessage('smimeDigestRepeatedErrorMsg', 
				[smimeMessage.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIME - ${message} - messageSMIME.id: ${messageSMIME.id}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:message,
                    metaInf: MetaInfMsg.getErrorMsg(methodName, "hashRepeated"))
		}
		return validateSignersCerts(smimeMessage, locale)
	}

    public ResponseVS verifyUserCertificate(UserVS userVS) {
        ResponseVS validationResponse = CertUtil.verifyCertificate(getTrustAnchors(), false, [userVS.getCertificate()])
        X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
        userVS.setCertificateCA(getTrustedCertsHashMap().get(certCaResult?.getSerialNumber()?.longValue()))
        log.debug("verifyCertificate - user '${userVS.nif}' cert issuer: " + certCaResult?.getSubjectDN()?.toString() +
                " - CA certificateVS.id : " + userVS.getCertificateCA().getId());
        return validationResponse
    }


    public ResponseVS validateCertificates(List<X509Certificate> certificateList) {
        log.debug("validateCertificates")
        ResponseVS validationResponse = CertUtil.verifyCertificate(getTrustAnchors(), false, certificateList)
        //X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
        return validationResponse
    }

	public ResponseVS validateSignersCerts(SMIMEMessage smimeMessage, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		Set<UserVS> signersVS = smimeMessage.getSigners();
		if(signersVS.isEmpty()) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
			messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale))
		Set<UserVS> checkedSigners = new HashSet<UserVS>()
        UserVS checkedSigner = null
        UserVS anonymousSigner = null
        CertExtensionCheckerVS extensionChecker
        String signerNIF = org.votingsystem.util.NifUtils.validate(smimeMessage.getSigner().getNif())
		for(UserVS userVS: signersVS) {
			try {
                if(userVS.getTimeStampToken() != null) {
                    ResponseVS timestampValidationResp = timeStampService.validateToken(
                            userVS.getTimeStampToken(), locale)
                    log.debug("validateSignersCerts - timestampValidationResp - " +
                            "statusCode:${timestampValidationResp.statusCode} - message:${timestampValidationResp.message}")
                    if(ResponseVS.SC_OK != timestampValidationResp.statusCode) {
                        log.error("validateSignersCerts - TIMESTAMP ERROR - ${timestampValidationResp.message}")
                        return timestampValidationResp
                    }
                } else {
                    String msg = messageSource.getMessage('documentWithoutTimeStampErrorMsg', null, locale)
                    log.error("ERROR - validateSignersCerts - ${msg}")
                    return new ResponseVS(message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST, reason:msg,
                            metaInf: MetaInfMsg.getErrorMsg(methodName, "timestampMissing"))
                }
				ResponseVS validationResponse = verifyUserCertificate(userVS)
                extensionChecker = validationResponse.data.extensionChecker
                ResponseVS responseVS = null
                if(extensionChecker.isAnonymousSigner()) {
                    log.debug("validateSignersCerts - is anonymous signer")
                    anonymousSigner = userVS
                    responseVS = new ResponseVS(ResponseVS.SC_OK).setUserVS(anonymousSigner)
                } else {
                    responseVS = subscriptionVSService.checkUser(userVS, locale)
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
                extensionChecker:extensionChecker])
	}

    @Transactional
    private ResponseVS processMessageVS(byte[] messageVSBytes, ContentTypeVS contenType, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        JSONObject messageVSJSON = (JSONObject) JSONSerializer.toJSON(new String(messageVSBytes, "UTF-8"));

        SMIMEMessage smimeSender = new SMIMEMessage(new ByteArrayInputStream(
                Base64.decode(messageVSJSON.smimeMessage.getBytes())))
        ResponseVS responseVS = processSMIMERequest(smimeSender, contenType, locale)
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
        MessageSMIME messageSMIMEReq = responseVS.data
        UserVS fromUser = messageSMIMEReq.getUserVS()
        def messageJSON = JSON.parse(messageSMIMEReq.getSmimeMessage()?.getSignedContent())
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
    private ResponseVS processSMIMERequest(SMIMEMessage smimeMessageReq, ContentTypeVS contenType, Locale locale) {
        if (smimeMessageReq?.isValidSignature()) {
            log.debug "processSMIMERequest - isValidSignature"
            ResponseVS certValidationResponse = null;
            switch(contenType) {
                //case ContentTypeVS.VICKET: break;
                default:
                    certValidationResponse = validateSMIME(smimeMessageReq, locale);
            }
            TypeVS typeVS = TypeVS.OK;
            if(contenType && ContentTypeVS.VICKET == contenType) typeVS = TypeVS.VICKET;
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
            return new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIME)
        } else if(smimeMessageReq) {
            log.error "**** Filter - processSMIMERequest - signature ERROR - "
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:messageSource.getMessage('signatureErrorMsg', null, locale))
        }
    }

    public ResponseVS encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert) throws Exception {
        log.debug("encryptToCMS")
        return getEncryptor().encryptToCMS(dataToEncrypt, receiverCert);
    }

    public ResponseVS encryptToCMS(byte[] dataToEncrypt, PublicKey  receptorPublicKey) throws Exception {
        log.debug("encryptToCMS")
        return getEncryptor().encryptToCMS(dataToEncrypt, receptorPublicKey);
    }


    public ResponseVS encryptMessage(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        log.debug("encryptMessage(...) - ");
        try {
            return getEncryptor().encryptMessage(bytesToEncrypt, publicKey);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(messageSource.getMessage('dataToEncryptErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    public ResponseVS encryptMessage(byte[] bytesToEncrypt, X509Certificate receiverCert, Locale locale) throws Exception {
        log.debug("encryptMessage(...)");
        try {
            byte[] result = getEncryptor().encryptMessage(bytesToEncrypt, receiverCert);
            new ResponseVS(ResponseVS.SC_OK, result);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(messageSource.getMessage('dataToEncryptErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to decrypt files attached to SMIME (not signed) messages
     */
    public ResponseVS decryptMessage (byte[] encryptedFile, Locale locale) {
        log.debug "decryptMessage"
        try {
            return getEncryptor().decryptMessage(encryptedFile);
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS(message:messageSource.getMessage('encryptedMessageErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }
    /**
     * Method to decrypt files attached to SMIME (not signed) messages
     */
    public ResponseVS decryptCMS (byte[] encryptedFile, Locale locale) {
        log.debug " - decryptCMS"
        try {
            return getEncryptor().decryptCMS(serverPrivateKey, encryptedFile)
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS(message:messageSource.getMessage('encryptedMessageErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to encrypt SMIME signed messages
     */
    ResponseVS encryptSMIMEMessage(byte[] bytesToEncrypt, X509Certificate receiverCert, Locale locale) throws Exception {
        log.debug("encryptSMIMEMessage(...) ");
        try {
            return getEncryptor().encryptSMIMEMessage(bytesToEncrypt, receiverCert);
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS(messageSource.getMessage('dataToEncryptErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to decrypt SMIME signed messages
     */
    ResponseVS decryptSMIMEMessage(byte[] encryptedMessageBytes, Locale locale) {
        log.debug("decryptSMIMEMessage ")
        try {
            return getEncryptor().decryptSMIMEMessage(encryptedMessageBytes);
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS(message:messageSource.getMessage('encryptedMessageErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    private Encryptor getEncryptor() {
        if(encryptor == null) encryptor = init().encryptor
        return encryptor;
    }

	private SignedMailGenerator getSignedMailGenerator() {
		if(signedMailGenerator == null) signedMailGenerator = init().signedMailGenerator
		return signedMailGenerator
	}

}