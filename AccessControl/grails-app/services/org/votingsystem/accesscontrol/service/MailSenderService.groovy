package org.votingsystem.accesscontrol.service

import grails.gsp.PageRenderer
import static org.springframework.context.i18n.LocaleContextHolder.*
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
	
	
	public void sendTextMail(String toUser, String msg, String subject) {
		log.debug "sendTextMail - toUser:${toUser} - subject:${subject}"
		runAsync {
			SimpleMailMessage message = new SimpleMailMessage();
			//message.setFrom(fromUser);
			message.setTo(toUser);
			message.setSubject(subject);
			message.setText(msg);
			mailSender.send(message);
		}
	}

    public void sendHTMLMail(String toUser, String htmlMessage, String subject) {
        log.debug "sendHTMLMail - toUser:${toUser} - subject:${subject}"
        runAsync {
            MimeMessage message = mailSender.createMimeMessage();
            try {
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "ISO-8859-1");
                //helper.setFrom("from");
                helper.setTo(toUser);
                helper.setSubject(subject);
                helper.setText(htmlMessage, true)
                mailSender.send(message);
            } catch(MessagingException ex) {
                log.error(ex.getMessage(), ex)
            }
        }
    }
	
	public void sendBackupMsg (BackupRequestVS request) {
		log.debug "sendInstruccionesDescarga - email:${request.email} - request:${request.id}"
		def subject;
		String eventVSSubject
		String fromUser
		String requestURL
		String downloadURL
		BackupRequestVS.withTransaction {
			eventVSSubject = request.PDFDocumentVS?.eventVS?.subject
			if(eventVSSubject) subject = (eventVSSubject.length() > 90)?eventVSSubject.substring(0, 90) + "...":eventVSSubject
			fromUser = "${request.PDFDocumentVS?.userVS?.name} ${request.PDFDocumentVS?.userVS?.firstName}"
			requestURL = "${grailsApplication.config.grails.serverURL}/backupVS/${request.id}"
			downloadURL = "${grailsApplication.config.grails.serverURL}/backupVS/download/${request.id}"
		}
		log.debug "fromUser:${fromUser} - requestURL:${requestURL} - downloadURL:${downloadURL}"
		String emailSubject = messageSource.getMessage('downloadBackupMailSubject', null, locale)
		
		String htmlMessage = groovyPageRenderer.render (view:"/mail/backupRequestMessage", model:[fromUser:fromUser,
				   requestURL:requestURL, subject:subject, downloadURL:downloadURL])
        sendHTMLMail(request.email, htmlMessage, emailSubject);
   }
	
	public void sendRepresentativeAccreditations (BackupRequestVS request, String dateStr) {
		log.debug "sendRepresentativeAccreditations - email:${request.email} - request:${request.id}"
		String subject;
		UserVS userRequest
		String userRequestName
		UserVS representative
		String representativeName
		String requestURL
		String downloadURL
		BackupRequestVS.withTransaction {
			representative = request.representative
			userRequest = request.messageSMIME?.userVS
			representativeName = "${representative?.name} ${representative?.firstName}"
			userRequestName = "${userRequest?.name} ${userRequest?.firstName}"
			requestURL = "${grailsApplication.config.grails.serverURL}/backupVS/${request.id}"
			downloadURL = "${grailsApplication.config.grails.serverURL}/backupVS/download/${request.id}"
		}
		String emailSubject = messageSource.getMessage('representativeAccreditationsMailSubject',
			[representativeName].toArray(), locale)
		
		String htmlMessage = groovyPageRenderer.render (
				view:"/mail/RepresentativeAccreditationRequestDownloadInstructions", 
				model:[fromUser:userRequestName, dateStr:dateStr, pageTitle:emailSubject,
						requestURL:requestURL, representative:representativeName, downloadURL:downloadURL])

        sendHTMLMail(request.email, htmlMessage, emailSubject);

	}
	
	public void sendRepresentativeVotingHistory (BackupRequestVS request, String dateFromStr, String dateToStr) {
		log.debug "sendRepresentativeVotingHistory - email:${request.email} - request:${request.id}"
		String subject;
		UserVS userRequest
		String userRequestName
		UserVS representative
		String representativeName
		String requestURL
		String downloadURL
		BackupRequestVS.withTransaction {
			representative = request.representative
			userRequest = request.messageSMIME?.userVS
			representativeName = "${representative?.name} ${representative?.firstName}"
			userRequestName = "${userRequest?.name} ${userRequest?.firstName}"
			requestURL = "${grailsApplication.config.grails.serverURL}/backupVS/${request.id}"
			urlDescaimport grails.gsp.PageRendererrga = "${grailsApplication.config.grails.serverURL}/backupVS/download/${request.id}"
		}
		String emailSubject = messageSource.getMessage('representativeVotingHistoryMailSubject',
			[representativeName].toArray(), locale)
		
		String htmlMessage = groovyPageRenderer.render (
			view:"/mail/RepresentativeVotingHistoryDownloadInstructions",
			model:[fromUser:userRequestName, dateFromStr:dateFromStr, dateToStr:dateToStr,
				pageTitle:emailSubject,	requestURL:requestURL, representative:representativeName, downloadURL:downloadURL])

        sendHTMLMail(request.email, htmlMessage, emailSubject);

	}
	
	def sendPDF(String email, String subject, String bodyView, Map model, byte[] pdfBytes) {
		log.debug "- sendPDF - to userVS:${email}"
        String htmlMessage = groovyPageRenderer.render (view:bodyView, model:model)
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            //helper.setFrom("from");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlMessage);
            ByteArrayResource byteArrayResource = new ByteArrayResource(pdfBytes)
            helper.addAttachment("attached.pdf", byteArrayResource);
            mailSender.send(message);
        } catch(MessagingException ex) {
            log.error(ex.getMessage(), ex)
        }
    }
	
}
