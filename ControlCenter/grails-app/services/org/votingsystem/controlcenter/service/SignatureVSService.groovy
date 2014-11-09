package org.votingsystem.controlcenter.service

import grails.transaction.Transactional
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.smime.SMIMESignedGeneratorVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.MetaInfMsg
import javax.mail.Header
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.CertPathValidatorException
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import static org.springframework.context.i18n.LocaleContextHolder.*

@Transactional
class SignatureVSService {

    //static transactional = false

    def grailsApplication;
    def messageSource;
    def subscriptionVSService
    def timeStampService
    private File serverCertChainFile;
    private X509Certificate serverCert;
    private Encryptor encryptor;
    private SMIMESignedGeneratorVS signedMailGenerator;
    private static Set<X509Certificate> trustedCerts;
    private static Set<TrustAnchor> trustAnchors;
    private static HashMap<Long, CertificateVS> trustedCertsHashMap;
    private static HashMap<Long, Set<TrustAnchor>> eventTrustedAnchorsMap = new HashMap<Long, Set<TrustAnchor>>();

    private synchronized Map init() {
        log.debug "init"
        File keyStoreFile = grailsApplication.mainContext.getResource(
                grailsApplication.config.vs.keyStorePath).getFile()
        String keyAlias = grailsApplication.config.vs.signKeysAlias
        String password = grailsApplication.config.vs.signKeysPassword
        signedMailGenerator = new SMIMESignedGeneratorVS(FileUtils.getBytesFromFile(keyStoreFile),
                keyAlias, password.toCharArray(), ContextVS.SIGN_MECHANISM);
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
        java.security.cert.Certificate[] chain = keyStore.getCertificateChain(keyAlias);
        serverCert = (X509Certificate) keyStore.getCertificate(keyAlias);
        byte[] pemCertsArray
        for (int i = 0; i < chain.length; i++) {
            checkAuthorityCertDB(chain[i])
            if(!pemCertsArray) pemCertsArray = CertUtils.getPEMEncoded (chain[i])
            else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtils.getPEMEncoded (chain[i]))
        }
        serverCertChainFile = grailsApplication.mainContext.getResource(
                grailsApplication.config.vs.certChainPath)?.getFile();
        PrivateKey serverPrivateKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray())
        serverCertChainFile.createNewFile()
        serverCertChainFile.setBytes(pemCertsArray)
        encryptor = new Encryptor(serverCert, serverPrivateKey);
        initCertAuthorities();
        return [serverCert:serverCert, serverCertChainFile:serverCertChainFile ,
                signedMailGenerator:signedMailGenerator, encryptor:encryptor];
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

    @Transactional
    private synchronized Map initCertAuthorities() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        File directory=  grailsApplication.mainContext.getResource(
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
        List<CertificateVS> trustedCertsList = CertificateVS.createCriteria().list(offset: 0) {
            eq("state", CertificateVS.State.OK)
            eq("type",	CertificateVS.Type.CERTIFICATE_AUTHORITY)
        }
        trustedCerts = new HashSet<X509Certificate>()
        trustedCertsHashMap = new HashMap<Long, CertificateVS>();
        for(CertificateVS certificateVS : trustedCertsList) {
            X509Certificate x509Cert = CertUtils.loadCertificate(certificateVS.content)
            trustedCerts.add(x509Cert)
            trustedCertsHashMap.put(x509Cert?.getSerialNumber()?.longValue(), certificateVS)
            String certData = "${x509Cert?.getSubjectDN()} - numSerie:${x509Cert?.getSerialNumber()?.longValue()}"
            log.debug "$methodName - certificateVS.id: '${certificateVS?.id}' - $certData"
        }
        log.debug("$methodName - Num. system Cert Authorities: ${trustedCerts?.size()}")
        return [trustedCerts:trustedCerts, trustedCertsHashMap:trustedCertsHashMap];
    }

    public boolean isSignerCertificate(Set<UserVS> signers, X509Certificate cert) {
        boolean result = false
        log.debug "isSignerCertificate - cert num. serie: ${cert.getSerialNumber().longValue()}"
        signers.each {
            long signerId = ((UserVS)it).getCertificate().getSerialNumber().longValue()
            if(signerId == cert.getSerialNumber().longValue()) result = true;
        }
        return result
    }

    public ResponseVS validateSMIMEVote(SMIMEMessage smimeMessageReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
        MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:smimeMessageReq.getContentDigestStr())
        if(messageSMIME) throw new ExceptionVS(messageSource.getMessage('smimeDigestRepeatedErrorMsg',
                [smimeMessageReq.getContentDigestStr()].toArray(), locale))
        String msg
        ResponseVS responseVS
        VoteVS voteVS = smimeMessageReq.voteVS
        UserVS checkedSigner = null
        if(!voteVS?.getX509Certificate()) throw new ExceptionVS(messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale))
        if (voteVS.getRepresentativeURL()) {
            checkedSigner = UserVS.findWhere(url:voteVS.getRepresentativeURL())
            if(!checkedSigner) checkedSigner = new UserVS(url:voteVS.getRepresentativeURL(),
                    nif:voteVS.getRepresentativeURL(), type:UserVS.Type.REPRESENTATIVE).save()
        }
        String accessControlURL = voteVS.accessControlURL
        AccessControlVS accessControl = AccessControlVS.findWhere(serverURL:accessControlURL)
        if (!accessControl) throw new ExceptionVS(messageSource.getMessage('voteAccessControlUnknownErrorMsg', null, locale))
        EventVS eventVS = EventVS.findWhere(accessControlVS:accessControl, accessControlEventVSId:voteVS.getEventVS().getId())

        if (!eventVS) throw new ExceptionVS(messageSource.getMessage('voteEventVSElectionUnknownErrorMsg', null, locale))
        if(eventVS.state != EventVS.State.ACTIVE)
            throw new ExceptionVS(messageSource.getMessage('electionClosed', [eventVS.subject].toArray(), locale))

        CertificateVS certificateVS = CertificateVS.findWhere(hashCertVSBase64:voteVS.hashCertVSBase64)
        if (certificateVS) {
            log.error("$methodName - repeated vote - hashCertVSBase64:${voteVS.hashCertVSBase64}")
            VoteVS repeatedVoteVS = VoteVS.findWhere(certificateVS:certificateVS)
            msg = messageSource.getMessage('voteRepeatedErrorMsg', [repeatedVoteVS.id].toArray(), locale)
            log.error("validateSMIMEVote - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST_REPEATED, eventVS:eventVS, message:msg)
        }
        Set<TrustAnchor> eventTrustedAnchors = getEventTrustedAnchors(eventVS)
        responseVS = timeStampService.validateToken(voteVS.getTimeStampToken(), eventVS)
        if(ResponseVS.SC_OK != responseVS.statusCode) {
            responseVS.type = TypeVS.VOTE_ERROR
            responseVS.eventVS = eventVS
            return responseVS
        }
        X509Certificate checkedCert = voteVS.getX509Certificate()
        try {
            CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                    eventTrustedAnchors, false, [checkedCert])
            X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex)
            throw new ExceptionVS(messageSource.getMessage('certValidationErrorMsg',
                    [checkedCert.getSubjectDN()?.toString()].toArray(), locale))

        }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS, smimeMessage:smimeMessageReq,
                data:[checkedSigner:checkedSigner])
    }

    public CertUtils.CertValidatorResultVS verifyUserCertificate(UserVS userVS) throws Exception {
        CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                getTrustAnchors(), false, [userVS.getCertificate()])
        X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
        log.debug("verifyCertificate - user '${userVS.nif}' cert issuer: " + certCaResult?.getSubjectDN()?.toString());
        return validatorResult
    }

    public ResponseVS validateSMIMEVoteCancellation(String url, SMIMEMessage smimeMessage) {
        log.debug("validateSMIMEVoteCancellation - url: ${url}")
        EventVS eventVS = null
        EventVS.withTransaction { eventVS = EventVS.findByUrl(url) }
        if(!eventVS) {
            String msg = messageSource.getMessage('eventVSNotFound', [url].toArray(), locale)
            log.error("validateSMIMEVoteCancellation - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
        }
        MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:smimeMessage.getContentDigestStr())
        if(messageSMIME) {
            String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
                    [smimeMessage.getContentDigestStr()].toArray(), locale)
            log.error("validateSMIMEVoteCancellation - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
        }
        return validateVoteCerts(smimeMessage,	eventVS)
    }

    public Set<TrustAnchor> getEventTrustedAnchors(EventVS eventVS) {
        Set<TrustAnchor> eventTrustedAnchors = eventTrustedAnchorsMap.get(eventVS.id)
        if(!eventTrustedAnchors) {
            CertificateVS eventCACert = CertificateVS.findWhere(eventVSElection:eventVS, state:CertificateVS.State.OK,
                    type:CertificateVS.Type.VOTEVS_ROOT)
            X509Certificate certCAEventVS = eventCACert.getX509Cert()
            eventTrustedAnchors = new HashSet<TrustAnchor>()
            eventTrustedAnchors.add(new TrustAnchor(certCAEventVS, null))
            eventTrustedAnchors.addAll(getTrustAnchors())
            eventTrustedAnchorsMap.put(eventVS.id, eventTrustedAnchors)
        }
        return eventTrustedAnchors
    }


    public ResponseVS validateVoteCerts(SMIMEMessage smimeMessageReq, EventVS eventVS) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
        Set<UserVS> signersVS = smimeMessageReq.getSigners();
        String msg
        if(signersVS.isEmpty()) throw new ExceptionVS(messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale))
        Set<X509Certificate> eventTrustedAnchors = getEventTrustedAnchors(eventVS)
        for(UserVS userVS: signersVS) {
            log.debug("$methodName - validating signer: ${userVS.getCertificate().getSubjectDN()}")
            try {
                CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                        eventTrustedAnchors, false, [userVS.getCertificate()])
                X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
                log.debug("$methodName - certCaResult: " + certCaResult?.getSubjectDN()?.toString()+
                        "- serialNumber: " + certCaResult?.getSerialNumber()?.longValue());
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex)
                msg = messageSource.getMessage('certValidationErrorMsg',
                        [userVS.getCertificate().getSubjectDN()?.toString()].toArray(), locale)
                log.error ("$methodName - msg:$msg - EventVS '${eventVS.id}'")
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
            }
        }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS, smimeMessage:smimeMessageReq)
    }

    public ResponseVS validateSMIME(SMIMEMessage smimeMessageReq) {
        log.debug("validateSMIME")
        MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:smimeMessageReq.getContentDigestStr())
        if(messageSMIME) {
            String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
                    [smimeMessageReq.getContentDigestStr()].toArray(), locale)
            log.error("validateSMIME - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
        }
        return validateSignersCerts(smimeMessageReq)
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
        String signerNIF = smimeMessage.getSigner().getNif()
        boolean isTimeStamped = false
        for(UserVS userVS: signersVS) {
            try {
                if(userVS.getTimeStampToken()) {
                    timeStampService.validateToken(userVS.getTimeStampToken())
                    isTimeStamped = true
                }
                validatorResult = CertUtils.verifyCertificate(getTrustAnchors(), false, [userVS.getCertificate()])
                X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
                userVS.setCertificateCA(trustedCertsHashMap.get(certCaResult?.getSerialNumber()?.longValue()))
                log.debug("$methodName - user cert issuer: " + certCaResult?.getSubjectDN()?.toString() +
                        " - issuer serialNumber: " + certCaResult?.getSerialNumber()?.longValue());
                ResponseVS responseVS = null
                if(validatorResult.getChecker().isAnonymousSigner()) {
                    log.debug("$methodName - anonymous signer")
                    anonymousSigner = userVS
                    responseVS = new ResponseVS(ResponseVS.SC_OK)
                    responseVS.setUserVS(anonymousSigner)
                } else {
                    responseVS = subscriptionVSService.checkUser(userVS)
                    if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
                    if(userVS.getNif().equals(signerNIF)) checkedSigner = responseVS.userVS;
                }
                checkedSigners.add(responseVS.userVS)
            } catch (CertPathValidatorException ex) {
                log.error(ex.getMessage(), ex)
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
                        messageSource.getMessage('unknownCAErrorMsg', null, locale))
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex)
                return new ResponseVS(message:ex.getMessage(), statusCode:ResponseVS.SC_ERROR)
            }
        }
        if(!isTimeStamped) throw new ExceptionVS(messageSource.getMessage('documentWithoutTimeStampErrorMsg', null,
                locale), MetaInfMsg.getErrorMsg(methodName, 'timestampMissing'))
        return new ResponseVS(statusCode:ResponseVS.SC_OK, smimeMessage:smimeMessage,
                data:[checkedSigners:checkedSigners, checkedSigner:checkedSigner, anonymousSigner:anonymousSigner,
                      extensionChecker:validatorResult.getChecker()])
    }

    public SMIMEMessage getSMIME (String fromUser,String toUser,String textToSign,String subject,Header header) {
        log.debug "getSMIME - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
        return getSignedMailGenerator().getSMIME(fromUser, toUser, textToSign, subject, header);
    }

    public synchronized SMIMEMessage getSMIMEMultiSigned (
            String fromUser, String toUser,	final SMIMEMessage smimeMessage, String subject) {
        log.debug("getSMIMEMultiSigned - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'");
        return getSignedMailGenerator().getSMIMEMultiSigned(fromUser, toUser, smimeMessage, subject);
    }

    public byte[] encryptMessage(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        return getEncryptor().encryptMessage(bytesToEncrypt, publicKey);
    }

    /**
     * Method to decrypt files attached to SMIME (not signed) messages
     */
    public ResponseVS decryptMessage (byte[] encryptedFile) {
        return getEncryptor().decryptMessage(encryptedFile);
    }

    /**
     * Method to encrypt SMIME signed messages
     */
    ResponseVS encryptSMIME(byte[] bytesToEncrypt, X509Certificate receiverCert) throws Exception {
        return getEncryptor().encryptSMIME(bytesToEncrypt, receiverCert);
    }

    /**
     * Method to decrypt SMIME signed messages
     */
    ResponseVS decryptSMIME(byte[] encryptedMessageBytes) {
        return getEncryptor().decryptSMIME(encryptedMessageBytes);
    }

    public Set<X509Certificate> getTrustedCerts() {
        if(!trustedCerts || trustedCerts.isEmpty()) trustedCerts = initCertAuthorities().trustedCerts
        return trustedCerts;
    }

    public Set<TrustAnchor> getTrustAnchors() {
        if(!trustAnchors) {
            Set<X509Certificate> trustedCerts = getTrustedCerts()
            trustAnchors = new HashSet<TrustAnchor>();
            for(X509Certificate certificate: trustedCerts) {
                trustAnchors.add(new TrustAnchor(certificate, null));
            }
        }
        return trustAnchors;
    }

    public X509Certificate getServerCert() {
        if(serverCert == null) serverCert = init().serverCert
        return serverCert
    }

    public File getServerCertChain() {
        if(serverCertChainFile == null) serverCertChainFile = init().serverCertChainFile;
        return serverCertChainFile;
    }

    private Encryptor getEncryptor() {
        if(encryptor == null) encryptor = init().encryptor;
        return encryptor;
    }

    private SMIMESignedGeneratorVS getSignedMailGenerator() {
        if(signedMailGenerator == null) signedMailGenerator = init().signedMailGenerator;
        return signedMailGenerator
    }

}