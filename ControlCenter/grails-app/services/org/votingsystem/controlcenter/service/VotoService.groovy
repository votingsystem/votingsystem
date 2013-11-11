package org.votingsystem.controlcenter.service

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import org.votingsystem.model.ContextVS;
import org.votingsystem.controlcenter.model.*
import org.votingsystem.groovy.util.StringUtils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import grails.converters.JSON
import java.io.File;
import java.util.concurrent.Executors
import java.util.concurrent.Executor
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import org.votingsystem.signature.util.*;
import org.votingsystem.signature.smime.*;
import java.io.File;
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.ByteArrayBody
import java.nio.charset.Charset
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.ApplicationContext;
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;
import java.security.cert.CertificateFactory
import java.util.Locale;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* */
class VotoService {

    static transactional = true

	def messageSource
	def firmaService
    def grailsApplication  
    def httpService
	def encryptionService
	
	
	public synchronized ResponseVS validateVote(MessageSMIME messageSMIMEReq, Locale locale) {
		log.debug ("validateVote - ")
		EventoVotacion evento = messageSMIMEReq.evento
		String msg
		try {
			SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()			
			
			def votoJSON = JSON.parse(smimeMessageReq.getSignedContent())
			String voteUUID = votoJSON.UUID
			OpcionDeEvento opcionSeleccionada = OpcionDeEvento.findWhere(
				opcionDeEventoId:String.valueOf(votoJSON.opcionSeleccionadaId))
			if (!opcionSeleccionada || (opcionSeleccionada.eventoVotacion.id != evento.id)) {
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
						message:messageSource.getMessage('votingOptionNotFound',
						[votoJSON.opcionSeleccionadaId, evento.id].toArray(), locale))
			}
			
			X509Certificate certificadoFirma = smimeMessageReq.getFirmante()?.getCertificate()
			Certificado certificado = new Certificado(esRaiz:false, estado: Certificado.Estado.OK,
				tipo:Certificado.Tipo.VOTO, contenido:certificadoFirma.getEncoded(),
				usuario:messageSMIMEReq.usuario, numeroSerie:certificadoFirma.getSerialNumber().longValue(), 
				eventoVotacion:evento, validoDesde:certificadoFirma.getNotBefore(), 
				validoHasta:certificadoFirma.getNotAfter())
			certificado.setSigningCert(certificadoFirma)
			certificado.save()
			
			String urlVotosControlAcceso = "${evento.controlAcceso.serverURL}/voto"
			String localServerURL = grailsApplication.config.grails.serverURL
			
			String signedVoteDigest = smimeMessageReq.getContentDigestStr()
						
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = evento.controlAcceso.nombre
			String subject = messageSource.getMessage(
				'validacionVoto.smimeMessageSubject', null, locale)
			smimeMessageReq.setMessageID("${localServerURL}/messageSMIME/${messageSMIMEReq.id}")

			SMIMEMessageWrapper smimeVoteValidation = firmaService.
					getMultiSignedMimeMessage(fromUser, toUser, smimeMessageReq, subject)
					
			ResponseVS encryptResponse = encryptionService.encryptSMIMEMessage(
				smimeVoteValidation.getBytes(), evento.getControlAccesoCert(), locale);
			if (ResponseVS.SC_OK != encryptResponse.statusCode) {
				log.error("validateVote - encryptResponse ERROR - > ${encryptResponse.message}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
					tipo:Tipo.VOTO_CON_ERRORES, evento:evento, message:encryptResponse.message)
			} 
			
			messageSMIMEReq.tipo = TypeVS.VOTO_VALIDADO_CENTRO_CONTROL
			messageSMIMEReq.contenido = smimeVoteValidation.getBytes()
			messageSMIMEReq.save();
					
			byte[] encryptResponseBytes = encryptResponse.messageBytes
			//String encryptResponseStr = new String(encryptResponseBytes)
			//log.debug(" - encryptResponseStr: ${encryptResponseStr}")
			ResponseVS respuesta = httpService.sendMessage(encryptResponseBytes,
				ContextVS.SIGNED_AND_ENCRYPTED_CONTENT_TYPE, urlVotosControlAcceso)
			if (ResponseVS.SC_OK == respuesta.statusCode) {
				SMIMEMessageWrapper smimeMessageResp = new SMIMEMessageWrapper(
					new ByteArrayInputStream(respuesta.message.getBytes()));
				if(!smimeMessageResp.getContentDigestStr().equals(signedVoteDigest)) {
					log.error("validateVote - ERROR digest del voto enviado: " + signedVoteDigest +
						" - digest del voto recibido: " + smimeMessageResp.getContentDigestStr())
					return new ResponseVS(statusCode:ResponseVS.SC_ERROR, 
						tipo:Tipo.VOTO_CON_ERRORES, evento:evento, message:messageSource.
						getMessage('voteContentErrorMsg', null, locale))
				}
				respuesta = firmaService.validateVoteValidationCerts(smimeMessageResp, evento, locale)
				if(ResponseVS.SC_OK != respuesta.statusCode) {
					log.error("validateVote - validateVoteValidationCerts ERROR - > ${respuesta.message}")
					return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
						tipo:Tipo.VOTO_CON_ERRORES, evento:evento, message:respuesta.message)
				} 
				messageSMIMEReq.tipo = TypeVS.VOTO_VALIDADO_CONTROL_ACCESO
				messageSMIMEReq.contenido = smimeMessageResp.getBytes()
				MessageSMIME messageSMIMEResp = messageSMIMEReq
				MessageSMIME.withTransaction {
					messageSMIMEResp.save()
				}
				Voto voto = new Voto(opcionDeEvento:opcionSeleccionada,
					eventoVotacion:evento, estado:Voto.Estado.OK,
					certificado:certificado, messageSMIME:messageSMIMEResp)
				voto.save()
				return new ResponseVS(statusCode:ResponseVS.SC_OK, evento:evento, 
					tipo:Tipo.VOTO_VALIDADO_CONTROL_ACCESO,
					certificado:certificadoFirma, messageSMIME:messageSMIMEResp)
			} else {
				msg = messageSource.getMessage('accessRequestVoteErrorMsg', 
					[respuesta.message].toArray(), locale)
				log.error("validateVote - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION, 
					tipo:Tipo.VOTO_CON_ERRORES, message:msg)
			}
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
				message:messageSource.getMessage('voteErrorMsg', null, locale), 
				tipo:Tipo.VOTO_CON_ERRORES, evento:evento)
		}
	}
    
    def obtenerAsuntoEvento(byte[] signedDataBytes) {
        log.debug "obtenerAsuntoEvento"
        ByteArrayInputStream bais = new ByteArrayInputStream(signedDataBytes);
        MimeMessage email = new MimeMessage(null, bais);
        return email.getSubject()
    }
			
    EventoVotacion obtenerEventoAsociado(String asunto) {
        log.debug "obtenerEventoAsociado - asunto del Token: ${asunto}"
        EventoVotacion eventoVotacion
        //[Token de Acceso]-serverURL-eventoVotacionId
        String[] camposAsunto = asunto.split("-") 
        String serverURL = camposAsunto[1].toString()
        ControlAcceso controlAcceso = ControlAcceso.findWhere(serverURL:serverURL)
        if (controlAcceso) {
            eventoVotacion = EventoVotacion.findWhere(controlAcceso:controlAcceso,
            eventoVotacionId:camposAsunto[2])
        }
        return eventoVotacion
    }
	
	public synchronized ResponseVS processCancel (
			MessageSMIME messageSMIMEReq, Locale locale) {
		log.debug ("processCancel")
		EventoVotacion evento
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		String msg
		try {
			def anulacionJSON = JSON.parse(smimeMessageReq.getSignedContent())
			def origenHashCertificadoVoto = anulacionJSON.origenHashCertificadoVoto
			def hashCertificadoVotoBase64 = anulacionJSON.hashCertificadoVotoBase64
			def hashCertificadoVoto = CMSUtils.obtenerHashBase64(origenHashCertificadoVoto,
				"${grailsApplication.config.VotingSystem.votingHashAlgorithm}")
			if (!hashCertificadoVotoBase64.equals(hashCertificadoVoto))
					return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
						message:messageSource.getMessage(
						'anulacionVoto.errorEnHashCertificado', null, locale))
			AnuladorVoto anuladorVoto = AnuladorVoto.findWhere(
				hashCertificadoVotoBase64:hashCertificadoVotoBase64) 
			if(anuladorVoto) {
				String voteURL = "${grailsApplication.config.grails.serverURL}/voto/${anuladorVoto.voto.id}"
				return new ResponseVS(statusCode:ResponseVS.SC_ANULACION_REPETIDA,
					messageSMIME:anuladorVoto.messageSMIME, tipo:Tipo.ANULADOR_VOTO_ERROR,
					message:messageSource.getMessage('voteAlreadyCancelled', 
						[voteURL].toArray(), locale), evento:anuladorVoto.eventoVotacion)
			}
			def certificado = Certificado.findWhere(hashCertificadoVotoBase64:hashCertificadoVotoBase64)
			if (!certificado)
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
					tipo:Tipo.ANULADOR_VOTO_ERROR,
					message:messageSource.getMessage(
					'anulacionVoto.errorCertificadoNoEncontrado', null, locale))
			def voto = Voto.findWhere(certificado:certificado)
			if(!voto) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
					tipo:Tipo.ANULADOR_VOTO_ERROR,
					message:messageSource.getMessage(
					'anulacionVoto.errorVotoNoEncontrado', null, locale))
			evento = voto.eventoVotacion

			voto.estado = Voto.Estado.ANULADO
			voto.save()
			certificado.estado = Certificado.Estado.ANULADO
			certificado.save()
			
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = messageSMIMEReq.getUsuario()?.getNif()
			String subject = messageSource.getMessage(
				'mime.asunto.anulacionVotoValidada', null, locale)

			SMIMEMessageWrapper smimeMessageResp = firmaService.
					getMultiSignedMimeMessage(fromUser, toUser, smimeMessageReq, subject)
					
			MessageSMIME messageSMIMEResp = new MessageSMIME(valido:true,
				smimeMessage:smimeMessageResp, smimePadre:messageSMIMEReq,
				evento:evento, tipo:Tipo.RECIBO)
			
			messageSMIMEResp.save()
			if (!messageSMIMEResp.save()) {
			    messageSMIMEResp.errors.each { 
					msg = "${msg} - ${it}"
					log.error("processCancel - ${it}")
				}
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
					tipo:Tipo.ANULADOR_VOTO_ERROR, message:msg, evento:evento)
			}
			
			anuladorVoto = new AnuladorVoto(voto:voto,
				certificado:certificado, eventoVotacion:evento,
				origenHashCertificadoVotoBase64:origenHashCertificadoVoto,
				hashCertificadoVotoBase64:hashCertificadoVotoBase64,
				messageSMIME:messageSMIMEResp)
			if (!anuladorVoto.save()) {
			    anuladorVoto.errors.each {
					msg = "${msg} - ${it}" 
					log.error("processCancel - ${it}")
				}
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
					tipo:Tipo.ANULADOR_VOTO_ERROR, message:msg, evento:evento)
			} else {
				log.debug("processCancel - anuladorVoto.id: ${anuladorVoto.id}")
				return new ResponseVS(statusCode:ResponseVS.SC_OK, evento:evento,
					messageSMIME:messageSMIMEResp, tipo:Tipo.ANULADOR_VOTO)
			}			
		}catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(message:messageSource.getMessage(evento:evento,
				'error.encryptErrorMsg', null, locale), statusCode:ResponseVS.SC_ERROR_PETICION)
		}
	}
			
	public Map getVotoMap(Voto voto) {
		if(!voto) return [:]
		HexBinaryAdapter hexConverter = new HexBinaryAdapter();
		String hashHex = hexConverter.marshal(
			voto.certificado?.hashCertificadoVotoBase64?.getBytes());
		Map votoMap = [id:voto.id,
			hashCertificadoVotoBase64:voto.certificado.hashCertificadoVotoBase64,
			opcionDeEventoId:voto.opcionDeEvento.opcionDeEventoId,
			eventoVotacionId:voto.eventoVotacion.eventoVotacionId,
			eventoVotacionURL:voto.eventoVotacion?.url,
			estado:voto?.estado?.toString(),
			certificadoURL:"${grailsApplication.config.grails.serverURL}/certificado/voto/hashHex/${hashHex}",
			votoSMIMEURL:"${grailsApplication.config.grails.serverURL}/messageSMIME/${voto.messageSMIME.id}"]
		if(Voto.Estado.ANULADO == voto?.estado) {
			votoMap.anulacionURL="${grailsApplication.config.grails.serverURL}/anuladorVoto/voto/${voto.id}"
		}
		return votoMap
	}
 
	public Map getAnuladorVotoMap(AnuladorVoto anulador) {
		if(!anulador) return [:]
		Map anuladorMap = [id:anulador.id,
			votoURL:"${grailsApplication.config.grails.serverURL}/voto/${anulador.voto.id}",
			anuladorSMIMEURL:"${grailsApplication.config.grails.serverURL}/messageSMIME/${anulador.messageSMIME.id}"]
		return anuladorMap
	}
}