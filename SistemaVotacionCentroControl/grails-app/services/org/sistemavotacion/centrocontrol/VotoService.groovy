package org.sistemavotacion.centrocontrol

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import org.sistemavotacion.centrocontrol.modelo.*
import org.sistemavotacion.utils.StringUtils;
import grails.converters.JSON
import java.io.File;
import java.util.concurrent.Executors
import java.util.concurrent.Executor
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import org.sistemavotacion.seguridad.*;
import org.sistemavotacion.smime.*;
import java.io.File;
import org.apache.http.conn.HttpHostConnectException
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.ByteArrayBody
import java.nio.charset.Charset
import groovyx.net.http.ContentType
import net.sf.json.JSONSerializer
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
	
	
	public synchronized Respuesta validateVote(MensajeSMIME mensajeSMIMEReq, Locale locale) {
		log.debug ("validateVote - ")
		EventoVotacion evento = mensajeSMIMEReq.evento
		String msg
		try {
			SMIMEMessageWrapper smimeMessageReq = mensajeSMIMEReq.getSmimeMessage()			
			
			def votoJSON = JSON.parse(smimeMessageReq.getSignedContent())
			String voteUUID = votoJSON.UUID
			OpcionDeEvento opcionSeleccionada = OpcionDeEvento.findWhere(
				opcionDeEventoId:String.valueOf(votoJSON.opcionSeleccionadaId))
			if (!opcionSeleccionada || (opcionSeleccionada.eventoVotacion.id != evento.id)) {
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
						mensaje:messageSource.getMessage('votingOptionNotFound',
						[votoJSON.opcionSeleccionadaId, evento.id].toArray(), locale))
			}
			
			X509Certificate certificadoFirma = smimeMessageReq.getFirmante()?.getCertificate()
			Certificado certificado = new Certificado(esRaiz:false, estado: Certificado.Estado.OK,
				tipo:Certificado.Tipo.VOTO, contenido:certificadoFirma.getEncoded(),
				usuario:mensajeSMIMEReq.usuario, numeroSerie:certificadoFirma.getSerialNumber().longValue(), 
				eventoVotacion:evento, validoDesde:certificadoFirma.getNotBefore(), 
				validoHasta:certificadoFirma.getNotAfter())
			certificado.setSigningCert(certificadoFirma)
			certificado.save()
			
			String urlVotosControlAcceso = "${evento.controlAcceso.serverURL}" +
				"${grailsApplication.config.SistemaVotacion.sufijoURLNotificacionVotoControlAcceso}"
			String localServerURL = grailsApplication.config.grails.serverURL
			
			String signedVoteDigest = smimeMessageReq.getContentDigestStr()
						
			String fromUser = grailsApplication.config.SistemaVotacion.serverName
			String toUser = evento.controlAcceso.nombre
			String subject = messageSource.getMessage(
				'validacionVoto.smimeMessageSubject', null, locale)
			smimeMessageReq.setMessageID("${localServerURL}/mensajeSMIME/${mensajeSMIMEReq.id}")

			SMIMEMessageWrapper smimeVoteValidation = firmaService.
					getMultiSignedMimeMessage(fromUser, toUser, smimeMessageReq, subject)
					
			Respuesta encryptResponse = encryptionService.encryptSMIMEMessage(
				smimeVoteValidation.getBytes(), evento.getControlAccesoCert(), locale);
			if (Respuesta.SC_OK != encryptResponse.codigoEstado) {
				log.error("validateVote - encryptResponse ERROR - > ${encryptResponse.mensaje}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
					tipo:Tipo.VOTO_CON_ERRORES, evento:evento, mensaje:encryptResponse.mensaje)
			} 
			
			mensajeSMIMEReq.tipo = Tipo.VOTO_VALIDADO_CENTRO_CONTROL
			mensajeSMIMEReq.contenido = smimeVoteValidation.getBytes()
			mensajeSMIMEReq.save();
					
			byte[] encryptResponseBytes = encryptResponse.messageBytes
			//String encryptResponseStr = new String(encryptResponseBytes)
			//log.debug(" - encryptResponseStr: ${encryptResponseStr}")
			
			String contentType = "${grailsApplication.config.pkcs7SignedContentType};" +
				"${grailsApplication.config.pkcs7EncryptedContentType}"
			Respuesta respuesta = httpService.sendMessage(encryptResponseBytes,
				contentType, urlVotosControlAcceso)
			if (Respuesta.SC_OK == respuesta.codigoEstado) {
				SMIMEMessageWrapper smimeMessageResp = new SMIMEMessageWrapper(
					new ByteArrayInputStream(respuesta.mensaje.getBytes()));
				if(!smimeMessageResp.getContentDigestStr().equals(signedVoteDigest)) {
					log.error("validateVote - ERROR digest del voto enviado: " + signedVoteDigest +
						" - digest del voto recibido: " + smimeMessageResp.getContentDigestStr())
					return new Respuesta(codigoEstado:Respuesta.SC_ERROR, 
						tipo:Tipo.VOTO_CON_ERRORES, evento:evento, mensaje:messageSource.
						getMessage('voteContentErrorMsg', null, locale))
				}
				respuesta = firmaService.validateVoteValidationCerts(smimeMessageResp, evento, locale)
				if(Respuesta.SC_OK != respuesta.codigoEstado) {
					log.error("validateVote - validateVoteValidationCerts ERROR - > ${respuesta.mensaje}")
					return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
						tipo:Tipo.VOTO_CON_ERRORES, evento:evento, mensaje:respuesta.mensaje)
				} 
				mensajeSMIMEReq.tipo = Tipo.VOTO_VALIDADO_CONTROL_ACCESO
				mensajeSMIMEReq.contenido = smimeMessageResp.getBytes()
				MensajeSMIME mensajeSMIMEResp = mensajeSMIMEReq
				MensajeSMIME.withTransaction {
					mensajeSMIMEResp.save()
				}
				Voto voto = new Voto(opcionDeEvento:opcionSeleccionada,
					eventoVotacion:evento, estado:Voto.Estado.OK,
					certificado:certificado, mensajeSMIME:mensajeSMIMEResp)
				voto.save()
				return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento, 
					tipo:Tipo.VOTO_VALIDADO_CONTROL_ACCESO,
					certificado:certificadoFirma, mensajeSMIME:mensajeSMIMEResp)
			} else {
				msg = messageSource.getMessage('accessRequestVoteErrorMsg', 
					[respuesta.mensaje].toArray(), locale)
				log.error("validateVote - ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					tipo:Tipo.VOTO_CON_ERRORES, mensaje:msg)
			}
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
				mensaje:messageSource.getMessage('voteErrorMsg', null, locale), 
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
	
	public synchronized Respuesta processCancel (
			MensajeSMIME mensajeSMIMEReq, Locale locale) {
		log.debug ("processCancel")
		EventoVotacion evento
		SMIMEMessageWrapper smimeMessageReq = mensajeSMIMEReq.getSmimeMessage()
		String msg
		try {
			def anulacionJSON = JSON.parse(smimeMessageReq.getSignedContent())
			def origenHashCertificadoVoto = anulacionJSON.origenHashCertificadoVoto
			def hashCertificadoVotoBase64 = anulacionJSON.hashCertificadoVotoBase64
			def hashCertificadoVoto = CMSUtils.obtenerHashBase64(origenHashCertificadoVoto,
				"${grailsApplication.config.SistemaVotacion.votingHashAlgorithm}")
			if (!hashCertificadoVotoBase64.equals(hashCertificadoVoto))
					return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
						mensaje:messageSource.getMessage(
						'anulacionVoto.errorEnHashCertificado', null, locale))
			AnuladorVoto anuladorVoto = AnuladorVoto.findWhere(
				hashCertificadoVotoBase64:hashCertificadoVotoBase64) 
			if(anuladorVoto) {
				String voteURL = "${grailsApplication.config.grails.serverURL}/voto/${anuladorVoto.voto.id}"
				return new Respuesta(codigoEstado:Respuesta.SC_ANULACION_REPETIDA,
					mensajeSMIME:anuladorVoto.mensajeSMIME, tipo:Tipo.ANULADOR_VOTO_ERROR,
					mensaje:messageSource.getMessage('voteAlreadyCancelled', 
						[voteURL].toArray(), locale), evento:anuladorVoto.eventoVotacion)
			}
			def certificado = Certificado.findWhere(hashCertificadoVotoBase64:hashCertificadoVotoBase64)
			if (!certificado)
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					tipo:Tipo.ANULADOR_VOTO_ERROR,
					mensaje:messageSource.getMessage(
					'anulacionVoto.errorCertificadoNoEncontrado', null, locale))
			def voto = Voto.findWhere(certificado:certificado)
			if(!voto) return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					tipo:Tipo.ANULADOR_VOTO_ERROR,
					mensaje:messageSource.getMessage(
					'anulacionVoto.errorVotoNoEncontrado', null, locale))
			evento = voto.eventoVotacion

			voto.estado = Voto.Estado.ANULADO
			voto.save()
			certificado.estado = Certificado.Estado.ANULADO
			certificado.save()
			
			String fromUser = grailsApplication.config.SistemaVotacion.serverName
			String toUser = mensajeSMIMEReq.getUsuario()?.getNif()
			String subject = messageSource.getMessage(
				'mime.asunto.anulacionVotoValidada', null, locale)

			SMIMEMessageWrapper smimeMessageResp = firmaService.
					getMultiSignedMimeMessage(fromUser, toUser, smimeMessageReq, subject)
					
			MensajeSMIME mensajeSMIMEResp = new MensajeSMIME(valido:true,
				smimeMessage:smimeMessageResp, smimePadre:mensajeSMIMEReq,
				evento:evento, tipo:Tipo.RECIBO)
			
			mensajeSMIMEResp.save()
			if (!mensajeSMIMEResp.save()) {
			    mensajeSMIMEResp.errors.each { 
					msg = "${msg} - ${it}"
					log.error("processCancel - ${it}")
				}
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					tipo:Tipo.ANULADOR_VOTO_ERROR, mensaje:msg, evento:evento)
			}
			
			anuladorVoto = new AnuladorVoto(voto:voto,
				certificado:certificado, eventoVotacion:evento,
				origenHashCertificadoVotoBase64:origenHashCertificadoVoto,
				hashCertificadoVotoBase64:hashCertificadoVotoBase64,
				mensajeSMIME:mensajeSMIMEResp)
			if (!anuladorVoto.save()) {
			    anuladorVoto.errors.each {
					msg = "${msg} - ${it}" 
					log.error("processCancel - ${it}")
				}
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					tipo:Tipo.ANULADOR_VOTO_ERROR, mensaje:msg, evento:evento)
			} else {
				log.debug("processCancel - anuladorVoto.id: ${anuladorVoto.id}")
				return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento,
					mensajeSMIME:mensajeSMIMEResp, tipo:Tipo.ANULADOR_VOTO)
			}			
		}catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(mensaje:messageSource.getMessage(evento:evento,
				'error.encryptErrorMsg', null, locale), codigoEstado:Respuesta.SC_ERROR_PETICION)
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
			votoSMIMEURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME/${voto.mensajeSMIME.id}"]
		if(Voto.Estado.ANULADO == voto?.estado) {
			votoMap.anulacionURL="${grailsApplication.config.grails.serverURL}/anuladorVoto/voto/${voto.id}"
		}
		return votoMap
	}
 
	public Map getAnuladorVotoMap(AnuladorVoto anulador) {
		if(!anulador) return [:]
		Map anuladorMap = [id:anulador.id,
			votoURL:"${grailsApplication.config.grails.serverURL}/voto/${anulador.voto.id}",
			anuladorSMIMEURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME/${anulador.mensajeSMIME.id}"]
		return anuladorMap
	}
}