package org.sistemavotacion.controlacceso

import java.security.MessageDigest
import java.util.Date;
import java.util.Locale;
import java.util.Map;

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
			if(!representative || Usuario.Type.REPRESENTATIVE != representative.type ||
				 usuario.nif == representative.nif) {
				String msg = messageSource.getMessage('representativeNifErrorMsg', 
					[mensajeJSON.representativeNif].toArray(), locale)
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
				//===== prueba representationDocument.representative.save()
				representationDocument.save()
			}
			representationDocument = new RepresentationDocument(activationSMIME:mensajeSMIME,
				user:usuario, representative:representative, state:RepresentationDocument.State.OK);
			representative.representative = representative
			usuario.representative
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
		byte[] imageBytes, Locale locale) {
		log.debug("saveRepresentativeData -")
		try {
			Tipo tipo = Tipo.REPRESENTATIVE_DATA
			def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
			if(respuestaUsuario.codigoEstado != Respuesta.SC_OK) return respuestaUsuario
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
			Image image = null
			Usuario usuario = respuestaUsuario.usuario
			MensajeSMIME mensajeSMIME = new MensajeSMIME(tipo:tipo,
				usuario:usuario, valido:true,
				contenido:smimeMessage.getBytes())
			Image.withTransaction {
				image = Image.findWhere(usuario:usuario)
			}
			if(image) {
				image.type = Image.Type.REPRESENTATIVE_CANCELLED
				image.save()
			}
			Image newImage = new Image(usuario:usuario, mensajeSMIME:mensajeSMIME,
				type:Image.Type.REPRESENTATIVE, fileBytes:imageBytes)
			String message = null
			if(Usuario.Type.USER == usuario.type) {
				usuario.type = Usuario.Type.REPRESENTATIVE
				usuario.representationsNumber = 1;
				message = messageSource.getMessage('representativeDataCreatedOKMsg', 
					[usuario.nombre, usuario.primerApellido].toArray(), locale)
			} else message = messageSource.getMessage('representativeDataUpdatedMsg', 
				[usuario.nombre, usuario.primerApellido].toArray(), locale)
			usuario.setInfo(mensajeJSON.representativeInfo)
			mensajeSMIME.save()
			usuario.representativeMessage = mensajeSMIME
			//====== usuario.save()
			newImage.save()
			return new Respuesta(codigoEstado:Respuesta.SC_OK, mensaje:message)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_EJECUCION,
				mensaje:ex.getMessage())
		}

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
