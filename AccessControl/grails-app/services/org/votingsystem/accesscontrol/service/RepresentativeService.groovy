package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import org.bouncycastle.util.encoders.Base64
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.AccessRequestVS
import org.votingsystem.model.BackupRequestVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.ImageVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.RepresentationDocumentVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VoteVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.apache.commons.lang.time.DateUtils
import org.votingsystem.util.NifUtils

import java.security.MessageDigest
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat


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
	LinkGenerator grailsLinkGenerator
	/**
	 * Creates backup of the state of all the representatives for a closed event
	 */
	private synchronized ResponseVS getAccreditationsBackupForEvent (EventVSElection event, Locale locale){
		log.debug("getAccreditationsBackupForEvent - event: ${event.id}")
		String msg = null
		if(!event) {
			msg = messageSource.getMessage('nullParamErrorMsg', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:msg)
		}
		if(event.isActive(Calendar.getInstance().getTime())) {
			msg = messageSource.getMessage('eventOpenErrorMsg',  [event.id].toArray(), locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:msg)	
		}
		
		Map<String, File> mapFiles = filesService.getBackupFiles(event, TypeVS.REPRESENTATIVE_DATA, locale)
		File zipResult   = mapFiles.zipResult
		File metaInfFile = mapFiles.metaInfFile
		File filesDir    = mapFiles.filesDir

		Date selectedDate = event.getDateFinish();
        String downloadFileName = messageSource.getMessage('repAccreditationsBackupForEventFileName',
                [event.id].toArray(), locale)

		if(zipResult.exists()) {
			log.debug("getAccreditationsBackupForEvent - backup file already exists")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.ZIP,
                    messageBytes: zipResult.getBytes(), message: downloadFileName)
		}		
		
		Map optionsMap = [:]
		event.fieldsEventVS.each {option ->
			def numVoteRequests = VoteVS.countByOptionSelectedAndState(option, VoteVS.State.OK)
			def voteCriteria = VoteVS.createCriteria()
			def numUsersWithVote = voteCriteria.count {
				createAlias("certificateVS", "certificateVS")
				isNull("certificateVS.userVS")
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
		
		int numRepresentatives = UserVS.
			countByTypeAndRepresentativeRegisterDateLessThanEquals(
				UserVS.Type.REPRESENTATIVE, selectedDate)
		log.debug("num representatives: ${numRepresentatives}")
		
		int numRepresentativesWithAccessRequest = 0
		int numRepresentativesWithVote = 0
		int numTotalRepresented = 0
		int numTotalRepresentedWithAccessRequest = 0
		int numVotesRepresentedByRepresentatives = 0	
		
		long representativeBegin = System.currentTimeMillis()
		def criteria = UserVS.createCriteria()
		def representatives = criteria.scroll {
			eq("type", UserVS.Type.REPRESENTATIVE)
			le("representativeRegisterDate", selectedDate)
		}
		Map representativesMap = [:]
		while (representatives.next()) {
			UserVS representative = (UserVS) representatives.get(0);
			
			DecimalFormat formatted = new DecimalFormat("00000000");
			int delegationsBatch = 0
			String representativeBaseDir = "${filesDir.absolutePath}/${representatives.getRowNumber()}_representative_${representative.nif}/batch_${formatted.format(++delegationsBatch)}"
			new File(representativeBaseDir).mkdirs()

			if(representative.type != UserVS.Type.REPRESENTATIVE) {
				//check if active on selected date
				MessageSMIME revocationMessage = MessageSMIME.findByTypeAndDateCreatedLessThan(
					TypeVS.REPRESENTATIVE_REVOKE, selectedDate)
				if(revocationMessage) {
					log.debug("${representative.nif} wasn't active on ${selectedDate}")
					continue
				}
			}
			//representative active on selected date
			def representationDoc
			int numRepresented = 1 //The representative itself
			int numRepresentedWithAccessRequest = 0
			
			def representationDocumentCriteria = RepresentationDocumentVS.createCriteria()
			def representationDocuments = representationDocumentCriteria.scroll {
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
			while (representationDocuments.next()) {
				++numRepresented
				RepresentationDocumentVS repDocument = (RepresentationDocumentVS) representationDocuments.get(0);
				UserVS represented = repDocument.userVS
				AccessRequestVS representedAccessRequest = AccessRequestVS.findWhere(
					state:AccessRequestVS.State.OK, userVS:represented, eventVSElection:event)
				String repDocFileName = null
				if(representedAccessRequest) {
					numRepresentedWithAccessRequest++
					repDocFileName = "${representativeBaseDir}/${representationDocuments.getRowNumber()}_WithRequest_RepDoc_${represented.nif}.p7m"
				} else repDocFileName = "${representativeBaseDir}/${representationDocuments.getRowNumber()}_RepDoc_${represented.nif}.p7m"
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
			AccessRequestVS accessRequestVS = null
			State state = State.WITHOUT_ACCESS_REQUEST
			AccessRequestVS.withTransaction {
				accessRequestVS = AccessRequestVS.findWhere(
					eventVSElection:event, userVS:representative,
					state:AccessRequestVS.State.OK)
				if(accessRequestVS) {//Representative has access request
					numRepresentativesWithAccessRequest++;
					state = State.WITH_ACCESS_REQUEST
				}
			}
			VoteVS representativeVote
			if(accessRequestVS) {
				VoteVS.withTransaction {
					def voteCriteria = VoteVS.createCriteria()
					representativeVote = voteCriteria.get () {
						createAlias("certificateVS", "certificateVS")
						eq("certificateVS.userVS", representative)
						eq("state", VoteVS.State.OK)
						eq("eventVS", event)
					}
				}
			}
			int numVotesRepresentedByRepresentative = 0
			if(representativeVote) {
				state = State.WITH_VOTE
				++numRepresentativesWithVote
				numVotesRepresentedByRepresentative =
					numRepresented  - numRepresentedWithAccessRequest
				numVotesRepresentedByRepresentatives += numVotesRepresentedByRepresentative
				optionsMap[representativeVote.optionSelected.id].numVotesResult += numVotesRepresentedByRepresentative
			}			
			
			Map representativeMap = [id:representative.id,
				optionSelectedId:representativeVote?.optionSelected?.id,
				numRepresentedWithVote:numRepresentedWithAccessRequest,
				numRepresentations: numRepresented,
				numVotesRepresented:numVotesRepresentedByRepresentative]
			representativesMap[representative.nif] = representativeMap
			
			String elapsedTimeStr = getElapsedTimeHoursMinutesMillisFromMilliseconds(
				System.currentTimeMillis() - representativeBegin)
			
			
			/*File representativesReportFile = mapFiles.representativesReportFile
			representativesReportFile.write("")
			String csvLine = "${representative.nif}, " +
				"numRepresented:${formatted.format(numRepresented)}, " +
			    "numRepresentedWithAccessRequest:${formatted.format(numRepresentedWithAccessRequest)}, " +
			    "${state.toString()}\n"
			representativesReportFile.append(csvLine)*/
			log.debug("processed ${representatives.getRowNumber()} of ${numRepresentatives} representatives - ${elapsedTimeStr}")
			if(((representatives.getRowNumber() + 1)  % 100) == 0) {
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
		
		return new ResponseVS(data:metaInfMap,
			statusCode:ResponseVS.SC_OK)
	}
	
	/**
	 * Makes a Representative map
	 * (Estimativo, puede haber cambios en el recuento final. Se pueden producir incosistencias 
	 * en los resultados debido a los usuarios que hayan cambiado de representante a mitad de la votación. 
	 * En el backup no se presenta este fallo)
	 */
	private synchronized ResponseVS getAccreditationsMapForEvent (
			EventVSElection event, Locale locale){
		log.debug("getAccreditationsMapForEvent - event: ${event?.id}")
		String msg = null
		if(!event) {
			msg = messageSource.getMessage('nullParamErrorMsg', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:msg)
		}
		
		Date selectedDate = Calendar.getInstance().getTime();
		/*if(event.getDateFinish().before(selectedDate)) {
			log.debug("Event finished, fetching map from backup data")
			Map eventMetaInfMap = JSON.parse(event.metaInf)
			Map representativeAccreditationsMap = eventMetaInfMap[
				Type.REPRESENTATIVE_DATA.toString()]
			return new Respuesta(statusCode:ResponseVS.SC_OK, 
				data:representativeAccreditationsMap)
		}*/
		log.debug("getAccreditationsMapForEvent - selectedDate: ${selectedDate} ")
		Map optionsMap = [:]
		event.fieldsEventVS.each {option ->
			def numVoteRequests = VoteVS.countByOpcionDeEventoAndState(option, VoteVS.State.OK)
			def voteCriteria = VoteVS.createCriteria()
			def numUsersWithVote = voteCriteria.count {
				createAlias("certificateVS", "certificateVS")
				isNull("certificateVS.userVS")
				eq("state", VoteVS.State.OK)
				eq("eventVSElection", event)
				eq("fieldEventVS", option)
			}
			int numRepresentativesWithVote = numVoteRequests - numUsersWithVote
			Map optionMap = [content:option.content,
				numVoteRequests:numVoteRequests, numUsersWithVote:numUsersWithVote,
				numRepresentativesWithVote:numRepresentativesWithVote,
				numVotesResult:numUsersWithVote]
			optionsMap[option.id] = optionMap
		}		
		
		int numRepresentatives = UserVS.
			countByTypeAndRepresentativeRegisterDateLessThanEquals(
			UserVS.Type.REPRESENTATIVE, selectedDate)
		int numRepresentativesWithAccessRequest = 0
		
		AccessRequestVS.withTransaction {
			def criteria = AccessRequestVS.createCriteria()
			numRepresentativesWithAccessRequest = criteria.count {
				createAlias("userVS", "userVS")
				eq("userVS.type", UserVS.Type.REPRESENTATIVE)
				eq("state", AccessRequestVS.State.OK)
				eq("eventVSElection", event)
			}
		}		
		
		int numRepresentativesWithVote = 0
		int numTotalRepresented = 0
		int numTotalRepresentedWithAccessRequest = 0
		int numVotesRepresentedByRepresentatives = 0
		
		Map representativesMap = [:]
		
		UserVS.withTransaction {
			def criteria = UserVS.createCriteria()
			def representatives = criteria.scroll {
				eq("type", UserVS.Type.REPRESENTATIVE)
				le("representativeRegisterDate", selectedDate)
			}
			
			while (representatives.next()) {
				UserVS representative = (UserVS) representatives.get(0);
				
				int numRepresented = 0
				RepresentationDocumentVS.withTransaction {
					def representationDocumentCriteria = RepresentationDocumentVS.createCriteria()
					//The representative itself
					numRepresented = 1 + representationDocumentCriteria.count {
						isNull("cancellationSMIME")
						eq("representative", representative)
					}
					numTotalRepresented += numRepresented
				}	

				log.debug("${representative.nif} represents '${numRepresented}' users")
	
				def numRepresentedWithAccessRequest = 0
				AccessRequestVS.withTransaction {
					def accessRequestCriteria = AccessRequestVS.createCriteria()
					numRepresentedWithAccessRequest = accessRequestCriteria.count {
						createAlias("userVS","userVS")
						eq("userVS.representative", representative)
						eq("state", AccessRequestVS.State.OK)
						eq("eventVSElection", event)
					}
				}
				numTotalRepresentedWithAccessRequest += numRepresentedWithAccessRequest
							
				def representativeVote
				VoteVS.withTransaction {
					def voteCriteria = VoteVS.createCriteria()
					representativeVote = voteCriteria.get {
						createAlias("certificateVS", "certificateVS")
						eq("certificateVS.userVS", representative)
						eq("state", VoteVS.State.OK)
						eq("eventVSElection", event)
					}
				}
				int numVotesRepresentedByRepresentative = 0
				
				if(representativeVote) {
					++numRepresentativesWithVote
					numVotesRepresentedByRepresentative =
						numRepresented  - numRepresentedWithAccessRequest
					optionsMap[representativeVote.getFieldEventVS.id].numVotesResult += numVotesRepresentedByRepresentative
				}
				numVotesRepresentedByRepresentatives += numVotesRepresentedByRepresentative
				
				Map representativeMap = [id:representative.id,
					optionSelectedId:representativeVote?.getFieldEventVS?.id,
					numRepresentedWithVote:numRepresentedWithAccessRequest,
					numRepresentations: numRepresented,
					numVotesRepresented:numVotesRepresentedByRepresentative]
				

				representativesMap[representative.nif] = representativeMap
				if((representatives.getRowNumber() % 100) == 0) {
					sessionFactory.currentSession.flush()
					sessionFactory.currentSession.clear()
				}
			}
			
		}

		def metaInfMap = [numRepresentatives:numRepresentatives,
			numRepresentativesWithAccessRequest:numRepresentativesWithAccessRequest,
			numRepresentativesWithVote:numRepresentativesWithVote,
			numRepresentedWithAccessRequest:numTotalRepresentedWithAccessRequest,
			numRepresented:numTotalRepresented,
			numVotesRepresentedByRepresentatives:numVotesRepresentedByRepresentatives,
			representatives:representativesMap, options:optionsMap]
		
		return new ResponseVS(statusCode:ResponseVS.SC_OK, data:metaInfMap)
	}
	
					
	private synchronized ResponseVS getAccreditationsBackup (
		UserVS representative, Date selectedDate, Locale locale){
		log.debug("getAccreditationsBackup - representative: ${representative.nif}" +
			" - selectedDate: ${selectedDate}")
		def representationDocuments
		RepresentationDocumentVS.withTransaction {
			def criteria = RepresentationDocumentVS.createCriteria()
			representationDocuments = criteria.scroll {
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
		}
		
		def selectedDateStr = getShortStringFromDate(selectedDate)
		String serviceURLPart = messageSource.getMessage(
			'representativeAcreditationsBackupPath', [representative.nif].toArray(), locale)
		def basedir = "${grailsApplication.config.VotingSystem.backupCopyPath}" +
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
				 return new ResponseVS(statusCode:ResponseVS.SC_OK, 
					 message:backupURL, data:metaInfMap)
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
		return new ResponseVS(statusCode:ResponseVS.SC_OK, 
			data:metaInfMap, message:backupURL)
	}
	
	//{"operation":"REPRESENTATIVE_SELECTION","representativeNif":"...","representativeName":"...","UUID":"..."}
	public synchronized ResponseVS saveDelegation(MessageSMIME messageSMIMEReq, Locale locale) {
		log.debug("saveDelegation -")
		//def future = callAsync {}
		//return future.get(30, TimeUnit.SECONDS)
		MessageSMIME messageSMIME = null
		SMIMEMessageWrapper smimeMessage = messageSMIMEReq.getSmimeMessage()
		RepresentationDocumentVS representationDocument = null
		UserVS userVS = messageSMIMEReq.getUserVS()
		String msg = null
		try { 
			if(UserVS.Type.REPRESENTATIVE == userVS.type) {
				msg = messageSource.getMessage('userIsRepresentativeErrorMsg',
					[userVS.nif].toArray(), locale)
				log.error "saveDelegation - ERROR - user '${userVS.nif}' is REPRESENTATIVE - ${msg}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.REPRESENTATIVE_SELECTION_ERROR)
			}
			def messageJSON = JSON.parse(smimeMessage.getSignedContent())
			String requestValidatedNIF =  NifUtils.validate(messageJSON.representativeNif)
			if(userVS.nif == requestValidatedNIF) {
				msg = messageSource.getMessage('representativeSameUserNifErrorMsg',
					[requestValidatedNIF].toArray(), locale)
				log.error("saveDelegation - ERROR SAME USER SELECTION - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
					message:msg, type:TypeVS.REPRESENTATIVE_SELECTION_ERROR)
			}
			if(!requestValidatedNIF || !messageJSON.operation || !messageJSON.representativeNif ||
				(TypeVS.REPRESENTATIVE_SELECTION != TypeVS.valueOf(messageJSON.operation))) {
				msg = messageSource.getMessage('representativeSelectionDataErrorMsg', null, locale)
				log.error("saveDelegation - ERROR DATA - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
					message:msg, type:TypeVS.REPRESENTATIVE_SELECTION_ERROR)
			}
			UserVS representative = UserVS.findWhere(
				nif:requestValidatedNIF, type:UserVS.Type.REPRESENTATIVE)
			if(!representative) {
				msg = messageSource.getMessage('representativeNifErrorMsg',
					[requestValidatedNIF].toArray(), locale)
				log.error "saveDelegation - ERROR NIF REPRESENTATIVE - ${msg}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.REPRESENTATIVE_SELECTION_ERROR)
			}
			userVS.representative = representative
			RepresentationDocumentVS.withTransaction {
				representationDocument = RepresentationDocumentVS.findWhere(
                        userVS:userVS, state:RepresentationDocumentVS.State.OK)
				if(representationDocument) {
					log.debug("cancelRepresentationDocument - User changing representative")
					representationDocument.state = RepresentationDocumentVS.State.CANCELLED
					representationDocument.cancellationSMIME = messageSMIMEReq
					representationDocument.dateCanceled = userVS.getTimeStampToken().
							getTimeStampInfo().getGenTime();
					representationDocument.save(flush:true)
					log.debug("cancelRepresentationDocument - cancelled user '${userVS.nif}' " +
                            " - representationDocument ${representationDocument.id}")
				} else log.debug("cancelRepresentationDocument - user '${userVS.nif}' without representative")
				representationDocument = new RepresentationDocumentVS(activationSMIME:messageSMIMEReq,
                        userVS:userVS, representative:representative, state:RepresentationDocumentVS.State.OK);
				representationDocument.save()
			}
						
			msg = messageSource.getMessage('representativeAssociatedMsg',
				[messageJSON.representativeName, userVS.nif].toArray(), locale)
			log.debug "saveDelegation - ${msg}"
			
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = userVS.getNif()
			String subject = messageSource.getMessage(
					'representativeSelectValidationSubject', null, locale)
			SMIMEMessageWrapper smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(
				fromUser, toUser, smimeMessage, subject)
			MessageSMIME messageSMIMEResp = new MessageSMIME( smimeMessage:smimeMessageResp,
					type:TypeVS.RECEIPT, smimeParent: messageSMIMEReq, content:smimeMessageResp.getBytes())
			MessageSMIME.withTransaction {
				messageSMIMEResp.save();
			}
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg,
				data:messageSMIMEResp, type:TypeVS.REPRESENTATIVE_SELECTION)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			msg = messageSource.getMessage(
				'representativeSelectErrorMsg', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
				message:msg, type:TypeVS.REPRESENTATIVE_SELECTION_ERROR)
		}
	}

    public ResponseVS saveAnonymousDelegation(MessageSMIME messageSMIMEReq, Locale locale) {
        log.debug("saveAnonymousDelegation")

    }


	private void cancelRepresentationDocument(MessageSMIME messageSMIMEReq, UserVS userVS) {
		RepresentationDocumentVS.withTransaction {
			RepresentationDocumentVS representationDocument = RepresentationDocumentVS.
				findWhere(userVS:userVS, state:RepresentationDocumentVS.State.OK)
			if(representationDocument) {
				log.debug("cancelRepresentationDocument - User changing representative")
				representationDocument.state = RepresentationDocumentVS.State.CANCELLED
				representationDocument.cancellationSMIME = messageSMIMEReq
				representationDocument.dateCanceled = userVS.getTimeStampToken().getTimeStampInfo().getGenTime();
				representationDocument.save(flush:true)
				log.debug("cancelRepresentationDocument - user '${userVS.nif}' " +
                        " - representationDocument ${representationDocument.id}")
			} else log.debug("cancelRepresentationDocument - user '${userVS.nif}' doesn't have representative")
		}
	}
	
    ResponseVS saveRepresentativeData(MessageSMIME messageSMIMEReq, 
		byte[] imageBytes, Locale locale) {
		log.debug("saveRepresentativeData - ")
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		UserVS userVS = messageSMIMEReq.getUserVS()
		log.debug("saveRepresentativeData - userVS: ${userVS.nif}")
		String msg
		try {
			def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
			String base64ImageHash = messageJSON.base64ImageHash
			MessageDigest messageDigest = MessageDigest.getInstance(ContextVS.VOTING_DATA_DIGEST);
			byte[] resultDigest =  messageDigest.digest(imageBytes);
			String base64ResultDigest = new String(Base64.encode(resultDigest));
			log.debug("saveRepresentativeData - base64ImageHash: ${base64ImageHash}" + 
				" - server calculated base64ImageHash: ${base64ResultDigest}")
			if(!base64ResultDigest.equals(base64ImageHash)) {
				msg = messageSource.getMessage('imageHashErrorMsg', null, locale)
				log.error("saveRepresentativeData - ERROR ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.REPRESENTATIVE_DATA_ERROR)
			}
			//String base64EncodedImage = messageJSON.base64RepresentativeEncodedImage
			//BASE64Decoder decoder = new BASE64Decoder();
			//byte[] imageFileBytes = decoder.decodeBuffer(base64EncodedImage);
			ImageVS newImage = new ImageVS(userVS:userVS, messageSMIME:messageSMIMEReq,
				type:ImageVS.Type.REPRESENTATIVE, fileBytes:imageBytes)
			if(UserVS.Type.REPRESENTATIVE != userVS.type) {
				userVS.type = UserVS.Type.REPRESENTATIVE
				userVS.representativeRegisterDate = Calendar.getInstance().getTime()
				userVS.representative = null
				cancelRepresentationDocument(messageSMIMEReq, userVS);
				msg = messageSource.getMessage('representativeDataCreatedOKMsg', 
					[userVS.name, userVS.firstName].toArray(), locale)
			} else {
				def representations = UserVS.countByRepresentative(userVS)
				msg = messageSource.getMessage('representativeDataUpdatedMsg',
					[userVS.name, userVS.firstName].toArray(), locale)
			} 
			userVS.setDescription(messageJSON.representativeInfo)
			userVS.representativeMessage = messageSMIMEReq
			UserVS.withTransaction {
				userVS.save(flush:true)
			}
			ImageVS.withTransaction {
				def images = ImageVS.findAllWhere(userVS:userVS,
					type:ImageVS.Type.REPRESENTATIVE)
				images?.each {
					it.type = ImageVS.Type.REPRESENTATIVE_CANCELLED
					it.save()
				}
				newImage.save(flush:true)
			}
			
			MessageSMIME.withTransaction {
				def previousMessages = MessageSMIME.findAllWhere(userVS:userVS,
					type:TypeVS.REPRESENTATIVE_DATA)
				previousMessages?.each {message ->
					message.type = TypeVS.REPRESENTATIVE_DATA_OLD
					message.save()
				}
			}
			log.debug "saveRepresentativeData - user:${userVS.nif} - image: ${newImage.id}"
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg,  type:TypeVS.REPRESENTATIVE_DATA)
		} catch(Exception ex) {
			log.error ("${ex.getMessage()} - user: ${userVS.nif}", ex)
			msg = messageSource.getMessage('representativeDataErrorMsg', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
				message:msg, type:TypeVS.REPRESENTATIVE_DATA_ERROR)
		}
    }
	
	private ResponseVS getVotingHistoryBackup (UserVS representative,
		Date dateFrom, Date dateTo, Locale locale){
		log.debug("getVotingHistoryBackup - representative: ${representative.nif}" + 
			" - dateFrom: ${dateFrom} - dateTo: ${dateTo}")
		
		def dateFromStr = getShortStringFromDate(dateFrom)
		def dateToStr = getShortStringFromDate(dateTo)
		
		String serviceURLPart = messageSource.getMessage(
			'representativeVotingHistoryBackupPartPath', [representative.nif].toArray(), locale)
		def basedir = "${grailsApplication.config.VotingSystem.backupCopyPath}" +
			"/RepresentativeHistoryVoting/${dateFromStr}_${dateToStr}/${serviceURLPart}"
		log.debug("getVotingHistoryBackup - basedir: ${basedir}")
		File zipResult = new File("${basedir}.zip")

		String datePathPart = getShortStringFromDate(Calendar.getInstance().getTime())
		String backupURL = "/backup/${datePathPart}/${serviceURLPart}.zip"
		String webappBackupPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${backupURL}"
		
		File metaInfFile;
		if(zipResult.exists()) {
			 metaInfFile = new File("${basedir}/meta.inf")
			 if(metaInfFile) {
				 def metaInfMap = JSON.parse(metaInfFile.text)
				 log.debug("getVotingHistoryBackup - ${zipResult.name} already exists");
				 return new ResponseVS(statusCode:ResponseVS.SC_OK,
					 data:metaInfMap, message:backupURL)
			 }
		}
		new File(basedir).mkdirs()
		String voteFileName = messageSource.getMessage('voteFileName', null, locale)
		int numVotes = 0
		VoteVS.withTransaction {
			def voteCriteria = VoteVS.createCriteria()
			def representativeVotes = voteCriteria.scroll {
				createAlias("certificateVS", "certificateVS")
				eq("certificateVS.userVS", representative)
				eq("state", VoteVS.State.OK)
				between("dateCreated", dateFrom, dateTo)
			}
			while (representativeVotes.next()) {
				numVotes++
				VoteVS vote = (VoteVS) representativeVotes.get(0);
				MessageSMIME voteSMIME = vote.messageSMIME
				
				String voteId = String.format('%08d', vote.id)
				String voteFilePath = "${basedir}/${voteFileName}_${voteId}.p7m"
				MessageSMIME messageSMIME = vote.messageSMIME
				File smimeFile = new File(voteFilePath)
				smimeFile.setBytes(messageSMIME.content)
			}
			log.debug("${representative.nif} -> ${numVotes} votes ");
			if((representativeVotes.getRowNumber() % 100) == 0) {
				sessionFactory.currentSession.flush() 
				sessionFactory.currentSession.clear()
			}
		}
		def metaInfMap = [dateFrom: getStringFromDate(dateFrom),
			dateTo:getStringFromDate(dateTo), numVotes:numVotes,
			representativeURL:"${grailsApplication.config.grails.serverURL}/representative/${representative.id}"]
		String metaInfJSONStr = metaInfMap as JSON
		metaInfFile = new File("${basedir}/meta.inf")
		metaInfFile.write(metaInfJSONStr)
		def ant = new AntBuilder()
		ant.zip(destfile: zipResult, basedir: basedir)
		ant.copy(file: zipResult, tofile: webappBackupPath)
		
		log.debug("getVotingHistoryBackup - zipResult: ${zipResult.absolutePath} " + 
			" - backupURL: ${backupURL}")
		return new ResponseVS(statusCode:ResponseVS.SC_OK, 
			data:metaInfMap, message:backupURL)
	}

    ResponseVS validateAnonymousRequest(MessageSMIME messageSMIMEReq, Locale locale) {
        log.debug("validateAnonymousRequest")
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS userVS = messageSMIMEReq.getUserVS()
        String msg
        try {
            if(userVS.getDelegationFinish() != null) {
                MessageSMIME userDelegation = MessageSMIME.findWhere(type:TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION,
                        userVS:userVS)
                String userDelegationURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${userDelegation?.id}"
                msg = messageSource.getMessage('userWithPreviousDelegationErrorMsg' ,[userVS.nif,
                        userVS.delegationFinish].toArray(), locale)
                log.error(msg)
                return new ResponseVS(statusCode: ResponseVS.SC_ERROR, contentType: ContentTypeVS.JSON,
                        data:[message:msg, URL:userDelegationURL])
            }
            def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
            TypeVS operationType = TypeVS.valueOf(messageJSON.operation)
            if (messageJSON.accessControlURL && messageJSON.weeksOperationActive &&
                    (TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION == operationType)) {
                msg = messageSource.getMessage('requestWithErrorsMsg', null, locale)
                log.error("validateAnonymousRequest - msg: ${msg} - ${messageJSON}")
                return new ResponseVS(statusCode: ResponseVS.SC_ERROR,contentType:ContentTypeVS.JSON,data:[message:msg])
            }
            // _ TODO _
            //Date nearestMinute = DateUtils.round(now, Calendar.MINUTE);
            //??? Date nearestMonday = DateUtils.round(now, Calendar.MONDAY);
            Date delegationFinish = DateUtils.addDays(Calendar.getInstance().getTime(),
                    Integer.valueOf(messageJSON.weeksOperationActive) * 7)
            userVS.setDelegationFinish(delegationFinish)
            userVS.save()
            return new ResponseVS(statusCode: ResponseVS.SC_OK, type: TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION,
                    userVS:userVS, data:[weeksOperationActive:messageJSON.weeksOperationActive])
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR, type:TypeVS.ERROR,
                    message:messageSource.getMessage('anonymousDelegationErrorMsg', null, locale))
        }
    }
	
	ResponseVS processVotingHistoryRequest(MessageSMIME messageSMIMEReq, Locale locale) {
		log.debug("processVotingHistoryRequest")
		SMIMEMessageWrapper smimeMessage = messageSMIMEReq.getSmimeMessage()
		UserVS userVS = messageSMIMEReq.getUserVS()
		def messageJSON
		String msg
		try {
			TypeVS type = TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST 
			//REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR
			messageJSON = JSON.parse(smimeMessage.getSignedContent())
			Date dateFrom = getDateFromString(messageJSON.dateFrom)
			Date dateTo = getDateFromString(messageJSON.dateTo)
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
				msg = messageSource.getMessage('operationErrorMsg',
					[messageJSON.operation].toArray(), locale)
				log.error "processVotingHistoryRequest - OPERATION ERROR - ${msg}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:msg, type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR)
			}
			String requestValidatedNIF =  NifUtils.validate(messageJSON.representativeNif)

			UserVS representative = UserVS.findWhere(nif:requestValidatedNIF,
				type:UserVS.Type.REPRESENTATIVE)
			if(!representative) {
				msg = messageSource.getMessage('representativeNifErrorMsg',
					[requestValidatedNIF].toArray(), locale)
				log.error "processVotingHistoryRequest - USER NOT REPRESENTATIVE ${msg}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, 
					type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR)
			}

			runAsync {
				ResponseVS backupGenResponseVS
				backupGenResponseVS = getVotingHistoryBackup(representative, dateFrom,  dateTo, locale)
				if(ResponseVS.SC_OK == backupGenResponseVS?.statusCode) {
		
					BackupRequestVS solicitudCopia = new BackupRequestVS(
						filePath:backupGenResponseVS.message,
						type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST, 
						representative:representative,
						messageSMIME:messageSMIMEReq, email:messageJSON.email)
					log.debug("messageSMIME: ${messageSMIMEReq.id} - ${solicitudCopia.type}");
					BackupRequestVS.withTransaction {
						if (!solicitudCopia.save()) {
							solicitudCopia.errors.each { 
								log.error("processVotingHistoryRequest - ERROR solicitudCopia - ${it}")}
						}
						
					}
					mailSenderService.sendRepresentativeVotingHistory(
						solicitudCopia, messageJSON.dateFrom, messageJSON.dateTo, locale)
				} else log.error("Error generando archivo de copias de respaldo");
			}
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			msg = messageSource.getMessage('requestErrorMsg', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
				message:msg, type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR)
		}
		msg = messageSource.getMessage('backupRequestOKMsg',
			[messageJSON.email].toArray(), locale)
		return new ResponseVS(statusCode:ResponseVS.SC_OK,
			type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST, message:msg)
	}
	
	ResponseVS processRevoke(MessageSMIME messageSMIMEReq, Locale locale) {
		String msg = null;
		SMIMEMessageWrapper smimeMessage = messageSMIMEReq.getSmimeMessage();
		UserVS userVS = messageSMIMEReq.getUserVS();
		log.debug("processRevoke - user ${userVS.nif}")
		try{
			def messageJSON = JSON.parse(smimeMessage.getSignedContent())
			TypeVS operationType = TypeVS.valueOf(messageJSON.operation)
			if(TypeVS.REPRESENTATIVE_REVOKE != operationType) {
				msg = messageSource.getMessage('operationErrorMsg',
					[messageJSON.operation].toArray(), locale)
				log.error "processRevoke - OPERATION ERROR - ${msg}" 
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:msg, type:TypeVS.REPRESENTATIVE_REVOKE_ERROR)
			}
			if(UserVS.Type.REPRESENTATIVE != userVS.type) {
				msg = messageSource.getMessage('unsubscribeRepresentativeUserErrorMsg',
					[userVS.nif].toArray(), locale)
				log.error "processRevoke - USER TYPE ERROR - ${msg}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.REPRESENTATIVE_REVOKE_ERROR)
			}
			//(TODO notify users)=====
			def representedUsers
			UserVS.withTransaction {
				def criteria = UserVS.createCriteria()
				representedUsers = criteria.scroll {
					eq("representative", userVS)
				}
				while (representedUsers.next()) {
					UserVS representedUser = (UserVS) representedUsers.get(0);
					representedUsers.type = UserVS.Type.USER_WITH_CANCELLED_REPRESENTATIVE
					representedUser.representative = null
					representedUser.save()
					if((representedUsers.getRowNumber() % 100) == 0) {
						sessionFactory.currentSession.flush() 
						sessionFactory.currentSession.clear()
						log.debug("processRevoke - processed ${representedUsers.getRowNumber()} user updates")
					}	
				}
			}

			RepresentationDocumentVS.withTransaction {
				def repDocCriteria = RepresentationDocumentVS.createCriteria()
				def representationDocuments = repDocCriteria.scroll {
					eq("state", RepresentationDocumentVS.State.OK)
					eq("representative", userVS)
				}
				while (representationDocuments.next()) {
					RepresentationDocumentVS representationDocument = (RepresentationDocumentVS) representationDocuments.get(0);
					representationDocument.state = RepresentationDocumentVS.State.CANCELLED_BY_REPRESENTATIVE
					representationDocument.cancellationSMIME = messageSMIMEReq
					representationDocument.dateCanceled = userVS.getTimeStampToken().
						getTimeStampInfo().getGenTime()
					representationDocument.save()
					if((representationDocuments.getRowNumber() % 100) == 0) {
						sessionFactory.currentSession.flush() 
						sessionFactory.currentSession.clear()
						log.debug("processRevoke - processed ${representationDocuments.getRowNumber()} representationDocument updates")
					}
				}
			}
			
			userVS.representativeMessage = messageSMIMEReq
			userVS.type = UserVS.Type.EX_REPRESENTATIVE
			
			UserVS.withTransaction  {
				userVS.save()
			}
			
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = userVS.getNif()
			String subject = messageSource.getMessage(
					'unsubscribeRepresentativeValidationSubject', null, locale)

			SMIMEMessageWrapper smimeMessageResp = signatureVSService.
				getMultiSignedMimeMessage(fromUser, toUser, smimeMessage, subject)
				
			MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent: messageSMIMEReq,
				content:smimeMessageResp.getBytes())
			MessageSMIME.withTransaction {
				messageSMIMEResp.save();
			}
			log.error "processRevoke - saved MessageSMIME '${messageSMIMEResp.id}'"
			msg =  messageSource.getMessage('representativeRevokeMsg',
				[userVS.getNif()].toArray(), locale)
			return new ResponseVS(statusCode:ResponseVS.SC_OK, 
				data:messageSMIMEResp, userVS:userVS,
				type:TypeVS.REPRESENTATIVE_REVOKE, message:msg )
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			msg = messageSource.getMessage(
				'representativeRevokeErrorMsg',null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
				message:msg, type:TypeVS.REPRESENTATIVE_REVOKE_ERROR)
		}
	}
	
	//{"operation":"REPRESENTATIVE_ACCREDITATIONS_REQUEST","representativeNif":"...",
	//"representativeName":"...","selectedDate":"2013-05-20 09:50:33","email":"...","UUID":"..."}
	ResponseVS processAccreditationsRequest(MessageSMIME messageSMIMEReq, Locale locale) {
		String msg = null
		SMIMEMessageWrapper smimeMessage = messageSMIMEReq.getSmimeMessage()
		UserVS userVS = messageSMIMEReq.getUserVS();
		log.debug("processAccreditationsRequest - userVS '{userVS.nif}'")
		RepresentationDocumentVS representationDocument = null
		def messageJSON = null
		try {
			messageJSON = JSON.parse(smimeMessage.getSignedContent())
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
					ResponseVS backupGenResponseVS = getAccreditationsBackup(
						representative, selectedDate ,locale)
					if(ResponseVS.SC_OK == backupGenResponseVS?.statusCode) {
						File archivoCopias = backupGenResponseVS.file
						BackupRequestVS solicitudCopia = new BackupRequestVS(
							filePath:archivoCopias.getAbsolutePath(),
							type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST,
							representative:representative,
							messageSMIME:messageSMIMEReq, email:messageJSON.email)
						BackupRequestVS.withTransaction {
							if (!solicitudCopia.save()) {
								solicitudCopia.errors.each {
									log.error("processAccreditationsRequest - ERROR solicitudCopia - ${it}")}
							}
						}
						log.debug("processAccreditationsRequest - saved BackupRequestVS '${solicitudCopia.id}'");
						mailSenderService.sendRepresentativeAccreditations(
							solicitudCopia, messageJSON.selectedDate, locale)
					} else log.error("processAccreditationsRequest - ERROR creating backup");
			}
			msg = messageSource.getMessage('backupRequestOKMsg',
				[messageJSON.email].toArray(), locale)
			new ResponseVS(statusCode:ResponseVS.SC_OK,	message:msg,
				type:TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			msg = messageSource.getMessage('representativeAccreditationRequestErrorMsg', null, locale)
			return new ResponseVS(message:msg,
				statusCode:ResponseVS.SC_ERROR,
				type:TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR)
		}
	}
		
	public Map getRepresentativeMap(UserVS representative) {
		//log.debug("getRepresentativeMap: ${representative.id} ")
		String representativeMessageURL = 
			"${grailsApplication.config.grails.serverURL}/messageSMIME/${representative.representativeMessage?.id}"
		ImageVS image
		ImageVS.withTransaction {
			image = ImageVS.findByTypeAndUserVS (ImageVS.Type.REPRESENTATIVE, representative)
		}
		String imageURL = "${grailsApplication.config.grails.serverURL}/representative/image/${image?.id}"
		String URL = "${grailsLinkGenerator.link(controller: 'representative', absolute:true)}/${representative?.id}"
		def numRepresentations = UserVS.countByRepresentative(representative) + 1//plus the representative itself
		def representativeMap = [id: representative.id, nif:representative.nif,
             URL:URL, representativeMessageURL:representativeMessageURL,
			 imageURL:imageURL, numRepresentations:numRepresentations,
			 name: representative.name, firstName:representative.firstName]
		return representativeMap
	}
	
	public Map getRepresentativeDetailedMap(UserVS representative) {
		Map representativeMap = getRepresentativeMap(representative)
		representativeMap.description = representative.description
		representativeMap.votingHistory = []
		return representativeMap
	}
	
}