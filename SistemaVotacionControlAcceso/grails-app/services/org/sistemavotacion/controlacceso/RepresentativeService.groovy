package org.sistemavotacion.controlacceso

import java.security.MessageDigest
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import javax.mail.internet.MimeMessage

import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.DateUtils;

import grails.converters.JSON
import sun.misc.BASE64Decoder;

class RepresentativeService {
	
	def subscripcionService
	def messageSource
	def grailsApplication
	def mailSenderService

	Respuesta saveUserRepresentative(SMIMEMessageWrapper smimeMessage, Locale locale) {
		log.debug("saveUserRepresentative -")
		MensajeSMIME mensajeSMIME = null
		RepresentationDocument representationDocument = null
		try { 
			Tipo tipo = Tipo.REPRESENTATIVE_SELECTION
			def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
			if(respuestaUsuario.codigoEstado != Respuesta.SC_OK) return respuestaUsuario
			Usuario usuario = respuestaUsuario.usuario

			Usuario representative = Usuario.findWhere(nif:mensajeJSON.representativeNif)
			mensajeSMIME = new MensajeSMIME(tipo:tipo,
				usuario:usuario, valido:true, contenido:smimeMessage.getBytes())
			String msg = null
			if(Usuario.Type.REPRESENTATIVE == usuario.type) {
				msg = messageSource.getMessage('userIsRepresentativeErrorMsg', 
					[usuario.nif].toArray(), locale)
				log.error "${msg} - user nif '${usuario.nif}' - representative nif '${representative.nif}'"
				mensajeSMIME.motivo = msg
				mensajeSMIME.valido = false
				mensajeSMIME.save()
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
			}
			if(!representative || Usuario.Type.REPRESENTATIVE != representative.type ||
				 usuario.nif == representative.nif) {
				if(usuario.nif == representative.nif) {
					msg = messageSource.getMessage('representativeSameUserNifErrorMsg',
						[mensajeJSON.representativeNif].toArray(), locale)
				} else {
					msg = messageSource.getMessage('representativeNifErrorMsg',
						[mensajeJSON.representativeNif].toArray(), locale)
				}
				log.error "${msg} - user nif '${usuario.nif}' - representative nif '${representative.nif}'"
				mensajeSMIME.motivo = msg
				mensajeSMIME.valido = false
				mensajeSMIME.save()
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
			}
			representationDocument = RepresentationDocument.findWhere(
				user:usuario, state:RepresentationDocument.State.OK)
			mensajeSMIME.save()
			if(representationDocument) {
				log.debug("user representative change")
				representationDocument.state = RepresentationDocument.State.CANCELLED
				representationDocument.cancellationSMIME=mensajeSMIME
				representationDocument.dateCanceled = new Date(System.currentTimeMillis());
				representationDocument.representative.representationsNumber--
				representationDocument.save()
			}
			representationDocument = new RepresentationDocument(activationSMIME:mensajeSMIME,
				user:usuario, representative:representative, state:RepresentationDocument.State.OK);
			usuario.representative = representative
			representative.representationsNumber++
			representationDocument.save()
			String resultMsg = messageSource.getMessage('representativeAssociatedMsg',
				[mensajeJSON.representativeName, usuario.nif].toArray(), locale)
			log.debug resultMsg
			return new Respuesta(codigoEstado:Respuesta.SC_OK, mensaje:resultMsg,
				mensajeSMIME:mensajeSMIME, usuario:usuario)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			if(mensajeSMIME) {
				mensajeSMIME.motivo = ex.getMessage()
				mensajeSMIME.valido = false;
				mensajeSMIME.save()
			}
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_EJECUCION,
				mensaje:ex.getMessage())
		}
	}
	
    Respuesta saveRepresentativeData(SMIMEMessageWrapper smimeMessage, 
		Long representaiveId, byte[] imageBytes, Locale locale) {
		log.debug("saveRepresentativeData - representaiveId: ${representaiveId}")
		try {
			Tipo tipo = Tipo.REPRESENTATIVE_DATA
			def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
			if(respuestaUsuario.codigoEstado != Respuesta.SC_OK) return respuestaUsuario
			Usuario usuario = respuestaUsuario.usuario
			if(representaiveId) {
				Usuario representative = Usuario.get(representaiveId)
				if(representaiveId) {
					String representativeName = "${representative.nombre} ${representative.primerApellido}"
					if(representative.nif != usuario.nif) {
						String message = messageSource.getMessage('editRepresentativeNifErrorMsg',[
								usuario.nif, representativeName].toArray(), locale)
						log.debug message
						return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
							mensaje:message)
					}
				}
			}
			String base64ImageHash = mensajeJSON.base64ImageHash
			MessageDigest messageDigest = MessageDigest.getInstance(
				grailsApplication.config.SistemaVotacion.votingHashAlgorithm);
			byte[] resultDigest =  messageDigest.digest(imageBytes);
			String base64ResultDigest = new String(Base64.encode(resultDigest));
			log.debug("saveRepresentativeData - base64ImageHash: ${base64ImageHash}" + 
				" - server calculated base64ImageHash: ${base64ResultDigest}")
			if(!base64ResultDigest.equals(base64ImageHash)) {
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:messageSource.getMessage('imageHashErrorMsg', null, locale))
			}
			//String base64EncodedImage = mensajeJSON.base64RepresentativeEncodedImage
			//BASE64Decoder decoder = new BASE64Decoder();
			//byte[] imageFileBytes = decoder.decodeBuffer(base64EncodedImage);
			def images = null
			MensajeSMIME mensajeSMIME = new MensajeSMIME(tipo:tipo,
				usuario:usuario, valido:true,
				contenido:smimeMessage.getBytes())
			Image.withTransaction {
				images = Image.findAllWhere(usuario:usuario)
				images?.each {
					it.type = Image.Type.REPRESENTATIVE_CANCELLED
					it.save()
				}
			}
			Image newImage = new Image(usuario:usuario, mensajeSMIME:mensajeSMIME,
				type:Image.Type.REPRESENTATIVE, fileBytes:imageBytes)
			String message = null
			if(Usuario.Type.REPRESENTATIVE != usuario.type) {
				usuario.type = Usuario.Type.REPRESENTATIVE
				usuario.representationsNumber = 1;
				message = messageSource.getMessage('representativeDataCreatedOKMsg', 
					[usuario.nombre, usuario.primerApellido].toArray(), locale)
			} else message = messageSource.getMessage('representativeDataUpdatedMsg', 
				[usuario.nombre, usuario.primerApellido].toArray(), locale)
			usuario.setInfo(mensajeJSON.representativeInfo)
			mensajeSMIME.save()
			usuario.representativeMessage = mensajeSMIME
			newImage.save()
			return new Respuesta(codigoEstado:Respuesta.SC_OK, mensaje:message)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_EJECUCION,
				mensaje:ex.getMessage())
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
				 return new Respuesta(codigoEstado:Respuesta.SC_OK, file:baseDirZipped,
					 cantidad:new Integer(metaInfJSON.numberVotes))
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
			ByteArrayInputStream bais = new ByteArrayInputStream(mensajeSMIME.contenido);
			MimeMessage msg = new MimeMessage(null, bais);
			File smimeFile = new File("${basedir}/${votoFileName}_${i}")
			FileOutputStream fos = new FileOutputStream(smimeFile);
			msg.writeTo(fos);
			fos.close();
			i++;
		}*/
		def metaInfMap = [numberVotes:i, dateFrom: DateUtils.getStringFromDate(dateFrom),
			dateTo:DateUtils.getStringFromDate(dateTo),
			representativeURL:"${grailsApplication.config.grails.serverURL}/representative/detailed?id=${representative.id}"]
		String metaInfJSONStr = metaInfMap as JSON
		metaInfFile = new File("${basedir}/meta.inf")
		metaInfFile.write(metaInfJSONStr)
		def ant = new AntBuilder()
		ant.zip(destfile: "${basedir}.zip", basedir: basedir)
		return new Respuesta(codigoEstado:Respuesta.SC_OK, cantidad:i, file:new File("${basedir}.zip"))
	}
		
	private Respuesta getAccreditationsBackup (Usuario representative,
		Date selectedDate, Locale locale){
		log.debug("getAccreditationsBackup - representative: ${representative.id}" +
			" - selectedDate: ${selectedDate}")
		def criteria = RepresentationDocument.createCriteria()
		def results
		def criteriaCancelled = RepresentationDocument.createCriteria()
		def cencelledResults
		Date todayDate = DateUtils.getTodayDate()
		RepresentationDocument.withTransaction {
			results = criteria.list {
				eq("state", RepresentationDocument.State.OK)
				eq("representative", representative)
				le("dateCreated", selectedDate)
			}
			
			cencelledResults = criteriaCancelled.list {
				eq("state", RepresentationDocument.State.CANCELLED)
				le("dateCreated", selectedDate)
				between("dateCanceled", selectedDate, todayDate)
			}
		}
		results.addAll(cencelledResults)
		
		def fecha = DateUtils.getShortStringFromDate(DateUtils.getTodayDate())
		String zipNamePrefix = messageSource.getMessage(
			'representativeAcreditationsBackupFileName', null, locale);
		def basedir = "${grailsApplication.config.SistemaVotacion.baseRutaCopiaRespaldo}" +
			"/AccreditationsBackup/${selectedDate}/${zipNamePrefix}_${representative.nif}"
		log.debug("getAccreditationsBackup - basedir: ${basedir}")
		File baseDirZipped = new File("${basedir}.zip")
		File metaInfFile;
		if(baseDirZipped.exists()) {
			 metaInfFile = new File("${basedir}/meta.inf")
			 if(metaInfFile) {
				 def metaInfJSON = JSON.parse(metaInfFile.text)
				 return new Respuesta(codigoEstado:Respuesta.SC_OK, file:baseDirZipped,
					 cantidad:new Integer(metaInfJSON.numberOfAccreditations))
			 }
		}
		new File(basedir).mkdirs()
		String accreditationFileName = messageSource.getMessage('accreditationFileName', null, locale)
		int i = 0
		results.each { it ->
			MensajeSMIME mensajeSMIME = it.activationSMIME
			ByteArrayInputStream bais = new ByteArrayInputStream(mensajeSMIME.contenido);
			MimeMessage msg = new MimeMessage(null, bais);
			File smimeFile = new File("${basedir}/${accreditationFileName}_${i}")
			FileOutputStream fos = new FileOutputStream(smimeFile);
			msg.writeTo(fos);
			fos.close();
			i++;
		}
		def metaInfMap = [numberOfAccreditations:i, selectedDate: DateUtils.getStringFromDate(selectedDate),
			representativeURL:"${grailsApplication.config.grails.serverURL}/representative/detailed?id=${representative.id}"]
		String metaInfJSONStr = metaInfMap as JSON
		metaInfFile = new File("${basedir}/meta.inf")
		metaInfFile.write(metaInfJSONStr)
		def ant = new AntBuilder()
		ant.zip(destfile: "${basedir}.zip", basedir: basedir)
		return new Respuesta(codigoEstado:Respuesta.SC_OK, cantidad:i, file:new File("${basedir}.zip"))
	}
	
	Respuesta precessVotingHistoryRequest(SMIMEMessageWrapper smimeMessage, Locale locale) {
		log.debug("precessVotingHistoryRequest -")
		MensajeSMIME mensajeSMIME = null
		def mensajeJSON
		try {
			Tipo tipo = Tipo.REPRESENTATIVE_VOTING_HISTORY_REQUEST 
			mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
			if(respuestaUsuario.codigoEstado != Respuesta.SC_OK) return respuestaUsuario
			Usuario usuario = respuestaUsuario.usuario
			mensajeSMIME = new MensajeSMIME(tipo:tipo, usuario:usuario, valido:true,
				contenido:smimeMessage.getBytes())
			Usuario representative = Usuario.findWhere(nif:mensajeJSON.representativeNif)
			if(!representative || Usuario.Type.REPRESENTATIVE != representative.type) {
				String msg = messageSource.getMessage('representativeNifErrorMsg',
					[mensajeJSON.representativeNif].toArray(), locale)
				log.error "${msg} - user nif '${usuario.nif}' - representative nif '${representative.nif}'"
				mensajeSMIME.motivo = msg
				mensajeSMIME.valido = false
				mensajeSMIME.save()
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
			}
			Date dateFrom = DateUtils.getDateFromString(mensajeJSON.dateFrom)
			Date dateTo = DateUtils.getDateFromString(mensajeJSON.dateTo)
			runAsync {
				Respuesta respuestaGeneracionBackup
				respuestaGeneracionBackup =getVotingHistoryBackup(representative, dateFrom,  dateTo, locale)
				if(Respuesta.SC_OK == respuestaGeneracionBackup?.codigoEstado) {
					File archivoCopias = respuestaGeneracionBackup.file
					SolicitudCopia solicitudCopia = new SolicitudCopia(
						filePath:archivoCopias.getAbsolutePath(),
						type:SolicitudCopia.Type.REPRESENTATIVE_VOTING_HISTORY, 
						representative:representative,
						mensajeSMIME:mensajeSMIME, email:mensajeJSON.email, 
						numeroCopias:respuestaGeneracionBackup.cantidad)
					log.error("mensajeSMIME: ${mensajeSMIME.id} - ${solicitudCopia.type}");
					SolicitudCopia.withTransaction {
						solicitudCopia.save()
					}
					mailSenderService.sendRepresentativeVotingHistory(
						solicitudCopia, mensajeJSON.dateFrom, mensajeJSON.dateTo, locale)
				} else log.error("Error generando archivo de copias de respaldo");
			}
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			if(mensajeSMIME) {
				mensajeSMIME.motivo = ex.getMessage()
				mensajeSMIME.valido = false;
				mensajeSMIME.save()
			}
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_EJECUCION,
				mensaje:ex.getMessage())
		}
		if(mensajeSMIME) mensajeSMIME.save()
		new Respuesta(codigoEstado:Respuesta.SC_OK,
			mensaje:messageSource.getMessage('backupRequestOKMsg',
			[mensajeJSON.email].toArray(), locale))
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
	
	Respuesta processUnsubscribeRequest(SMIMEMessageWrapper smimeMessage, Locale locale) {
		log.debug("processUnsubscribeRequest -")
		MensajeSMIME mensajeSMIME = null
		Tipo tipo = Tipo.REPRESENTATIVE_UNSUBSCRIBE_REQUEST
		def mensajeJSON
		String message
		try{
			mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			Tipo operationType = Tipo.valueOf(mensajeJSON.operation)
			if(Tipo.REPRESENTATIVE_UNSUBSCRIBE_REQUEST != operationType) {
				message = messageSource.getMessage('operationErrorMsg',
					[mensajeJSON.operation].toArray(), locale)
				log.debug message
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:message)
			}
			Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
			if(respuestaUsuario.codigoEstado != Respuesta.SC_OK) return respuestaUsuario
			Usuario usuario = respuestaUsuario.usuario
			mensajeSMIME = new MensajeSMIME(tipo:tipo, usuario:usuario, valido:true,
				contenido:smimeMessage.getBytes())
			if(Usuario.Type.REPRESENTATIVE != usuario.type) {
				message = messageSource.getMessage('unsubscribeRepresentativeUserErrorMsg',
					[usuario.nif].toArray(), locale)
				log.debug message
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:message)
			}
			mensajeSMIME.save()
			//(TODO notify users)
			Usuario.withTransaction {
				def representedUsers = Usuario.findAllWhere(representative:usuario)
				log.debug "representedUsers.size(): ${representedUsers.size()}"
				representedUsers?.each {
					log.debug " - checking user - ${it.id}"
					it.type = Usuario.Type.USER_WITH_CANCELLED_REPRESENTATIVE
					it.representative = null
					it.save()
				}
			}
			def representationDocuments 
			RepresentationDocument.withTransaction {
				representationDocuments = RepresentationDocument.findAllWhere(
					state:RepresentationDocument.State.OK, representative:usuario)
				//TODO ===== check timestamp
				Date cancelDate = new Date(System.currentTimeMillis())
				representationDocuments.each {
					log.debug " - checking representationDocument - ${it.id}"
					it.state = RepresentationDocument.State.CANCELLED_BY_REPRESENTATIVE
					it.cancellationSMIME = mensajeSMIME
					it.dateCanceled = cancelDate
					it.save()
				}
			}
			usuario.representativeMessage = mensajeSMIME
			usuario.type = Usuario.Type.EX_REPRESENTATIVE
			usuario.representationsNumber = 0
			Usuario.withTransaction  {
				usuario.save()
			}
			return new Respuesta(codigoEstado:Respuesta.SC_OK, mensajeSMIME:mensajeSMIME,
				usuario:usuario)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			if(mensajeSMIME) {
				mensajeSMIME.motivo = ex.getMessage()
				mensajeSMIME.valido = false;
				mensajeSMIME.save()
			}
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_EJECUCION,
				mensaje:ex.getMessage())
		}
	}
	
	
	Respuesta processAccreditationsRequest(SMIMEMessageWrapper smimeMessage, Locale locale) {
		log.debug("processAccreditationsRequest -")
		MensajeSMIME mensajeSMIME = null
		RepresentationDocument representationDocument = null
		def mensajeJSON
		try {
			Tipo tipo = Tipo.REPRESENTATIVE_ACCREDITATIONS_REQUEST
			mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
			if(respuestaUsuario.codigoEstado != Respuesta.SC_OK) return respuestaUsuario
			Usuario usuario = respuestaUsuario.usuario
			mensajeSMIME = new MensajeSMIME(tipo:tipo, usuario:usuario, valido:true,
				contenido:smimeMessage.getBytes())
			Usuario representative = Usuario.findWhere(nif:mensajeJSON.representativeNif)
			if(!representative || Usuario.Type.REPRESENTATIVE != representative.type) {
			   String msg = messageSource.getMessage('representativeNifErrorMsg',
				   [mensajeJSON.representativeNif].toArray(), locale)
			   log.error "${msg} - user nif '${usuario.nif}' - representative nif '${representative.nif}'"
			   mensajeSMIME.motivo = msg
			   mensajeSMIME.valido = false
			   mensajeSMIME.save()
			   return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
		   }
			Date selectedDate = DateUtils.getDateFromString(mensajeJSON.selectedDate)
			runAsync {
					Respuesta respuestaGeneracionBackup = getAccreditationsBackup(
						representative, selectedDate ,locale)
					if(Respuesta.SC_OK == respuestaGeneracionBackup?.codigoEstado) {
						File archivoCopias = respuestaGeneracionBackup.file
						SolicitudCopia solicitudCopia = new SolicitudCopia(
							filePath:archivoCopias.getAbsolutePath(),
							type:SolicitudCopia.Type.REPRESENTATIVE_ACCREDITATIONS,
							representative:representative,
							mensajeSMIME:mensajeSMIME, email:mensajeJSON.email,
							numeroCopias:respuestaGeneracionBackup.cantidad)
						SolicitudCopia.withTransaction {
							solicitudCopia.save()
						}
						mailSenderService.sendRepresentativeAccreditations(
							solicitudCopia, mensajeJSON.selectedDate, locale)
					} else log.error("Error generando archivo de copias de respaldo");
			}
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			if(mensajeSMIME) {
				mensajeSMIME.motivo = ex.getMessage()
				mensajeSMIME.valido = false;
				mensajeSMIME.save()
			}
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_EJECUCION,
				mensaje:ex.getMessage())
		}
		if(mensajeSMIME) mensajeSMIME.save()
		new Respuesta(codigoEstado:Respuesta.SC_OK,
				mensaje:messageSource.getMessage('backupRequestOKMsg',
				[mensajeJSON.email].toArray(), locale))
	}
		
	public Map getRepresentativeJSONMap(Usuario usuario) {
		//log.debug("getRepresentativeJSONMap: ${usuario.id} ")
		
		String representativeMessageURL = 
			"${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtener?id=${usuario.representativeMessage?.id}"
		Image image
		Image.withTransaction {
			image = Image.findByTypeAndUsuario (Image.Type.REPRESENTATIVE, usuario)
		}
		String imageURL = "${grailsApplication.config.grails.serverURL}/representative/image?id=${image?.id}" 
		String infoURL = "${grailsApplication.config.grails.serverURL}/representative/info?id=${usuario?.id}" 
		
		def representativeMap = [id: usuario.id, nif:usuario.nif, infoURL:infoURL, 
			 representativeMessageURL:representativeMessageURL,
			 imageURL:imageURL, representationsNumber:usuario.representationsNumber,
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