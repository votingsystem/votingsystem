package org.votingsystem.controlcenter.service

import grails.transaction.Transactional
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.CertExtensionCheckerVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils

import javax.mail.Header
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.CertPathValidatorException
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

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
	private SignedMailGenerator signedMailGenerator;
	private static Set<X509Certificate> trustedCerts;
    private static Set<TrustAnchor> trustAnchors;
	private static HashMap<Long, CertificateVS> trustedCertsHashMap;
	private static HashMap<Long, Set<TrustAnchor>> eventTrustedAnchorsMap = new HashMap<Long, Set<TrustAnchor>>();

	private synchronized Map init() {
		log.debug "init"
		File keyStoreFile = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.keyStorePath).getFile()
		String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
		String password = grailsApplication.config.VotingSystem.signKeysPassword
		signedMailGenerator = new SignedMailGenerator(FileUtils.getBytesFromFile(keyStoreFile),
			aliasClaves, password.toCharArray(), ContextVS.SIGN_MECHANISM);
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
		java.security.cert.Certificate[] chain = keyStore.getCertificateChain(aliasClaves);
        serverCert = (X509Certificate) keyStore.getCertificate(aliasClaves);
		byte[] pemCertsArray
		trustedCerts = new HashSet<X509Certificate>()
		for (int i = 0; i < chain.length; i++) {
			log.debug " --- init - Adding local kesystore cert '${i}' -> 'SubjectDN: ${chain[i].getSubjectDN()}'"
			trustedCerts.add(chain[i])
			if(!pemCertsArray) pemCertsArray = CertUtil.getPEMEncoded (chain[i])
			else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtil.getPEMEncoded (chain[i]))
		}
		serverCertChainFile = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certChainPath)?.getFile();
        PrivateKey serverPrivateKey = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray())
		serverCertChainFile.createNewFile()
		serverCertChainFile.setBytes(pemCertsArray)
        encryptor = new Encryptor(serverCert, serverPrivateKey);
		initCertAuthorities();
        return [serverCert:serverCert, serverCertChainFile:serverCertChainFile ,
                signedMailGenerator:signedMailGenerator, trustedCerts:trustedCerts, encryptor:encryptor];
	}

    private synchronized ResponseVS initCertAuthorities() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
		try {
			trustedCertsHashMap = new HashMap<Long, CertificateVS>();
			File directory=  grailsApplication.mainContext.getResource(
				grailsApplication.config.VotingSystem.certAuthoritiesDirPath).getFile()
			File[] acFiles = directory.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String fileName) {
					return fileName.startsWith("AC_") && fileName.endsWith(".pem");
				}
			  });
			for(File caFile:acFiles) {
				trustedCerts.addAll(CertUtil.fromPEMToX509CertCollection(FileUtils.getBytesFromFile(caFile)));
			}
			CertificateVS.withTransaction {
				def trustedCertsDB = CertificateVS.createCriteria().list {
					eq("state", CertificateVS.State.OK)
                    eq("type",	CertificateVS.Type.CERTIFICATE_AUTHORITY)
				}
				trustedCertsDB.each { certificate ->
					ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
					X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
					trustedCerts.add(certX509)
				}
			}
			
			for(X509Certificate x509Certificate:trustedCerts) {
                String certData = "${x509Certificate?.getSubjectDN()} - numSerie:${x509Certificate?.getSerialNumber()?.longValue()}"
				CertificateVS certificate = CertificateVS.findBySerialNumber(x509Certificate.getSerialNumber().longValue())
				if(!certificate) {
					certificate = new CertificateVS(isRoot:CertUtil.isSelfSigned(x509Certificate),
						type:CertificateVS.Type.CERTIFICATE_AUTHORITY, state:CertificateVS.State.OK,
						content:x509Certificate.getEncoded(), serialNumber:x509Certificate.getSerialNumber().longValue(),
						validFrom:x509Certificate.getNotBefore(), validTo:x509Certificate.getNotAfter())
					CertificateVS.withTransaction {
						certificate.save()
					}
                    log.debug "$methodName - ADDED NEW CA CERT - $certData - certificateVS.id:'${certificate?.id}'"
				} else log.debug "$methodName - $certData - certificateVS.id: '${certificate?.id}'"
				trustedCertsHashMap.put(x509Certificate?.getSerialNumber()?.longValue(), certificate)
			}			
			log.debug("Número de Autoridades Certificadoras en sistema: ${trustedCerts?.size()}")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"Importadas Autoridades Certificadoras")
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage())
		}
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
	
	public ResponseVS validateSMIMEVote(SMIMEMessageWrapper smimeMessageReq, Locale locale) {
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
            log.error("validateVoteCerts - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST_REPEATED, eventVS:eventVS, message:msg)
        }
        Set<TrustAnchor> eventTrustedAnchors = getEventTrustedAnchors(eventVS)
        responseVS = timeStampService.validateToken(voteVS.getTimeStampToken(), eventVS, locale)
        if(ResponseVS.SC_OK != responseVS.statusCode) {
            responseVS.type = TypeVS.VOTE_ERROR
            responseVS.eventVS = eventVS
            return responseVS
        }
        X509Certificate checkedCert = voteVS.getX509Certificate()
        try {
            ResponseVS validationResponse = CertUtil.verifyCertificate(eventTrustedAnchors, false, [checkedCert])
            X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex)
            throw new ExceptionVS(messageSource.getMessage('certValidationErrorMsg',
                    [checkedCert.getSubjectDN()?.toString()].toArray(), locale))

        }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS, smimeMessage:smimeMessageReq,
                data:[checkedSigner:checkedSigner])
	}

	public ResponseVS validateSMIMEVoteCancellation(String url, SMIMEMessageWrapper messageWrapper, Locale locale) {
		log.debug("validateSMIMEVoteCancellation - url: ${url}")
		EventVS eventVS = null
        EventVS.withTransaction { eventVS = EventVS.findByUrl(url) }
		if(!eventVS) {
			String msg = messageSource.getMessage('eventVSNotFound', [url].toArray(), locale)
			log.error("validateSMIMEVoteCancellation - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:messageWrapper.getContentDigestStr())
		if(messageSMIME) {
			String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIMEVoteCancellation - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
		return validateVoteCerts(messageWrapper,	eventVS, locale)
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


    public ResponseVS validateVoteCerts(SMIMEMessageWrapper smimeMessageReq, EventVS eventVS, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
        Set<UserVS> signersVS = smimeMessageReq.getSigners();
        String msg
        if(signersVS.isEmpty()) throw new ExceptionVS(messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale))
        Set<X509Certificate> eventTrustedAnchors = getEventTrustedAnchors(eventVS)
        for(UserVS userVS: signersVS) {
            log.debug("$methodName - validating signer: ${userVS.getCertificate().getSubjectDN()}")
            try {
                ResponseVS validationResponse = CertUtil.verifyCertificate(eventTrustedAnchors, false, [userVS.getCertificate()])
                X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
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

	public ResponseVS validateSMIME(SMIMEMessageWrapper smimeMessageReq, Locale locale) {
		log.debug("validateSMIME")
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:smimeMessageReq.getContentDigestStr())
		if(messageSMIME) {
			String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
				[smimeMessageReq.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIME - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
		return validateSignersCerts(smimeMessageReq, locale)
	}

    public ResponseVS validateSignersCerts(SMIMEMessageWrapper messageWrapper, Locale locale) {
        Set<UserVS> signersVS = messageWrapper.getSigners();
        if(signersVS.isEmpty()) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
                messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale))
        Set<UserVS> checkedSigners = new HashSet<UserVS>()
        UserVS checkedSigner = null
        UserVS anonymousSigner = null
        CertExtensionCheckerVS extensionChecker
        String signerNIF = messageWrapper.getSigner().getNif()
        messageWrapper.get
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
                } else if(messageWrapper.getTimeStampToken() == null) {
                    String msg = messageSource.getMessage('documentWithoutTimeStampErrorMsg', null, locale)
                    log.error("ERROR - validateSignersCerts - ${msg}")
                    return new ResponseVS(message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
                }
                ResponseVS validationResponse = CertUtil.verifyCertificate(getTrustAnchors(), false, [userVS.getCertificate()])
                X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
                userVS.setCertificateCA(trustedCertsHashMap.get(certCaResult?.getSerialNumber()?.longValue()))
                log.debug("validateSignersCerts - user cert issuer: " + certCaResult?.getSubjectDN()?.toString() +
                        " - issuer serialNumber: " + certCaResult?.getSerialNumber()?.longValue());
                extensionChecker = validationResponse.data.extensionChecker
                ResponseVS responseVS = null
                if(extensionChecker.isAnonymousSigner()) {
                    log.debug("validateSignersCerts - anonymous signer")
                    anonymousSigner = userVS
                    responseVS = new ResponseVS(ResponseVS.SC_OK)
                    responseVS.setUserVS(anonymousSigner)
                } else {
                    responseVS = subscriptionVSService.checkUser(userVS, locale)
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
        return new ResponseVS(statusCode:ResponseVS.SC_OK, smimeMessage:messageWrapper,
                data:[checkedSigners:checkedSigners, checkedSigner:checkedSigner, anonymousSigner:anonymousSigner,
                      extensionChecker:extensionChecker])
    }
		
	public File getSignedFile (String fromUser, String toUser, String textToSign, String subject, Header header) {
		log.debug "getSignedFile - textToSign: ${textToSign}"
		MimeMessage mimeMessage = getSignedMailGenerator().genMimeMessage(
			fromUser, toUser, textToSign, subject, header)
		File resultFile = File.createTempFile("smime", "p7m");
		mimeMessage.writeTo(new FileOutputStream(resultFile));
		return resultFile
	}
		
	public SMIMEMessageWrapper getSMIMEMessage (String fromUser,String toUser,String textToSign,String subject,Header header) {
		log.debug "getSMIMEMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
		if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        SMIMEMessageWrapper mimeMessage = getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, header)
		return mimeMessage;
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
		return getSignedMailGenerator().genMultiSignedMessage(smimeMessage, subject);
	}

    public ResponseVS encryptMessage(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        log.debug("encryptMessage");
        try {
            return getEncryptor().encryptMessage(bytesToEncrypt, publicKey);
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
     * Method to encrypt SMIME signed messages
     */
    ResponseVS encryptSMIMEMessage(byte[] bytesToEncrypt, X509Certificate receiverCert, Locale locale) throws Exception {
        log.debug(" - encryptSMIMEMessage(...) ");
        try {
            return getEncryptor().encryptSMIMEMessage(bytesToEncrypt, receiverCert);
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS(message: messageSource.getMessage('dataToEncryptErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to decrypt SMIME signed messages
     */
    ResponseVS decryptSMIMEMessage(byte[] encryptedMessageBytes, Locale locale) {
        log.debug(" - decryptSMIMEMessage")
        try {
            return getEncryptor().decryptSMIMEMessage(encryptedMessageBytes);
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS(message:messageSource.getMessage('encryptedMessageErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

	public Set<X509Certificate> getTrustedCerts() {
		if(!trustedCerts || trustedCerts.isEmpty()) trustedCerts = init().trustedCerts
		return trustedCerts;
	}

    public Set<TrustAnchor> getTrustAnchors() {
        if(!trustAnchors) {
            Set<X509Certificate> trustedCerts = getTrustedCerts()
            trustAnchors = new HashSet<TrustAnchor>();
            for(X509Certificate certificate: trustedCerts) {
                TrustAnchor anchor = new TrustAnchor(certificate, null);
                trustAnchors.add(anchor);
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

	private SignedMailGenerator getSignedMailGenerator() {
        if(signedMailGenerator == null) signedMailGenerator = init().signedMailGenerator;
		return signedMailGenerator
	}
	
}