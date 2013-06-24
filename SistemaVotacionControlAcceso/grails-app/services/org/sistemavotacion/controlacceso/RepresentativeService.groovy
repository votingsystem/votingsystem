package org.sistemavotacion.controlacceso

import org.codehaus.groovy.grails.web.json.JSONObject;
import org.sistemavotacion.controlacceso.modelo.Respuesta;

import java.security.MessageDigest
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

import org.apache.commons.logging.Log;
import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.StringUtils

import grails.converters.JSON
import sun.misc.BASE64Decoder;
import org.hibernate.criterion.Projections
import java.util.concurrent.TimeUnit

class RepresentativeService {
	
	public enum State{WITHOUT_ACCESS_REQUEST, WITH_ACCESS_REQUEST, WITH_VOTE}
	
	def subscripcionService
	def messageSource
	def grailsApplication
	def mailSenderService
	def firmaService
	def filesService
	def eventoService

	/*
	 * Creates backup of the state of all the representatives for a closed event
	 */
	private synchronized Respuesta getAccreditationsBackupForEvent (
			EventoVotacion event, Locale locale){
		log.debug("getAccreditationsBackupForEvent - event: ${event.id}")
		String msg = null
		if(!event) {
			msg = messageSource.getMessage('nullParamErrorMsg', null, locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR, mensaje:msg)
		}
		if(event.isOpen(DateUtils.getTodayDate())) {
			msg = messageSource.getMessage('eventOpenErrorMsg', 
				[event.id].toArray(), locale) 			
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR, mensaje:msg)	
		}
		
		Map<String, File> mapFiles = filesService.getBackupFiles(event, 
			Tipo.REPRESENTATIVE_ACCREDITATIONS, locale)
		File zipResult   = mapFiles.zipResult
		File metaInfFile = mapFiles.metaInfFile
		File filesDir    = mapFiles.filesDir

		Date selectedDate = event.getDateFinish();

		if(zipResult.exists()) {
			log.debug("getAccreditationsBackupForEvent - backup file already exists")
			return new Respuesta(codigoEstado:Respuesta.SC_OK,
				file:zipResult)
		}
		
		def representatives = null;

		Usuario.withTransaction {
			representatives = Usuario.findAllByTypeAndRepresentativeRegisterDateLessThanEquals(
				Usuario.Type.REPRESENTATIVE, selectedDate)
			/*def criteria = Usuario.createCriteria()
			representatives = criteria.list (max: 1000000, offset: 0) {
				eq("type", 	 Usuario.Type.REPRESENTATIVE)
				le("representativeRegisterDate", selectedDate)
			}*/
		}

		log.debug("num representatives: ${representatives.size()}")
		
		int numRepresentatives = 0
		int numRepresentativesWithAccessRequest = 0
		int numRepresentativesWithVote = 0
		int numTotalRepresented = 0
		int numTotalRepresentedWithAccessRequest = 0
		int numVotesRepresentedByRepresentatives = 0

		//Only counts representatives active when event finish
		if(!representatives.isEmpty()) {
			File representativesReportFile = mapFiles.representativesReportFile
			representativesReportFile.write("")
			representatives.each { representative ->

				String representativeBaseDir = "${filesDir.absolutePath}/representative_${representative.nif}"
				new File(representativeBaseDir).mkdirs()

				if(representative.type != Usuario.Type.REPRESENTATIVE) {
					//check if active on selected date
					def revocationMessage
					MensajeSMIME.withTransaction {
						def criteria = MensajeSMIME.createCriteria()
						revocationMessage = criteria.list (max: 1000000, offset: 0) {
							eq("tipo", Tipo.REPRESENTATIVE_REVOKE)
							le("dateCreated", selectedDate)
						}
					}
					if(revocationMessage) {
						log.debug("${representative.nif} wasn't active on ${selectedDate}")
						return
					}
				}
				//representative active on selected date
				numRepresentatives++;
				def representationDoc
				int numRepresented = 1 //The representative itself
				int numUsersWithRepresentativeWithAccessRequest = 0
				RepresentationDocument.withTransaction {
					def criteria = RepresentationDocument.createCriteria()
					representationDoc = criteria.list (max: 1000000, offset: 0) {
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
					numRepresented = representationDoc.totalCount + 1; //The representative itself
					numTotalRepresented += numRepresented
					log.debug("${representative.nif} has ${numRepresented} representations")
				}
				representationDoc.each {representationDocument ->
					Usuario represented = representationDocument.user
					SolicitudAcceso representedAccessRequest = SolicitudAcceso.findWhere(
						estado:SolicitudAcceso.Estado.OK, usuario:represented, eventoVotacion:event)
					String repDocFileName = null 
					if(representedAccessRequest) {
						numUsersWithRepresentativeWithAccessRequest++
						repDocFileName = "${representativeBaseDir}/WithRequest_RepDoc_${represented.nif}.p7m"
					} else repDocFileName = "${representativeBaseDir}/RepDoc_${represented.nif}.p7m"
					File representationDocFile = new File(repDocFileName)
					representationDocFile.setBytes(representationDocument.activationSMIME.contenido)
				}
				log.debug("representative: ${representative.nif} - numUsersWithRepresentativeWithAccessRequest: ${numUsersWithRepresentativeWithAccessRequest}")
				numTotalRepresentedWithAccessRequest += numUsersWithRepresentativeWithAccessRequest
				SolicitudAcceso solicitudAcceso = null
				State state = State.WITHOUT_ACCESS_REQUEST
				SolicitudAcceso.withTransaction {
					solicitudAcceso = SolicitudAcceso.findWhere(
						eventoVotacion:event,
						estado:SolicitudAcceso.Estado.OK, usuario:representative)
					if(solicitudAcceso) {//Representative has access request
						numRepresentativesWithAccessRequest++;
						state = State.WITH_ACCESS_REQUEST
					}
				}
				def voteResults
				if(solicitudAcceso?.id) {
					Voto.withTransaction {
						def criteria = Voto.createCriteria()
						voteResults = criteria.list (offset: 0) {
							createAlias("certificado", "certificado")
							eq("certificado.usuario", representative)
							eq("estado", Voto.Estado.OK)
							eq("eventoVotacion", event)
						}
					}
				}
				if(voteResults && !voteResults.isEmpty()) {
					state = State.WITH_VOTE
					numRepresentativesWithVote++
					numVotesRepresentedByRepresentatives += numRepresented - numUsersWithRepresentativeWithAccessRequest
				}
				
				String csvLine = "${representative.nif}, " + 
					 "numRepresented:${String.format('%08d', numRepresented)}, " +
					"numUsersWithRepresentativeWithAccessRequest:${String.format('%08d', numUsersWithRepresentativeWithAccessRequest)}, " +
					"${state.toString()}\n"
				log.debug("csvLine -> ${csvLine}")
				
				representativesReportFile.append(csvLine)
			}//end representative iteration				
		}

		def metaInfMap = [numRepresentatives:numRepresentatives,
			numRepresentativesWithAccessRequest:numRepresentativesWithAccessRequest,
			numRepresentativesWithVote:numRepresentativesWithVote,
			numUsersWithRepresentativeWithAccessRequest:numTotalRepresentedWithAccessRequest,
			numRepresented:numTotalRepresented, 
			numVotesRepresentedByRepresentatives:numVotesRepresentedByRepresentatives]
		
		eventoService.updateEventMetaInf(event, Tipo.REPRESENTATIVE_ACCREDITATIONS, metaInfMap)
		
		/*def ant = new AntBuilder()
		ant.zip(destfile: zipResult, basedir: filesDir.absolutePath)*/
		
		return new Respuesta(metaInf:metaInfMap,
			codigoEstado:Respuesta.SC_OK)
	}
	
	/*
	 * Makes a Representative map representing the state on the moment
	 */
	private synchronized Respuesta getAccreditationsMapForEvent (
			EventoVotacion event, Locale locale){
		log.debug("getAccreditationsBackupForEvent - event: ${event.id}")
		String msg = null
		if(!event) {
			msg = messageSource.getMessage('nullParamErrorMsg', null, locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR, mensaje:msg)
		}
		Date selectedDate = DateUtils.getTodayDate();
		
		def representatives = Usuario.findAllWhere(type:Usuario.Type.REPRESENTATIVE);

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
		representatives.each { representative ->
			int numRepresented = 1 //The representative itself
			int numUsersWithRepresentativeWithAccessRequest = 0
			numRepresented = Usuario.countByRepresentative(representative) + 1; //The representative itself
			numTotalRepresented += numRepresented
			log.debug("${representative.nif} has ${numRepresented} representations")
			
			def representativeVote
			Voto.withTransaction {
				def criteria = Voto.createCriteria()
				representativeVote = criteria.get {
					createAlias("certificado", "certificado")
					eq("certificado.usuario", representative)
					eq("estado", Voto.Estado.OK)
					eq("eventoVotacion", event)
				}
			}
			if(representativeVote) ++numRepresentativesWithVote

			def numRepresentedWithAccessRequest
			SolicitudAcceso.withTransaction {
				def criteria = SolicitudAcceso.createCriteria()
				numRepresentedWithAccessRequest = criteria.count {
					createAlias("usuario","usuario")
					eq("usuario.representative", representative)
					eq("estado", SolicitudAcceso.Estado.OK)
					eq("eventoVotacion", event)
				}
			}			
			numTotalRepresentedWithAccessRequest += numRepresentedWithAccessRequest
			
			Map representativeMap = [
				optionSelectedId:representativeVote?.opcionDeEvento?.id,
				numRepresentedWithVote:numRepresentedWithAccessRequest,
				numTotalRepresentations: numRepresented]
			representativesMap[representative.nif] = representativeMap
		}
		
		def metaInfMap = [numRepresentatives:representatives.size(),
			numRepresentativesWithAccessRequest:numRepresentativesWithAccessRequest,
			numRepresentativesWithVote:numRepresentativesWithVote,
			numUsersWithRepresentativeWithAccessRequest:numTotalRepresentedWithAccessRequest,
			numRepresented:numTotalRepresented,
			numVotesRepresentedByRepresentatives:numVotesRepresentedByRepresentatives,
			representatives:representativesMap]
		
		return new Respuesta(codigoEstado:Respuesta.SC_OK,
			metaInf:metaInfMap)
	}
	
					
	private Respuesta getAccreditationsBackup (Usuario representative,
		Date selectedDate, Locale locale){
		log.debug("getAccreditationsBackup - representative: ${representative.id}" +
			" - selectedDate: ${selectedDate}")
		def results
		RepresentationDocument.withTransaction {
			def criteria = RepresentationDocument.createCriteria()
			results = criteria.list {
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
		
		log.debug("getAccreditationsBackup - number of representations: ${results?.size()}")
		
		def selectedDateStr = DateUtils.getShortStringFromDate(DateUtils.getTodayDate())
		String zipNamePrefix = messageSource.getMessage(
			'representativeAcreditationsBackupFileName', null, locale);
		def basedir = "${grailsApplication.config.SistemaVotacion.baseRutaCopiaRespaldo}" +
			"/AccreditationsBackup/${selectedDateStr}/${zipNamePrefix}_${representative.nif}"
		log.debug("getAccreditationsBackup - basedir: ${basedir}")
		File zipResult = new File("${basedir}.zip")
		File metaInfFile;
		if(zipResult.exists()) {
			 metaInfFile = new File("${basedir}/meta.inf")
			 if(metaInfFile) {
				 def metaInfJSON = JSON.parse(metaInfFile.text)
				 log.debug("getAccreditationsBackup - send previous request")
				 Map datos = [cantidad:metaInfJSON.numberOfAccreditations]
				 return new Respuesta(codigoEstado:Respuesta.SC_OK, 
					 file:zipResult, datos:datos)
			 }
		}
		new File(basedir).mkdirs()
		String accreditationFileName = messageSource.getMessage('accreditationFileName', null, locale)
		int i = 0
		MensajeSMIME.withTransaction {
			results.each { acReq ->
				MensajeSMIME mensajeSMIME = acReq.activationSMIME
				log.debug("getAccreditationsBackup - copying mensajeSMIME '${mensajeSMIME.id}'")
				File smimeFile = new File("${basedir}/${accreditationFileName}_${i}")
				smimeFile.setBytes(mensajeSMIME.contenido)
				i++;
			}
		}

		def metaInfMap = [numberOfAccreditations:i, selectedDate: DateUtils.getStringFromDate(selectedDate),
			representativeURL:"${grailsApplication.config.grails.serverURL}/representative/${representative.id}"]
		String metaInfJSONStr = metaInfMap as JSON
		metaInfFile = new File("${basedir}/meta.inf")
		metaInfFile.write((metaInfMap as JSON).toString())
		def ant = new AntBuilder()
		ant.zip(destfile: "${basedir}.zip", basedir: basedir)
		zipResult = new File("${basedir}.zip")
		log.debug("getAccreditationsBackup - destfile.name '${zipResult.name}'")
		Map datos = [cantidad:i]
		return new Respuesta(codigoEstado:Respuesta.SC_OK, datos:datos, file:zipResult)
	}
	
	//{"operation":"REPRESENTATIVE_SELECTION","representativeNif":"...","representativeName":"...","UUID":"..."}
	public synchronized Respuesta saveUserRepresentative(MensajeSMIME mensajeSMIMEReq, Locale locale) {
		log.debug("saveUserRepresentative -")
		//def future = callAsync {}
		//return future.get(30, TimeUnit.SECONDS)
		MensajeSMIME mensajeSMIME = null
		SMIMEMessageWrapper smimeMessage = mensajeSMIMEReq.getSmimeMessage()
		RepresentationDocument representationDocument = null
		Usuario usuario = mensajeSMIMEReq.getUsuario()
		String msg = null
		try { 
			if(Usuario.Type.REPRESENTATIVE == usuario.type) {
				msg = messageSource.getMessage('userIsRepresentativeErrorMsg',
					[usuario.nif].toArray(), locale)
				log.error "saveUserRepresentative - ERROR - user '${usuario.nif}' is REPRESENTATIVE - ${msg}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:msg, tipo:Tipo.REPRESENTATIVE_SELECTION_ERROR)
			}
			def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			String requestValidatedNIF =  StringUtils.validarNIF(mensajeJSON.representativeNif)
			if(usuario.nif == requestValidatedNIF) {
				msg = messageSource.getMessage('representativeSameUserNifErrorMsg',
					[requestValidatedNIF].toArray(), locale)
				log.error("saveUserRepresentative - ERROR SAME USER SELECTION - ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
					mensaje:msg, tipo:Tipo.REPRESENTATIVE_SELECTION_ERROR)
			}
			if(!requestValidatedNIF || !mensajeJSON.operation || !mensajeJSON.representativeNif ||
				(Tipo.REPRESENTATIVE_SELECTION != Tipo.valueOf(mensajeJSON.operation))) {
				msg = messageSource.getMessage('representativeSelectionDataErrorMsg', null, locale)
				log.error("saveUserRepresentative - ERROR DATA - ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
					mensaje:msg, tipo:Tipo.REPRESENTATIVE_SELECTION_ERROR)
			}
			Usuario representative = Usuario.findWhere(
				nif:requestValidatedNIF, type:Usuario.Type.REPRESENTATIVE)
			if(!representative) {
				msg = messageSource.getMessage('representativeNifErrorMsg',
					[requestValidatedNIF].toArray(), locale)
				log.error "saveUserRepresentative - ERROR NIF REPRESENTATIVE - ${msg}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:msg, tipo:Tipo.REPRESENTATIVE_SELECTION_ERROR)
			}
			
			usuario.representative = representative
			RepresentationDocument.withTransaction {
				
				representationDocument = RepresentationDocument.findWhere(
					user:usuario, state:RepresentationDocument.State.OK)
				if(representationDocument) {
					log.debug("cancelRepresentationDocument - User changing representative")
					representationDocument.state = RepresentationDocument.State.CANCELLED
					representationDocument.cancellationSMIME = mensajeSMIMEReq
					representationDocument.dateCanceled = usuario.getTimeStampToken().
							getTimeStampInfo().getGenTime();
					representationDocument.save(flush:true)
					log.debug("cancelRepresentationDocument - cancelled user '${usuario.nif}' representationDocument ${representationDocument.id}")
				} else log.debug("cancelRepresentationDocument - user '${usuario.nif}' doesn't have representative")
				
				
				representationDocument = new RepresentationDocument(activationSMIME:mensajeSMIMEReq,
					user:usuario, representative:representative, state:RepresentationDocument.State.OK);
				representationDocument.save()
			}
						
			msg = messageSource.getMessage('representativeAssociatedMsg',
				[mensajeJSON.representativeName, usuario.nif].toArray(), locale)
			log.debug "saveUserRepresentative - ${msg}"
			
			String fromUser = grailsApplication.config.SistemaVotacion.serverName
			String toUser = usuario.getNif()
			String subject = messageSource.getMessage(
					'representativeSelectValidationSubject', null, locale)
			SMIMEMessageWrapper smimeMessageResp = firmaService.getMultiSignedMimeMessage(
				fromUser, toUser, smimeMessage, subject)
			MensajeSMIME mensajeSMIMEResp = new MensajeSMIME(
					smimeMessage:smimeMessageResp,
					tipo:Tipo.RECIBO, smimePadre: mensajeSMIMEReq, 
					valido:true, contenido:smimeMessageResp.getBytes())
			MensajeSMIME.withTransaction {
				mensajeSMIMEResp.save();
			}
			return new Respuesta(codigoEstado:Respuesta.SC_OK, mensaje:msg,
				mensajeSMIME:mensajeSMIMEResp, tipo:Tipo.REPRESENTATIVE_SELECTION)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			msg = messageSource.getMessage(
				'representativeSelectErrorMsg', null, locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
				mensaje:msg, tipo:Tipo.REPRESENTATIVE_SELECTION_ERROR)
		}

	}
	
	private void cancelRepresentationDocument(MensajeSMIME mensajeSMIMEReq, Usuario usuario) {
		RepresentationDocument.withTransaction {
			RepresentationDocument representationDocument = RepresentationDocument.
				findWhere(user:usuario, state:RepresentationDocument.State.OK)
			if(representationDocument) {
				log.debug("cancelRepresentationDocument - User changing representative")
				representationDocument.state = RepresentationDocument.State.CANCELLED
				representationDocument.cancellationSMIME = mensajeSMIMEReq
				representationDocument.dateCanceled = usuario.getTimeStampToken().
						getTimeStampInfo().getGenTime();
				representationDocument.save(flush:true)
				log.debug("cancelRepresentationDocument - cancelled user '${usuario.nif}' representationDocument ${representationDocument.id}")
			} else log.debug("cancelRepresentationDocument - user '${usuario.nif}' doesn't have representative")
		}
	}
	
    Respuesta saveRepresentativeData(MensajeSMIME mensajeSMIMEReq, 
		byte[] imageBytes, Locale locale) {
		log.debug("saveRepresentativeData - ")
		SMIMEMessageWrapper smimeMessageReq = mensajeSMIMEReq.getSmimeMessage()
		Usuario usuario = mensajeSMIMEReq.getUsuario()
		log.debug("saveRepresentativeData - usuario: ${usuario.nif}")
		String msg
		try {
			def mensajeJSON = JSON.parse(smimeMessageReq.getSignedContent())
			String base64ImageHash = mensajeJSON.base64ImageHash
			MessageDigest messageDigest = MessageDigest.getInstance(
				grailsApplication.config.SistemaVotacion.votingHashAlgorithm);
			byte[] resultDigest =  messageDigest.digest(imageBytes);
			String base64ResultDigest = new String(Base64.encode(resultDigest));
			log.debug("saveRepresentativeData - base64ImageHash: ${base64ImageHash}" + 
				" - server calculated base64ImageHash: ${base64ResultDigest}")
			if(!base64ResultDigest.equals(base64ImageHash)) {
				msg = messageSource.getMessage('imageHashErrorMsg', null, locale)
				log.error("saveRepresentativeData - ERROR ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:msg, tipo:Tipo.REPRESENTATIVE_DATA_ERROR)
			}
			//String base64EncodedImage = mensajeJSON.base64RepresentativeEncodedImage
			//BASE64Decoder decoder = new BASE64Decoder();
			//byte[] imageFileBytes = decoder.decodeBuffer(base64EncodedImage);
			Image newImage = new Image(usuario:usuario, mensajeSMIME:mensajeSMIMEReq,
				type:Image.Type.REPRESENTATIVE, fileBytes:imageBytes)
			if(Usuario.Type.REPRESENTATIVE != usuario.type) {
				usuario.type = Usuario.Type.REPRESENTATIVE
				usuario.representativeRegisterDate = DateUtils.getTodayDate()
				usuario.representative = null
				cancelRepresentationDocument(mensajeSMIMEReq, usuario);				
				msg = messageSource.getMessage('representativeDataCreatedOKMsg', 
					[usuario.nombre, usuario.primerApellido].toArray(), locale)
			} else {
				def representations = Usuario.countByRepresentative(usuario)
				msg = messageSource.getMessage('representativeDataUpdatedMsg',
					[usuario.nombre, usuario.primerApellido].toArray(), locale)
			} 
			usuario.setInfo(mensajeJSON.representativeInfo)
			usuario.representativeMessage = mensajeSMIMEReq
			Usuario.withTransaction {
				usuario.save(flush:true)
			}
			Image.withTransaction {
				def images = Image.findAllWhere(usuario:usuario)
				images?.each {
					it.type = Image.Type.REPRESENTATIVE_CANCELLED
					it.save()
				}
				newImage.save(flush:true)
			}
			
			MensajeSMIME.withTransaction {
				def previousMessages = MensajeSMIME.findAllWhere(usuario:usuario,
					tipo:Tipo.REPRESENTATIVE_DATA)
				previousMessages?.each {message ->
					message.tipo = Tipo.REPRESENTATIVE_DATA_OLD
					message.save()
				}
			}
			
			log.debug "saveRepresentativeData - user:${usuario.nif} - image: ${newImage.id}"
			return new Respuesta(codigoEstado:Respuesta.SC_OK, mensaje:msg, 
				tipo:Tipo.REPRESENTATIVE_DATA, mensajeSMIME:mensajeSMIMEReq)
		} catch(Exception ex) {
			log.error ("${ex.getMessage()} - user: ${usuario.nif}", ex)
			msg = messageSource.getMessage('representativeDataErrorMsg', null, locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
				mensaje:msg, tipo:Tipo.REPRESENTATIVE_DATA_ERROR)
		}
    }

	
	private Respuesta getVotingHistoryBackup (Usuario representative, 
		Date dateFrom, Date dateTo, Locale locale){
		log.debug("getVotingHistoryBackup - representative: ${representative.id}" + 
			" - dateFrom: ${dateFrom} - dateTo: ${dateTo}")
		
		def dateFromStr = DateUtils.getShortStringFromDate(dateFrom)
		def dateToStr = DateUtils.getShortStringFromDate(dateTo)
		
		String zipNamePrefix = messageSource.getMessage(
			'representativeHistoryVotingBackupFileName', null, locale);
		def basedir = "${grailsApplication.config.SistemaVotacion.baseRutaCopiaRespaldo}" +
			"/RepresentativeHistoryVoting/${dateFromStr}_${dateToStr}/${zipNamePrefix}_${representative.nif}"
		log.debug("getVotingHistoryBackup - basedir: ${basedir}")
		File zipResult = new File("${basedir}.zip")
		File metaInfFile;
		if(zipResult.exists()) {
			 metaInfFile = new File("${basedir}/meta.inf")
			 if(metaInfFile) {
				 def metaInfJSON = JSON.parse(metaInfFile.text)
				 log.debug("============= getVotingHistoryBackup - ${zipResult.name} already exists");
				 /*Map datos = [cantidad:metaInfJSON.numberVotes]
				 return new Respuesta(codigoEstado:Respuesta.SC_OK, file:zipResult,
					 datos:datos)*/
			 }
		}
			
		new File(basedir).mkdirs()
		String voteFileName = messageSource.getMessage('voteFileName', null, locale)
		int i = 0
		log.debug("============= TODO");
		/*def criteria = RepresentationDocument.createCriteria()
		def results = criteria.list {
			eq("state", RepresentationDocument.State.OK)
			eq("representative", representative)
			and {
				le("dateCreated", selectedDate)
			}
		}
		results.each { it ->
			MensajeSMIME mensajeSMIME = it.activationSMIME
			File smimeFile = new File("${basedir}/${voteFileName}_${i}")
			smimeFile.setBytes(mensajeSMIME.contenido)
			i++;
		}*/
		def metaInfMap = [numberVotes:i, dateFrom: DateUtils.getStringFromDate(dateFrom),
			dateTo:DateUtils.getStringFromDate(dateTo),
			representativeURL:"${grailsApplication.config.grails.serverURL}/representative/${representative.id}"]
		String metaInfJSONStr = metaInfMap as JSON
		metaInfFile = new File("${basedir}/meta.inf")
		metaInfFile.write(metaInfJSONStr)
		def ant = new AntBuilder()
		ant.zip(destfile: "${basedir}.zip", basedir: basedir)
		Map datos = [cantidad:i]
		return new Respuesta(codigoEstado:Respuesta.SC_OK, datos:datos, file:new File("${basedir}.zip"))
	}

	
	Respuesta processVotingHistoryRequest(MensajeSMIME mensajeSMIMEReq, Locale locale) {
		log.debug("processVotingHistoryRequest -")
		SMIMEMessageWrapper smimeMessage = mensajeSMIMEReq.getSmimeMessage()
		Usuario usuario = mensajeSMIMEReq.getUsuario()
		def mensajeJSON
		String msg
		try {
			Tipo tipo = Tipo.REPRESENTATIVE_VOTING_HISTORY_REQUEST 
			//REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR
			mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			Date dateFrom = DateUtils.getDateFromString(mensajeJSON.dateFrom)
			Date dateTo = DateUtils.getDateFromString(mensajeJSON.dateTo)
			if(dateFrom.after(dateTo)) {
				log.error "processVotingHistoryRequest - DATE ERROR - dateFrom '${dateFrom}' dateTo '${dateTo}'"
				DateFormat formatter = new SimpleDateFormat("dd MMM 'de' yyyy 'a las' HH:mm");
				

				msg = messageSource.getMessage('dateRangeErrorMsg',[formatter.format(dateFrom), 
					formatter.format(dateTo)].toArray(), locale) 
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:msg, tipo:Tipo.REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR)
			}
			Tipo operationType = Tipo.valueOf(mensajeJSON.operation)
			if(Tipo.REPRESENTATIVE_VOTING_HISTORY_REQUEST != operationType) {
				msg = messageSource.getMessage('operationErrorMsg',
					[mensajeJSON.operation].toArray(), locale)
				log.error "processVotingHistoryRequest - OPERATION ERROR - ${msg}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:msg, tipo:Tipo.REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR)
			}
			String requestValidatedNIF =  StringUtils.validarNIF(mensajeJSON.representativeNif)

			Usuario representative = Usuario.findWhere(nif:requestValidatedNIF,
				type:Usuario.Type.REPRESENTATIVE)
			if(!representative) {
				msg = messageSource.getMessage('representativeNifErrorMsg',
					[requestValidatedNIF].toArray(), locale)
				log.error "processVotingHistoryRequest - USER NOT REPRESENTATIVE ${msg}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg, 
					tipo:Tipo.REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR)
			}

			runAsync {
				Respuesta respuestaGeneracionBackup
				respuestaGeneracionBackup = getVotingHistoryBackup(representative, dateFrom,  dateTo, locale)
				if(Respuesta.SC_OK == respuestaGeneracionBackup?.codigoEstado) {
					File archivoCopias = respuestaGeneracionBackup.file
					
					SolicitudCopia solicitudCopia = new SolicitudCopia(
						filePath:archivoCopias.getAbsolutePath(),
						type:Tipo.REPRESENTATIVE_VOTING_HISTORY_REQUEST, 
						representative:representative,
						mensajeSMIME:mensajeSMIMEReq, email:mensajeJSON.email)
					log.debug("mensajeSMIME: ${mensajeSMIMEReq.id} - ${solicitudCopia.type}");
					SolicitudCopia.withTransaction {
						if (!solicitudCopia.save()) {
							solicitudCopia.errors.each { 
								log.error("processVotingHistoryRequest - ERROR solicitudCopia - ${it}")}
						}
						
					}
					mailSenderService.sendRepresentativeVotingHistory(
						solicitudCopia, mensajeJSON.dateFrom, mensajeJSON.dateTo, locale)
				} else log.error("Error generando archivo de copias de respaldo");
			}
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			msg = messageSource.getMessage('requestErrorMsg', null, locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
				mensaje:msg, tipo:Tipo.REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR)
		}
		msg = messageSource.getMessage('backupRequestOKMsg',
			[mensajeJSON.email].toArray(), locale)
		return new Respuesta(codigoEstado:Respuesta.SC_OK,
			tipo:Tipo.REPRESENTATIVE_VOTING_HISTORY_REQUEST, mensaje:msg)
	}
	
	Respuesta processRevoke(MensajeSMIME mensajeSMIMEReq, Locale locale) {
		String msg = null;
		SMIMEMessageWrapper smimeMessage = mensajeSMIMEReq.getSmimeMessage();
		Usuario usuario = mensajeSMIMEReq.getUsuario();
		log.debug("processRevoke - user ${usuario.nif}")
		try{
			def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			Tipo operationType = Tipo.valueOf(mensajeJSON.operation)
			if(Tipo.REPRESENTATIVE_REVOKE != operationType) {
				msg = messageSource.getMessage('operationErrorMsg',
					[mensajeJSON.operation].toArray(), locale)
				log.error "processRevoke - OPERATION ERROR - ${msg}" 
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:msg, tipo:Tipo.REPRESENTATIVE_REVOKE_ERROR)
			}
			if(Usuario.Type.REPRESENTATIVE != usuario.type) {
				msg = messageSource.getMessage('unsubscribeRepresentativeUserErrorMsg',
					[usuario.nif].toArray(), locale)
				log.error "processRevoke - USER TYPE ERROR - ${msg}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:msg, tipo:Tipo.REPRESENTATIVE_REVOKE_ERROR)
			}
			//(TODO notify users)=====
			Usuario.withTransaction {
				def representedUsers = Usuario.findAllWhere(representative:usuario)
				log.debug "processRevoke - number of represented users : ${representedUsers.size()}"
				representedUsers?.each {
					log.debug "processRevoke -  updating user - ${it.id}"
					it.type = Usuario.Type.USER_WITH_CANCELLED_REPRESENTATIVE
					it.representative = null
					it.save()
				}
			}
			def representationDocuments 
			RepresentationDocument.withTransaction {
				representationDocuments = RepresentationDocument.findAllWhere(
					state:RepresentationDocument.State.OK, representative:usuario)
				representationDocuments.each {
					log.debug " - checking representationDocument - ${it.id}"
					it.state = RepresentationDocument.State.CANCELLED_BY_REPRESENTATIVE
					it.cancellationSMIME = mensajeSMIME
					it.dateCanceled = usuario.getTimeStampToken().
						getTimeStampInfo().getGenTime()
					it.save()
				}
			}
			usuario.representativeMessage = mensajeSMIMEReq
			usuario.type = Usuario.Type.EX_REPRESENTATIVE
			
			Usuario.withTransaction  {
				usuario.save()
			}
			
			String fromUser = grailsApplication.config.SistemaVotacion.serverName
			String toUser = usuario.getNif()
			String subject = messageSource.getMessage(
					'unsubscribeRepresentativeValidationSubject', null, locale)

			SMIMEMessageWrapper smimeMessageResp = firmaService.
				getMultiSignedMimeMessage(fromUser, toUser, smimeMessage, subject)
				
			MensajeSMIME mensajeSMIMEResp = new MensajeSMIME(
				tipo:Tipo.RECIBO, smimePadre: mensajeSMIMEReq,
				valido:true, contenido:smimeMessageResp.getBytes())
			MensajeSMIME.withTransaction {
				mensajeSMIMEResp.save();
			}
			log.error "processRevoke - saved MensajeSMIME '${mensajeSMIMEResp.id}'"
			msg =  messageSource.getMessage(
				'representativeRevokeMsg',[usuario.getNif()].toArray(), locale)
			return new Respuesta(codigoEstado:Respuesta.SC_OK, 
				mensajeSMIME:mensajeSMIMEResp, usuario:usuario, 
				tipo:Tipo.REPRESENTATIVE_REVOKE, mensaje:msg )
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			msg = messageSource.getMessage(
				'representativeRevokeErrorMsg',null, locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
				mensaje:msg, tipo:Tipo.REPRESENTATIVE_REVOKE_ERROR)
		}
	}
	
	//{"operation":"REPRESENTATIVE_ACCREDITATIONS_REQUEST","representativeNif":"...",
	//"representativeName":"...","selectedDate":"2013-05-20 09:50:33","email":"...","UUID":"..."}
	Respuesta processAccreditationsRequest(MensajeSMIME mensajeSMIMEReq, Locale locale) {
		String msg = null
		SMIMEMessageWrapper smimeMessage = mensajeSMIMEReq.getSmimeMessage()
		Usuario usuario = mensajeSMIMEReq.getUsuario();
		log.debug("processAccreditationsRequest - usuario '{usuario.nif}'")
		RepresentationDocument representationDocument = null
		def mensajeJSON = null
		try {
			mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			String requestValidatedNIF =  StringUtils.validarNIF(mensajeJSON.representativeNif)
			Date selectedDate = DateUtils.getDateFromString(mensajeJSON.selectedDate)
			if(!requestValidatedNIF || !mensajeJSON.operation || 
				(Tipo.REPRESENTATIVE_ACCREDITATIONS_REQUEST != Tipo.valueOf(mensajeJSON.operation))||
				!selectedDate || !mensajeJSON.email || !mensajeJSON.UUID ){
				msg = messageSource.getMessage('representativeAccreditationRequestErrorMsg', null, locale)
				log.error "processAccreditationsRequest - ERROR DATA - ${msg} - ${mensajeJSON.toString()}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg,
					tipo:Tipo.REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR)
			}
			Usuario representative = Usuario.findWhere(nif:requestValidatedNIF,
							type:Usuario.Type.REPRESENTATIVE)
			if(!representative) {
			   msg = messageSource.getMessage('representativeNifErrorMsg',
				   [requestValidatedNIF].toArray(), locale)
			   log.error "processAccreditationsRequest - ERROR REPRESENTATIVE - ${msg}"
			   return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg,
				   tipo:Tipo.REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR)
		   }
			runAsync {
					Respuesta respuestaGeneracionBackup = getAccreditationsBackup(
						representative, selectedDate ,locale)
					if(Respuesta.SC_OK == respuestaGeneracionBackup?.codigoEstado) {
						File archivoCopias = respuestaGeneracionBackup.file
						SolicitudCopia solicitudCopia = new SolicitudCopia(
							filePath:archivoCopias.getAbsolutePath(),
							type:Tipo.REPRESENTATIVE_VOTING_HISTORY_REQUEST,
							representative:representative,
							mensajeSMIME:mensajeSMIMEReq, email:mensajeJSON.email)
						SolicitudCopia.withTransaction {
							if (!solicitudCopia.save()) {
								solicitudCopia.errors.each {
									log.error("processAccreditationsRequest - ERROR solicitudCopia - ${it}")}
							}
						}
						log.debug("processAccreditationsRequest - saved SolicitudCopia '${solicitudCopia.id}'");
						mailSenderService.sendRepresentativeAccreditations(
							solicitudCopia, mensajeJSON.selectedDate, locale)
					} else log.error("processAccreditationsRequest - ERROR creating backup");
			}
			msg = messageSource.getMessage('backupRequestOKMsg',
				[mensajeJSON.email].toArray(), locale)
			new Respuesta(codigoEstado:Respuesta.SC_OK,	mensaje:msg,
				tipo:Tipo.REPRESENTATIVE_ACCREDITATIONS_REQUEST)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			msg = messageSource.getMessage('representativeAccreditationRequestErrorMsg', null, locale)
			return new Respuesta(mensaje:msg,
				codigoEstado:Respuesta.SC_ERROR,
				tipo:Tipo.REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR)
		}

	}
		
	public Map getRepresentativeJSONMap(Usuario usuario) {
		//log.debug("getRepresentativeJSONMap: ${usuario.id} ")
		
		String representativeMessageURL = 
			"${grailsApplication.config.grails.serverURL}/mensajeSMIME/${usuario.representativeMessage?.id}"
		Image image
		Image.withTransaction {
			image = Image.findByTypeAndUsuario (Image.Type.REPRESENTATIVE, usuario)
		}
		String imageURL = "${grailsApplication.config.grails.serverURL}/representative/image/${image?.id}" 
		String infoURL = "${grailsApplication.config.grails.serverURL}/representative/${usuario?.id}" 
		
		def numRepresentations = Usuario.countByRepresentative(usuario) + 1//plus the representative itself
		
		def representativeMap = [id: usuario.id, nif:usuario.nif, infoURL:infoURL, 
			 representativeMessageURL:representativeMessageURL,
			 imageURL:imageURL, numRepresentations:numRepresentations,
			 nombre: usuario.nombre, primerApellido:usuario.primerApellido]
		return representativeMap
	}
	
	public Map getRepresentativeDetailedJSONMap(Usuario usuario) {
		Map representativeMap = getRepresentativeJSONMap(usuario)
		representativeMap.info = usuario.info
		representativeMap.votingHistory = []
		return representativeMap
	}
	
}