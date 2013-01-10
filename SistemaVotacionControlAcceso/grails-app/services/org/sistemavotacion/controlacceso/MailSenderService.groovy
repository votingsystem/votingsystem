package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import java.util.Locale;

class MailSenderService {
	
	boolean transactional = false
	def mailService;
	def grailsApplication
	def messageSource

	public void sendMail (String email, String url, String mailSubject, String bodyView) {
		log.debug "- Sending mail -" + mailSubject + "- to email:${email}"
	    runAsync {
		   mailService.sendMail {
			   to email
			   subject mailSubject
			   body(view:bodyView, model:[url:url])
		   }
	   }
    }	
	
	public void sendInstruccionesDescargaCopiaSeguridad (SolicitudCopia solicitud, Locale locale) {
		log.debug "sendInstruccionesDescarga - email:${solicitud.email} - solicitud:${solicitud.id}"
		def asunto;
		String asuntoEvento
		String solicitante
		String urlSolicitud
		String urlDescarga
		SolicitudCopia.withTransaction {
			asuntoEvento = solicitud.documento?.evento?.asunto
			if(asuntoEvento) asunto = (asuntoEvento.length() > 90)?asuntoEvento.substring(0, 90) + "...":asuntoEvento
			solicitante = "${solicitud.documento?.usuario?.nombre} ${solicitud.documento?.usuario?.primerApellido}"
			urlSolicitud = "${grailsApplication.config.grails.serverURL}/solicitudCopia/obtenerSolicitud?id=${solicitud.id}"
			urlDescarga = "${grailsApplication.config.grails.serverURL}/solicitudCopia/obtener?id=${solicitud.id}"
		}
		log.debug "solicitante:${solicitante} - urlSolicitud:${urlSolicitud} - urlDescarga:${urlDescarga}"
		String asuntoEmail = messageSource.getMessage('mailSenderService.asuntoMailDescargaCopiaSeguridad', null, locale)
		runAsync {
		   mailService.sendMail {
			   to solicitud.email
			   subject asuntoEmail
			   body(view:"/mail/notificacionUrlCopias", model:[solicitante:solicitante,
				   urlSolicitud:urlSolicitud, asuntoManifiesto:asunto, urlDescarga:urlDescarga])
		   }
	   }
   }
	
	
	def enviarPDFAdjunto(Usuario usuario, String bodyView, byte[] pdfBytes) {
		log.debug "- enviarOfertaCliente - to usuario:${cliente.email}"
		runAsync {
			mailService.sendMail {
				multipart true
				to usuario.email
				subject "Notificación"
				body(view:bodyView, model:[usuario:usuario])
				attachBytes "Adjunto.pdf", "application/pdf", pdfBytes
			}
		}
	}
	
}
