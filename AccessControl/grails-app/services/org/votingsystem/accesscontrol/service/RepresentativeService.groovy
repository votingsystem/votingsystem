package org.votingsystem.accesscontrol.service

import org.codehaus.groovy.grails.web.json.JSONObject;
import org.codehaus.groovy.grails.web.mapping.LinkGenerator;

import java.security.MessageDigest
import java.text.DateFormat;
import java.text.DecimalFormat
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

import org.apache.commons.logging.Log;
import org.bouncycastle.util.encoders.Base64;
import org.votingsystem.accesscontrol.model.*
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.StringUtils

import grails.converters.JSON
import sun.misc.BASE64Decoder;

import org.hibernate.criterion.Projections

import java.util.concurrent.TimeUnit

class RepresentativeService {
	
	enum State {WITHOUT_ACCESS_REQUEST, WITH_ACCESS_REQUEST, WITH_VOTE}
	
	def subscripcionService
	def messageSource
	def grailsApplication
	def mailSenderService
	def firmaService
	def filesService
	def eventoService
	def sessionFactory
	LinkGenerator grailsLinkGenerator
	/**
	 * Creates backup of the state of all the representatives for a closed event
	 */
	private synchronized ResponseVS getAccreditationsBackupForEvent (
			EventoVotacion event, Locale locale){
		log.debug("getAccreditationsBackupForEvent - event: ${event.id}")
		String msg = null
		if(!event) {
			msg = messageSource.getMessage('nullParamErrorMsg', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:msg)
		}
		if(event.isOpen(DateUtils.getTodayDate())) {
			msg = messageSource.getMessage('eventOpenErrorMsg', 
				[event.id].toArray(), locale) 			
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:msg)	
		}
		
		Map<String, File> mapFiles = filesService.getBackupFiles(event, 
			TypeVS.REPRESENTATIVE_DATA, locale)
		File zipResult   = mapFiles.zipResult
		File metaInfFile = mapFiles.metaInfFile
		File filesDir    = mapFiles.filesDir

		Date selectedDate = event.getDateFinish();

		if(zipResult.exists()) {
			log.debug("getAccreditationsBackupForEvent - backup file already exists")
			return new ResponseVS(statusCode:ResponseVS.SC_OK,
				file:zipResult)
		}		
		
		Map optionsMap = [:]
		event.opciones.each {option ->
			def numVoteRequests = Voto.countByOpcionDeEventoAndEstado(option, Voto.Estado.OK)
			def voteCriteria = Voto.createCriteria()
			def numUsersWithVote = voteCriteria.count {
				createAlias("certificado", "certificado")
				isNull("certificado.usuario")
				eq("estado", Voto.Estado.OK)
				eq("eventoVotacion", event)
				eq("opcionDeEvento", option)
			}
			int numRepresentativesWithVote = numVoteRequests - numUsersWithVote
			Map optionMap = [content:option.contenido,
				numVoteRequests:numVoteRequests, numUsersWithVote:numUsersWithVote,
				numRepresentativesWithVote:numRepresentativesWithVote,
				numVotesResult:numUsersWithVote]
			optionsMap[option.id] = optionMap
		}		
		
		int numRepresentatives = Usuario.
			countByTypeAndRepresentativeRegisterDateLessThanEquals(
				Usuario.Type.REPRESENTATIVE, selectedDate)
		log.debug("num representatives: ${numRepresentatives}")
		
		int numRepresentativesWithAccessRequest = 0
		int numRepresentativesWithVote = 0
		int numTotalRepresented = 0
		int numTotalRepresentedWithAccessRequest = 0
		int numVotesRepresentedByRepresentatives = 0	
		
		long representativeBegin = System.currentTimeMillis()
		def criteria = Usuario.createCriteria()
		def representatives = criteria.scroll {
			eq("type", Usuario.Type.REPRESENTATIVE)
			le("representativeRegisterDate", selectedDate)
		}
		Map representativesMap = [:]
		while (representatives.next()) {
			Usuario representative = (Usuario) representatives.get(0);			
			
			DecimalFormat formatted = new DecimalFormat("00000000");
			int delegationsBatch = 0
			String representativeBaseDir = "${filesDir.absolutePath}/${representatives.getRowNumber()}_representative_${representative.nif}/batch_${formatted.format(++delegationsBatch)}"
			new File(representativeBaseDir).mkdirs()

			if(representative.type != Usuario.Type.REPRESENTATIVE) {
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
			
			def representationDocumentCriteria = RepresentationDocument.createCriteria()
			def representationDocuments = representationDocumentCriteria.scroll {
					eq("representative", representative)
					le("dateCreated", selectedDate)
					or {
						eq("state", RepresentationDocument.State.CANCELLED)
						eq("state", RepresentationDocument.State.OK)
					}
					or {
						isNull("dateCanceled")
						gt("dateCanceled", selectedDate)
					}
			}
			while (representationDocuments.next()) {
				++numRepresented
				RepresentationDocument repDocument = (RepresentationDocument) representationDocuments.get(0);
				Usuario represented = repDocument.user
				SolicitudAcceso representedAccessRequest = SolicitudAcceso.findWhere(
					estado:SolicitudAcceso.Estado.OK, usuario:represented, eventoVotacion:event)
				String repDocFileName = null
				if(representedAccessRequest) {
					numRepresentedWithAccessRequest++
					repDocFileName = "${representativeBaseDir}/${representationDocuments.getRowNumber()}_WithRequest_RepDoc_${represented.nif}.p7m"
				} else repDocFileName = "${representativeBaseDir}/${representationDocuments.getRowNumber()}_RepDoc_${represented.nif}.p7m"
				File representationDocFile = new File(repDocFileName)
				representationDocFile.setBytes(repDocument.activationSMIME.contenido)
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
			SolicitudAcceso solicitudAcceso = null
			State state = State.WITHOUT_ACCESS_REQUEST
			SolicitudAcceso.withTransaction {
				solicitudAcceso = SolicitudAcceso.findWhere(
					eventoVotacion:event, usuario:representative,
					estado:SolicitudAcceso.Estado.OK)
				if(solicitudAcceso) {//Representative has access request
					numRepresentativesWithAccessRequest++;
					state = State.WITH_ACCESS_REQUEST
				}
			}
			Voto representativeVote
			if(solicitudAcceso) {
				Voto.withTransaction {
					def voteCriteria = Voto.createCriteria()
					representativeVote = voteCriteria.get () {
						createAlias("certificado", "certificado")
						eq("certificado.usuario", representative)
						eq("estado", Voto.Estado.OK)
						eq("eventoVotacion", event)
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
				optionsMap[representativeVote.opcionDeEvento.id].numVotesResult += numVotesRepresentedByRepresentative
			}			
			
			Map representativeMap = [id:representative.id,
				optionSelectedId:representativeVote?.opcionDeEvento?.id,
				numRepresentedWithVote:numRepresentedWithAccessRequest,
				numRepresentations: numRepresented,
				numVotesRepresented:numVotesRepresentedByRepresentative]
			representativesMap[representative.nif] = representativeMap
			
			String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
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
			EventoVotacion event, Locale locale){
		log.debug("getAccreditationsMapForEvent - event: ${event?.id}")
		String msg = null
		if(!event) {
			msg = messageSource.getMessage('nullParamErrorMsg', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:msg)
		}
		
		Date selectedDate = DateUtils.getTodayDate();	
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
		event.opciones.each {option ->
			def numVoteRequests = Voto.countByOpcionDeEventoAndEstado(option, Voto.Estado.OK)
			def voteCriteria = Voto.createCriteria()
			def numUsersWithVote = voteCriteria.count {
				createAlias("certificado", "certificado")
				isNull("certificado.usuario")
				eq("estado", Voto.Estado.OK)
				eq("eventoVotacion", event)
				eq("opcionDeEvento", option)
			}
			int numRepresentativesWithVote = numVoteRequests - numUsersWithVote
			Map optionMap = [content:option.contenido,
				numVoteRequests:numVoteRequests, numUsersWithVote:numUsersWithVote,
				numRepresentativesWithVote:numRepresentativesWithVote,
				numVotesResult:numUsersWithVote]
			optionsMap[option.id] = optionMap
		}		
		
		int numRepresentatives = Usuario.
			countByTypeAndRepresentativeRegisterDateLessThanEquals(
			Usuario.Type.REPRESENTATIVE, selectedDate)		
		int numRepresentativesWithAccessRequest = 0
		
		SolicitudAcceso.withTransaction {
			def criteria = SolicitudAcceso.createCriteria()
			numRepresentativesWithAccessRequest = criteria.count {
				createAlias("usuario", "usuario")
				eq("usuario.type", Usuario.Type.REPRESENTATIVE)
				eq("estado", SolicitudAcceso.Estado.OK)
				eq("eventoVotacion", event)
			}
		}		
		
		int numRepresentativesWithVote = 0
		int numTotalRepresented = 0
		int numTotalRepresentedWithAccessRequest = 0
		int numVotesRepresentedByRepresentatives = 0
		
		Map representativesMap = [:]
		
		Usuario.withTransaction {
			def criteria = Usuario.createCriteria()
			def representatives = criteria.scroll {
				eq("type", Usuario.Type.REPRESENTATIVE)
				le("representativeRegisterDate", selectedDate)
			}
			
			while (representatives.next()) {
				Usuario representative = (Usuario) representatives.get(0);
				
				int numRepresented = 0
				RepresentationDocument.withTransaction {
					def representationDocumentCriteria = RepresentationDocument.createCriteria()
					//The representative itself
					numRepresented = 1 + representationDocumentCriteria.count {
						isNull("cancellationSMIME")
						eq("representative", representative)
					}
					numTotalRepresented += numRepresented
				}	

				log.debug("${representative.nif} represents '${numRepresented}' users")
	
				def numRepresentedWithAccessRequest = 0
				SolicitudAcceso.withTransaction {
					def accessRequestCriteria = SolicitudAcceso.createCriteria()
					numRepresentedWithAccessRequest = accessRequestCriteria.count {
						createAlias("usuario","usuario")
						eq("usuario.representative", representative)
						eq("estado", SolicitudAcceso.Estado.OK)
						eq("eventoVotacion", event)
					}
				}
				numTotalRepresentedWithAccessRequest += numRepresentedWithAccessRequest
							
				def representativeVote
				Voto.withTransaction {
					def voteCriteria = Voto.createCriteria()
					representativeVote = voteCriteria.get {
						createAlias("certificado", "certificado")
						eq("certificado.usuario", representative)
						eq("estado", Voto.Estado.OK)
						eq("eventoVotacion", event)
					}
				}
				int numVotesRepresentedByRepresentative = 0
				
				if(representativeVote) {
					++numRepresentativesWithVote
					numVotesRepresentedByRepresentative =
						numRepresented  - numRepresentedWithAccessRequest
					optionsMap[representativeVote.opcionDeEvento.id].numVotesResult += numVotesRepresentedByRepresentative
				}
				numVotesRepresentedByRepresentatives += numVotesRepresentedByRepresentative
				
				Map representativeMap = [id:representative.id,
					optionSelectedId:representativeVote?.opcionDeEvento?.id,
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
		Usuario representative, Date selectedDate, Locale locale){
		log.debug("getAccreditationsBackup - representative: ${representative.nif}" +
			" - selectedDate: ${selectedDate}")
		def representationDocuments
		RepresentationDocument.withTransaction {
			def criteria = RepresentationDocument.createCriteria()
			representationDocuments = criteria.scroll {
				eq("representative", representative)
				le("dateCreated", selectedDate)
				or {
					eq("state", RepresentationDocument.State.CANCELLED)
					eq("state", RepresentationDocument.State.OK)
				}
				or {
					isNull("dateCanceled")
					gt("dateCanceled", selectedDate)
				}
			}
		}
		
		def selectedDateStr = DateUtils.getShortStringFromDate(selectedDate)
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
			RepresentationDocument representationDocument = (RepresentationDocument)representationDocuments.get(0);
			MessageSMIME messageSMIME = representationDocument.activationSMIME
			File smimeFile = new File("${basedir}/${accreditationFileName}_${representationDocument.id}")
			smimeFile.setBytes(messageSMIME.contenido)
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
	public synchronized ResponseVS saveUserRepresentative(MessageSMIME messageSMIMEReq, Locale locale) {
		log.debug("saveUserRepresentative -")
		//def future = callAsync {}
		//return future.get(30, TimeUnit.SECONDS)
		MessageSMIME messageSMIME = null
		SMIMEMessageWrapper smimeMessage = messageSMIMEReq.getSmimeMessage()
		RepresentationDocument representationDocument = null
		Usuario usuario = messageSMIMEReq.getUsuario()
		String msg = null
		try { 
			if(Usuario.Type.REPRESENTATIVE == usuario.type) {
				msg = messageSource.getMessage('userIsRepresentativeErrorMsg',
					[usuario.nif].toArray(), locale)
				log.error "saveUserRepresentative - ERROR - user '${usuario.nif}' is REPRESENTATIVE - ${msg}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.REPRESENTATIVE_SELECTION_ERROR)
			}
			def messageJSON = JSON.parse(smimeMessage.getSignedContent())
			String requestValidatedNIF =  StringUtils.validarNIF(messageJSON.representativeNif)
			if(usuario.nif == requestValidatedNIF) {
				msg = messageSource.getMessage('representativeSameUserNifErrorMsg',
					[requestValidatedNIF].toArray(), locale)
				log.error("saveUserRepresentative - ERROR SAME USER SELECTION - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
					message:msg, type:TypeVS.REPRESENTATIVE_SELECTION_ERROR)
			}
			if(!requestValidatedNIF || !messageJSON.operation || !messageJSON.representativeNif ||
				(TypeVS.REPRESENTATIVE_SELECTION != TypeVS.valueOf(messageJSON.operation))) {
				msg = messageSource.getMessage('representativeSelectionDataErrorMsg', null, locale)
				log.error("saveUserRepresentative - ERROR DATA - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
					message:msg, type:TypeVS.REPRESENTATIVE_SELECTION_ERROR)
			}
			Usuario representative = Usuario.findWhere(
				nif:requestValidatedNIF, type:Usuario.Type.REPRESENTATIVE)
			if(!representative) {
				msg = messageSource.getMessage('representativeNifErrorMsg',
					[requestValidatedNIF].toArray(), locale)
				log.error "saveUserRepresentative - ERROR NIF REPRESENTATIVE - ${msg}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.REPRESENTATIVE_SELECTION_ERROR)
			}
			
			usuario.representative = representative
			RepresentationDocument.withTransaction {
				
				representationDocument = RepresentationDocument.findWhere(
					user:usuario, state:RepresentationDocument.State.OK)
				if(representationDocument) {
					log.debug("cancelRepresentationDocument - User changing representative")
					representationDocument.state = RepresentationDocument.State.CANCELLED
					representationDocument.cancellationSMIME = messageSMIMEReq
					representationDocument.dateCanceled = usuario.getTimeStampToken().
							getTimeStampInfo().getGenTime();
					representationDocument.save(flush:true)
					log.debug("cancelRepresentationDocument - cancelled user '${usuario.nif}' representationDocument ${representationDocument.id}")
				} else log.debug("cancelRepresentationDocument - user '${usuario.nif}' without representative")
				
				
				representationDocument = new RepresentationDocument(activationSMIME:messageSMIMEReq,
					user:usuario, representative:representative, state:RepresentationDocument.State.OK);
				representationDocument.save()
			}
						
			msg = messageSource.getMessage('representativeAssociatedMsg',
				[messageJSON.representativeName, usuario.nif].toArray(), locale)
			log.debug "saveUserRepresentative - ${msg}"
			
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = usuario.getNif()
			String subject = messageSource.getMessage(
					'representativeSelectValidationSubject', null, locale)
			SMIMEMessageWrapper smimeMessageResp = firmaService.getMultiSignedMimeMessage(
				fromUser, toUser, smimeMessage, subject)
			MessageSMIME messageSMIMEResp = new MessageSMIME(
					smimeMessage:smimeMessageResp,
					type:TypeVS.RECEIPT, smimePadre: messageSMIMEReq, 
					valido:true, contenido:smimeMessageResp.getBytes())
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
	
	private void cancelRepresentationDocument(MessageSMIME messageSMIMEReq, Usuario usuario) {
		RepresentationDocument.withTransaction {
			RepresentationDocument representationDocument = RepresentationDocument.
				findWhere(user:usuario, state:RepresentationDocument.State.OK)
			if(representationDocument) {
				log.debug("cancelRepresentationDocument - User changing representative")
				representationDocument.state = RepresentationDocument.State.CANCELLED
				representationDocument.cancellationSMIME = messageSMIMEReq
				representationDocument.dateCanceled = usuario.getTimeStampToken().
						getTimeStampInfo().getGenTime();
				representationDocument.save(flush:true)
				log.debug("cancelRepresentationDocument - user '${usuario.nif}' representationDocument ${representationDocument.id}")
			} else log.debug("cancelRepresentationDocument - user '${usuario.nif}' doesn't have representative")
		}
	}
	
    ResponseVS saveRepresentativeData(MessageSMIME messageSMIMEReq, 
		byte[] imageBytes, Locale locale) {
		log.debug("saveRepresentativeData - ")
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		Usuario usuario = messageSMIMEReq.getUsuario()
		log.debug("saveRepresentativeData - usuario: ${usuario.nif}")
		String msg
		try {
			def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
			String base64ImageHash = messageJSON.base64ImageHash
			MessageDigest messageDigest = MessageDigest.getInstance(
				grailsApplication.config.VotingSystem.votingHashAlgorithm);
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
			Image newImage = new Image(usuario:usuario, messageSMIME:messageSMIMEReq,
				type:Image.Type.REPRESENTATIVE, fileBytes:imageBytes)
			if(Usuario.Type.REPRESENTATIVE != usuario.type) {
				usuario.type = Usuario.Type.REPRESENTATIVE
				usuario.representativeRegisterDate = DateUtils.getTodayDate()
				usuario.representative = null
				cancelRepresentationDocument(messageSMIMEReq, usuario);				
				msg = messageSource.getMessage('representativeDataCreatedOKMsg', 
					[usuario.nombre, usuario.primerApellido].toArray(), locale)
			} else {
				def representations = Usuario.countByRepresentative(usuario)
				msg = messageSource.getMessage('representativeDataUpdatedMsg',
					[usuario.nombre, usuario.primerApellido].toArray(), locale)
			} 
			usuario.setInfo(messageJSON.representativeInfo)
			usuario.representativeMessage = messageSMIMEReq
			Usuario.withTransaction {
				usuario.save(flush:true)
			}
			Image.withTransaction {
				def images = Image.findAllWhere(usuario:usuario,
					type:Image.Type.REPRESENTATIVE)
				images?.each {
					it.type = Image.Type.REPRESENTATIVE_CANCELLED
					it.save()
				}
				newImage.save(flush:true)
			}
			
			MessageSMIME.withTransaction {
				def previousMessages = MessageSMIME.findAllWhere(usuario:usuario,
					type:TypeVS.REPRESENTATIVE_DATA)
				previousMessages?.each {message ->
					message.type = TypeVS.REPRESENTATIVE_DATA_OLD
					message.save()
				}
			}
			
			log.debug "saveRepresentativeData - user:${usuario.nif} - image: ${newImage.id}"
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, 
				type:TypeVS.REPRESENTATIVE_DATA, messageSMIME:messageSMIMEReq)
		} catch(Exception ex) {
			log.error ("${ex.getMessage()} - user: ${usuario.nif}", ex)
			msg = messageSource.getMessage('representativeDataErrorMsg', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
				message:msg, type:TypeVS.REPRESENTATIVE_DATA_ERROR)
		}
    }

	
	private ResponseVS getVotingHistoryBackup (Usuario representative, 
		Date dateFrom, Date dateTo, Locale locale){
		log.debug("getVotingHistoryBackup - representative: ${representative.nif}" + 
			" - dateFrom: ${dateFrom} - dateTo: ${dateTo}")
		
		def dateFromStr = DateUtils.getShortStringFromDate(dateFrom)
		def dateToStr = DateUtils.getShortStringFromDate(dateTo)
		
		String serviceURLPart = messageSource.getMessage(
			'representativeVotingHistoryBackupPartPath', [representative.nif].toArray(), locale)
		def basedir = "${grailsApplication.config.VotingSystem.backupCopyPath}" +
			"/RepresentativeHistoryVoting/${dateFromStr}_${dateToStr}/${serviceURLPart}"
		log.debug("getVotingHistoryBackup - basedir: ${basedir}")
		File zipResult = new File("${basedir}.zip")

		String datePathPart = DateUtils.getShortStringFromDate(DateUtils.getTodayDate())
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
		Voto.withTransaction {
			def voteCriteria = Voto.createCriteria()
			def representativeVotes = voteCriteria.scroll {
				createAlias("certificado", "certificado")
				eq("certificado.usuario", representative)
				eq("estado", Voto.Estado.OK)
				between("dateCreated", dateFrom, dateTo)
			}
			while (representativeVotes.next()) {
				numVotes++
				Voto vote = (Voto) representativeVotes.get(0);
				MessageSMIME voteSMIME = vote.messageSMIME
				
				String voteId = String.format('%08d', vote.id)
				String voteFilePath = "${basedir}/${voteFileName}_${voteId}.p7m"
				MessageSMIME messageSMIME = vote.messageSMIME
				File smimeFile = new File(voteFilePath)
				smimeFile.setBytes(messageSMIME.contenido)
			}
			log.debug("${representative.nif} -> ${numVotes} votes ");
			if((representativeVotes.getRowNumber() % 100) == 0) {
				sessionFactory.currentSession.flush() 
				sessionFactory.currentSession.clear()
			}
		}
		def metaInfMap = [dateFrom: DateUtils.getStringFromDate(dateFrom),
			dateTo:DateUtils.getStringFromDate(dateTo), numVotes:numVotes, 
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

	
	ResponseVS processVotingHistoryRequest(MessageSMIME messageSMIMEReq, Locale locale) {
		log.debug("processVotingHistoryRequest -")
		SMIMEMessageWrapper smimeMessage = messageSMIMEReq.getSmimeMessage()
		Usuario usuario = messageSMIMEReq.getUsuario()
		def messageJSON
		String msg
		try {
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
				msg = messageSource.getMessage('operationErrorMsg',
					[messageJSON.operation].toArray(), locale)
				log.error "processVotingHistoryRequest - OPERATION ERROR - ${msg}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:msg, type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR)
			}
			String requestValidatedNIF =  StringUtils.validarNIF(messageJSON.representativeNif)

			Usuario representative = Usuario.findWhere(nif:requestValidatedNIF,
				type:Usuario.Type.REPRESENTATIVE)
			if(!representative) {
				msg = messageSource.getMessage('representativeNifErrorMsg',
					[requestValidatedNIF].toArray(), locale)
				log.error "processVotingHistoryRequest - USER NOT REPRESENTATIVE ${msg}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, 
					type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR)
			}

			runAsync {
				ResponseVS respuestaGeneracionBackup
				respuestaGeneracionBackup = getVotingHistoryBackup(representative, dateFrom,  dateTo, locale)
				if(ResponseVS.SC_OK == respuestaGeneracionBackup?.statusCode) {
		
					SolicitudCopia solicitudCopia = new SolicitudCopia(
						filePath:respuestaGeneracionBackup.message,
						type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST, 
						representative:representative,
						messageSMIME:messageSMIMEReq, email:messageJSON.email)
					log.debug("messageSMIME: ${messageSMIMEReq.id} - ${solicitudCopia.type}");
					SolicitudCopia.withTransaction {
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
		Usuario usuario = messageSMIMEReq.getUsuario();
		log.debug("processRevoke - user ${usuario.nif}")
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
			if(Usuario.Type.REPRESENTATIVE != usuario.type) {
				msg = messageSource.getMessage('unsubscribeRepresentativeUserErrorMsg',
					[usuario.nif].toArray(), locale)
				log.error "processRevoke - USER TYPE ERROR - ${msg}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.REPRESENTATIVE_REVOKE_ERROR)
			}
			//(TODO notify users)=====
			def representedUsers
			Usuario.withTransaction {
				def criteria = Usuario.createCriteria()
				representedUsers = criteria.scroll {
					eq("representative", usuario)
				}
				while (representedUsers.next()) {
					Usuario representedUser = (Usuario) representedUsers.get(0);
					representedUsers.type = Usuario.Type.USER_WITH_CANCELLED_REPRESENTATIVE
					representedUser.representative = null
					representedUser.save()
					if((representedUsers.getRowNumber() % 100) == 0) {
						sessionFactory.currentSession.flush() 
						sessionFactory.currentSession.clear()
						log.debug("processRevoke - processed ${representedUsers.getRowNumber()} user updates")
					}	
				}
			}

			RepresentationDocument.withTransaction {
				def repDocCriteria = RepresentationDocument.createCriteria()
				def representationDocuments = repDocCriteria.scroll {
					eq("state", RepresentationDocument.State.OK)
					eq("representative", usuario)
				}
				while (representationDocuments.next()) {
					RepresentationDocument representationDocument = (RepresentationDocument) representationDocuments.get(0);
					representationDocument.state = RepresentationDocument.State.CANCELLED_BY_REPRESENTATIVE
					representationDocument.cancellationSMIME = messageSMIMEReq
					representationDocument.dateCanceled = usuario.getTimeStampToken().
						getTimeStampInfo().getGenTime()
					representationDocument.save()
					if((representationDocuments.getRowNumber() % 100) == 0) {
						sessionFactory.currentSession.flush() 
						sessionFactory.currentSession.clear()
						log.debug("processRevoke - processed ${representationDocuments.getRowNumber()} representationDocument updates")
					}
				}
			}
			
			usuario.representativeMessage = messageSMIMEReq
			usuario.type = Usuario.Type.EX_REPRESENTATIVE
			
			Usuario.withTransaction  {
				usuario.save()
			}
			
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = usuario.getNif()
			String subject = messageSource.getMessage(
					'unsubscribeRepresentativeValidationSubject', null, locale)

			SMIMEMessageWrapper smimeMessageResp = firmaService.
				getMultiSignedMimeMessage(fromUser, toUser, smimeMessage, subject)
				
			MessageSMIME messageSMIMEResp = new MessageSMIME(
				type:TypeVS.RECEIPT, smimePadre: messageSMIMEReq,
				valido:true, contenido:smimeMessageResp.getBytes())
			MessageSMIME.withTransaction {
				messageSMIMEResp.save();
			}
			log.error "processRevoke - saved MessageSMIME '${messageSMIMEResp.id}'"
			msg =  messageSource.getMessage('representativeRevokeMsg',
				[usuario.getNif()].toArray(), locale)
			return new ResponseVS(statusCode:ResponseVS.SC_OK, 
				data:messageSMIMEResp, userVS:usuario, 
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
		Usuario usuario = messageSMIMEReq.getUsuario();
		log.debug("processAccreditationsRequest - usuario '{usuario.nif}'")
		RepresentationDocument representationDocument = null
		def messageJSON = null
		try {
			messageJSON = JSON.parse(smimeMessage.getSignedContent())
			String requestValidatedNIF =  StringUtils.validarNIF(messageJSON.representativeNif)
			Date selectedDate = DateUtils.getDateFromString(messageJSON.selectedDate)
			if(!requestValidatedNIF || !messageJSON.operation || 
				(TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST != TypeVS.valueOf(messageJSON.operation))||
				!selectedDate || !messageJSON.email || !messageJSON.UUID ){
				msg = messageSource.getMessage('representativeAccreditationRequestErrorMsg', null, locale)
				log.error "processAccreditationsRequest - ERROR DATA - ${msg} - ${messageJSON.toString()}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
					type:TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR)
			}
			Usuario representative = Usuario.findWhere(nif:requestValidatedNIF,
							type:Usuario.Type.REPRESENTATIVE)
			if(!representative) {
			   msg = messageSource.getMessage('representativeNifErrorMsg',
				   [requestValidatedNIF].toArray(), locale)
			   log.error "processAccreditationsRequest - ERROR REPRESENTATIVE - ${msg}"
			   return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
				   type:TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR)
		   }
			runAsync {
					ResponseVS respuestaGeneracionBackup = getAccreditationsBackup(
						representative, selectedDate ,locale)
					if(ResponseVS.SC_OK == respuestaGeneracionBackup?.statusCode) {
						File archivoCopias = respuestaGeneracionBackup.file
						SolicitudCopia solicitudCopia = new SolicitudCopia(
							filePath:archivoCopias.getAbsolutePath(),
							type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST,
							representative:representative,
							messageSMIME:messageSMIMEReq, email:messageJSON.email)
						SolicitudCopia.withTransaction {
							if (!solicitudCopia.save()) {
								solicitudCopia.errors.each {
									log.error("processAccreditationsRequest - ERROR solicitudCopia - ${it}")}
							}
						}
						log.debug("processAccreditationsRequest - saved SolicitudCopia '${solicitudCopia.id}'");
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
		
	public Map getRepresentativeJSONMap(Usuario representative) {
		//log.debug("getRepresentativeJSONMap: ${representative.id} ")		
		String representativeMessageURL = 
			"${grailsApplication.config.grails.serverURL}/messageSMIME/${representative.representativeMessage?.id}"
		Image image
		Image.withTransaction {
			image = Image.findByTypeAndUsuario (Image.Type.REPRESENTATIVE, representative)
		}
		String imageURL = "${grailsApplication.config.grails.serverURL}/representative/image/${image?.id}" 
		String infoURL = "${grailsApplication.config.grails.serverURL}/representative/${representative?.id}" 
		String webPageURL = "${grailsLinkGenerator.link(controller: 'representative')}/${representative?.id}"
		def numRepresentations = Usuario.countByRepresentative(representative) + 1//plus the representative itself
		def representativeMap = [id: representative.id, nif:representative.nif,
			 webPageURL:webPageURL, infoURL:infoURL, 
			 representativeMessageURL:representativeMessageURL,
			 imageURL:imageURL, numRepresentations:numRepresentations,
			 nombre: representative.nombre, primerApellido:representative.primerApellido]
		return representativeMap
	}
	
	public Map getRepresentativeDetailedJSONMap(Usuario representative) {
		Map representativeMap = getRepresentativeJSONMap(representative)
		representativeMap.info = representative.info
		representativeMap.votingHistory = []
		return representativeMap
	}
	
}