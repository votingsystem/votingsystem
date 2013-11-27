package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.AccessRequestVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.FieldEventVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TagVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VoteVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils

import javax.mail.Header
import java.security.cert.X509Certificate
import java.text.DecimalFormat
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class EventVSElectionService {

    def tagVSService
    def subscriptionVSService
    def fieldEventVSService
    def signatureVSService
    def eventVSService
    def grailsApplication
	def keyStoreService
	def httpService
	def messageSource
	def encryptionService
	def representativeService
	def filesService
	def timeStampVSService
	def sessionFactory

    ResponseVS saveEvent(MessageSMIME messageSMIMEReq, Locale locale) {
		EventVSElection eventVS = null
		UserVS userSigner = messageSMIMEReq.getUserVS()
		log.debug("saveEvent - signer: ${userSigner?.nif}")
		String msg = null
		ResponseVS responseVS = null
		try {		
			String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
			def messageJSON = JSON.parse(documentStr)
			if (!messageJSON.controlCenter || !messageJSON.controlCenter.serverURL) {
				msg = messageSource.getMessage('error.requestWithoutControlCenter', null, locale)
				log.error "saveEvent - DATA ERROR - ${msg} - messageJSON: ${messageJSON}" 
				return new ResponseVS(type:TypeVS.VOTING_EVENT_ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
			}
			eventVS = new EventVSElection(subject:messageJSON.subject, content:messageJSON.content, userVS:userSigner,
					dateBegin: new Date().parse("yyyy/MM/dd HH:mm:ss", messageJSON.dateBegin),
					dateFinish: new Date().parse("yyyy/MM/dd HH:mm:ss", messageJSON.dateFinish))
			responseVS = subscriptionVSService.checkControlCenter(messageJSON.controlCenter.serverURL, locale)
			if(ResponseVS.SC_OK != responseVS.statusCode) {
				log.error "saveEvent - CHECKING CONTROL CENTER ERROR - ${responseVS.message}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:responseVS.message,
                        type:TypeVS.VOTING_EVENT_ERROR)
			}  
			eventVS.controlCenterVS = responseVS.data.controlCenterVS
            messageJSON.controlCenter.id = eventVS.controlCenterVS.id
            eventVS.certChainControlCenter = responseVS.data.certificateVS.certChainPEM
            ByteArrayInputStream bais = new ByteArrayInputStream(responseVS.data.certificateVS.content)
			X509Certificate x509ControlCenterCert = CertUtil.loadCertificateFromStream (bais)
			responseVS = eventVSService.setEventDatesState(eventVS,locale)
			if(ResponseVS.SC_OK != responseVS.statusCode) {
				log.error "saveEvent - EVENT DATES ERROR - ${responseVS.message}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:responseVS.message,
                        type:TypeVS.VOTING_EVENT_ERROR)
			}
			eventVS.cardinality = EventVS.Cardinality.EXCLUSIVE
			messageJSON.accessControl = [serverURL:grailsApplication.config.grails.serverURL,
				name:grailsApplication.config.VotingSystem.serverName] as JSONObject
			if (messageJSON.tags) {
				Set<TagVS> tagSet = tagVSService.save(messageJSON.tags)
				if(tagSet) eventVS.setTagVSSet(tagSet)
			}
			EventVSElection.withTransaction { eventVS.save() }
			if (messageJSON.fieldsEventVS) {
				Set<FieldEventVS> fieldsEventVS = fieldEventVSService.saveFieldsEventVS(eventVS, messageJSON.fieldsEventVS)
				JSONArray arrayFieldsEventVS = new JSONArray()
				fieldsEventVS.each { opcion ->
						arrayFieldsEventVS.add([id:opcion.id, content:opcion.content] as JSONObject  )
				}
				messageJSON.fieldsEventVS = arrayFieldsEventVS
			}
			log.debug(" ------ Saved voting event '${eventVS.id}'")
			messageJSON.id = eventVS.id
			messageJSON.URL = "${grailsApplication.config.grails.serverURL}/eventVSElection/${eventVS.id}"
			messageJSON.dateCreated = DateUtils.getStringFromDate(eventVS.dateCreated)
			messageJSON.type = TypeVS.VOTING_EVENT
			responseVS = keyStoreService.generateElectionKeysStore(eventVS)
			if(ResponseVS.SC_OK != responseVS.statusCode) {
				log.error "saveEvent - ERROR GENERATING EVENT KEYSTRORE- ${responseVS.message}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:responseVS.message,
                        type:TypeVS.VOTING_EVENT_ERROR, eventVS:eventVS)
			} 
			messageJSON.certCAVotacion = new String(CertUtil.getPEMEncoded (responseVS.data))
			File certChain = grailsApplication.mainContext.getResource(
				grailsApplication.config.VotingSystem.certChainPath).getFile();
			messageJSON.certChain = new String(certChain.getBytes())
			
			X509Certificate certUsuX509 = userSigner.getCertificate()
			messageJSON.userVS = new String(CertUtil.getPEMEncoded (certUsuX509))
			String controlCenterEventsURL = "${eventVS.controlCenterVS.serverURL}/eventVSElection"

			Header header = new Header ("serverURL", "${grailsApplication.config.grails.serverURL}");
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = eventVS.controlCenterVS.getName()
			String subject = messageSource.getMessage('mime.subject.votingEventValidated', null, locale)
			byte[] smimeMessageRespBytes = signatureVSService.getSignedMimeMessage(
				fromUser, toUser, messageJSON.toString(), subject, header)
			ResponseVS encryptResponse = encryptionService.encryptSMIMEMessage(smimeMessageRespBytes,
                    x509ControlCenterCert, locale)
			if(ResponseVS.SC_OK != encryptResponse.statusCode) {
				eventVS.state = EventVS.State.ERROR
                eventVS.metaInf = encryptResponse.message
				EventVS.withTransaction { eventVS.save() }
				log.error "saveEvent - ERROR ENCRYPTING MSG - ${encryptResponse.message}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:encryptResponse.message,
                        eventVS:eventVS, type:TypeVS.VOTING_EVENT_ERROR)
			}
			ResponseVS responseVSNotificacion = httpService.sendMessage(
				encryptResponse.messageBytes, ContentTypeVS.SIGNED_AND_ENCRYPTED, controlCenterEventsURL)
			if(ResponseVS.SC_OK != responseVSNotificacion.statusCode) {
				eventVS.state = EventVS.State.ERROR
                eventVS.metaInf = responseVSNotificacion.message
				EventVS.withTransaction { eventVS.save() }
				msg = messageSource.getMessage('controlCenterCommunicationErrorMsg',
					[responseVSNotificacion.message].toArray(), locale)	
				log.error "saveEvent - ERROR NOTIFYING CONTROL CENTER - ${msg}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:msg, type:TypeVS.VOTING_EVENT_ERROR, eventVS:eventVS)
			}
			MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT,
				smimeParent:messageSMIMEReq, eventVS:eventVS,  content:smimeMessageRespBytes)
			MessageSMIME.withTransaction {
				messageSMIMEResp.save()
			}
			return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS,
					type:TypeVS.VOTING_EVENT, data:messageSMIMEResp)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			msg = messageSource.getMessage('publishVotingErrorMessage', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
				message:msg, type:TypeVS.VOTING_EVENT_ERROR, eventVS:eventVS)
		}
    }
    
	public synchronized ResponseVS generarCopiaRespaldo (EventVSElection eventVS, Locale locale) {
		log.debug("generarCopiaRespaldo - eventVSId: ${eventVS.id}")
		ResponseVS responseVS;
		String msg = null
		try {
			if (eventVS.isActive(DateUtils.getTodayDate())) {
				msg = messageSource.getMessage('eventDateNotFinished', null, locale)
				String currentDateStr = DateUtils.getStringFromDate(
					new Date(System.currentTimeMillis()))
				log.error("generarCopiaRespaldo - DATE ERROR  ${msg} - " + 
					"fecha actual '${currentDateStr}' fecha final eventVS '${eventVS.dateFinish}'")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.BACKUP_ERROR)
			}
			
			Map<String, File> mapFiles = filesService.getBackupFiles(eventVS, TypeVS.VOTING_EVENT, locale)
			File zipResult   = mapFiles.zipResult
			File metaInfFile = mapFiles.metaInfFile
			File filesDir    = mapFiles.filesDir
			
			String serviceURLPart = messageSource.getMessage(
				'votingBackupPartPath', [eventVS.id].toArray(), locale)
			String datePathPart = DateUtils.getShortStringFromDate(eventVS.getDateFinish())
			String backupURL = "/backup/${datePathPart}/${serviceURLPart}.zip"
			String webappBackupPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${backupURL}"
			
			if(zipResult.exists()) {
				log.debug("generarCopiaRespaldo - backup file already exists")
				return new ResponseVS(statusCode:ResponseVS.SC_OK, message:backupURL)
			}
			responseVS = representativeService.getAccreditationsBackupForEvent(eventVS, locale)
			Map representativeDataMap = responseVS.data
			if(ResponseVS.SC_OK != responseVS.statusCode) {
				log.error("generarCopiaRespaldo - REPRESENTATIVE DATA GEN ERROR  ${responseVS.message}")
				return responseVS
			}			
			
			responseVS = signatureVSService.getEventTrustedCerts(eventVS, locale)
			if(ResponseVS.SC_OK != responseVS.statusCode) {
				responseVS.type = TypeVS.BACKUP_ERROR
				return responseVS
			}
			
			Set<X509Certificate> systemTrustedCerts = signatureVSService.getTrustedCerts()
			byte[] systemTrustedCertsPEMBytes = CertUtil.getPEMEncoded(systemTrustedCerts)
			File systemTrustedCertsFile = new File("${filesDir.absolutePath}/systemTrustedCerts.pem")
			systemTrustedCertsFile.setBytes(systemTrustedCertsPEMBytes)
			
			Set<X509Certificate> eventTrustedCerts = (Set<X509Certificate>) responseVS.data
			byte[] eventTrustedCertsPEMBytes = CertUtil.getPEMEncoded(eventTrustedCerts)
			File eventTrustedCertsFile = new File("${filesDir.absolutePath}/eventTrustedCerts.pem")
			eventTrustedCertsFile.setBytes(eventTrustedCertsPEMBytes)

			byte[] timeStampCertPEMBytes = timeStampVSService.getSigningCert()
			File timeStampCertFile = new File("${filesDir.absolutePath}/timeStampCert.pem")
			timeStampCertFile.setBytes(timeStampCertPEMBytes)
				
			int numTotalVotes = VoteVS.countByStateAndEventVSElection(VoteVS.State.OK, eventVS)
			int numTotalAccessRequests = AccessRequestVS.countByStateAndEventVSElection(
				AccessRequestVS.State.OK, eventVS)
			def backupMetaInfMap = [numVotes:numTotalVotes,
				numAccessRequest:numTotalAccessRequests]
			Map eventMetaInfMap = eventVSService.getMetaInfMap(eventVS)
			eventMetaInfMap.put(TypeVS.BACKUP.toString(), backupMetaInfMap);
			eventMetaInfMap.put(TypeVS.REPRESENTATIVE_DATA.toString(), representativeDataMap);
			
			metaInfFile.write((eventMetaInfMap as JSON).toString())
			
			String voteFileName = messageSource.getMessage('voteFileName', null, locale)
			String representativeVoteFileName = messageSource.getMessage(
				'representativeVoteFileName', null, locale)
			String accessRequestFileName = messageSource.getMessage(
				'accessRequestFileName', null, locale)
			
			DecimalFormat formatted = new DecimalFormat("00000000");
			int votesBatch = 0;
			String votesBaseDir="${filesDir.absolutePath}/votes/batch_${formatted.format(++votesBatch)}"
			new File(votesBaseDir).mkdirs()

			int accessRequestBatch = 0;
			String accessRequestBaseDir="${filesDir.absolutePath}/accessRequest/batch_${formatted.format(++accessRequestBatch)}"
			new File(accessRequestBaseDir).mkdirs()
			def votes = null
			long begin = System.currentTimeMillis()
			VoteVS.withTransaction {
				def criteria = VoteVS.createCriteria()
				votes = criteria.scroll {
					eq("state", VoteVS.State.OK)
					eq("eventVSElection", eventVS)
				}
				while (votes.next()) {
					VoteVS voteVS = (VoteVS) votes.get(0);
					UserVS representative = voteVS?.certificateVS?.getUserVS
					String voteFilePath = null
					if(representative) {//representative vote, not anonymous
						voteFilePath = "${votesBaseDir}/${representativeVoteFileName}_${representative.nif}.p7m"
					} else {
						//user vote, is anonymous
						voteFilePath = "${votesBaseDir}${voteFileName}_${formatted.format(voteVS.id)}.p7m"
					}
					MessageSMIME messageSMIME = voteVS.messageSMIME
					File smimeFile = new File(voteFilePath)
					smimeFile.setBytes(messageSMIME.content)
					if((votes.getRowNumber() % 100) == 0) {
						String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
							System.currentTimeMillis() - begin)
						log.debug("processed ${votes.getRowNumber()} votes of ${numTotalVotes} - ${elapsedTimeStr}");
						sessionFactory.currentSession.flush()
						sessionFactory.currentSession.clear()
					}
					if(((votes.getRowNumber() + 1) % 2000) == 0) {
						votesBaseDir="${filesDir.absolutePath}/votes/batch_${formatted.format(++votesBatch)}"
						new File(votesBaseDir).mkdirs()
					}	
				}
			}
			
			def accessRequests = null
			begin = System.currentTimeMillis()
			AccessRequestVS.withTransaction {
				def criteria = AccessRequestVS.createCriteria()
				accessRequests = criteria.scroll {
					eq("state", AccessRequestVS.State.OK)
					eq("eventVSElection", eventVS)
				}
				while (accessRequests.next()) {
					AccessRequestVS accessRequest = (AccessRequestVS) accessRequests.get(0);
					MessageSMIME messageSMIME = accessRequest.messageSMIME
					File smimeFile = new File("${accessRequestBaseDir}/${accessRequestFileName}_${accessRequest.userVS.nif}.p7m")
					smimeFile.setBytes(messageSMIME.content)
					if((accessRequests.getRowNumber() % 100) == 0) {
						String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
							System.currentTimeMillis() - begin)
						log.debug(" - accessRequest ${accessRequests.getRowNumber()} of ${numTotalAccessRequests} - ${elapsedTimeStr}");
						sessionFactory.currentSession.flush()
						sessionFactory.currentSession.clear()
					} 
					if(((accessRequests.getRowNumber() + 1) % 2000) == 0) {
						accessRequestBaseDir="${filesDir.absolutePath}/accessRequest/batch_${formatted.format(++accessRequestBatch)}"
						new File(accessRequestBaseDir).mkdirs()
					}
				}
				
			}
			
			def ant = new AntBuilder()
			ant.zip(destfile: zipResult, basedir: "${filesDir}") {
				fileset(dir:"${filesDir}/..", includes: "meta.inf")
			}
			ant.copy(file: zipResult, tofile: webappBackupPath)
			
			if (!eventVS.isAttached()) { eventVS.attach() }
			eventVS.metaInf = eventMetaInfMap as JSON
			eventVS.save()
			
			log.debug("zip backup of event ${eventVS.id} on file ${zipResult.absolutePath}")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:backupURL,
				data:eventMetaInfMap)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg =  messageSource.getMessage('backupError',
				[eventVS?.id].toArray(), locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, 
				message:msg, type:TypeVS.BACKUP_ERROR)
		}

	}
        
    public Map getStatisticsMap (EventVSElection eventVS, Locale locale) {
        log.debug("getStatisticsMap - eventId: ${eventVS?.id}")
        if(!eventVS) return null
        def statisticsMap = new HashMap()
        statisticsMap.fieldsEventVS = []
        statisticsMap.id = eventVS.id
        statisticsMap.subject = eventVS.subject
        statisticsMap.numAccessRequests = AccessRequestVS.countByEventVSElection(eventVS)
        statisticsMap.numAccessRequestsOK = AccessRequestVS.countByEventVSElectionAndState(
                        eventVS, AccessRequestVS.State.OK)
        statisticsMap.numAccessRequestsCancelled =   AccessRequestVS.countByEventVSElectionAndState(
                        eventVS, AccessRequestVS.State.CANCELLED)
        statisticsMap.numVotesVS = VoteVS.countByEventVSElection(eventVS)
        statisticsMap.numVotesVSOK = VoteVS.countByEventVSElectionAndState(
                        eventVS, VoteVS.State.OK)
        statisticsMap.numVotesVSVotesVSCANCELLED = VoteVS.countByEventVSElectionAndState(
                eventVS, VoteVS.State.CANCELLED)
        eventVS.fieldsEventVS.each { opcion ->
            def numVotesVS = VoteVS.countByOpcionDeEventoAndState(
                    opcion, VoteVS.State.OK)
            def opcionMap = [id:opcion.id, content:opcion.content,
                    numVotesVS:numVotesVS]
            statisticsMap.fieldsEventVS.add(opcionMap)
        }
        return statisticsMap
    }
	
}