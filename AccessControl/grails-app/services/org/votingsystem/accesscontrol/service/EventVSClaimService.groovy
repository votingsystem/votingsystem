package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.EventVS
import org.votingsystem.model.SignatureVS
import org.votingsystem.model.EventVSClaim
import org.votingsystem.model.FieldEventVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TagVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils

import java.security.cert.X509Certificate
import java.text.DecimalFormat
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class EventVSClaimService {
		
    static transactional = true

    def tagVSService
    def signatureVSService
    def eventVSService
    def grailsApplication
	def messageSource
	def filesService
	def timeStampVSService
	def sessionFactory

    ResponseVS saveEvent(MessageSMIME messageSMIMEReq, Locale locale) {
		EventVSClaim eventVS
		try {
			UserVS signerVS = messageSMIMEReq.getUserVS()
			String documentStr = messageSMIMEReq.getSmimeMessage().getSignedContent()
			log.debug("saveEvent - signerVS: ${signerVS.nif} - documentStr: ${documentStr}")
			def messageJSON = JSON.parse(documentStr)
			Date dateFinish = new Date().parse("yyyy/MM/dd HH:mm:ss", messageJSON.dateFinish)
			if(dateFinish.before(DateUtils.getTodayDate())) {
				String msg = messageSource.getMessage(
					'publishDocumentDateErrorMsg', 
					[DateUtils.getStringFromDate(dateFinish)].toArray(), locale)
				log.error("DATE ERROR - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
					message:msg, type:TypeVS.CLAIM_EVENT_ERROR, eventVS:eventVS)
			}
			eventVS = new EventVSClaim(userVS:signerVS, subject:messageJSON.subject, content:messageJSON.content,
					backupAvailable:messageJSON.backupAvailable, dateFinish:dateFinish)
			if(messageJSON.cardinality) eventVS.cardinality =
				EventVS.Cardinality.valueOf(messageJSON.cardinality)
			else eventVS.cardinality = EventVS.Cardinality.EXCLUSIVE
			if(messageJSON.dateBegin) eventVS.dateBegin = new Date().parse(
                    "yyyy/MM/dd HH:mm:ss", messageJSON.dateBegin)
			else eventVS.dateBegin = DateUtils.getTodayDate();
			ResponseVS responseVS = eventVSService.setEventDatesState(eventVS, locale)
			if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
			eventVS = responseVS.eventVS.save()
			if (messageJSON.tags) {
				Set<TagVS> tagSet = tagVSService.save(messageJSON.tags)
				eventVS.setTagVSSet(tagSet)
			}
			messageJSON.id = eventVS.id
			messageJSON.dateCreated = DateUtils.getStringFromDate(eventVS.dateCreated)
			messageJSON.type = TypeVS.CLAIM_EVENT
			JSONArray arrayCampos = new JSONArray()
			messageJSON.fieldsEventVS?.each { campoItem ->
				def campo = new FieldEventVS(eventVS:eventVS, content:campoItem.content)
				campo.save();
				arrayCampos.add(new JSONObject([id:campo.id, content:campo.content]))
			}
			messageJSON.accessControl = [serverURL:grailsApplication.config.grails.serverURL,
				name:grailsApplication.config.VotingSystem.serverName]  as JSONObject
			messageJSON.fieldsEventVS = arrayCampos
			
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = signerVS.getNif()
			String subject = messageSource.getMessage(
					'mime.subject.claimEventValidated', null, locale)
			byte[] smimeMessageRespBytes = signatureVSService.getSignedMimeMessage(
				fromUser, toUser,  messageJSON.toString(), subject, null)
			MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIMEReq,
                    eventVS:eventVS, content:smimeMessageRespBytes)
			MessageSMIME.withTransaction { messageSMIMEResp.save() }
			return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS, data:messageSMIMEResp,
                    type:TypeVS.CLAIM_EVENT)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, type:TypeVS.CLAIM_EVENT_ERROR, eventVS:eventVS,
				message:messageSource.getMessage('publishClaimErrorMessage', null, locale))
		}
    }

    public synchronized ResponseVS generarCopiaRespaldo (EventVSClaim event, Locale locale) {
        log.debug("generarCopiaRespaldo - eventId: ${event.id}")
		ResponseVS responseVS;
        if (!event) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
				messageSource.getMessage('requestWithoutEventVS', null, locale))
        }		
		Map<String, File> mapFiles = filesService.getBackupFiles(
			event, TypeVS.CLAIM_EVENT, locale)
		File metaInfFile = mapFiles.metaInfFile
		File filesDir = mapFiles.filesDir
		File zipResult   = mapFiles.zipResult
		
		String serviceURLPart = messageSource.getMessage(
			'claimsBackupPartPath', [event.id].toArray(), locale)
		String datePathPart = DateUtils.getShortStringFromDate(event.getDateFinish())
		String backupURL = "/backup/${datePathPart}/${serviceURLPart}.zip"
		String webappBackupPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${backupURL}"
		
		if(zipResult.exists()) {
			log.debug("generarCopiaRespaldo - backup file already exists")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:backupURL)
		}		
		
		int numSignatures = SignatureVS.countByEvento(event)
		def backupMetaInfMap = [numSignatures:numSignatures]
		Map eventMetaInfMap =  eventVSService.getMetaInfMap(event)
		eventMetaInfMap.put(TypeVS.BACKUP.toString(), backupMetaInfMap);
		event.metaInf = eventMetaInfMap as JSON
		EventVS.withTransaction {
			event.save()
		}
		metaInfFile.write((eventMetaInfMap as JSON).toString())
		
		String fileNamePrefix = messageSource.getMessage('claimLbl', null, locale);
		
		DecimalFormat formatted = new DecimalFormat("00000000");
		int claimsBatch = 0;
		String baseDir="${filesDir.absolutePath}/batch_${formatted.format(++claimsBatch)}"
		new File(baseDir).mkdirs()
		
		long begin = System.currentTimeMillis()
		SignatureVS.withTransaction {
			def criteria = SignatureVS.createCriteria()
			def firmasRecibidas = criteria.scroll {
				eq("eventVS", event)
				eq("type", TypeVS.CLAIM_EVENT_SIGN)
			}
			
			
			
			while (firmasRecibidas.next()) {
				SignatureVS firma = (SignatureVS) firmasRecibidas.get(0);
				MessageSMIME messageSMIME = firma.messageSMIME
				File smimeFile = new File("${baseDir}/${fileNamePrefix}_${formatted.format(firmasRecibidas.getRowNumber())}.p7m")
				smimeFile.setBytes(messageSMIME.content)
				if((firmasRecibidas.getRowNumber() % 100) == 0) {
					String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
						System.currentTimeMillis() - begin)
					log.debug(" - accessRequest ${firmasRecibidas.getRowNumber()} of ${numSignatures} - ${elapsedTimeStr}");
					sessionFactory.currentSession.flush()
					sessionFactory.currentSession.clear()
				}
				if(((firmasRecibidas.getRowNumber() + 1) % 2000) == 0) {
					baseDir="${filesDir.absolutePath}/batch_${formatted.format(++claimsBatch)}"
					new File(baseDir).mkdirs()
				}
			}
		}	

		Set<X509Certificate> systemTrustedCerts = signatureVSService.getTrustedCerts()
		byte[] systemTrustedCertsPEMBytes = CertUtil.getPEMEncoded(systemTrustedCerts)
		File systemTrustedCertsFile = new File("${filesDir.absolutePath}/systemTrustedCerts.pem")
		systemTrustedCertsFile.setBytes(systemTrustedCertsPEMBytes)
		
		byte[] timeStampCertPEMBytes = timeStampVSService.getSigningCert()
		File timeStampCertFile = new File("${filesDir.absolutePath}/timeStampCert.pem")
		timeStampCertFile.setBytes(timeStampCertPEMBytes)
		
		def ant = new AntBuilder()
		ant.zip(destfile: zipResult, basedir: filesDir) {
			fileset(dir:"${filesDir}/..", includes: "meta.inf")
		}
		ant.copy(file: zipResult, tofile: webappBackupPath)

		return new ResponseVS(statusCode:ResponseVS.SC_OK,
			type:TypeVS.CLAIM_EVENT, message:backupURL)
    }

}