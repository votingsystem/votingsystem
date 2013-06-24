package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import java.util.Locale;
import javax.mail.internet.MimeMessage
import grails.converters.JSON
import grails.gsp.PageRenderer;
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.FileSystemResource
import org.springframework.mail.javamail.MimeMessageHelper
import javax.mail.MessagingException;

class MailSenderService {
	
	static transactional = false
	
	def mailSender;
	def grailsApplication
	def messageSource
	PageRenderer groovyPageRenderer
	
	
	public void sendMail(String toUser, String msg, String subject) {
		log.debug "sendMail - toUser:${toUser} - subject:${subject}"
		runAsync {
			SimpleMailMessage message = new SimpleMailMessage();
			//message.setFrom(fromUser);
			message.setTo(toUser);
			message.setSubject(subject);
			message.setText(msg);
			mailSender.send(message);
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
			urlSolicitud = "${grailsApplication.config.grails.serverURL}/solicitudCopia/${solicitud.id}"
			urlDescarga = "${grailsApplication.config.grails.serverURL}/solicitudCopia/download/${solicitud.id}"
		}
		log.debug "solicitante:${solicitante} - urlSolicitud:${urlSolicitud} - urlDescarga:${urlDescarga}"
		String asuntoEmail = messageSource.getMessage('mailSenderService.asuntoMailDescargaCopiaSeguridad', null, locale)
		
		String renderedSrc = groovyPageRenderer.render (
			view:"/mail/notificacionUrlCopias", model:[solicitante:solicitante,
				   urlSolicitud:urlSolicitud, asuntoManifiesto:asunto, urlDescarga:urlDescarga])
		sendMail(solicitud.email, renderedSrc, asuntoEmail);
   }
	
	public void sendRepresentativeAccreditations (
		SolicitudCopia solicitud, String dateStr, Locale locale) {
		log.debug "sendRepresentativeAccreditations - email:${solicitud.email} - solicitud:${solicitud.id}"
		String asunto;
		Usuario userRequest
		String userRequestName
		Usuario representative
		String representativeName
		String urlSolicitud
		String urlDescarga
		SolicitudCopia.withTransaction {
			representative = solicitud.representative
			userRequest = solicitud.mensajeSMIME?.usuario
			representativeName = "${representative?.nombre} ${representative?.primerApellido}"
			userRequestName = "${userRequest?.nombre} ${userRequest?.primerApellido}"
			urlSolicitud = "${grailsApplication.config.grails.serverURL}/solicitudCopia/${solicitud.id}"
			urlDescarga = "${grailsApplication.config.grails.serverURL}/solicitudCopia/download/${solicitud.id}"
		}
		String emailSubject = messageSource.getMessage('representativeAccreditationsMailSubject',
			[representativeName].toArray(), locale)
		
		String renderedSrc = groovyPageRenderer.render (
				view:"/mail/RepresentativeAccreditationRequestDownloadInstructions", 
				model:[solicitante:userRequestName, dateStr:dateStr, pageTitle:emailSubject,
						urlSolicitud:urlSolicitud, representative:representativeName, urlDescarga:urlDescarga])
				
		sendMail(solicitud.email, renderedSrc, emailSubject);

	}
	
	public void sendRepresentativeVotingHistory (SolicitudCopia solicitud, 
		String dateFromStr, String dateToStr, Locale locale) {
		log.debug "sendRepresentativeVotingHistory - email:${solicitud.email} - solicitud:${solicitud.id}"
		String asunto;
		Usuario userRequest
		String userRequestName
		Usuario representative
		String representativeName
		String urlSolicitud
		String urlDescarga
		SolicitudCopia.withTransaction {
			representative = solicitud.representative
			userRequest = solicitud.mensajeSMIME?.usuario
			representativeName = "${representative?.nombre} ${representative?.primerApellido}"
			userRequestName = "${userRequest?.nombre} ${userRequest?.primerApellido}"
			urlSolicitud = "${grailsApplication.config.grails.serverURL}/solicitudCopia/${solicitud.id}"
			urlDescaimport grails.gsp.PageRendererrga = "${grailsApplication.config.grails.serverURL}/solicitudCopia/download/${solicitud.id}"
		}
		String emailSubject = messageSource.getMessage('representativeVotingHistoryMailSubject',
			[representativeName].toArray(), locale)
		
		String renderedSrc = groovyPageRenderer.render (
			view:"/mail/RepresentativeVotingHistoryDownloadInstructions",
			model:[solicitante:userRequestName, dateFromStr:dateFromStr, dateToStr:dateToStr,
				pageTitle:emailSubject,	urlSolicitud:urlSolicitud, representative:representativeName, urlDescarga:urlDescarga])
		
		sendMail(solicitud.email, renderedSrc, emailSubject);

	}
	
	def sendPDF(String email, String subject, 
		String bodyView, Map model, byte[] pdfBytes) {
		log.debug "- sendPDF - to usuario:${cliente.email}"
		
		String renderedSrc = groovyPageRenderer.render (
			view:bodyView, model:model)
		
		MimeMessage message = mailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
	 
			//helper.setFrom("from");
			helper.setTo(email);
			helper.setSubject(subject);
			helper.setText(renderedSrc);
	 
			ByteArrayResource byteArrayResource = new ByteArrayResource(pdfBytes)
			
			helper.addAttachment("attached.pdf", byteArrayResource);
	 
	   } catch(MessagingException ex) {
			log.error(ex.getMessage(), ex)
	   }
		mailSender.send(message);
		
	}
	
}
