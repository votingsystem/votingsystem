package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import grails.transaction.Transactional
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.accesscontrol.model.RepresentativeDocument
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.throwable.ValidationExceptionVS

import static org.springframework.context.i18n.LocaleContextHolder.*
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.util.NifUtils
import java.security.MessageDigest
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat

@Transactional
class RepresentativeService {
	
	enum State {WITHOUT_ACCESS_REQUEST, WITH_ACCESS_REQUEST, WITH_VOTE}
	
	def subscriptionVSService
	def messageSource
	def grailsApplication
	def mailSenderService
	def signatureVSService
	def filesService
	def eventVSService
	def sessionFactory
    def representativeDelegationService
	LinkGenerator grailsLinkGenerator

	/**
	 * Creates backup of the state of all the representatives for a closed event
	 */
	private synchronized ResponseVS getAccreditationsBackupForEvent (EventVSElection event){
		log.debug("getAccreditationsBackupForEvent --- event: ${event.id}")
		/*if(event.isActive(Calendar.getInstance().getTime())) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:messageSource.getMessage('eventActiveErrorMsg',
                    [event.id].toArray(), locale))
		}*/
		Map<String, File> mapFiles = filesService.getBackupFiles(event, TypeVS.REPRESENTATIVE_DATA)
		File zipResult   = mapFiles.zipResult
		File filesDir    = mapFiles.filesDir
		File metaInfFile = mapFiles.metaInfFile
        String downloadFileName = messageSource.getMessage('repAccreditationsBackupForEventFileName',
                [event.id].toArray(), locale)
		if(zipResult.exists()) {
			log.debug("getAccreditationsBackupForEvent - backup file already exists")
			Map metaInfFileMap = JSONSerializer.toJSON(reportsFile.text)
			return new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.ZIP,
                    messageBytes: zipResult.getBytes(), message: downloadFileName, data:metaInfFileMap)
		}
		Map optionsMap = [:]
        for(FieldEventVS option : event.fieldsEventVS) {
			def numVoteRequests = VoteVS.countByOptionSelectedAndState(option, VoteVS.State.OK)
			def numUsersWithVote = VoteVS.createCriteria().count {
				certificateVS{ isNull("userVS") }
				eq("state", VoteVS.State.OK)
				eq("eventVS", event)
				eq("optionSelected", option)

			}
			int numRepresentativesWithVote = numVoteRequests - numUsersWithVote
			Map optionMap = [content:option.content,
				numVoteRequests:numVoteRequests, numUsersWithVote:numUsersWithVote,
				numRepresentativesWithVote:numRepresentativesWithVote,
				numVotesResult:numUsersWithVote]
			optionsMap[option.id] = optionMap
		}
		int numRepresentatives = RepresentativeDocument.createCriteria().count {
			le("dateCreated", event.getDateBegin())
			or {
				isNull("dateCanceled")
				ge("dateCanceled", event.getDateFinish())
			}
		}
		log.debug("num representatives: ${numRepresentatives}")
		int numRepresentativesWithAccessRequest = 0
		int numRepresentativesWithVote = 0
		int numTotalRepresented = 0
		int numTotalRepresentedWithAccessRequest = 0
		int numVotesRepresentedByRepresentatives = 0
		long representativeBegin = System.currentTimeMillis()
		def representativeDocs = RepresentativeDocument.createCriteria().scroll {
			lt("dateCreated", event.getDateFinish())
			or {
				isNull("dateCanceled")
				gt("dateCanceled", event.getDateFinish())
			}
		}
		Map representativesMap = [:]
		while (representativeDocs.next()) {
			UserVS representative = (UserVS) representativeDocs.get(0).getUserVS();
			DecimalFormat formatted = new DecimalFormat("00000000");
			int delegationsBatch = 0
			String representativeBaseDir = "${filesDir.absolutePath}/representative_${representative.nif}/" +
					"batch_${formatted.format(++delegationsBatch)}"
			new File(representativeBaseDir).mkdirs()
			//representative active on selected date
			int numRepresented = 1 //The representative itself
			int numRepresentedWithAccessRequest = 0
			def representationDocuments = RepresentationDocumentVS.createCriteria().scroll {
					eq("representative", representative)
					le("dateCreated", event.getDateBegin())
					or {
						isNull("dateCanceled")
						gt("dateCanceled", event.getDateFinish())
					}
			}
			while (representationDocuments.next()) {
				++numRepresented
				RepresentationDocumentVS repDocument = (RepresentationDocumentVS) representationDocuments.get(0);
				UserVS represented = repDocument.userVS
				AccessRequestVS representedAccessRequest = AccessRequestVS.findWhere(
					state:AccessRequestVS.State.OK, userVS:represented, eventVSElection:event)
				String repDocFileName = null
				if(representedAccessRequest) {
					numRepresentedWithAccessRequest++
					repDocFileName = "${representativeBaseDir}/${representationDocuments.getRowNumber()}_delegation_with_vote_${represented.nif}.p7m"
				} else repDocFileName = "${representativeBaseDir}/${representationDocuments.getRowNumber()}_delegation_${represented.nif}.p7m"
				File representationDocFile = new File(repDocFileName)
				representationDocFile.setBytes(repDocument.activationSMIME.content)
				if(((representationDocuments.getRowNumber() + 1)  % 100) == 0) {
					sessionFactory.currentSession.flush()
					sessionFactory.currentSession.clear()
					log.debug("Representative ${representative.nif} - processed ${representationDocuments.getRowNumber()} representations");
				}
				if(((representationDocuments.getRowNumber() + 1) % 2000) == 0) {
					representativeBaseDir= "${filesDir.absolutePath}/representative_${representative.nif}/batch_${formatted.format(++delegationsBatch)}"
					new File(representativeBaseDir).mkdirs()
				}
			}
			numTotalRepresented += numRepresented			
			numTotalRepresentedWithAccessRequest += numRepresentedWithAccessRequest
			State state = State.WITHOUT_ACCESS_REQUEST
            AccessRequestVS accessRequestVS = AccessRequestVS.findWhere(eventVSElection:event, userVS:representative,
                    state:AccessRequestVS.State.OK)
            VoteVS representativeVote = null;
            if(accessRequestVS) {//Representative has access request
                numRepresentativesWithAccessRequest++;
                state = State.WITH_ACCESS_REQUEST
                representativeVote = VoteVS.createCriteria().get () {
					certificateVS{eq("userVS", representative)}
                    eq("state", VoteVS.State.OK)
                    eq("eventVS", event)
                }
            }
			int numVotesRepresentedByRepresentative = 0
			if(representativeVote) {
				state = State.WITH_VOTE
				++numRepresentativesWithVote
				numVotesRepresentedByRepresentative = numRepresented  - numRepresentedWithAccessRequest
				numVotesRepresentedByRepresentatives += numVotesRepresentedByRepresentative
				optionsMap[representativeVote.optionSelected.id].numVotesResult += numVotesRepresentedByRepresentative
			}
			Map representativeMap = [id:representative.id,
				optionSelectedId:representativeVote?.optionSelected?.id,
				numRepresentedWithVote:numRepresentedWithAccessRequest,
				numRepresentations: numRepresented,
				numVotesRepresented:numVotesRepresentedByRepresentative]
			representativesMap[representative.nif] = representativeMap
			String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
				System.currentTimeMillis() - representativeBegin)
			/*File reportFile = mapFiles.reportFile
			reportFile.write("")
			String csvLine = "${representative.nif}, " +
				"numRepresented:${formatted.format(numRepresented)}, " +
			    "numRepresentedWithAccessRequest:${formatted.format(numRepresentedWithAccessRequest)}, " +
			    "${state.toString()}\n"
			reportFile.append(csvLine)*/
			log.debug("processed ${representativeDocs.getRowNumber()} of ${numRepresentatives} representatives - ${elapsedTimeStr}")
			if(((representativeDocs.getRowNumber() + 1)  % 100) == 0) {
				sessionFactory.currentSession.flush()
				sessionFactory.currentSession.clear()
			}
		}
		def metaInfMap = [numRepresentatives:numRepresentatives,
			numRepresentativesWithAccessRequest:numRepresentativesWithAccessRequest,
			numRepresentativesWithVote:numRepresentativesWithVote,
			numRepresentedWithAccessRequest:numTotalRepresentedWithAccessRequest,
			numRepresented:numTotalRepresented, 
			numVotesRepresentedByRepresentatives:numVotesRepresentedByRepresentatives,
			representatives: representativesMap, options:optionsMap]
		metaInfFile.write((metaInfMap as JSON).toString())
		return new ResponseVS(data:metaInfMap, statusCode:ResponseVS.SC_OK)
	}

	private synchronized ResponseVS getAccreditationsBackup (UserVS representative, Date selectedDate){
		log.debug("getAccreditationsBackup - representative: ${representative.nif}" +" - selectedDate: ${selectedDate}")
		def representationDocuments = RepresentationDocumentVS.createCriteria().scroll {
            eq("representative", representative)
            le("dateCreated", selectedDate)
            or {
                eq("state", RepresentationDocumentVS.State.CANCELLED)
                eq("state", RepresentationDocumentVS.State.OK)
            }
            or {
                isNull("dateCanceled")
                gt("dateCanceled", selectedDate)
            }
        }
		String selectedDateStr = DateUtils.getDateStr(selectedDate,"yyyy/MM/dd")
		String serviceURLPart = messageSource.getMessage('representativeAcreditationsBackupPath',
                [representative.nif].toArray(), locale)
		String basedir = "${grailsApplication.config.vs.backupCopyPath}" +
			"/AccreditationsBackup/${selectedDateStr}/${serviceURLPart}"
		String backupURL = "/backup/${selectedDateStr}/${serviceURLPart}.zip"
		String webappBackupPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${backupURL}"
		File zipResult = new File("${basedir}.zip")
		File metaInfFile;
		if(zipResult.exists()) {
			 metaInfFile = new File("${basedir}/meta.inf")
			 if(metaInfFile) {
				 def metaInfMap = JSON.parse(metaInfFile.text)
				 log.debug("getAccreditationsBackup - send previous request")
				 return new ResponseVS(statusCode:ResponseVS.SC_OK, message:backupURL, data:metaInfMap)
			 }
		}
		new File(basedir).mkdirs()
		String accreditationFileName = messageSource.getMessage('accreditationFileName', null, locale)
		int numAccreditations = 0
		while (representationDocuments.next()) {
			++numAccreditations
			RepresentationDocumentVS representationDocument = (RepresentationDocumentVS)representationDocuments.get(0);
			MessageSMIME messageSMIME = representationDocument.activationSMIME
			File smimeFile = new File("${basedir}/${accreditationFileName}_${representationDocument.id}")
			smimeFile.setBytes(messageSMIME.content)
			if((representationDocuments.getRowNumber() % 100) == 0) {
				sessionFactory.currentSession.flush() 
				sessionFactory.currentSession.clear()
				log.debug("getAccreditationsBackup - processed ${representationDocuments.getRowNumber()} representations")
			}
				
		}
		def metaInfMap = [numAccreditations:numAccreditations, selectedDate: selectedDateStr,
			representativeURL:"${grailsApplication.config.grails.serverURL}/representative/${representative.id}"]
		metaInfFile = new File("${basedir}/meta.inf")
		metaInfFile.write((metaInfMap as JSON).toString())
		def ant = new AntBuilder()
		ant.zip(destfile: zipResult, basedir: basedir)
		ant.copy(file: zipResult, tofile: webappBackupPath)
		log.debug("getAccreditationsBackup - destfile.name '${zipResult.name}'")
		return new ResponseVS(statusCode:ResponseVS.SC_OK, data:metaInfMap, message:backupURL)
	}

    @Transactional
    ResponseVS saveRepresentativeData(MessageSMIME messageSMIMEReq, byte[] imageBytes) {
		SMIMEMessage smimeMessageReq = messageSMIMEReq.getSMIME()
		UserVS userVS = messageSMIMEReq.getUserVS()
		String msg
        AnonymousDelegation anonymousDelegation = representativeDelegationService.getAnonymousDelegation(userVS)
        if(anonymousDelegation) throw new ValidationExceptionVS(this.getClass(), messageSource.getMessage(
                'representativeRequestWithActiveAnonymousDelegation', null, locale))
        JSONObject messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
        String base64ImageHash = messageJSON.base64ImageHash
        MessageDigest messageDigest = MessageDigest.getInstance(ContextVS.VOTING_DATA_DIGEST);
        byte[] resultDigest =  messageDigest.digest(imageBytes);
        String base64ResultDigest = Base64.getEncoder().encodeToString(resultDigest);
        if(!base64ResultDigest.equals(base64ImageHash)) throw new ExceptionVS(
                messageSource.getMessage('imageHashErrorMsg', null, locale))
        //String base64EncodedImage = messageJSON.base64RepresentativeEncodedImage
        //BASE64Decoder decoder = new BASE64Decoder();
        //byte[] imageFileBytes = decoder.decodeBuffer(base64EncodedImage);

        if(UserVS.Type.REPRESENTATIVE != userVS.type) {
            userVS.setType(UserVS.Type.REPRESENTATIVE).setRepresentative(null).save()
            representativeDelegationService.cancelRepresentationDocument(messageSMIMEReq, userVS);
            msg = messageSource.getMessage('representativeDataCreatedOKMsg',
                    [userVS.firstName, userVS.lastName].toArray(), locale)
        } else {
            msg = messageSource.getMessage('representativeDataUpdatedMsg',
                    [userVS.firstName, userVS.lastName].toArray(), locale)
        }
        List<ImageVS> images = ImageVS.findAllWhere(userVS:userVS, type:ImageVS.Type.REPRESENTATIVE)
        for(ImageVS imageVS : images) {
            imageVS.setType(ImageVS.Type.REPRESENTATIVE_CANCELLED).save()
        }
		ImageVS newImage = new ImageVS(userVS:userVS, messageSMIME:messageSMIMEReq,
				type:ImageVS.Type.REPRESENTATIVE, fileBytes:imageBytes).save()

		RepresentativeDocument representativeDocument = RepresentativeDocument.findWhere(userVS:userVS,
				state:RepresentativeDocument.State.OK);
		if(representativeDocument) {
			representativeDocument.setState(RepresentativeDocument.State.RENEWED).setCancellationSMIME(messageSMIMEReq).save()
		}
		new RepresentativeDocument(userVS:userVS, activationSMIME:messageSMIMEReq,
				description: messageJSON.representativeInfo).save();
        log.debug "saveRepresentativeData - user:${userVS.nif} - image: ${newImage.id}"
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg,  type:TypeVS.REPRESENTATIVE_DATA)
    }

	private ResponseVS getVotingHistoryBackup (UserVS representative, Date dateFrom, Date dateTo){
		log.debug("getVotingHistoryBackup - representative: ${representative.nif}" + 
			" - dateFrom: ${dateFrom} - dateTo: ${dateTo}")
		
		def dateFromStr = DateUtils.getDateStr(dateFrom, "yyyy/MM/dd")
		def dateToStr = DateUtils.getDateStr(dateTo,"yyyy/MM/dd")
		
		String serviceURLPart = messageSource.getMessage(
			'representativeVotingHistoryBackupPartPath', [representative.nif].toArray(), locale)
		def basedir = "${grailsApplication.config.vs.backupCopyPath}" +
			"/RepresentativeHistoryVoting/${dateFromStr}_${dateToStr}/${serviceURLPart}"
		log.debug("getVotingHistoryBackup - basedir: ${basedir}")
		File zipResult = new File("${basedir}.zip")
		String datePathPart = DateUtils.getDateStr(Calendar.getInstance().getTime(), "yyyy/MM/dd")
		String backupURL = "/backup/${datePathPart}/${serviceURLPart}.zip"
		String webappBackupPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${backupURL}"
		File metaInfFile;
		if(zipResult.exists()) {
			 metaInfFile = new File("${basedir}/meta.inf")
			 if(metaInfFile) {
				 def metaInfMap = JSON.parse(metaInfFile.text)
				 log.debug("getVotingHistoryBackup - ${zipResult.name} already exists");
				 return new ResponseVS(statusCode:ResponseVS.SC_OK, data:metaInfMap, message:backupURL)
			 }
		}
		new File(basedir).mkdirs()
		int numVotes = 0
        def representativeVotes = VoteVS.createCriteria().scroll {
			certificateVS{eq("userVS", representative)}
            eq("state", VoteVS.State.OK)
            between("dateCreated", dateFrom, dateTo)
        }
        while (representativeVotes.next()) {
            numVotes++
            VoteVS vote = (VoteVS) representativeVotes.get(0);
            MessageSMIME voteSMIME = vote.messageSMIME
            String voteId = String.format('%08d', vote.id)
            String voteFilePath = "${basedir}/vote_${voteId}.p7m"
            MessageSMIME messageSMIME = vote.messageSMIME
            File smimeFile = new File(voteFilePath)
            smimeFile.setBytes(messageSMIME.content)
        }
        log.debug("${representative.nif} -> ${numVotes} votes ");
        if((representativeVotes.getRowNumber() % 100) == 0) {
            sessionFactory.currentSession.flush()
            sessionFactory.currentSession.clear()
        }
		def metaInfMap = [numVotes:numVotes, dateFrom:DateUtils.getDateStr(dateFrom), dateTo:DateUtils.getDateStr(dateTo),
			    representativeURL:"${grailsApplication.config.grails.serverURL}/representative/${representative.id}"]
		String metaInfJSONStr = metaInfMap as JSON
		metaInfFile = new File("${basedir}/meta.inf")
		metaInfFile.write(metaInfJSONStr)
		def ant = new AntBuilder()
		ant.zip(destfile: zipResult, basedir: basedir)
		ant.copy(file: zipResult, tofile: webappBackupPath)
		log.debug("getVotingHistoryBackup - zipResult: ${zipResult.absolutePath} - backupURL: ${backupURL}")
		return new ResponseVS(statusCode:ResponseVS.SC_OK, data:metaInfMap, message:backupURL)
	}
	
	ResponseVS processVotingHistoryRequest(MessageSMIME messageSMIMEReq) {
		log.debug("processVotingHistoryRequest")
		SMIMEMessage smimeMessage = messageSMIMEReq.getSMIME()
		UserVS userVS = messageSMIMEReq.getUserVS()
		def messageJSON
		String msg
        TypeVS type = TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST
        //REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR
        messageJSON = JSON.parse(smimeMessage.getSignedContent())
        Date dateFrom = DateUtils.getDateFromString(messageJSON.dateFrom)
        Date dateTo = DateUtils.getDateFromString(messageJSON.dateTo)
        if(dateFrom.after(dateTo)) {
            log.error "processVotingHistoryRequest - DATE ERROR - dateFrom '${dateFrom}' dateTo '${dateTo}'"
            DateFormat formatter = new SimpleDateFormat("dd MMM 'de' yyyy 'a las' HH:mm");
            msg = messageSource.getMessage('dateRangeErrorMsg',[formatter.format(dateFrom),
                    formatter.format(dateTo)].toArray(), locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:msg, type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR)
        }
        TypeVS operationType = TypeVS.valueOf(messageJSON.operation)
        if(TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST != operationType) {
            msg = messageSource.getMessage('operationErrorMsg', [messageJSON.operation].toArray(), locale)
            log.error "processVotingHistoryRequest - OPERATION ERROR - ${msg}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:msg, type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR)
        }
        String requestValidatedNIF =  NifUtils.validate(messageJSON.representativeNif)
        UserVS representative = UserVS.findWhere(nif:requestValidatedNIF, type:UserVS.Type.REPRESENTATIVE)
        if(!representative) {
            msg = messageSource.getMessage('representativeNifErrorMsg', [requestValidatedNIF].toArray(), locale)
            log.error "processVotingHistoryRequest - USER NOT REPRESENTATIVE ${msg}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR)
        }
        runAsync {
            ResponseVS backupGenResponseVS
            backupGenResponseVS = getVotingHistoryBackup(representative, dateFrom,  dateTo)
            if(ResponseVS.SC_OK == backupGenResponseVS?.statusCode) {
                BackupRequestVS backupRequest = new BackupRequestVS(
                        filePath:backupGenResponseVS.message,
                        type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST,
                        representative:representative,
                        messageSMIME:messageSMIMEReq, email:messageJSON.email)
                log.debug("messageSMIME: ${messageSMIMEReq.id} - ${backupRequest.type}");
                BackupRequestVS.withTransaction {
                    if (!backupRequest.save()) {
                        backupRequest.errors.each {
                            log.error("processVotingHistoryRequest - ERROR backupRequest - ${it}")}
                    }
                }
                mailSenderService.sendRepresentativeVotingHistory(
                        backupRequest, messageJSON.dateFrom, messageJSON.dateTo)
            } else log.error("Error generating backup");
        }
		msg = messageSource.getMessage('backupRequestOKMsg', [messageJSON.email].toArray(), locale)
		return new ResponseVS(statusCode:ResponseVS.SC_OK,
			type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST, message:msg)
	}
	
	ResponseVS processRevoke(MessageSMIME messageSMIMEReq) {
		SMIMEMessage smimeMessage = messageSMIMEReq.getSMIME();
		UserVS userVS = messageSMIMEReq.getUserVS();
		RepresentativeDocument representativeDocument = RepresentativeDocument.findWhere(
				userVS:userVS, state: RepresentativeDocument.State.OK)
		if(!representativeDocument) throw new ExceptionVS(
				messageSource.getMessage('unsubscribeRepresentativeUserErrorMsg', [userVS.nif].toArray(), locale))
		log.debug("processRevoke - user ${userVS.nif}")
        JSONObject messageJSON = JSON.parse(smimeMessage.getSignedContent())
        TypeVS operationType = TypeVS.valueOf(messageJSON.operation)
        if(TypeVS.REPRESENTATIVE_REVOKE != operationType) throw new ExceptionVS(
                messageSource.getMessage('operationErrorMsg', [messageJSON.operation].toArray(), locale))
        if(UserVS.Type.REPRESENTATIVE != userVS.type) throw new ExceptionVS(
                messageSource.getMessage('unsubscribeRepresentativeUserErrorMsg', [userVS.nif].toArray(), locale))
        //(TODO notify users)=====
        def representedUsers = UserVS.createCriteria().scroll {
            eq("representative", userVS)
        }
        while (representedUsers.next()) {
            UserVS representedUser = (UserVS) representedUsers.get(0);
			representedUser.setRepresentative(null).save()
            if((representedUsers.getRowNumber() % 100) == 0) {
                sessionFactory.currentSession.flush()
                sessionFactory.currentSession.clear()
                log.debug("processRevoke - processed ${representedUsers.getRowNumber()} user updates")
            }
        }
        def representationDocuments = RepresentationDocumentVS.createCriteria().scroll {
            eq("state", RepresentationDocumentVS.State.OK)
            eq("representative", userVS)
        }
        while (representationDocuments.next()) {
            RepresentationDocumentVS representationDocument = (RepresentationDocumentVS) representationDocuments.get(0);
            representationDocument.setState(RepresentationDocumentVS.State.CANCELLED_BY_REPRESENTATIVE).setCancellationSMIME(
					messageSMIMEReq).setDateCanceled(userVS.getTimeStampToken().getTimeStampInfo().getGenTime()).save()
            if((representationDocuments.getRowNumber() % 100) == 0) {
                sessionFactory.currentSession.flush()
                sessionFactory.currentSession.clear()
                log.debug("processRevoke - processed ${representationDocuments.getRowNumber()} representationDocument updates")
            }
        }
        userVS.setType(UserVS.Type.USER).save()
        String toUser = userVS.getNif()
        String subject = messageSource.getMessage('unsubscribeRepresentativeValidationSubject', null, locale)
        SMIMEMessage smimeMessageResp = signatureVSService.getSMIMEMultiSigned(toUser, smimeMessage, subject)
        messageSMIMEReq.setSMIME(smimeMessageResp)
		representativeDocument.setCancellationSMIME(messageSMIMEReq).setDateCanceled(Calendar.getInstance().getTime()).save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, messageSMIME: messageSMIMEReq, userVS:userVS,
                type:TypeVS.REPRESENTATIVE_REVOKE, contentType: ContentTypeVS.JSON_SIGNED,
				message:messageSource.getMessage('representativeRevokeMsg', [userVS.getNif()].toArray(), locale))
	}
	
	//{"operation":"REPRESENTATIVE_ACCREDITATIONS_REQUEST","representativeNif":"...",
	//"representativeName":"...","selectedDate":"2013-05-20 09:50:33","email":"...","UUID":"..."}
	ResponseVS processAccreditationsRequest(MessageSMIME messageSMIMEReq) {
		String msg = null
		SMIMEMessage smimeMessage = messageSMIMEReq.getSMIME()
		UserVS userVS = messageSMIMEReq.getUserVS();
        JSONObject messageJSON = JSON.parse(smimeMessage.getSignedContent())
        String requestValidatedNIF =  NifUtils.validate(messageJSON.representativeNif)
        Date selectedDate = getDateFromString(messageJSON.selectedDate)
        if(!requestValidatedNIF || !messageJSON.operation ||
                (TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST != TypeVS.valueOf(messageJSON.operation))||
                !selectedDate || !messageJSON.email || !messageJSON.UUID ){
            msg = messageSource.getMessage('representativeAccreditationRequestErrorMsg', null, locale)
            log.error "processAccreditationsRequest - ERROR DATA - ${msg} - ${messageJSON.toString()}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR)
        }
        UserVS representative = UserVS.findWhere(nif:requestValidatedNIF,
                type:UserVS.Type.REPRESENTATIVE)
        if(!representative) {
            msg = messageSource.getMessage('representativeNifErrorMsg',
                    [requestValidatedNIF].toArray(), locale)
            log.error "processAccreditationsRequest - ERROR REPRESENTATIVE - ${msg}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR)
        }
        runAsync {
            ResponseVS backupGenResponseVS = getAccreditationsBackup( representative, selectedDate ,locale)
            if(ResponseVS.SC_OK == backupGenResponseVS?.statusCode) {
                File backupFile = backupGenResponseVS.file
                BackupRequestVS backupRequest = new BackupRequestVS(filePath:backupFile.getAbsolutePath(),
                        type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST, representative:representative,
                        messageSMIME:messageSMIMEReq, email:messageJSON.email)
                BackupRequestVS.withTransaction {
                    if (!backupRequest.save()) {
                        backupRequest.errors.each {
                            log.error("processAccreditationsRequest - ERROR backupRequest - ${it}")}
                    }
                }
                log.debug("processAccreditationsRequest - saved BackupRequestVS '${backupRequest.id}'");
                mailSenderService.sendRepresentativeAccreditations(backupRequest, messageJSON.selectedDate)
            } else log.error("processAccreditationsRequest - ERROR creating backup");
        }
        msg = messageSource.getMessage('backupRequestOKMsg', [messageJSON.email].toArray(), locale)
        new ResponseVS(statusCode:ResponseVS.SC_OK,	message:msg,
                type:TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST)
	}
		
	public Map getRepresentativeMap(UserVS representative) {
		RepresentativeDocument representativeDocument = RepresentativeDocument.findWhere(userVS:representative,
			state: RepresentativeDocument.State.OK)
		String representativeMessageURL =
			"${grailsApplication.config.grails.serverURL}/messageSMIME/${representativeDocument.activationSMIME?.id}"
		String imageURL = "${grailsLinkGenerator.link(controller: 'representative', absolute:true)}/${representative?.id}/image"
		String URL = "${grailsLinkGenerator.link(controller: 'representative', absolute:true)}/${representative?.id}"
		def numRepresentations = UserVS.countByRepresentative(representative) + 1//plus the representative itself
		def representativeMap = [id: representative.id, nif:representative.nif, type:representative.type.toString(),
             URL:URL, representativeMessageURL:representativeMessageURL,
			 imageURL:imageURL, numRepresentations:numRepresentations,
			 name: representative.name, firstName:representative.firstName, lastName:representative.lastName]
		return representativeMap
	}
	
	public Map getRepresentativeDetailedMap(UserVS representative) {
		Map representativeMap = getRepresentativeMap(representative)
		representativeMap.description = representative.description
		representativeMap.votingHistory = []
		return representativeMap
	}
	
}