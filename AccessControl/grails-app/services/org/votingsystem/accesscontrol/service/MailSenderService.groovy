package org.votingsystem.accesscontrol.service

import grails.gsp.PageRenderer
import org.springframework.core.io.ByteArrayResource
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.MimeMessageHelper
import org.votingsystem.model.BackupRequestVS
import org.votingsystem.model.UserVS

import javax.mail.MessagingException
import javax.mail.internet.MimeMessage

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
	
	public void sendInstruccionesDescargaCopiaSeguridad (BackupRequestVS solicitud, Locale locale) {
		log.debug "sendInstruccionesDescarga - email:${solicitud.email} - solicitud:${solicitud.id}"
		def subject;
		String eventVSSubject
		String solicitante
		String urlSolicitud
		String urlDescarga
		BackupRequestVS.withTransaction {
			eventVSSubject = solicitud.PDFDocumentVS?.eventVS?.subject
			if(eventVSSubject) subject = (eventVSSubject.length() > 90)?eventVSSubject.substring(0, 90) + "...":eventVSSubject
			solicitante = "${solicitud.PDFDocumentVS?.userVS?.name} ${solicitud.PDFDocumentVS?.userVS?.firstName}"
			urlSolicitud = "${grailsApplication.config.grails.serverURL}/solicitudCopia/${solicitud.id}"
			urlDescarga = "${grailsApplication.config.grails.serverURL}/solicitudCopia/download/${solicitud.id}"
		}
		log.debug "solicitante:${solicitante} - urlSolicitud:${urlSolicitud} - urlDescarga:${urlDescarga}"
		String emailSubject = messageSource.getMessage('downloadBackupMailSubject', null, locale)
		
		String renderedSrc = groovyPageRenderer.render (
			view:"/mail/notificacionUrlCopias", model:[solicitante:solicitante,
				   urlSolicitud:urlSolicitud, eventVSManifestSubject:subject, urlDescarga:urlDescarga])
		sendMail(solicitud.email, renderedSrc, emailSubject);
   }
	
	public void sendRepresentativeAccreditations (
		BackupRequestVS solicitud, String dateStr, Locale locale) {
		log.debug "sendRepresentativeAccreditations - email:${solicitud.email} - solicitud:${solicitud.id}"
		String subject;
		UserVS userRequest
		String userRequestName
		UserVS representative
		String representativeName
		String urlSolicitud
		String urlDescarga
		BackupRequestVS.withTransaction {
			representative = solicitud.representative
			userRequest = solicitud.messageSMIME?.userVS
			representativeName = "${representative?.name} ${representative?.firstName}"
			userRequestName = "${userRequest?.name} ${userRequest?.firstName}"
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
	
	public void sendRepresentativeVotingHistory (BackupRequestVS solicitud,
		String dateFromStr, String dateToStr, Locale locale) {
		log.debug "sendRepresentativeVotingHistory - email:${solicitud.email} - solicitud:${solicitud.id}"
		String subject;
		UserVS userRequest
		String userRequestName
		UserVS representative
		String representativeName
		String urlSolicitud
		String urlDescarga
		BackupRequestVS.withTransaction {
			representative = solicitud.representative
			userRequest = solicitud.messageSMIME?.userVS
			representativeName = "${representative?.name} ${representative?.firstName}"
			userRequestName = "${userRequest?.name} ${userRequest?.firstName}"
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
	
	def sendPDF(String email, String subject, String bodyView, Map model, byte[] pdfBytes) {
		log.debug "- sendPDF - to userVS:${email}"
		
		String renderedSrc = groovyPageRenderer.render (view:bodyView, model:model)
		
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
