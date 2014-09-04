package org.votingsystem.controlcenter.service

import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.CertExtensionCheckerVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.util.ApplicationContextHolder
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

class SignatureVSService {
	
	static transactional = false
	
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
	private static HashMap<Long, Set<X509Certificate>> eventTrustedCertsHashMap = 
			new HashMap<Long, Set<X509Certificate>>();
	private static HashMap<Long, Set<X509Certificate>> eventValidationTrustedCertsHashMap =
			new HashMap<Long, Set<X509Certificate>>();
			

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
				def criteria = CertificateVS.createCriteria()
				def trustedCertsDB = criteria.list {
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
				log.debug "initCertAuthorities - SubjectDN: ${x509Certificate?.getSubjectDN()} - " +
                        " - numSerie:${x509Certificate?.getSerialNumber()?.longValue()}"
				CertificateVS certificate
				CertificateVS.withTransaction {
					certificate = CertificateVS.findBySerialNumber(x509Certificate.getSerialNumber().longValue())
				}
				if(!certificate) {
					certificate = new CertificateVS(isRoot:CertUtil.isSelfSigned(x509Certificate),
						type:CertificateVS.Type.CERTIFICATE_AUTHORITY, state:CertificateVS.State.OK,
						content:x509Certificate.getEncoded(), serialNumber:x509Certificate.getSerialNumber().longValue(),
						validFrom:x509Certificate.getNotBefore(), validTo:x509Certificate.getNotAfter())
					CertificateVS.withTransaction {
						certificate.save()
					}
                    log.debug "initCertAuthorities - ADDED NEW CA CERT certificateVS.id:'${certificate?.id}'"
				} else log.debug "initCertAuthorities -- CA: ${certificate.getSerialNumber().longValue()} --- " +
                        " - database id: ${certificate?.id}"
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
	
	public ResponseVS validateSMIMEVote(SMIMEMessageWrapper messageWrapper, Locale locale) {
		log.debug("validateSMIMEVote -")
        MessageSMIME messageSMIME = null
        MessageSMIME.withTransaction {
            messageSMIME = MessageSMIME.findWhere(base64ContentDigest:messageWrapper.getContentDigestStr())
        }
		if(messageSMIME) {
			String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIMEVote - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
		return validateVoteCerts(messageWrapper, locale)
	}

	public ResponseVS validateSMIMEVoteCancelation(String url, SMIMEMessageWrapper messageWrapper, Locale locale) {
		log.debug("validateSMIMEVoteCancelation - url: ${url}")
		EventVS eventVS = null
        EventVS.withTransaction { eventVS = EventVS.findByUrl(url) }
		if(!eventVS) {
			String msg = messageSource.getMessage('eventVSNotFound', [url].toArray(), locale)
			log.error("validateSMIMEVoteCancelation - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:messageWrapper.getContentDigestStr())
		if(messageSMIME) {
			String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIMEVoteCancelation - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
		return validateVoteValidationCerts(messageWrapper,	eventVS, locale)
	}

	public ResponseVS validateVoteCerts(SMIMEMessageWrapper smimeMessageReq, Locale locale) {
		String msg
		ResponseVS responseVS
		EventVS eventVS
		VoteVS voteVS = smimeMessageReq.voteVS
        UserVS checkedSigner = null
		if(!voteVS?.getX509Certificate()) {
			msg = messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale)
			log.error ("validateVoteCerts - ERROR SIGNERS - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.VOTE_ERROR, message:msg)
		} 
		if (voteVS.getRepresentativeURL()) {
            checkedSigner = UserVS.findWhere(url:voteVS.getRepresentativeURL())
			if(!checkedSigner) {
                checkedSigner = new UserVS(url:voteVS.getRepresentativeURL(), nif:voteVS.getRepresentativeURL(),
                        type:UserVS.Type.REPRESENTATIVE)
                checkedSigner.save()
			}
		}
		String accessControlURL = voteVS.accessControlURL
		AccessControlVS accessControl = AccessControlVS.findWhere(serverURL:accessControlURL)
		if (!accessControl) {
			msg = messageSource.getMessage('voteAccessControlUnknownErrorMsg', null, locale)
			log.error ("validateVoteCerts - ERROR SERVER URL - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VOTE_ERROR)
		} 
		EventVS.withTransaction {
			eventVS = EventVS.findWhere(accessControlVS:accessControl, accessControlEventVSId:voteVS.getEventVS().getId())
		}
		if (!eventVS) {
			msg = messageSource.getMessage('voteEventVSElectionUnknownErrorMsg', null, locale)
			log.error ("validateVoteCerts - ERROR EVENT NOT FOUND - Event '${voteVS?.getEventVS()?.getId()}' " +
				"voteAccessControlURL: '${accessControlURL}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VOTE_ERROR)
		}
		if(eventVS.state != EventVS.State.ACTIVE) {
			msg = messageSource.getMessage('electionClosed', [eventVS.subject].toArray(), locale)
			log.error ("validateVoteCerts - ERROR EVENT '${eventVS.id}' STATE -> ${eventVS.state}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VOTE_ERROR)
		}
		CertificateVS certificateVS = CertificateVS.findWhere(hashCertVSBase64:voteVS.hashCertVSBase64)
		if (certificateVS) {
			log.error("validateVoteCerts - repeated vote - hashCertVSBase64:${voteVS.hashCertVSBase64}")
			VoteVS repeatedVoteVS = VoteVS.findWhere(certificateVS:certificateVS)
			msg = messageSource.getMessage('voteRepeatedErrorMsg', [repeatedVoteVS.id].toArray(), locale)
			log.error("validateVoteCerts - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST_REPEATED, eventVS:eventVS, message:msg)
		}
		Set<X509Certificate> eventTrustedCerts = eventTrustedCertsHashMap.get(eventVS?.id)
		if(!eventTrustedCerts) {
			CertificateVS eventCACert
			CertificateVS.withTransaction {
				eventCACert = CertificateVS.findWhere(eventVSElection:eventVS, state:CertificateVS.State.OK,
					type:CertificateVS.Type.VOTEVS_ROOT)
			}
			if(!eventCACert) {
				msg = messageSource.getMessage('eventVSElectionCertNotFound', null, locale)
				log.error ("validateVoteCerts - EventVS '${eventVS.id}' without CA cert")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
			} 
			ByteArrayInputStream bais =	new ByteArrayInputStream(eventCACert.content)
			X509Certificate certCAEventVS = CertUtil.loadCertificateFromStream (bais)
			eventTrustedCerts = new HashSet<X509Certificate>()
			eventTrustedCerts.add(certCAEventVS)
			eventTrustedCertsHashMap.put(eventVS.id, eventTrustedCerts)
		}
		responseVS = timeStampService.validateToken(voteVS.getTimeStampToken(), eventVS, locale)
		if(ResponseVS.SC_OK != responseVS.statusCode) {
			responseVS.type = TypeVS.VOTE_ERROR
			responseVS.eventVS = eventVS
			return responseVS
		}
		X509Certificate checkedCert = voteVS.getX509Certificate()
		log.debug("validateVoteCerts - vote cert: ${checkedCert.getSubjectDN()}")
		try {
			ResponseVS validationResponse = CertUtil.verifyCertificate(checkedCert, eventTrustedCerts, false)
			X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
			log.debug("validateVoteCerts - certCaResult: " + certCaResult?.getSubjectDN()?.toString()+
					"- serialNumber: " + certCaResult?.getSerialNumber()?.longValue());
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('certValidationErrorMsg',
                    [checkedCert.getSubjectDN()?.toString()].toArray(), locale)
			log.error ("validateVoteCerts - msg: ${msg} - EventVS '${eventVS.id}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS, smimeMessage:smimeMessageReq,
                data:[checkedSigner:checkedSigner])
	}

	public ResponseVS validateVoteValidationCerts(SMIMEMessageWrapper smimeMessageReq, EventVS eventVS, Locale locale) {
		log.debug("validateVoteValidationCerts");
		Set<UserVS> signersVS = smimeMessageReq.getSigners();
		String msg
		if(signersVS.isEmpty()) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageSource.
					getMessage('documentWithoutSignersErrorMsg', null, locale))
		}
		Set<X509Certificate> eventTrustedCerts = eventValidationTrustedCertsHashMap.get(eventVS?.id)
		if(!eventTrustedCerts) {
			eventTrustedCerts = new HashSet<X509Certificate>()
			Collection<X509Certificate> accessControlCerts = CertUtil.
				fromPEMToX509CertCollection (eventVS.certChainAccessControl)
			Collection<X509Certificate> controlCenterCerts = CertUtil.fromPEMToX509CertCollection (serverCertChainFile.getBytes())
			eventTrustedCerts.addAll(accessControlCerts)
			eventTrustedCerts.addAll(controlCenterCerts)
			eventTrustedCerts.addAll(eventTrustedCertsHashMap.get(eventVS?.id))
			eventTrustedCertsHashMap.put(eventVS.id, eventTrustedCerts)
		}
		log.debug("validateAccessControlVoteCerts - num. signers: ${signersVS.size()} " +
			    " - num. eventVS trusted certs: ${eventTrustedCerts.size()}")
		for(UserVS userVS: signersVS) {
			log.debug("validateAccessControlVoteCerts - validating signer: ${userVS.getCertificate().getSubjectDN()}")
			try {
				ResponseVS validationResponse = CertUtil.verifyCertificate(
                        userVS.getCertificate(), eventTrustedCerts, false)
                X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
				log.debug("validateAccessControlVoteCerts - certCaResult: " + certCaResult?.getSubjectDN()?.toString()+
						"- serialNumber: " + certCaResult?.getSerialNumber()?.longValue());
			} catch (Exception ex) {
				log.error(ex.getMessage(), ex)
				msg = messageSource.getMessage('certValidationErrorMsg',
						[userVS.getCertificate().getSubjectDN()?.toString()].toArray(), locale)
				log.error ("validateAccessControlVoteCerts - msg:{msg} - EventVS '${eventVS.id}'")
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
		
	public File getSignedFile (String fromUser, String toUser,
		String textToSign, String subject, Header header) {
		log.debug "getSignedFile - textToSign: ${textToSign}"
		MimeMessage mimeMessage = getSignedMailGenerator().genMimeMessage(
			fromUser, toUser, textToSign, subject, header)
		File resultFile = File.createTempFile("smime", "p7m");
		mimeMessage.writeTo(new FileOutputStream(resultFile));
		return resultFile
	}
		
	public byte[] getSignedMimeMessage (String fromUser,String toUser,String textToSign,String subject,Header header) {
		log.debug "getSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
		if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		MimeMessage mimeMessage = getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, header)
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mimeMessage.writeTo(baos);
		baos.close();
		return baos.toByteArray();
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
        log.debug("--- - encryptMessage(...) - ");
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
        log.debug " - decryptMessage"
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