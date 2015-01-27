package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import net.sf.json.JSONSerializer
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.throwable.ValidationExceptionVS

import static org.springframework.context.i18n.LocaleContextHolder.*
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.throwable.ExceptionVS
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

    @Transactional ResponseVS saveEvent(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		UserVS userSigner = messageSMIMEReq.getUserVS()
        JSONObject messageJSON = JSON.parse(messageSMIMEReq.getSMIME()?.getSignedContent())
        Date dateElection = new Date().parse("yyyy/MM/dd HH:mm:ss", messageJSON.dateBegin)
        Date dateBegin = DateUtils.resetDay(dateElection).getTime()
        Date dateFinish = DateUtils.resetDay(DateUtils.addDays(dateElection, 1).getTime()).getTime()
        EventVSElection eventVS = new EventVSElection(subject:messageJSON.subject, content:messageJSON.content,
                userVS:userSigner, controlCenterVS:systemService.getControlCenter(), dateBegin: dateBegin,
                cardinality:EventVS.Cardinality.EXCLUSIVE, dateFinish: dateFinish)
        ResponseVS responseVS = eventVSService.setEventDatesState(eventVS)
        if(ResponseVS.SC_OK != responseVS.statusCode) throw new ValidationExceptionVS(
                responseVS.message, MetaInfMsg.getErrorMsg(methodName, "setEventDatesState"))
        else if(EventVS.State.TERMINATED ==  eventVS.state) throw new ValidationExceptionVS(
                messageSource.getMessage('eventFinishedErrorMsg', [DateUtils.getDayWeekDateStr(eventVS.dateFinish)].toArray(),
                locale), MetaInfMsg.getErrorMsg(methodName, "eventVSFinished"))
        messageJSON.dateFinish = DateUtils.getDateStr(dateFinish)
        messageJSON.controlCenterURL = systemService.getControlCenter().serverURL
        messageJSON.accessControl = [serverURL:grailsApplication.config.grails.serverURL,
                 name:grailsApplication.config.vs.serverName] as JSONObject
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
        messageJSON.dateCreated = DateUtils.getDateStr(eventVS.dateCreated)
        messageJSON.type = TypeVS.VOTING_EVENT
        responseVS = keyStoreService.generateElectionKeysStore(eventVS)
        messageJSON.certCAVotacion = new String(CertUtils.getPEMEncoded (responseVS.data))
        File certChain = grailsApplication.mainContext.getResource(grailsApplication.config.vs.certChainPath).getFile();
        messageJSON.certChain = new String(certChain.getBytes())
        X509Certificate certUsuX509 = userSigner.getCertificate()
        messageJSON.userVS = new String(CertUtils.getPEMEncoded (certUsuX509))
        Header header = new Header ("serverURL", "${grailsApplication.config.grails.serverURL}");
        String fromUser = grailsApplication.config.vs.serverName
        String toUser = eventVS.controlCenterVS.getName()
        String subject = messageSource.getMessage('mime.subject.votingEventValidated', null, locale)
        responseVS = signatureVSService.getSMIMETimeStamped(
                fromUser, toUser, messageJSON.toString(), subject, header)
        if(ResponseVS.SC_OK != responseVS.statusCode) throw new ExceptionVS(responseVS.getMessage())
        SMIMEMessage smimeMessage = responseVS.getSMIME()
        responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(),
                ContentTypeVS.JSON_SIGNED, "${eventVS.controlCenterVS.serverURL}/eventVSElection")
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
        MessageSMIME messageSMIME = new MessageSMIME(type:TypeVS.RECEIPT,
                smimeParent:messageSMIMEReq, eventVS:eventVS,  smimeMessage: smimeMessage).save()
        log.debug "$methodName - MessageSMIME receipt - id '${messageSMIME.id}'"
        eventVS.setState(EventVS.State.ACTIVE).save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS, type:TypeVS.VOTING_EVENT,
                messageSMIME: messageSMIME, contentType: ContentTypeVS.JSON_SIGNED)
    }

    public synchronized ResponseVS generateBackups () {
        Date dateFinishFrom = DateUtils.addDays(Calendar.getInstance().getTime(), -1)
        List<EventVSElection> terminatedPolls = EventVSElection.createCriteria().list {
            gt("dateFinish", dateFinishFrom)
            eq("backupAvailable", Boolean.FALSE)
        }
        for(EventVSElection eventVS : terminatedPolls) {
            eventVS.setState(EventVS.State.TERMINATED).save()
            generateBackup(eventVS)
        }
    }

    @Transactional
	public synchronized ResponseVS generateBackup (EventVSElection eventVS) throws ExceptionVS {
		log.debug("generateBackup - eventVS: ${eventVS.id}")
        /*if (eventVS.isActive(Calendar.getInstance().getTime())) {
            throw new ExceptionVS(messageSource.getMessage('eventActiveErrorMsg', [eventVS.id].toArray(), locale))
        }*/
        Map<String, File> mapFiles = filesService.getBackupFiles(eventVS, TypeVS.VOTING_EVENT)
        File zipResult   = mapFiles.zipResult
        File metaInfFile = mapFiles.metaInfFile
        File filesDir    = mapFiles.filesDir
        String datePathPart = DateUtils.getDateStr(eventVS.getDateFinish(),"yyyy_MM_dd")
        String backupURL = "/backup/${datePathPart}/${TypeVS.VOTING_EVENT.toString()}_${eventVS.id}.zip"
        String webResourcePath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${backupURL}"
        if(zipResult.exists()) {
            log.debug("generateBackup - backup file already exists")
            return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VOTING_EVENT, message:backupURL)
        }
        ResponseVS responseVS = representativeService.getAccreditationsBackupForEvent(eventVS)
        Map representativeDataMap = responseVS.data
        Set<X509Certificate> eventTrustedCerts = signatureVSService.getEventTrustedCerts(eventVS)
        Set<X509Certificate> systemTrustedCerts = signatureVSService.getTrustedCerts()
        File systemTrustedCertsFile = new File("${filesDir.absolutePath}/systemTrustedCerts.pem")
        systemTrustedCertsFile.setBytes(CertUtils.getPEMEncoded(systemTrustedCerts))

        File eventTrustedCertsFile = new File("${filesDir.absolutePath}/eventTrustedCerts.pem")
        eventTrustedCertsFile.setBytes(CertUtils.getPEMEncoded(eventTrustedCerts))

        File timeStampCertFile = new File("${filesDir.absolutePath}/timeStampCert.pem")
        timeStampCertFile.setBytes(timeStampService.getSigningCertPEMBytes())

        int numTotalVotes = VoteVS.countByStateAndEventVS(VoteVS.State.OK, eventVS)
        int numTotalAccessRequests = AccessRequestVS.countByStateAndEventVSElection(AccessRequestVS.State.OK, eventVS)
        def backupMetaInfMap = [numVotes:numTotalVotes, numAccessRequest:numTotalAccessRequests]
        Map eventMetaInfMap = eventVSService.getMetaInfMap(eventVS)
        eventMetaInfMap.put(TypeVS.BACKUP.toString(), backupMetaInfMap);
        eventMetaInfMap.put(TypeVS.REPRESENTATIVE_DATA.toString(), representativeDataMap);
        metaInfFile.write((eventMetaInfMap as JSON).toString())
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
            File smimeFile = null
            if(representative) {//not anonymous, representative vote
                smimeFile = new File("${votesBaseDir}/representativeVote_${representative.nif}.p7m")
            } else {//anonymous, user vote
                smimeFile = new File("${votesBaseDir}/vote_${formatted.format(voteVS.id)}.p7m")
            }
            MessageSMIME messageSMIME = voteVS.messageSMIME
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
            File smimeFile = new File("${accessRequestBaseDir}/accessRequest_${accessRequest.userVS.nif}.p7m")
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
        ant.zip(destfile: zipResult, basedir: "${mapFiles.baseDir}")
        //The file is copied and available to download but triggers a null pointer exception with ResourcesPlugin
        ant.copy(file: zipResult, tofile: webResourcePath)

        if (!eventVS.isAttached()) { eventVS.attach() }
        eventVS.setMetaInf((eventMetaInfMap as JSON).toString()).save()
        log.debug("zip backup of event ${eventVS.id} on file ${zipResult.absolutePath}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VOTING_EVENT,
                message:backupURL, data:eventMetaInfMap)
	}
        
    public Map getStatsMap (EventVSElection eventVS) {
        if(!eventVS) throw new ExceptionVS("EventVSElection null")
        def statsMap = new HashMap()
        statsMap.fieldsEventVS = []
        statsMap.id = eventVS.id
        statsMap.subject = eventVS.subject
        statsMap.numAccessRequests = AccessRequestVS.countByEventVSElection(eventVS)
        statsMap.numAccessRequestsOK = AccessRequestVS.countByEventVSElectionAndState(
                        eventVS, AccessRequestVS.State.OK)
        statsMap.numAccessRequestsCancelled =   AccessRequestVS.countByEventVSElectionAndState(
                        eventVS, AccessRequestVS.State.CANCELLED)
        statsMap.numVotesVS = VoteVS.countByEventVS(eventVS)
        statsMap.numVotesVSOK = VoteVS.countByEventVSAndState(eventVS, VoteVS.State.OK)
        statsMap.numVotesVSVotesVSCANCELLED = VoteVS.countByEventVSAndState(eventVS,VoteVS.State.CANCELLED)
        eventVS.fieldsEventVS.each { option ->
            def numVotesVS = VoteVS.countByOptionSelectedAndState(option, VoteVS.State.OK)
            def optionMap = [id:option.id, content:option.content, numVotesVS:numVotesVS]
            statsMap.fieldsEventVS.add(optionMap)
        }
        return statsMap
    }
	
}