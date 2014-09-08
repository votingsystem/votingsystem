package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import grails.transaction.Transactional
import org.votingsystem.util.MetaInfMsg

import javax.mail.Header
import java.security.cert.X509Certificate
import java.text.DecimalFormat

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class EventVSElectionService {

    def tagVSService
    def subscriptionVSService
    def fieldEventVSService
    def signatureVSService
    def eventVSService
    def grailsApplication
	def keyStoreService
	def messageSource
	def representativeService
	def filesService
	def timeStampService
	def sessionFactory
    def systemService

    @Transactional ResponseVS saveEvent(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
		EventVSElection eventVS = null
		UserVS userSigner = messageSMIMEReq.getUserVS()
		log.debug("saveEvent --- signer: ${userSigner?.nif}")
		String msg = null
		ResponseVS responseVS = null
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)

        eventVS = new EventVSElection(subject:messageJSON.subject, content:messageJSON.content, userVS:userSigner,
                controlCenterVS:systemService.getControlCenter(),
                dateBegin: new Date().parse("yyyy/MM/dd HH:mm:ss", messageJSON.dateBegin),
                dateFinish: new Date().parse("yyyy/MM/dd HH:mm:ss", messageJSON.dateFinish))

        responseVS = eventVSService.setEventDatesState(eventVS, locale)
        if(ResponseVS.SC_OK != responseVS.statusCode) {
            log.error "$methodName - EVENT DATES ERROR - ${responseVS.message}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:responseVS.message,
                    type:TypeVS.ERROR, metaInf:MetaInfMsg.getErrorMsg(methodName, "setEventDatesState"))
        } else eventVS.setState(null)
        eventVS.cardinality = EventVS.Cardinality.EXCLUSIVE
        messageJSON.controlCenterURL = systemService.getControlCenter().serverURL
        messageJSON.accessControl = [serverURL:grailsApplication.config.grails.serverURL,
                                     name:grailsApplication.config.VotingSystem.serverName] as JSONObject
        if (messageJSON.tags) {
            Set<TagVS> tagSet = tagVSService.save(messageJSON.tags)
            if(tagSet) eventVS.setTagVSSet(tagSet)
        }
        eventVS.save()
        Set<FieldEventVS> fieldsEventVS
        if (messageJSON.fieldsEventVS) {
            fieldsEventVS = fieldEventVSService.saveFieldsEventVS(eventVS, messageJSON.fieldsEventVS)
            JSONArray arrayFieldsEventVS = new JSONArray()
            fieldsEventVS.each {  arrayFieldsEventVS.add([id:it.id, content:it.content] as JSONObject  ) }
            messageJSON.fieldsEventVS = arrayFieldsEventVS
        }
        if(!fieldsEventVS || fieldsEventVS.size() < 2)
            throw new ExceptionVS(messageSource.getMessage('pollingMissignFieldsErrorMsg', null, locale))
        log.debug("$methodName - Saved voting event '${eventVS.id}'")
        messageJSON.id = eventVS.id
        messageJSON.URL = "${grailsApplication.config.grails.serverURL}/eventVSElection/${eventVS.id}"
        messageJSON.dateCreated = DateUtils.getStringFromDate(eventVS.dateCreated)
        messageJSON.type = TypeVS.VOTING_EVENT
        responseVS = keyStoreService.generateElectionKeysStore(eventVS)
        if(ResponseVS.SC_OK != responseVS.statusCode) {
            log.error "$methodName - ERROR GENERATING EVENT KEYSTRORE- ${responseVS.message}"
            throw new ExceptionVS(responseVS.message)
        }
        messageJSON.certCAVotacion = new String(CertUtil.getPEMEncoded (responseVS.data))
        File certChain = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certChainPath).getFile();
        messageJSON.certChain = new String(certChain.getBytes())
        X509Certificate certUsuX509 = userSigner.getCertificate()
        messageJSON.userVS = new String(CertUtil.getPEMEncoded (certUsuX509))

        Header header = new Header ("serverURL", "${grailsApplication.config.grails.serverURL}");
        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = eventVS.controlCenterVS.getName()
        String subject = messageSource.getMessage('mime.subject.votingEventValidated', null, locale)
        responseVS = signatureVSService.getTimestampedSignedMimeMessage(
                fromUser, toUser, messageJSON.toString(), subject, header)
        byte[] signedMessageBytes = responseVS.getSmimeMessage().getBytes()
        if(ResponseVS.SC_OK != responseVS.statusCode) throw new Exception(responseVS.getMessage())
        responseVS = HttpHelper.getInstance().sendData(signedMessageBytes,
                ContentTypeVS.SIGNED, "${eventVS.controlCenterVS.serverURL}/eventVSElection")
        if(ResponseVS.SC_OK != responseVS.statusCode) {
            throw new ExceptionVS(messageSource.getMessage('controlCenterCommunicationErrorMsg',
                    [responseVS.message].toArray(),locale))
        }

        CertificateVS controlCenterCert = CertificateVS.findWhere(type:CertificateVS.Type.ACTOR_VS,
                actorVS:systemService.getControlCenter(), state:CertificateVS.State.OK)
        X509Certificate controlCenterX509Cert = controlCenterCert.getX509Cert()
        CertificateVS eventVSControlCenterCertificate = new CertificateVS(actorVS:systemService.getControlCenter(),
                state:CertificateVS.State.OK, type:CertificateVS.Type.ACTOR_VS, eventVSElection:eventVS,
                content:controlCenterX509Cert.getEncoded(), serialNumber:controlCenterX509Cert.getSerialNumber().longValue(),
                validFrom:controlCenterX509Cert?.getNotBefore(), validTo:controlCenterX509Cert?.getNotAfter()).save()

        X509Certificate accessControlX509Cert = signatureVSService.getServerCert()
        CertificateVS eventVSAccessControlCertificate = new CertificateVS(
                state:CertificateVS.State.OK, type:CertificateVS.Type.ACTOR_VS, eventVSElection:eventVS,
                content:accessControlX509Cert.getEncoded(), serialNumber:accessControlX509Cert.getSerialNumber().longValue(),
                validFrom:accessControlX509Cert?.getNotBefore(), validTo:accessControlX509Cert?.getNotAfter()).save()

        MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT,
                smimeParent:messageSMIMEReq, eventVS:eventVS,  content:signedMessageBytes)
        messageSMIMEResp.save()
        eventVS.setState(EventVS.State.ACTIVE)
        eventVS.save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS, type:TypeVS.VOTING_EVENT,
                data:messageSMIMEResp)
    }

    @Transactional
	public synchronized ResponseVS generateBackup (EventVSElection eventVS, Locale locale) {
		log.debug("generateBackup - eventVSId: ${eventVS.id}")
		ResponseVS responseVS;
		String msg = null
		try {
			if (eventVS.isActive(Calendar.getInstance().getTime())) {
				msg = messageSource.getMessage('eventDateNotFinished', null, locale)
				String currentDateStr = DateUtils.getStringFromDate(
					new Date(System.currentTimeMillis()))
				log.error("generateBackup - DATE ERROR  ${msg} - " +
					"Actual date '${currentDateStr}' - dateFinish eventVS '${eventVS.dateFinish}'")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,  message:msg, type:TypeVS.ERROR)
			}
			
			Map<String, File> mapFiles = filesService.getBackupFiles(eventVS, TypeVS.VOTING_EVENT, locale)
			File zipResult   = mapFiles.zipResult
			File metaInfFile = mapFiles.metaInfFile
			File filesDir    = mapFiles.filesDir
			
			String serviceURLPart = messageSource.getMessage('votingBackupPartPath', [eventVS.id].toArray(), locale)
			String datePathPart = DateUtils.getShortStringFromDate(eventVS.getDateFinish())
			String backupURL = "/backup/${datePathPart}/${serviceURLPart}.zip"
			String webappBackupPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${backupURL}"
			
			if(zipResult.exists()) {
				log.debug("generateBackup - backup file already exists")
				return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VOTING_EVENT, message:backupURL)
			}
			responseVS = representativeService.getAccreditationsBackupForEvent(eventVS, locale)
			Map representativeDataMap = responseVS.data
			if(ResponseVS.SC_OK != responseVS.statusCode) {
				log.error("generateBackup - REPRESENTATIVE BACKUP DATA GENERATION ERROR:  ${responseVS.message}")
				return responseVS
			}			
			
			responseVS = signatureVSService.getEventTrustedCerts(eventVS, locale)
			if(ResponseVS.SC_OK != responseVS.statusCode) {
				responseVS.type = TypeVS.ERROR
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

			byte[] timeStampCertPEMBytes = timeStampService.getSigningCertPEMBytes()
			File timeStampCertFile = new File("${filesDir.absolutePath}/timeStampCert.pem")
			timeStampCertFile.setBytes(timeStampCertPEMBytes)
				
			int numTotalVotes = VoteVS.countByStateAndEventVS(VoteVS.State.OK, eventVS)
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
            votes = VoteVS.createCriteria().scroll {
                eq("state", VoteVS.State.OK)
                eq("eventVS", eventVS)
            }
            while (votes.next()) {
                VoteVS voteVS = (VoteVS) votes.get(0);
                UserVS representative = voteVS?.certificateVS?.userVS
                String voteFilePath = null
                if(representative) {//representative vote, not anonymous
                    voteFilePath = "${votesBaseDir}/${representativeVoteFileName}_${representative.nif}.p7m"
                } else {
                    //user vote, is anonymous
                    voteFilePath = "${votesBaseDir}/${voteFileName}_${formatted.format(voteVS.id)}.p7m"
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
			def accessRequests = null
			begin = System.currentTimeMillis()
            accessRequests = AccessRequestVS.createCriteria().scroll {
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
			
			def ant = new AntBuilder()
			ant.zip(destfile: zipResult, basedir: "${filesDir}") {
				fileset(dir:"${filesDir}/..", includes: "meta.inf")
			}
            //The file is copied and available to download but triggers a null pointer exception with ResourcesPlugin
			ant.copy(file: zipResult, tofile: webappBackupPath)
			
			if (!eventVS.isAttached()) { eventVS.attach() }
			eventVS.metaInf = eventMetaInfMap as JSON
			eventVS.save()
			
			log.debug("zip backup of event ${eventVS.id} on file ${zipResult.absolutePath}")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VOTING_EVENT,
                    message:backupURL, data:eventMetaInfMap)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg =  messageSource.getMessage('backupError', [eventVS?.id].toArray(), locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR,  message:msg, type:TypeVS.ERROR)
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
        statisticsMap.numVotesVS = VoteVS.countByEventVS(eventVS)
        statisticsMap.numVotesVSOK = VoteVS.countByEventVSAndState(eventVS, VoteVS.State.OK)
        statisticsMap.numVotesVSVotesVSCANCELLED = VoteVS.countByEventVSAndState(eventVS,VoteVS.State.CANCELLED)
        eventVS.fieldsEventVS.each { option ->
            def numVotesVS = VoteVS.countByOptionSelectedAndState(option, VoteVS.State.OK)
            def optionMap = [id:option.id, content:option.content, numVotesVS:numVotesVS]
            statisticsMap.fieldsEventVS.add(optionMap)
        }
        return statisticsMap
    }
	
}