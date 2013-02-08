package org.sistemavotacion.controlacceso

import javax.mail.internet.MimeMessage;
import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.utils.StringUtils;
import grails.converters.JSON;
import java.util.Locale;

class VotoService {
	
	def messageSource
	def grailsApplication
	def firmaService
	def httpService
	
    synchronized Respuesta validarFirmas(SMIMEMessageWrapper smimeMessage) {
		log.debug ("validarFirmas")		
		String localServerURL = grailsApplication.config.grails.serverURL
		InformacionVoto infoVoto = smimeMessage.informacionVoto
		String certServerURL = StringUtils.checkURL(infoVoto.controlAccesoURL)
		MensajeSMIME mensajeSMIME = new MensajeSMIME(
			tipo:Tipo.VOTO_VALIDADO_CENTRO_CONTROL,valido:true,
			contenido:smimeMessage.getBytes())
		String mensaje
		if (!localServerURL.equals(certServerURL)) {
			log.debug ("infoVoto.eventoURL: '${infoVoto.controlAccesoURL}'")
			log.debug ("localServerURL: '${localServerURL}' - certServerURL:'${certServerURL}'")
			mensaje = "El certificado de voto no es de este servidor"
		}	
		EventoVotacion evento = EventoVotacion.get(Long.valueOf(infoVoto.getEventoId()))
		if (!evento) 
			mensaje = "El evento con id '${infoVoto.getEventoId()}' no esta dado de alta en este servidor"
		mensajeSMIME.evento = evento
		Respuesta respuestaValidacionCA = firmaService.
			validarCertificacionFirmantesVoto(smimeMessage, evento);
		if(200 != respuestaValidacionCA.codigoEstado) return respuestaValidacionCA
		Certificado certificado = Certificado.findWhere(
			hashCertificadoVotoBase64:infoVoto.hashCertificadoVotoBase64,
			estado:Certificado.Estado.OK)
		if (!certificado) 
			mensaje = "El hash '${infoVoto.hashCertificadoVotoBase64}' no es válido"
		def votoJSON = JSON.parse(smimeMessage.getSignedContent())
		OpcionDeEvento opcionSeleccionada = 
			evento.comprobarOpcionId(Long.valueOf(votoJSON.opcionSeleccionadaId))
		if (!opcionSeleccionada)
			mensaje = "No existe ninguna opción con identificador '${opcionId}'"
		if (mensaje) {
			log.error(mensaje)
			return new Respuesta (codigoEstado:400, mensaje:mensaje)
		}
		mensajeSMIME.save()	
		MensajeSMIME multifirmaControlAcceso = new MensajeSMIME(smimePadre:mensajeSMIME,
			tipo:Tipo.VOTO_VALIDADO_CONTROL_ACCESO, valido:true, evento:evento)
		multifirmaControlAcceso.save()
		smimeMessage.setMessageID("${localServerURL}/mensajeSMIME/obtener?id=${multifirmaControlAcceso.id}")
		smimeMessage.setTo(infoVoto.hashCertificadoVotoBase64)
		
		MimeMessage multiFirmaMimeMessage = firmaService.generarMultifirma(
			smimeMessage, "[VOTO_VALIDADO_CONTROL_ACCESO]")
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		multiFirmaMimeMessage.writeTo(baos);		
		multifirmaControlAcceso.contenido = baos.toByteArray()
		multifirmaControlAcceso.save()

		Voto voto = new Voto(opcionDeEvento:opcionSeleccionada, 
			eventoVotacion:evento, estado:Voto.Estado.OK,
			certificado:certificado, mensajeSMIME:multifirmaControlAcceso)
		voto.save()
		certificado.estado = Certificado.Estado.UTILIZADO;
		certificado.save();
		return new Respuesta(codigoEstado:200, voto:voto)
    }
	
	public synchronized Respuesta validarAnulacion (SMIMEMessageWrapper smimeMessage, Locale locale) {
		log.debug ("validarAnulacion - ${smimeMessage.getSignedContent()}")
		def anulacionJSON = JSON.parse(smimeMessage.getSignedContent())
		def origenHashCertificadoVoto = anulacionJSON.origenHashCertificadoVoto
		def hashCertificadoVotoBase64 = anulacionJSON.hashCertificadoVotoBase64
		def origenHashSolicitudAcceso = anulacionJSON.origenHashSolicitudAcceso
		def hashSolicitudAccesoBase64 = anulacionJSON.hashSolicitudAccesoBase64
		if(!origenHashCertificadoVoto || !hashCertificadoVotoBase64 || 
			!origenHashSolicitudAcceso || !hashSolicitudAccesoBase64) {
			return new Respuesta(codigoEstado:400,
				mensaje:messageSource.getMessage('anulacionVoto.dataError', null, locale))
		}
		
		def hashCertificadoVoto = CMSUtils.obtenerHashBase64(origenHashCertificadoVoto, 
			"${grailsApplication.config.SistemaVotacion.votingHashAlgorithm}")
		def hashSolicitud = CMSUtils.obtenerHashBase64(origenHashSolicitudAcceso, 
			"${grailsApplication.config.SistemaVotacion.votingHashAlgorithm}")
		Tipo tipoPeticion = Tipo.ANULADOR_VOTO
		EventoVotacion eventoVotacion
		int codigoEstado = 400
		if (!hashSolicitudAccesoBase64.equals(hashSolicitud))
				return new Respuesta(codigoEstado:400, mensaje:messageSource.getMessage(
					'anulacionVoto.errorEnHashSolcitud', null, locale))
		def solicitudAcceso = SolicitudAcceso.findWhere(hashSolicitudAccesoBase64:hashSolicitudAccesoBase64)
		if (!solicitudAcceso) 
				return new Respuesta(codigoEstado:400, mensaje:messageSource.getMessage(
					'anulacionVoto.errorSolicitudNoEncontrada', null, locale))
		if(solicitudAcceso.estado.equals(SolicitudAcceso.Estado.ANULADO)) {
			return new Respuesta(codigoEstado:Respuesta.SC_ANULACION_REPETIDA, mensaje:messageSource.getMessage(
				'anulacionVoto.errorSolicitudAnulada', null, locale))
		}	
		if (!hashCertificadoVotoBase64.equals(hashCertificadoVoto))
				return new Respuesta(codigoEstado:400, mensaje:messageSource.getMessage(
					'anulacionVoto.errorEnHashCertificado', null, locale))
		def solicitudCSR = SolicitudCSRVoto.findWhere(hashCertificadoVotoBase64:hashCertificadoVotoBase64)
		if (!solicitudCSR)
				return new Respuesta(codigoEstado:400, mensaje:messageSource.getMessage(
					'anulacionVoto.errorSNoEncontrada', null, locale))
		def certificado = Certificado.findWhere(hashCertificadoVotoBase64:hashCertificadoVotoBase64)
		if (!certificado) return new Respuesta(codigoEstado:400, mensaje:messageSource.
			getMessage('anulacionVoto.errorSolicitudCSRNoEncontrada', null, locale))
		else eventoVotacion = certificado.eventoVotacion
		def voto = Voto.findWhere(certificado:certificado)
		if(voto){
			voto.estado = Voto.Estado.ANULADO
			voto.save()
			codigoEstado = Respuesta.SC_OK
		} else {
			log.debug ("validarAnulacion - ${messageSource.getMessage('anulacionVoto.errorVotoNoEncontrado', null, locale)}")
			tipoPeticion = Tipo.ANULADOR_SOLICITUD_ACCESO
			codigoEstado = Respuesta.SC_OK_ANULACION_SOLICITUD_ACCESO
		} 
		MensajeSMIME mensajeSMIME = new MensajeSMIME(tipo:tipoPeticion, valido:true, 
			contenido:smimeMessage.getBytes(), evento:eventoVotacion)
		mensajeSMIME.save()
		solicitudAcceso.estado = SolicitudAcceso.Estado.ANULADO
		solicitudAcceso.save()
		solicitudCSR.estado = SolicitudCSRVoto.Estado.ANULADA
		solicitudCSR.save()
		certificado.estado = Certificado.Estado.ANULADO
		certificado.save()
		String asuntoMultiFirmaMimeMessage = messageSource.getMessage('mime.asunto.anulacionVotoValidada', null, locale)

		MensajeSMIME mensajeSMIMEValidado = new MensajeSMIME(tipo:Tipo.FIRMA_VALIDADA,
			smimePadre:mensajeSMIME, evento:eventoVotacion, valido:true)
		mensajeSMIMEValidado.save();
		smimeMessage.setMessageID("${grailsApplication.config.grails.serverURL}" + 
			"/mensajeSMIME/obtener?id=${mensajeSMIMEValidado.id}")
		smimeMessage.setTo(hashCertificadoVotoBase64)
		
		MimeMessage multiFirmaMimeMessage = firmaService.generarMultifirma (
			smimeMessage, asuntoMultiFirmaMimeMessage)
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		multiFirmaMimeMessage.writeTo(baos);
				
		mensajeSMIMEValidado.contenido = baos.toByteArray()
		mensajeSMIMEValidado.save();
		
		AnuladorVoto anuladorVoto = new AnuladorVoto(mensajeSMIME:mensajeSMIMEValidado,
			solicitudAcceso:solicitudAcceso, solicitudCSRVoto:solicitudCSR,
			origenHashSolicitudAccesoBase64:origenHashSolicitudAcceso,
			origenHashCertificadoVotoBase64:origenHashCertificadoVoto,
			hashSolicitudAccesoBase64:hashSolicitudAccesoBase64,
			hashCertificadoVotoBase64:hashCertificadoVotoBase64,
			eventoVotacion:eventoVotacion,
			estado:AnuladorVoto.Estado.SIN_NOTIFICAR, voto:voto)
		anuladorVoto.save();
		return new Respuesta(codigoEstado:codigoEstado, anuladorVoto:anuladorVoto)
	}
	
	public synchronized Respuesta enviarAnulacion_A_CentroControl (AnuladorVoto anuladorVoto) {
		String centroControlURL = anuladorVoto.voto.eventoVotacion.centroControl.serverURL
		String urlAnulacionVoto = "${centroControlURL}${grailsApplication.config.SistemaVotacion.sufijoURLAnulacionVoto}"
		Respuesta respuestaCentroControl = httpService.enviarMensaje(urlAnulacionVoto, 
			anuladorVoto.mensajeSMIME.contenido)
		log.debug ("enviarAnulacion_A_CentroControl - respuesta : '${respuestaCentroControl.codigoEstado}' ")
		if (200 == respuestaCentroControl.codigoEstado) {
			anuladorVoto.estado = AnuladorVoto.Estado.NOTIFICADO
			anuladorVoto.merge()
		}
		return respuestaCentroControl
	}
	
}