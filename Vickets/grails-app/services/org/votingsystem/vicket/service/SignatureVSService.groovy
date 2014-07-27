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
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.signature.util.CertExtensionCheckerVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.util.FileUtils
import org.votingsystem.vicket.model.MessageVS
import org.votingsystem.vicket.util.MetaInfMsg

import javax.mail.Header
import javax.mail.internet.InternetAddress
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.CertPathValidatorException
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

class  SignatureVSService {

    static transactional = false
	
	private SignedMailGenerator signedMailGenerator;
    private static Set<TrustAnchor> trustAnchors;
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
		String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
		String password = grailsApplication.config.VotingSystem.signKeysPassword
		signedMailGenerator = new SignedMailGenerator(FileUtils.getBytesFromFile(keyStoreFile), 
			aliasClaves, password.toCharArray(), ContextVS.SIGN_MECHANISM);
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
		java.security.cert.Certificate[] chain = keyStore.getCertificateChain(aliasClaves);
		byte[] pemCertsArray
        localServerCertSigner = (X509Certificate) keyStore.getCertificate(aliasClaves);
		for (int i = 0; i < chain.length; i++) {
            X509Certificate x509ChainCert = chain[i]
            CertificateVS chainCertVS = CertificateVS.findWhere(serialNumber:x509ChainCert.getSerialNumber().longValue());
            if(!chainCertVS) {
                chainCertVS = new CertificateVS(isRoot:CertUtil.isSelfSigned(x509ChainCert),
                        type:CertificateVS.Type.CERTIFICATE_AUTHORITY, state:CertificateVS.State.OK,
                        content:x509ChainCert.getEncoded(), serialNumber:x509ChainCert.getSerialNumber().longValue(),
                        validFrom:x509ChainCert.getNotBefore(), validTo:x509ChainCert.getNotAfter()).save()
                log.debug "Adding local server cert to database  - serverCertificateVS: '${chainCertVS.id}'"
            } else log.debug "CA '${chain[i].getSubjectDN()}' - id: '${chainCertVS.id}'"
            if(localServerCertSigner.getSerialNumber().longValue() == chainCertVS.serialNumber) {
                serverCertificateVS = chainCertVS
            }
			if(!pemCertsArray) pemCertsArray = CertUtil.getPEMEncoded (x509ChainCert)
			else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtil.getPEMEncoded (x509ChainCert))
		}
        serverPrivateKey = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray())
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

	public boolean isSystemSignedMessage(Set<UserVS> signers) {
		boolean result = false
		log.debug "isSystemSignedMessage - localServerCert num. serie: ${localServerCertSigner.getSerialNumber().longValue()}"
		signers.each {
			long signerId = ((UserVS)it).getCertificate().getSerialNumber().longValue()
			log.debug " --- num serie signer: ${signerId}"
			if(signerId == localServerCertSigner.getSerialNumber().longValue()) result = true;
		}
		return result
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
	private void initCertAuthorities() {
        File directory = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certAuthoritiesDirPath).getFile()

        File[] acFiles = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String fileName) {
                return fileName.startsWith("AC_") && fileName.endsWith(".pem");
            }
        });
        Set<X509Certificate> fileSystemCerts = new HashSet<X509Certificate>()
        for(File caFile:acFiles) {
            Collection<X509Certificate> certCollection = CertUtil.fromPEMToX509CertCollection(FileUtils.getBytesFromFile(caFile))
            fileSystemCerts.add(certCollection.iterator().next());
        }
        for(X509Certificate x509Certificate:fileSystemCerts) {
            long serialNumber = x509Certificate.getSerialNumber().longValue()
            CertificateVS certificate = null
            certificate = CertificateVS.findBySerialNumber(serialNumber)
            if(!certificate) {
                boolean isRoot = CertUtil.isSelfSigned(x509Certificate)
                certificate = new CertificateVS(isRoot:isRoot, type:CertificateVS.Type.CERTIFICATE_AUTHORITY,
                        state:CertificateVS.State.OK, content:x509Certificate.getEncoded(), serialNumber:serialNumber,
                        validFrom:x509Certificate.getNotBefore(), validTo:x509Certificate.getNotAfter())
                certificate.save()
                log.debug "initCertAuthorities - ADDED NEW CA CERT '${x509Certificate?.getSubjectDN()}' - certificateVS.id:'${certificate?.id}'"
            } else {
                if(CertificateVS.State.OK != certificate.state) {
                    log.error "File system athority cert '${x509Certificate?.getSubjectDN()}' " +
                            " with certificateVS.id:'${certificate?.id}' state is '${certificate.state}'"
                } else log.debug "initCertAuthorities CERT OK - ${x509Certificate?.getSubjectDN()} --- serialNumber:${serialNumber}"
            }
        }
        loadCertAuthorities()
	}

    @Transactional public Map loadCertAuthorities() {
        trustedCertsHashMap = new HashMap<Long, CertificateVS>();
        Set<X509Certificate> trustedCertsSet = new HashSet<X509Certificate>();
        List<CertificateVS> trustedCertsList = CertificateVS.createCriteria().list(offset: 0) {
            eq("state", CertificateVS.State.OK)
            eq("type",	CertificateVS.Type.CERTIFICATE_AUTHORITY)
        }
        trustedCertsList.each { certificate ->
            X509Certificate x509Cert = certificate.getX509Cert()
            trustedCertsSet.add(x509Cert)
            trustedCertsHashMap.put(x509Cert.getSerialNumber()?.longValue(), certificate)
        }
        trustedCerts = new HashSet<X509Certificate>(trustedCertsSet);
        trustAnchors = new HashSet<TrustAnchor>();
        for(X509Certificate certificate: trustedCerts) {
            TrustAnchor anchor = new TrustAnchor(certificate, null);
            trustAnchors.add(anchor);
        }
        log.debug("loadCertAuthorities - loaded '${trustedCertsHashMap?.keySet().size()}' authorities")
        return [trustedCertsHashMap: trustedCertsHashMap, trustedCerts:trustedCerts, trustAnchors:trustAnchors]
    }


    public Set<TrustAnchor> getTrustAnchors() {
        if(!trustAnchors) trustAnchors = loadCertAuthorities().trustAnchors
        return trustAnchors;
    }

    private Set<X509Certificate> getTrustedCerts() {
        if(!trustedCerts)  trustedCerts = loadCertAuthorities().trustedCerts
        return trustedCerts;
    }

	public SMIMEMessageWrapper getSignedMimeMessage (String fromUser,String toUser,String textToSign,String subject, Header header) {
		log.debug "getSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
		if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		return getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, header);
	}

    public ResponseVS getTimestampedSignedMimeMessage (String fromUser,String toUser,String textToSign,String subject,
            Header... headers) {
        log.debug "getTimestampedSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
        if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        SMIMEMessageWrapper smimeMessage = getSignedMailGenerator().genMimeMessage(
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
		
	public synchronized SMIMEMessageWrapper getMultiSignedMimeMessage (
		String fromUser, String toUser,	final SMIMEMessageWrapper smimeMessage, String subject) {
		log.debug("getMultiSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'");
		if(fromUser) {
			fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setFrom(new InternetAddress(fromUser))
		} 
		if(toUser) {
			toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setHeader("To", toUser)
		}
		SMIMEMessageWrapper multiSignedMessage = getSignedMailGenerator().genMultiSignedMessage(smimeMessage, subject);
		return multiSignedMessage
	}

    @Transactional
	public ResponseVS validateSMIME(SMIMEMessageWrapper messageWrapper, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:messageWrapper.getContentDigestStr())
		if(messageSMIME) {
			String message = messageSource.getMessage('smimeDigestRepeatedErrorMsg', 
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIME - ${message} - messageSMIME.id: ${messageSMIME.id}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:message,
                    metaInf: MetaInfMsg.getErrorMsg(methodName, "hashRepeated"))
		}
		return validateSignersCertificate(messageWrapper, locale)
	}

    public ResponseVS verifyUserCertificate(UserVS userVS) {
        ResponseVS validationResponse = CertUtil.verifyCertificate(getTrustAnchors(), false, [userVS.getCertificate()])
        X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
        userVS.setCertificateCA(trustedCertsHashMap.get(certCaResult?.getSerialNumber()?.longValue()))
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

	public ResponseVS validateSignersCertificate(SMIMEMessageWrapper messageWrapper, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		Set<UserVS> signersVS = messageWrapper.getSigners();
		if(signersVS.isEmpty()) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
			messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale))
		Set<UserVS> checkedSigners = new HashSet<UserVS>()
        UserVS checkedSigner = null
        UserVS anonymousSigner = null
        CertExtensionCheckerVS extensionChecker
        String signerNIF = org.votingsystem.util.NifUtils.validate(messageWrapper.getSigner().getNif())
		for(UserVS userVS: signersVS) {
			try {
                if(userVS.getTimeStampToken() != null) {
                    ResponseVS timestampValidationResp = timeStampService.validateToken(
                            userVS.getTimeStampToken(), locale)
                    log.debug("validateSignersCertificate - timestampValidationResp - " +
                            "statusCode:${timestampValidationResp.statusCode} - message:${timestampValidationResp.message}")
                    if(ResponseVS.SC_OK != timestampValidationResp.statusCode) {
                        log.error("validateSignersCertificate - TIMESTAMP ERROR - ${timestampValidationResp.message}")
                        return timestampValidationResp
                    }
                } else {
                    String msg = messageSource.getMessage('documentWithoutTimeStampErrorMsg', null, locale)
                    log.error("ERROR - validateSignersCertificate - ${msg}")
                    return new ResponseVS(message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST, reason:msg,
                            metaInf: MetaInfMsg.getErrorMsg(methodName, "timestampMissing"))
                }
				ResponseVS validationResponse = verifyUserCertificate(userVS)
                extensionChecker = validationResponse.data.extensionChecker
                ResponseVS responseVS = null
                if(extensionChecker.isAnonymousSigner()) {
                    log.debug("validateSignersCertificate - is anonymous signer")
                    anonymousSigner = userVS
                    responseVS = new ResponseVS(ResponseVS.SC_OK)
                    responseVS.setUserVS(anonymousSigner)
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
		return new ResponseVS(statusCode:ResponseVS.SC_OK, smimeMessage:messageWrapper,
                data:[checkedSigners:checkedSigners, checkedSigner:checkedSigner, anonymousSigner:anonymousSigner,
                extensionChecker:extensionChecker])
	}

    @Transactional
    private ResponseVS processMessageVS(byte[] messageVSBytes, ContentTypeVS contenType, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        JSONObject messageVSJSON = (JSONObject) JSONSerializer.toJSON(new String(messageVSBytes, "UTF-8"));

        SMIMEMessageWrapper smimeSender = new SMIMEMessageWrapper(new ByteArrayInputStream(
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
    private ResponseVS processSMIMERequest(SMIMEMessageWrapper smimeMessageReq, ContentTypeVS contenType, Locale locale) {
        if (smimeMessageReq?.isValidSignature()) {
            log.debug "processSMIMERequest - isValidSignature"
            ResponseVS certValidationResponse = null;
            switch(contenType) {
                /*case ContentTypeVS.VICKET:
                    certValidationResponse = validateSMIMEVicket(smimeMessageReq, locale)
                    break;*/
                default:
                    certValidationResponse = validateSMIME(smimeMessageReq, locale);
            }
            TypeVS typeVS = TypeVS.OK;
            if(contenType && ContentTypeVS.VICKET == contenType) typeVS = TypeVS.VICKET;
            MessageSMIME messageSMIME
            if(ResponseVS.SC_OK != certValidationResponse.statusCode) {
                messageSMIME = new MessageSMIME(reason:certValidationResponse.message, type:TypeVS.SIGNATURE_ERROR,
                        metaInf:certValidationResponse.metaInf ,content:smimeMessageReq.getBytes())
                messageSMIME.save()
                log.error "*** Filter - processSMIMERequest - failed - status: ${certValidationResponse.statusCode}" +
                        " - message: ${certValidationResponse.message}"
                return certValidationResponse
            } else {
                messageSMIME = new MessageSMIME(signers:certValidationResponse.data?.checkedSigners,
                        anonymousSigner:certValidationResponse.data?.anonymousSigner,
                        userVS:certValidationResponse.data?.checkedSigner, smimeMessage:smimeMessageReq,
                        eventVS:certValidationResponse.eventVS, type:typeVS,
                        content:smimeMessageReq.getBytes(), base64ContentDigest:smimeMessageReq.getContentDigestStr())
                messageSMIME.save()
            }
            return new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIME)
        } else if(smimeMessageReq) {
            log.error "**** Filter - processSMIMERequest - signature ERROR - "
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:messageSource.getMessage('signatureErrorMsg', null, locale))
        }
    }

	public ResponseVS validateSMIMEVicket(SMIMEMessageWrapper messageWrapper, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:messageWrapper.getContentDigestStr())
		if(messageSMIME) {
			String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIMEVicket - ${msg} - messageSMIME.id: ${messageSMIME.id}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, 'base64ContentDigest'))
		}
		return validateVicketCerts(messageWrapper, locale)
	}

	public ResponseVS validateVicketCerts(SMIMEMessageWrapper smimeMessageReq, Locale locale) {
        log.debug("validateVicketCerts")
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