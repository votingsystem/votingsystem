package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.model.BackupRequestVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class MailBean {

    private static Logger log = Logger.getLogger(MailBean.class.getSimpleName());

    @Resource(name = "java:jboss/mail/gmail")
    private Session session;
    @Inject private ConfigVS config;
    @Inject MessagesBean messages;

    @Asynchronous
    public void send(String toUser, String subject, String msg) {
        try {
            Message message = new MimeMessage(session);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toUser));
            message.setSubject(subject);
            message.setText(msg);
            Transport.send(message);
        } catch (MessagingException e) {
            log.log(Level.WARNING, "Cannot send mail", e);
        }
    }

    @Asynchronous
    public void sendWithAttachment(String toUser, String subject, String msg, File attachment) {
        try {
            Message message = new MimeMessage(session);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toUser));
            message.setSubject(subject);
            message.setText(msg);


            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(msg);

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(attachment.getAbsolutePath());
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(attachment.getName());
            multipart.addBodyPart(messageBodyPart);

            Transport.send(message);
        } catch (MessagingException e) {
            log.log(Level.WARNING, "Cannot send mail", e);
        }
    }

    @Asynchronous
    public void sendBackupMsg (BackupRequestVS request, String content) {
        /*(view:"/mail/backupRequestMessage.jsp", model:[fromUser:fromUser, requestURL:requestURL,
        subject:subject, downloadURL:downloadURL])*/
        log.log(Level.FINE, "sendBackupMsg - email:" + request.getEmail() + " - request:"+ request.getId());
        UserVS toUser = request.getMessageSMIME().getUserVS();
        String downloadURL = config.getRestURL() + "/backupVS/request/id/" + request.getId() + "/download";
        String requestURL = config.getRestURL() + "/backupVS/request/id/" + request.getId();
        String subject = messages.get("downloadBackupMailSubject");
        send(toUser.getEmail(), subject, content);
    }

    @Asynchronous
    public void sendRepresentativeAccreditations (BackupRequestVS request, String content, Date electedDate) {
        /*view:"/mail/RepresentativeAccreditationRequestDownloadInstructions.jsp",
                model:[fromUser:userRequestName, dateStr:dateStr, pageTitle:emailSubject,
                requestURL:requestURL, representative:representativeName, downloadURL:downloadURL]
         */
        log.log(Level.FINE, "sendRepresentativeAccreditations - email:" + request.getEmail() + " - request:"+ request.getId());
        UserVS toUser = request.getMessageSMIME().getUserVS();
        String downloadURL = config.getRestURL() + "/backupVS/request/id/" + request.getId() + "/download";
        String requestURL = config.getRestURL() + "/backupVS/request/id/" + request.getId();
        String subject = messages.get("representativeAccreditationsMailSubject", request.getRepresentative().getName());
        send(toUser.getEmail(), subject, content);
    }

    @Asynchronous
    public void sendRepresentativeVotingHistory (BackupRequestVS request, String content, Date dateFrom, Date dateTo) {
        /*view:"/mail/RepresentativeVotingHistoryDownloadInstructions",
                model:[fromUser:userRequestName, dateFromStr:dateFromStr, dateToStr:dateToStr,
                pageTitle:emailSubject,	requestURL:requestURL, representative:representativeName, downloadURL:downloadURL]
         */
        log.log(Level.FINE, "sendRepresentativeVotingHistory - email:" + request.getEmail() + " - request:"+ request.getId());
        UserVS toUser = request.getMessageSMIME().getUserVS();
        String downloadURL = config.getRestURL() + "/backupVS/request/id/" + request.getId() + "/download";
        String requestURL = config.getRestURL() + "/backupVS/request/id/" + request.getId();
        String subject = messages.get("representativeAccreditationsMailSubject", request.getRepresentative().getName());
        send(toUser.getEmail(), subject, content);
    }

}