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

import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.StringUtils

import grails.converters.JSON
import sun.misc.BASE64Decoder;

class RepresentativeService {
	
	public enum Estate{WITHOUT_ACCESS_REQUEST, WITHOUT_VOTE, WITH_VOTE}
	
	def subscripcionService
	def messageSource
	def grailsApplication
	def mailSenderService
	def firmaService

	/*
	 * Creates backup of the state of all the representatives for a finished event
	 */
	private synchronized Respuesta getAccreditationsBackupForEvent (
			EventoVotacion event, Locale locale){
		log.debug("getAccreditationsBackupForEvent - event: ${event.id}")
		def datePathPart = DateUtils.getShortStringFromDate(event.fechaFin)
		def basedir = "${grailsApplication.config.SistemaVotacion.accreditationsForEventBackupDir}" +
			"/${datePathPart}/Event_${event.id}"
		File baseDirZipped = new File("${basedir}.zip")
		String msg = null
		if(baseDirZipped.exists()) {
			log.debug("getAccreditationsBackupForEvent - backup file already exists")
			return new Respuesta(codigoEstado:Respuesta.SC_OK,
				file:baseDirZipped)
		}
		List<Usuario> representatives = null;
		Usuario.withTransaction {
			representatives = Usuario.findAllWhere(type:Usuario.Type.REPRESENTATIVE)
		}
		File representativesReportFile = new File("${basedir}/representativeReport")
		String url = null
		int numberOfRepresentatives = representatives.size()
		int totalNumberOfRepresented = 0
		int representedWithVote = 0
		representatives.each { representative ->
			url = "${grailsApplication.config.grails.serverURL}/representative/${representative.id}"
			int numberOfRepresentations = 1
			RepresentationDocument.withTransaction {
				def criteria = RepresentationDocument.createCriteria()
				def representations = criteria.list {
					eq("representative", representative)
					le("dateCreated", selectedDate)
					or {
						eq("state", RepresentationDocument.State.CANCELLED)
						eq("state", RepresentationDocument.State.OK)
					}
					or {
						isNull("dateCanceled")
						gt("dateCanceled", event.fechaFin)
					}
				}
				numberOfRepresentations = representations.totalCount
			}
			totalNumberOfRepresented += numberOfRepresentations
			SolicitudAcceso solicitudAcceso
			SolicitudAcceso.withTransaction {
				solicitudAcceso = SolicitudAcceso.findWhere(
					estado:SolicitudAcceso.Estado.OK, usuario:representative)
				if(!solicitudAcceso) {
					msg = messageSource.getMessage('representativeWithoutVote',
						[representative.id, event.id].toArray(), locale)
					log.debug("getAccreditationsBackupForEvent - ${msg}")
					representativesReportFile.append("${representative.id}," +
						"${representative.nif}, ${url}, ${numberOfRepresentations}," + 
						"${Estate.WITHOUT_ACCESS_REQUEST.toString()}")
					return
				} else {//Representative has voted
					String representativeBaseDir = "${basedir}/representative_${representative.id}"
					File smimeFile = new File("${representativeBaseDir}/AccessRequest.p7s")
					smimeFile.setBytes(solicitudAcceso.mensajeSMIME.contenido)
					def voteResults
					File representedReportFile = new File("${representativeBaseDir}/representedReport")
					Voto.withTransaction {
						voteResults = Voto.withCriteria {
							createAlias("certificado", "certificado")
							//eq("certificado.tipo", Certificado.Tipo.VOTO)
							eq("certificado.usuario", representative)
							eq("estado", Voto.Estado.OK)
						}
						if(voteResults.isEmpty()) {
							log.error("getAccreditationsBackupForEvent - Representative ${representative.id} has no vote")
							representativesReportFile.append("${representative.id}," +
								"${representative.nif}, ${url}, ${numberOfRepresentations}," + 
								"${Estate.WITHOUT_VOTE.toString()}")
						} else {
							File voteFile = new File("${representativeBaseDir}/Vote.p7s")
							voteFile.setBytes(voteResults.iterator().next().mensajeSMIME.contenido)

							def representationsDocuments
							
							RepresentationDocument.withTransaction {
								def criteria = RepresentationDocument.createCriteria()
								representationsDocuments = criteria.list {
									eq("representative", representative)
									le("dateCreated", event.fechaFin)
									or {
										eq("state", RepresentationDocument.State.CANCELLED)
										eq("state", RepresentationDocument.State.OK)
									}
									or {
										isNull("dateCanceled")
										gt("dateCanceled", event.fechaFin)
									}
								}
								String representedBaseDir = "${representativeBaseDir}/represented"
								representationsDocuments.each {representationDocument ->
									Usuario represented = representationDocument.user
									File representationDoc = new File("${representedBaseDir}/RepDoc_${represented.id}.p7s")
									representationDoc.setBytes(representationDocument.activationSMIME.contenido)
									SolicitudAcceso representedAccessRequest = SolicitudAcceso.findWhere(
										estado:SolicitudAcceso.Estado.OK, usuario:represented, eventoVotacion:event)
									if(!representedAccessRequest) {
										log.error("getAccreditationsBackupForEvent - Represented '${represented.id}' has no vote")
										representedReportFile.append("${represented.id}," +
											"${represented.nif}, ${Estate.WITHOUT_ACCESS_REQUEST.toString()}")
									}else {
										representedWithVote++
										File representedAccessRequestFile = new File("${representedBaseDir}/AccessRequest_${represented.id}.p7s")
										representedAccessRequestFile.setBytes(representedAccessRequest.mensajeSMIME.contenido)
										representedReportFile.append("${represented.id}," +
											"${represented.nif}, ${Estate.WITH_VOTE.toString()}")
									}
								}
							}
						}
					}
				}
			}
			
			def metaInfMap = [eventFinishDate: DateUtils.getStringFromDate(event.fechaFin),
				eventURl:"${grailsApplication.config.grails.serverURL}/eventoVotacion/${event.id}",
				numberOfRepresentatives:numberOfRepresentatives, 
				representedWithVote:representedWithVote,
				totalNumberOfRepresented:totalNumberOfRepresented]
				
			
			def eventMetaInfJSON = JSON.parse(event.metaInf)
			
			def metaInfJSON = metaInfMap as JSON
			File metaInfFile = new File("${basedir}/meta.inf")
			
			eventMetaInfJSON.representativesReport = metaInfJSON
			
			event.metaInf = eventMetaInfJSON.toString()
			Evento.withTransaction {
				event.save()
			}
			metaInfFile.write(metaInfJSON.toString())
			def ant = new AntBuilder()
			ant.zip(destfile: baseDirZipped, basedir: basedir)
			
			return new Respuesta(codigoEstado:Respuesta.SC_OK, 
				file:baseDirZipped, datos:metaInfMap)
		}
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
		File baseDirZipped = new File("${basedir}.zip")
		File metaInfFile;
		if(baseDirZipped.exists()) {
			 metaInfFile = new File("${basedir}/meta.inf")
			 if(metaInfFile) {
				 def metaInfJSON = JSON.parse(metaInfFile.text)
				 log.debug("getAccreditationsBackup - send previous request")
				 Map datos = [cantidad:metaInfJSON.numberOfAccreditations]
				 return new Respuesta(codigoEstado:Respuesta.SC_OK, 
					 file:baseDirZipped, datos:datos)
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
		metaInfFile.write(metaInfJSONStr)
		def ant = new AntBuilder()
		ant.zip(destfile: "${basedir}.zip", basedir: basedir)
		baseDirZipped = new File("${basedir}.zip")
		log.debug("getAccreditationsBackup - destfile.name '${baseDirZipped.name}'")
		Map datos = [cantidad:i]
		return new Respuesta(codigoEstado:Respuesta.SC_OK, datos:datos, file:baseDirZipped)
	}
	
	//{"operation":"REPRESENTATIVE_SELECTION","representativeNif":"...","representativeName":"...","UUID":"..."}
	Respuesta saveUserRepresentative(MensajeSMIME mensajeSMIMEReq, Locale locale) {
		log.debug("saveUserRepresentative -")
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
			cancelRepresentationDocument(mensajeSMIMEReq, usuario);
			representationDocument = new RepresentationDocument(activationSMIME:mensajeSMIMEReq,
				user:usuario, representative:representative, state:RepresentationDocument.State.OK);
					
			Usuario.withTransaction {
				def userMetaInfJSON = JSON.parse(representative.metaInf)
				userMetaInfJSON.representationsNumber++
				representative.metaInf = userMetaInfJSON
				representative = representative.merge()
				representative.save(flush:true)
			}
			
			RepresentationDocument.withTransaction { 
				usuario.representative = representative
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
					tipo:Tipo.RECIBO, smimePadre: mensajeSMIMEReq, 
					valido:true, contenido:smimeMessageResp.getBytes())
			MensajeSMIME.withTransaction {
				mensajeSMIMEResp.save();
			}
			return new Respuesta(codigoEstado:Respuesta.SC_OK, mensaje:msg,
				mensajeSMIME:mensajeSMIMEResp, tipo:Tipo.REPRESENTATIVE_SELECTION)
		}  catch(Exception ex) {
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
				def userMetaInfJSON = JSON.parse(representationDocument.representative.metaInf)
				userMetaInfJSON.representationsNumber--
				representationDocument.representative.metaInf = userMetaInfJSON
				representationDocument.save()
				log.debug("cancelRepresentationDocument - cancelled user '${usuario.nif}' representationDocument ${representationDocument.id}")
			} else log.debug("cancelRepresentationDocument - user '${usuario.nif}' doesn't have representative")
		}

	}
	
    Respuesta saveRepresentativeData(MensajeSMIME mensajeSMIMEReq, 
		byte[] imageBytes, Locale locale) {
		log.debug("saveRepresentativeData - ")
		SMIMEMessageWrapper smimeMessageReq = mensajeSMIMEReq.getSmimeMessage()
		Usuario usuario = mensajeSMIMEReq.getUsuario()
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
				def userMetaInfJSON = JSON.parse(usuario.metaInf)
				userMetaInfJSON.representationsNumber = 1
				usuario.metaInf = userMetaInfJSON;
				usuario.representative = null
				cancelRepresentationDocument(mensajeSMIMEReq, usuario);				
				msg = messageSource.getMessage('representativeDataCreatedOKMsg', 
					[usuario.nombre, usuario.primerApellido].toArray(), locale)
			} else msg = messageSource.getMessage('representativeDataUpdatedMsg', 
				[usuario.nombre, usuario.primerApellido].toArray(), locale)
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
			log.debug "saveRepresentativeData - user:${usuario.id} - image: ${newImage.id}"
			return new Respuesta(codigoEstado:Respuesta.SC_OK, mensaje:msg, 
				tipo:Tipo.REPRESENTATIVE_DATA, mensajeSMIME:mensajeSMIMEReq)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
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
		File baseDirZipped = new File("${basedir}.zip")
		File metaInfFile;
		if(baseDirZipped.exists()) {
			 metaInfFile = new File("${basedir}/meta.inf")
			 if(metaInfFile) {
				 def metaInfJSON = JSON.parse(metaInfFile.text)
				 log.debug("============= getVotingHistoryBackup - ${baseDirZipped.name} already exists");
				 /*Map datos = [cantidad:metaInfJSON.numberVotes]
				 return new Respuesta(codigoEstado:Respuesta.SC_OK, file:baseDirZipped,
					 datos:datos)*/
			 }
		}
			
		new File(basedir).mkdirs()
		String votoFileName = messageSource.getMessage('votoFileName', null, locale)
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
			File smimeFile = new File("${basedir}/${votoFileName}_${i}")
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
						mensajeSMIME:mensajeSMIMEReq, email:mensajeJSON.email, 
						numeroCopias:respuestaGeneracionBackup.datos.cantidad)
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
	
	def prueba (Usuario usuario) {
		//(TODO notify users)
			Usuario.withTransaction {
				usuario.save()
				def representedUsers = Usuario.findAllWhere(representative:null)
				log.debug "representedUsers.size(): ${representedUsers.size()}"
				representedUsers?.each {
					log.debug " - checking user - ${it.id}"
					it.type = Usuario.Type.USER
					it.representative = usuario
					it.save()
				}
			}
			def representationDocuments 
			RepresentationDocument.withTransaction {
				representationDocuments = RepresentationDocument.findAllWhere(
					state:RepresentationDocument.State.CANCELLED_BY_REPRESENTATIVE, representative:usuario)
				//TODO ===== check timestamp
				Date cancelDate = new Date(System.currentTimeMillis())
				representationDocuments.each {
					log.debug " - checking representationDocument - ${it.id}"
					it.state = RepresentationDocument.State.OK
					//it.cancellationSMIME = mensajeSMIME
					it.dateCanceled = cancelDate
					it.save()
				}
			}
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
			
			def userMetaInfJSON = JSON.parse(usuario.metaInf)
			userMetaInfJSON.representationsNumber = 0
			usuario.metaInf = userMetaInfJSON;

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
							mensajeSMIME:mensajeSMIMEReq, email:mensajeJSON.email,
							numeroCopias:respuestaGeneracionBackup.cantidad)
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
		
		def userMetaInfJSON = JSON.parse(usuario.metaInf)
		def representativeMap = [id: usuario.id, nif:usuario.nif, infoURL:infoURL, 
			 representativeMessageURL:representativeMessageURL,
			 imageURL:imageURL, representationsNumber:userMetaInfJSON?.representationsNumber,
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