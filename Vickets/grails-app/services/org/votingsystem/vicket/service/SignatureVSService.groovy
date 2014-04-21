package org.votingsystem.vicket.service

import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.cms.CMSAlgorithm
import org.bouncycastle.cms.CMSEnvelopedData
import org.bouncycastle.cms.CMSEnvelopedDataParser
import org.bouncycastle.cms.CMSTypedStream
import org.bouncycastle.cms.RecipientInformation
import org.bouncycastle.cms.RecipientInformationStore
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator
import org.bouncycastle.jce.PKCS10CertificationRequest
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator
import org.bouncycastle.util.encoders.Base64
import org.votingsystem.callable.MessageTimeStamper
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
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.servlet.http.HttpServletRequest
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.CertPathValidatorException
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

class  SignatureVSService {
	
	private SignedMailGenerator signedMailGenerator;
	static Set<X509Certificate> trustedCerts;
	private KeyStore trustedCertsKeyStore
	static HashMap<Long, CertificateVS> trustedCertsHashMap;
	private X509Certificate localServerCertSigner;
    private PrivateKey serverPrivateKey;
    private CertificateVS serverCertificateVS
    private Encryptor encryptor;
	private static HashMap<Long, Set<TrustAnchor>> eventTrustedAnchorsHashMap =  new HashMap<Long, Set<TrustAnchor>>();
	private static HashMap<Long, Set<TrustAnchor>> controlCenterTrustedAnchorsHashMap =
            new HashMap<Long, Set<TrustAnchor>>();
	def grailsApplication;
	def messageSource
	def subscriptionVSService
	def timeStampService
	def sessionFactory
	
	public ResponseVS deleteTestCerts () {
		log.debug(" - deleteTestCerts - ")
        /*def d = new DefaultGrailsDomainClass(CertificateVS.class)
        d.persistentProperties.each { log.debug("============ ${it}") }*/
		int numTestCerts = CertificateVS.countByType(CertificateVS.Type.CERTIFICATE_AUTHORITY_TEST)
		log.debug(" - deleteTestCerts - numTestCerts: ${numTestCerts}")
		def testCerts = CertificateVS.createCriteria().scroll { eq("type", CertificateVS.Type.CERTIFICATE_AUTHORITY_TEST) }
		while (testCerts.next()) {
			CertificateVS cert = (CertificateVS) testCerts.get(0);
			int numCerts = CertificateVS.countByAuthorityCertificateVS(cert)
			def userCertCriteria = CertificateVS.createCriteria()
			def userTestCerts = userCertCriteria.scroll { eq("authorityCertificateVS", cert) }
			while (userTestCerts.next()) { 
				CertificateVS userCert = (CertificateVS) userTestCerts.get(0);
				userCert.delete()
				if((userTestCerts.getRowNumber() % 100) == 0) {
					sessionFactory.currentSession.flush()
					sessionFactory.currentSession.clear()
					log.debug(" - processed ${userTestCerts.getRowNumber()}/${numCerts} user certs from auth. cert ${cert.id}");
				}
			}
			CertificateVS.withTransaction { cert.delete() }
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK)
	}

	public synchronized Map init() throws Exception {
		log.debug(" - init - ")
        deleteTestCerts()
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
		trustedCerts = new HashSet<X509Certificate>()
		for (int i = 0; i < chain.length; i++) {
			log.debug "Adding local kesystore cert '${i}' -> 'SubjectDN: ${chain[i].getSubjectDN()}'"
			trustedCerts.add(chain[i])
			if(!pemCertsArray) pemCertsArray = CertUtil.getPEMEncoded (chain[i])
			else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtil.getPEMEncoded (chain[i]))
		}
		localServerCertSigner = (X509Certificate) keyStore.getCertificate(aliasClaves);

        serverCertificateVS = CertificateVS.findWhere(serialNumber:localServerCertSigner.getSerialNumber().longValue());
        if(!serverCertificateVS) {
            serverCertificateVS = new CertificateVS(isRoot:true, type:CertificateVS.Type.CERTIFICATE_AUTHORITY,
                    state:CertificateVS.State.OK, content:localServerCertSigner.getEncoded(),
                    serialNumber:localServerCertSigner.getSerialNumber().longValue(),
                    validFrom:localServerCertSigner.getNotBefore(), validTo:localServerCertSigner.getNotAfter()).save()
            log.debug "Adding local server cert to database  - serverCertificateVS: '${serverCertificateVS.id}'"
        }
        serverPrivateKey = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray())
		trustedCerts.add(localServerCertSigner)
		File certChainFile = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certChainPath).getFile();
		certChainFile.createNewFile()
		certChainFile.setBytes(pemCertsArray)
        encryptor = new Encryptor(localServerCertSigner, serverPrivateKey);
		initCertAuthorities();
        return [signedMailGenerator:signedMailGenerator, encryptor:encryptor, trustedCerts:trustedCerts,
                serverCertificateVS:serverCertificateVS,
                localServerCertSigner:localServerCertSigner, serverPrivateKey:serverPrivateKey];
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
	
	public Set<X509Certificate> getTrustedCerts() {
		if(!trustedCerts || trustedCerts.isEmpty()) trustedCerts = init().trustedCerts
		return trustedCerts;
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

	public KeyStore getTrustedCertsKeyStore() {
		if(!trustedCertsKeyStore ||
			trustedCertsKeyStore.size() != trustedCerts.size()) {
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
				long serialNumber = x509Certificate.getSerialNumber().longValue()
				log.debug "initCertAuthorities - checking - ${x509Certificate?.getSubjectDN()} --- serialNumber:${serialNumber}"
                CertificateVS certificate = null
				CertificateVS.withTransaction { certificate = CertificateVS.findBySerialNumber(serialNumber)}
				if(!certificate) {
					boolean isRoot = CertUtil.isSelfSigned(x509Certificate)
					certificate = new CertificateVS(isRoot:isRoot, type:CertificateVS.Type.CERTIFICATE_AUTHORITY,
						state:CertificateVS.State.OK, content:x509Certificate.getEncoded(), serialNumber:serialNumber,
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
			CertificateVS.withTransaction {
				def criteria = CertificateVS.createCriteria()
				def trustedCertsDB = criteria.list {
					eq("state", CertificateVS.State.OK)
					or {
						eq("type",	CertificateVS.Type.CERTIFICATE_AUTHORITY)
						if(EnvironmentVS.DEVELOPMENT  ==  ApplicationContextHolder.getEnvironment()) {
							eq("type", CertificateVS.Type.CERTIFICATE_AUTHORITY_TEST)
						}
					}
				}
				trustedCertsDB.each { certificate ->
					ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
					X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
					trustedCerts.add(certX509)
					trustedCertsHashMap.put(certX509?.getSerialNumber()?.longValue(), certificate)
				}
			}
			log.debug("trustedCerts.size(): ${trustedCerts?.size()}")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"CA Authorities initialized")
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage())
		}
	}
	
	def checkCancelledCerts () {
		log.debug "checkCancelledCerts - checkCancelledCerts"
		File directory = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.certAuthoritiesDirPath).getFile()
		String cancelSufix = "_CANCELLED"
		directory.eachFile() { file ->
			String fileName = file.getName().toUpperCase()
			if(fileName.endsWith(cancelSufix)) {
				int idx = fileName.indexOf(cancelSufix)
				fileName = fileName.substring(0, idx);
				if(fileName.endsWith("JKS")) {
					log.debug ("--- cancelando JKS -> " + fileName)
					KeyStore ks = KeyStore.getInstance("JKS");
					String password = grailsApplication.config.VotingSystem.signKeysPassword
					String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
					ks.load(new FileInputStream(file), password.toCharArray());
					java.security.cert.Certificate[] chain = ks.getCertificateChain(aliasClaves);				
					for (int i = 0; i < chain.length; i++) {
						X509Certificate cert = chain[i]
						cancelCert(cert.getSerialNumber().longValue())
					}
					file.delete();
				} else if (fileName.endsWith("PEM")) {
					log.debug ("--- cancelando PEM -> " + fileName)
					Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(
						FileUtils.getBytesFromFile(file))
					for (X509Certificate cert :certificates) {
						cancelCert(cert.getSerialNumber().longValue())
					}
					file.delete();
				}
			}
		}
	}
	
	private void cancelCert(long serialNumberCert) {
		log.debug "cancelCert - serialNumberCert: ${serialNumberCert}"
		CertificateVS.withTransaction {
			CertificateVS certificate = CertificateVS.findWhere(serialNumber:serialNumberCert)
			if(certificate) {
				log.debug "Comprobando certificateVS.id '${certificate?.id}'  --- "
				if(CertificateVS.State.OK == certificate.state) {
					certificate.cancelDate = new Date(System.currentTimeMillis());
					certificate.state = CertificateVS.State.CANCELLED;
					certificate.save()
					log.debug "cancelado certificateVS '${certificate?.id}'"
				} else log.debug "El certificateVS.id '${certificate?.id}' ya estaba cancelado"
			} else log.debug "No hay ningún certificateVS con num. serie '${serialNumberCert}'"
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
		
	public byte[] getSignedMimeMessage (String fromUser,String toUser,String textToSign,String subject, Header header) {
		log.debug "getSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
		if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		MimeMessage mimeMessage = getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, header)
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mimeMessage.writeTo(baos);
		baos.close();
		return baos.toByteArray();
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
	
	/*
	 * Método para poder añadir certificados de confianza en las pruebas de carga.
	 * El procedimiento para añadir una autoridad certificadora consiste en 
	 * añadir el certificado en formato pem en el directorio ./WEB-INF/cms
	 */
	public ResponseVS addCertificateAuthority (byte[] caPEM, Locale locale)  {
		log.debug("addCertificateAuthority");
		if(grails.util.Environment.PRODUCTION  ==  grails.util.Environment.current) {
			log.debug(" ### ADDING CERTS NOT ALLOWED IN PRODUCTION ENVIRONMENTS ###")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message: messageSource.getMessage('serviceDevelopmentModeMsg', null, locale))
		}
		if(!caPEM) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
			message: messageSource.getMessage('nullCertificateErrorMsg', null, locale))
		try {
			Collection<X509Certificate> certX509CertCollection = CertUtil.fromPEMToX509CertCollection(caPEM)
			for(X509Certificate cert: certX509CertCollection) {
				log.debug(" ------- addCertificateAuthority - adding cert: ${cert.getSubjectDN()}" + 
					" - serial number: ${cert.getSerialNumber()}");
				CertificateVS certificate = null
				CertificateVS.withTransaction {
					certificate = CertificateVS.findBySerialNumber(
						cert?.getSerialNumber()?.longValue())
					if(!certificate) {
						boolean isRoot = CertUtil.isSelfSigned(cert)
						certificate = new CertificateVS(isRoot:isRoot,
							type:CertificateVS.Type.CERTIFICATE_AUTHORITY_TEST,
							state:CertificateVS.State.OK,
							content:cert.getEncoded(),
							serialNumber:cert.getSerialNumber()?.longValue(),
							validFrom:cert.getNotBefore(),
							validTo:cert.getNotAfter())
						certificate.save()
						trustedCertsHashMap.put(cert?.getSerialNumber()?.longValue(), certificate)
					}
				}
				log.debug "Almacenada Autoridad Certificadora de pruebas con id:'${certificate?.id}'"
			}
            trustedCerts.addAll(certX509CertCollection)
			return new ResponseVS(statusCode:ResponseVS.SC_OK, 
				message:messageSource.getMessage('cert.newCACertMsg', null, locale))
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage())
		}
	}
	
	public CertificateVS getCACertificate(long serialNumber) {
		log.debug("getCACertificate - serialNumber: '${serialNumber}'")
		return trustedCertsHashMap.get(serialNumber)
	}
		
	public ResponseVS validateSMIME(SMIMEMessageWrapper messageWrapper, Locale locale) {
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:messageWrapper.getContentDigestStr())
		if(messageSMIME) {
			String message = messageSource.getMessage('smimeDigestRepeatedErrorMsg', 
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIME - ${message} - messageSMIME.id: ${messageSMIME.id}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:message)
		}
		return validateSignersCertificate(messageWrapper, locale)
	}
		
	public ResponseVS validateSignersCertificate(SMIMEMessageWrapper messageWrapper, Locale locale) {
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
                    log.debug("validateSignersCertificate - timestampValidationResp - " +
                            "statusCode:${timestampValidationResp.statusCode} - message:${timestampValidationResp.message}")
                    if(ResponseVS.SC_OK != timestampValidationResp.statusCode) {
                        log.error("validateSignersCertificate - TIMESTAMP ERROR - ${timestampValidationResp.message}")
                        return timestampValidationResp
                    }
                } else {
                    String msg = messageSource.getMessage('documentWithoutTimeStampErrorMsg', null, locale)
                    log.error("ERROR - validateSignersCertificate - ${msg}")
                    return new ResponseVS(message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST)
                }
				ResponseVS validationResponse = CertUtil.verifyCertificate(userVS.getCertificate(),
                        getTrustedCerts(), false)
				X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
				userVS.setCertificateCA(trustedCertsHashMap.get(certCaResult?.getSerialNumber()?.longValue()))
				log.debug("validateSignersCertificate - user cert issuer: " + certCaResult?.getSubjectDN()?.toString() +
                        " - issuer serialNumber: " + certCaResult?.getSerialNumber()?.longValue());
                extensionChecker = validationResponse.data.extensionChecker
                ResponseVS responseVS = null
                if(!extensionChecker.supportedExtensionsVS.isEmpty()) {
                    log.debug("validateSignersCertificate - anonymous signer")
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
                messageSMIME = new MessageSMIME(metaInf:certValidationResponse.message, type:TypeVS.ERROR,
                        content:smimeMessageReq.getBytes())
                MessageSMIME.withTransaction { messageSMIME.save() }
                log.error "*** Filter - processSMIMERequest - failed - status: ${certValidationResponse.statusCode}" +
                        " - message: ${certValidationResponse.message}"
                return certValidationResponse
            } else {
                messageSMIME = new MessageSMIME(signers:certValidationResponse.data?.checkedSigners,
                        anonymousSigner:certValidationResponse.data?.anonymousSigner,
                        userVS:certValidationResponse.data?.checkedSigner, smimeMessage:smimeMessageReq,
                        eventVS:certValidationResponse.eventVS, type:typeVS,
                        content:smimeMessageReq.getBytes(), base64ContentDigest:smimeMessageReq.getContentDigestStr())
                MessageSMIME.withTransaction {messageSMIME.save()}
            }
            return new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIME)
        } else if(smimeMessageReq) {
            log.error "**** Filter - processSMIMERequest - signature ERROR - "
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:messageSource.getMessage('signatureErrorMsg', null, locale))
        }
    }

	public ResponseVS validateSMIMEVicket(
		SMIMEMessageWrapper messageWrapper, Locale locale) {
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:messageWrapper.getContentDigestStr())
		if(messageSMIME) {
			String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIMEVicket - ${msg} - messageSMIME.id: ${messageSMIME.id}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
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
        log.debug("--- - encryptMessage(...) - ");
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
     * Method to decrypt files attached to SMIME (not signed) messages
     */
    /*public ResponseVS decryptCMS (byte[] encryptedFile, Locale locale) {
        log.debug " - decryptCMS"
        try {
            return getEncryptor().decryptCMS(serverPrivateKey, encryptedFile)
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS(message:messageSource.getMessage('encryptedMessageErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }*/



    public ResponseVS decryptCMS (byte[] base64EncryptedData, Locale locale) {
        log.debug "decryptCMS"
        byte[] cmsEncryptedData = Base64.decode(base64EncryptedData);
        CMSEnvelopedDataParser     ep = new CMSEnvelopedDataParser(cmsEncryptedData);
        RecipientInformationStore  recipients = ep.getRecipientInfos();
        Collection                 c = recipients.getRecipients();
        Iterator                   it = c.iterator();
        byte[] result = null;
        if (it.hasNext()) {
            RecipientInformation   recipient = (RecipientInformation)it.next();
            //assertEquals(recipient.getKeyEncryptionAlgOID(), PKCSObjectIdentifiers.rsaEncryption.getId());
            CMSTypedStream recData = recipient.getContentStream(new JceKeyTransEnvelopedRecipient(getServerPrivateKey()).setProvider(ContextVS.PROVIDER));
            InputStream           dataStream = recData.getContentStream();
            ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
            byte[]                buf = new byte[4096];
            int len = 0;
            while ((len = dataStream.read(buf)) >= 0) {
                dataOut.write(buf, 0, len);
            }
            dataOut.close();
            result = dataOut.toByteArray();
            return new ResponseVS(ResponseVS.SC_OK, result);
            //assertEquals(true, Arrays.equals(data, dataOut.toByteArray()));
        } else {
            String msg = messageSource.getMessage('encryptedMessageWithoutRecipientsErrorMsg', null, locale)
            log.error "decryptCMS - ${msg}"
            return new ResponseVS(ResponseVS.SC_ERROR_REQUEST, msg);
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
            return new ResponseVS(messageSource.getMessage('dataToEncryptErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to decrypt SMIME signed messages
     */
    ResponseVS decryptSMIMEMessage(byte[] encryptedMessageBytes, Locale locale) {
        log.debug(" - decryptSMIMEMessage ")
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