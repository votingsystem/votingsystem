package org.votingsystem.accesscontrol.service

import grails.transaction.Transactional
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.jce.PKCS10CertificationRequest
import org.votingsystem.callable.MessageTimeStamper
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.CertExtensionCheckerVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.StringUtils

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
	
	private SignedMailGenerator signedMailGenerator;
	private static Set<X509Certificate> trustedCerts;
    private static Set<TrustAnchor> trustAnchors;
	private KeyStore trustedCertsKeyStore
	static HashMap<Long, CertificateVS> trustedCertsHashMap;
    private java.security.cert.Certificate[] localServerCertChain
	private X509Certificate localServerCertSigner;
    private PrivateKey serverPrivateKey;
    private Encryptor encryptor;
	private static HashMap<Long, Set<TrustAnchor>> eventTrustedAnchorsMap =  new HashMap<Long, Set<TrustAnchor>>();
	def grailsApplication;
	def messageSource
	def subscriptionVSService
	def timeStampService
	def sessionFactory

	public synchronized Map init() throws Exception {
		log.debug(" - init - ")
		File keyStoreFile = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.keyStorePath).getFile()
		String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
		String password = grailsApplication.config.VotingSystem.signKeysPassword
		signedMailGenerator = new SignedMailGenerator(FileUtils.getBytesFromFile(keyStoreFile), 
			aliasClaves, password.toCharArray(), ContextVS.SIGN_MECHANISM);
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
		localServerCertChain = keyStore.getCertificateChain(aliasClaves);
		byte[] pemCertsArray
		trustedCerts = new HashSet<X509Certificate>()
		for (int i = 0; i < localServerCertChain.length; i++) {
			log.debug "Adding local kesystore cert '${i}' -> 'SubjectDN: ${localServerCertChain[i].getSubjectDN()}'"
			if(!pemCertsArray) pemCertsArray = CertUtil.getPEMEncoded (localServerCertChain[i])
			else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtil.getPEMEncoded (localServerCertChain[i]))
		}
		localServerCertSigner = (X509Certificate) keyStore.getCertificate(aliasClaves);
        serverPrivateKey = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray())
		File certChainFile = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certChainPath).getFile();
		certChainFile.createNewFile()
		certChainFile.setBytes(pemCertsArray)
        encryptor = new Encryptor(localServerCertSigner, serverPrivateKey);
		initCertAuthorities();
        return [signedMailGenerator:signedMailGenerator, encryptor:encryptor, trustedCerts:trustedCerts,
                localServerCertSigner:localServerCertSigner, serverPrivateKey:serverPrivateKey];
	}

    public X509Certificate getServerCert() {
        if(localServerCertSigner == null) localServerCertSigner = init().localServerCertSigner
        return localServerCertSigner
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

	public ResponseVS getEventTrustedCerts(EventVS event, Locale locale) {
		log.debug("getEventTrustedCerts")
		if(!event) return new ResponseVS(ResponseVS.SC_ERROR)
		CertificateVS eventVSCertificateVS = CertificateVS.findWhere(eventVSElection:event, state:CertificateVS.State.OK,
			type:CertificateVS.Type.VOTEVS_ROOT)
		if(!eventVSCertificateVS) {
			String msg = messageSource.getMessage('eventWithoutCAErrorMsg', [event.id].toArray(), locale)
			log.error ("getEventTrustedCerts - ERROR EVENT CA CERT -> '${msg}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,message:msg,
                    type:TypeVS.VOTE_ERROR, eventVS:event)
		}
		X509Certificate certCAEventVS = CertUtil.loadCertificateFromStream (
			new ByteArrayInputStream(eventVSCertificateVS.content))
		Set<X509Certificate> eventTrustedCerts = new HashSet<X509Certificate>()
		eventTrustedCerts.add(certCAEventVS)
		return new ResponseVS(statusCode:ResponseVS.SC_OK, data:eventTrustedCerts)
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

	public Set<TrustAnchor> getEventTrustedAnchors(EventVS eventVS, Locale locale) {
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
	
	public KeyStore getTrustedCertsKeyStore() {
		if(!trustedCertsKeyStore || trustedCertsKeyStore.size() != trustedCerts.size()) {
			trustedCertsKeyStore = KeyStore.getInstance("JKS");
			trustedCertsKeyStore.load(null, null);
			Set<X509Certificate> trustedCertsSet = getTrustedCerts()
			log.debug "trustedCerts.size: ${trustedCertsSet.size()}"
			for(X509Certificate certificate:trustedCertsSet) {
				trustedCertsKeyStore.setCertificateEntry(certificate.getSubjectDN().toString(), certificate);
			}
		}
		return trustedCertsKeyStore;
	}
	
	def initCertAuthorities() {
		try {
			trustedCertsHashMap = new HashMap<Long, CertificateVS>();
			File directory = grailsApplication.mainContext.getResource(
				grailsApplication.config.VotingSystem.certAuthoritiesDirPath).getFile()
			
			File[] acFiles = directory.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String fileName) {
					return fileName.startsWith("AC_") && fileName.endsWith(".pem");
				}
			  });
		    Set<X509Certificate> fileSystemCerts = new HashSet<X509Certificate>()
			for(File caFile:acFiles) {
				fileSystemCerts.addAll(CertUtil.fromPEMToX509CertCollection(FileUtils.getBytesFromFile(caFile)));
			}
			for(X509Certificate x509Certificate:fileSystemCerts) {
				long numSerie = x509Certificate.getSerialNumber().longValue()
				log.debug "initCertAuthorities - checking - ${x509Certificate?.getSubjectDN()} --- numSerie:${numSerie}"
                CertificateVS certificate = null
				CertificateVS.withTransaction { certificate = CertificateVS.findBySerialNumber(numSerie)}
				if(!certificate) {
					boolean isRoot = CertUtil.isSelfSigned(x509Certificate)
					certificate = new CertificateVS(isRoot:isRoot, type:CertificateVS.Type.CERTIFICATE_AUTHORITY,
						state:CertificateVS.State.OK, content:x509Certificate.getEncoded(), serialNumber:numSerie,
                        validFrom:x509Certificate.getNotBefore(), validTo:x509Certificate.getNotAfter())
					certificate.save()
					log.debug "initCertAuthorities - ADDED NEW CA CERT certificateVS.id:'${certificate?.id}'"
				} else {
					if(CertificateVS.State.OK != certificate.state) {
						log.error "File system athority cert '${x509Certificate?.getSubjectDN()}' " +
                                " with certificateVS.id:'${certificate?.id}' state is '${certificate.state}'"
					}
				}
			}
            loadCertAuthorities()
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"CA Authorities initialized")
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage())
		}
	}

    @Transactional public synchronized Map loadCertAuthorities() {
        trustedCertsHashMap = new HashMap<Long, CertificateVS>();
        trustedCerts = new HashSet<X509Certificate>();
        trustAnchors = new HashSet<TrustAnchor>();
        List<CertificateVS> trustedCertsList = CertificateVS.createCriteria().list(offset: 0) {
            eq("state", CertificateVS.State.OK)
            eq("type",	CertificateVS.Type.CERTIFICATE_AUTHORITY)
        }
        for (CertificateVS certificate : trustedCertsList) {
            X509Certificate x509Cert = certificate.getX509Cert()
            trustedCerts.add(x509Cert)
            trustedCertsHashMap.put(x509Cert.getSerialNumber()?.longValue(), certificate)
            trustAnchors.add(new TrustAnchor(x509Cert, null));
        }

        for (java.security.cert.Certificate certificate : localServerCertChain) {
            X509Certificate x509Cert = (X509Certificate)certificate
            trustedCerts.add(x509Cert)
            trustAnchors.add(new TrustAnchor(x509Cert, null));
        }

        log.debug("loadCertAuthorities - loaded '${trustedCertsHashMap?.keySet().size()}' authorities")
        return [trustedCertsHashMap: trustedCertsHashMap, trustedCerts:trustedCerts, trustAnchors:trustAnchors]
    }


    public Set<X509Certificate> getTrustedCerts() {
        if(!trustedCerts || trustedCerts.isEmpty()) trustedCerts = loadCertAuthorities().trustedCerts
        return trustedCerts;
    }

    public Set<TrustAnchor> getTrustAnchors() {
        if(!trustAnchors || trustAnchors.isEmpty()) trustAnchors = loadCertAuthorities().trustAnchors
        return trustAnchors;
    }

	private void cancelCert(long numSerieCert) {
		log.debug "cancelCert - numSerieCert: ${numSerieCert}"
		CertificateVS.withTransaction {
			CertificateVS certificate = CertificateVS.findWhere(serialNumber:numSerieCert)
			if(certificate) {
				log.debug "Comprobando certificateVS.id '${certificate?.id}'  --- "
				if(CertificateVS.State.OK == certificate.state) {
					certificate.cancelDate = new Date(System.currentTimeMillis());
					certificate.state = CertificateVS.State.CANCELLED;
					certificate.save()
					log.debug "cancelado certificateVS '${certificate?.id}'"
				} else log.debug "El certificateVS.id '${certificate?.id}' ya estaba cancelado"
			} else log.debug "No hay ningún certificateVS con num. serie '${numSerieCert}'"
		}
	}
	
	public File getSignedFile (String fromUser, String toUser,
		String textToSign, String subject, Header header) {
		log.debug "getSignedFile - textToSign: ${textToSign}"
		MimeMessage mimeMessage = getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, header)
		File resultFile = File.createTempFile("smime", "p7m");
		resultFile.deleteOnExit();
		mimeMessage.writeTo(new FileOutputStream(resultFile));
		return resultFile
	}
		
	public SMIMEMessageWrapper getSMIMEMessage (String fromUser,String toUser,String textToSign,String subject, Header header) {
		log.debug "getSMIMEMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
		if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        SMIMEMessageWrapper mimeMessage = getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, header)
		return mimeMessage;
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
	
	public CertificateVS getCACertificate(long numSerie) {
		log.debug("getCACertificate - numSerie: '${numSerie}'")
		return trustedCertsHashMap.get(numSerie)
	}
		
	public ResponseVS validateSMIME(SMIMEMessageWrapper messageWrapper, Locale locale) {
        MessageSMIME messageSMIME = null
        MessageSMIME.withTransaction {
            messageSMIME = MessageSMIME.findWhere(base64ContentDigest:messageWrapper.getContentDigestStr())
        }
		if(messageSMIME) {
			String message = messageSource.getMessage('smimeDigestRepeatedErrorMsg', 
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIME - ${message}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:message)
		}
		return validateSignersCerts(messageWrapper, locale)
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
                    return new ResponseVS(message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST)
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

	public ResponseVS validateSMIMEVote(SMIMEMessageWrapper smimeMessageReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:smimeMessageReq.getContentDigestStr())
		if(messageSMIME) {
			String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
				[smimeMessageReq.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIMEVote - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
        Set<UserVS> signersVS = smimeMessageReq.getSigners();
        String msg
        ResponseVS responseVS
        EventVSElection eventVS
        if(signersVS.isEmpty()) {
            msg = messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale)
            log.error ("$methodName - ERROR SIGNERS - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VOTE_ERROR)
        }
        VoteVS voteVS = smimeMessageReq.voteVS
        String localServerURL = grailsApplication.config.grails.serverURL
        String voteAccessControlURL = StringUtils.checkURL(voteVS.accessControlURL)
        if (!localServerURL.equals(voteAccessControlURL)) {
            msg = messageSource.getMessage('certVoteValidationErrorMsg',
                    [voteAccessControlURL, localServerURL].toArray(), locale)
            log.error ("$methodName - ERROR SERVER URL - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,type:TypeVS.VOTE_ERROR)
        }
        eventVS = EventVSElection.get(Long.valueOf(voteVS.getEventVS().getId()))
        if (!eventVS)  {
            msg = messageSource.getMessage('electionNotFound', [voteVS.getEventVS().getId()].toArray(), locale)
            log.error ("$methodName - ERROR EVENT NOT FOUND - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VOTE_ERROR)
        }
        if(eventVS.state != EventVS.State.ACTIVE) {
            msg = messageSource.getMessage('electionClosed', [eventVS.subject].toArray(), locale)
            log.error ("$methodName - ERROR EVENT '${eventVS.id}' STATE -> ${eventVS.state}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.VOTE_ERROR, eventVS:eventVS)
        }
        CertificateVS certificate = CertificateVS.findWhere(hashCertVSBase64:voteVS.hashCertVSBase64,
                state:CertificateVS.State.OK)
        if (!certificate) {
            msg = messageSource.getMessage('hashVoteValidationErrorMsg', [voteVS.hashCertVSBase64].toArray(), locale)
            log.error ("$methodName - ERROR CERT '${msg}'")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.VOTE_ERROR, eventVS:eventVS)
        }
        smimeMessageReq.voteVS.setCertificateVS(certificate)
        Set<TrustAnchor> trustedAnchors = getEventTrustedAnchors(eventVS, locale)
        ResponseVS validationResponse;
        X509Certificate certCaResult;
        X509Certificate checkedCert = voteVS.getX509Certificate()
        try {
            validationResponse = CertUtil.verifyCertificate(trustedAnchors, false, [checkedCert])
            certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
            log.debug("$methodName - vote cert -> CA Result: " + certCaResult?.getSubjectDN()?.toString()+
                    "- serialNumber: " + certCaResult?.getSerialNumber()?.longValue());
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex)
            msg = messageSource.getMessage('certValidationErrorMsg',
                    [checkedCert.getSubjectDN()?.toString()].toArray(), locale)
            log.error ("$methodName - ERROR VOTE CERT VALIDATION -> '${msg}'")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:msg, type:TypeVS.VOTE_ERROR, eventVS:eventVS)
        }
        Date signatureTime = voteVS.getTimeStampToken()?.getTimeStampInfo().getGenTime()
        if(!eventVS.isActive(signatureTime)) {
            msg = messageSource.getMessage("checkedDateRangeErrorMsg", [signatureTime,
                        eventVS.getDateBegin(), eventVS.getDateFinish()].toArray(), locale)
            log.error(msg)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.VOTE_ERROR,
                    message:msg, eventVS:eventVS)
        }
        if(voteVS.getServerCerts().isEmpty()) {//check control center certificate
            msg = messageSource.getMessage('controlCenterMissingSignatureErrorMsg', null, locale)
            log.error(" ERROR - MISSING CONTROL CENTER SIGNATURE - msg: ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.VOTE_ERROR, eventVS:eventVS)
        }
        checkedCert = voteVS.getServerCerts()?.iterator()?.next()
        try {
            validationResponse = CertUtil.verifyCertificate(trustedAnchors, false, [checkedCert])
            certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
            log.debug("$methodName - Control Center cert -> CA Result: " + certCaResult?.getSubjectDN()?.toString() +
                    "- numserie: " + certCaResult?.getSerialNumber()?.longValue());
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex)
            msg = messageSource.getMessage('certValidationErrorMsg',
                    [checkedCert.getSubjectDN()?.toString()].toArray(), locale)
            log.error ("$methodName - ERROR CONTROL CENTER CERT VALIDATION -> '${msg}'")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:msg, type:TypeVS.VOTE_ERROR, eventVS:eventVS)
        }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS,
                smimeMessage:smimeMessageReq, type:TypeVS.CONTROL_CENTER_VALIDATED_VOTE)
	}

    public ResponseVS encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert) throws Exception {
        log.debug("encryptToCMS ${new String(dataToEncrypt)}")
        return getEncryptor().encryptToCMS(dataToEncrypt, receiverCert);
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