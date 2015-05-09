package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.model.BackupRequestVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

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
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class MailBean {

    private static Logger log = Logger.getLogger(MailBean.class.getSimpleName());

    @Resource(name = "java:jboss/mail/gmail")
    private Session session;
    @Inject private ConfigVS config;

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
        //MessagesVS messages = MessagesVS.getCurrentInstance();
        /*(view:"/mail/backupRequestMessage.jsp", model:[fromUser:fromUser, requestURL:requestURL,
        subject:subject, downloadURL:downloadURL])*/
        log.log(Level.FINE, "sendBackupMsg - email:" + request.getEmail() + " - request:"+ request.getId());
        UserVS toUser = request.getMessageSMIME().getUserVS();
        String downloadURL = config.getRestURL() + "/backupVS/request/id/" + request.getId() + "/download";
        String requestURL = config.getRestURL() + "/backupVS/request/id/" + request.getId();
        //String subject = messages.get("downloadBackupMailSubject");
        //send(toUser.getEmail(), subject, content);
    }


}