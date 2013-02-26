package org.sistemavotacion.controlacceso

import grails.converters.JSON;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream
import javax.mail.internet.MimeMessage;
import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.smime.*;
import java.util.Locale;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class SolicitudAccesoService {
	
	static scope = "request"

    static transactional = true
	
	def messageSource
    def firmaService
    def grailsApplication
	def subscripcionService

    def Respuesta validarSolicitud(byte[] textoFirmado, Locale locale) {
		log.debug("validarSolicitud")
        def hashSolicitudAccesoBase64
        def tipoRespuesta
        def solicitudAcceso
        def mensajeSMIME
        def respuesta
        def mensajeJSON
        ByteArrayInputStream bais = new ByteArrayInputStream(textoFirmado);
		SMIMEMessageWrapper smimeMessage;
		try {
			smimeMessage = new SMIMEMessageWrapper(null, bais, null);
			firmaService.validarCertificacionFirmantes(smimeMessage, locale)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:ex.getMessage())
		}
		Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
		if(Respuesta.SC_OK != respuestaUsuario.codigoEstado) {
			response.status = respuestaUsuario.codigoEstado
			render respuestaUsuario.mensaje
			return false
		}
		Usuario usuario = respuestaUsuario.usuario
		mensajeSMIME = new MensajeSMIME(usuario:usuario,
			valido:smimeMessage.isValidSignature(),	contenido:smimeMessage.getBytes())
		String asunto = smimeMessage.getSubject() 
        String idEventoVotacion
		String etiquetaAsunto = messageSource.getMessage('mime.asunto.SolicitudAcceso', null, locale)
		log.debug("etiquetaAsunto - ${etiquetaAsunto}")
        if (asunto?.trim().contains(etiquetaAsunto)) {
            String[] cadenasAsunto = asunto.split("-");
            idEventoVotacion = cadenasAsunto[1].trim()
			log.debug("idEventoVotacion - ${idEventoVotacion}")
            def eventoVotacion = EventoVotacion.findById(Long.valueOf(idEventoVotacion))
            if (eventoVotacion) {
				mensajeSMIME.evento = eventoVotacion
				log.debug("eventoVotacion - ${eventoVotacion.id}")
				if (!eventoVotacion.isOpen()) {
					return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
						mensaje:messageSource.getMessage('evento.mensajeCerrado', null, locale))	
				}
                if (!smimeMessage.isValidSignature()) {
					String msg = messageSource.getMessage('error.FirmaConErrores', null, locale)
                    mensajeSMIME.tipo = Tipo.SOLICITUD_ACCESO_FALLO
					mensajeSMIME.motivo = msg
                    respuesta = new Respuesta(tipo:Tipo.SOLICITUD_ACCESO_FALLO,
                            codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
                } else {//Ha votado el usuario?
                    solicitudAcceso = SolicitudAcceso.findWhere(usuario:usuario, 
                            eventoVotacion:eventoVotacion, estado:Tipo.OK)
                    if (solicitudAcceso){
                            log.debug("El usuario ya ha Votado - id solicitud previa: " + solicitudAcceso.id)//
                            mensajeSMIME.tipo = Tipo.SOLICITUD_ACCESO_REPETIDA
							String msg = "${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtener?id=${solicitudAcceso.mensajeSMIME.id}"
                            respuesta = new Respuesta(tipo:Tipo.SOLICITUD_ACCESO_REPETIDA,
								solicitudAcceso:solicitudAcceso, 
								codigoEstado:Respuesta.SC_ERROR_VOTO_REPETIDO, mensaje:msg)
                    } else {//es el hash único?
                        mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
                        log.debug("validandoSolicitud - mensajeJSON: ${mensajeJSON}"  )
                        hashSolicitudAccesoBase64 = mensajeJSON.hashSolicitudAccesoBase64
                        boolean hashSolicitudAccesoRepetido = (SolicitudAcceso.findWhere(
                                hashSolicitudAccesoBase64:hashSolicitudAccesoBase64) != null)
                        log.debug("hashSolicitudAccesoRepetido: ${hashSolicitudAccesoRepetido}")
                        if (hashSolicitudAccesoRepetido) {
                                mensajeSMIME.tipo = Tipo.SOLICITUD_ACCESO_FALLO_HASH_REPETIDO
                                respuesta = new Respuesta(tipo:Tipo.SOLICITUD_ACCESO_FALLO_HASH_REPETIDO,
                                        codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:messageSource.getMessage('error.HashRepetido', null, locale))
                        } else {//Todo OK
                            mensajeSMIME.tipo = Tipo.SOLICITUD_ACCESO
							log.debug("usuario: ${usuario.nif}")
							solicitudAcceso = new SolicitudAcceso(usuario:usuario,
								mensajeSMIME:mensajeSMIME, estado: SolicitudAcceso.Estado.OK,
								hashSolicitudAccesoBase64:hashSolicitudAccesoBase64, 
								eventoVotacion:eventoVotacion)
                            respuesta = new Respuesta(tipo:Tipo.SOLICITUD_ACCESO,
                                    codigoEstado:Respuesta.SC_OK, evento:eventoVotacion, solicitudAcceso:solicitudAcceso)
                        }
                    }
                }   
                
            } else {
				log.debug("Evento no encontrado")
				mensajeSMIME.tipo = Tipo.SOLICITUD_ACCESO_FALLO
				respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
						mensaje:messageSource.getMessage( 'error.EventoNoEncontrado', 
						[idEventoVotacion].toArray(), locale))
			}
        } else {
			String msg = "Error - asunto recibido: '${asunto}' - asunto requerido: '${etiquetaAsunto}'"  
			log.debug(msg)
			mensajeSMIME.tipo = Tipo.SOLICITUD_ACCESO_FALLO
			mensajeSMIME.motivo = msg
			respuesta = new Respuesta(tipo:Tipo.SOLICITUD_ACCESO_FALLO,
					codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:messageSource.getMessage('mime.asunto.SolicitudAccesoError', 
					[asunto].toArray(), locale))
        }
        if(mensajeSMIME) mensajeSMIME.save();
        if(solicitudAcceso) solicitudAcceso.save();
        return respuesta
    }
	
	def rechazarSolicitud(SolicitudAcceso solicitudAcceso) {
		log.debug("rechazarSolicitud '${solicitudAcceso.id}'")
		solicitudAcceso = solicitudAcceso.merge()
		solicitudAcceso.estado = SolicitudAcceso.Estado.ANULADO
		solicitudAcceso.save()
	}

}