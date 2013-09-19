package pruebas;

import java.util.*;
import java.io.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author jgzornoza
 */
public class EmailAttachmentTest {
    
    private static Logger logger =  LoggerFactory.getLogger(EmailAttachmentTest.class);
    
    public static void main (String[] args) throws Exception {
        readAttachment();
    }
    
    //http://www.rgagnon.com/javadetails/java-0458.html
    //http://www.roseindia.net/javamail/ReadAttachment.shtml
    public static void readAttachment() throws FileNotFoundException, MessagingException, IOException, Exception {
        Properties props = System.getProperties();
        //props.put("mail.host", "smtp.dummydomain.com");
        //props.put("mail.transport.protocol", "smtp");

        Session mailSession = Session.getDefaultInstance(props, null);
        
        File fileInput = File.createTempFile("emailAttachment", "");
        fileInput.deleteOnExit();
        FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("testFiles/emailPDF"), fileInput); 
        
        InputStream source = new FileInputStream(fileInput);
        MimeMessage message = new MimeMessage(mailSession, source);
        Multipart multipart = (Multipart) message.getContent();
        //logger.debug(multipart.getCount());

        for (int i = 0; i < multipart.getCount(); i++) {
            //logger.debug(i);
            //logger.debug(multipart.getContentType());
            BodyPart bodyPart = multipart.getBodyPart(i);
            InputStream stream = bodyPart.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            File pdfFile = new File("./pdfFile.pdf");
            FileUtils.copyStreamToFile(stream, pdfFile);
            
            /*while (br.ready()) {
                logger.debug(br.readLine());
            }*/
        }
        logger.debug("Subject : " + message.getSubject());
        logger.debug("From : " + message.getFrom()[0]);
        logger.debug("--------------");
        logger.debug("Body : " +  message.getContent());
    }
    
    public static void writeAttachment() {
        // create some properties and get the default Session
        Properties props = System.getProperties();
        //props.put("mail.smtp.host", host);
        //props.put("mail.transport.protocol", "smtp");

        Session session = Session.getInstance(props, null);

        try {
            // create a message
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress("EmailFrom"));
            InternetAddress[] address = {new InternetAddress("EmailTo")};
            msg.setRecipients(Message.RecipientType.TO, address);
            msg.setSubject("Email Subject");

            // create and fill the first message part
            MimeBodyPart mbp1 = new MimeBodyPart();
            mbp1.setText("Message text");

            // create the second message part
            MimeBodyPart mbp2 = new MimeBodyPart();

            // attach the file to the message
            FileDataSource fds = new FileDataSource(".testPdf");
            mbp2.setDataHandler(new DataHandler(fds));
            mbp2.setFileName(fds.getName());

            // create the Multipart and add its parts to it
            Multipart mp = new MimeMultipart();
            mp.addBodyPart(mbp1);
            mp.addBodyPart(mbp2);

            // add the Multipart to the message
            msg.setContent(mp);

            // set the Date: header
            msg.setSentDate(new Date());

            // send the message
            Transport.send(msg);

        } 
        catch (MessagingException ex) {
            ex.printStackTrace();
        }
    }

 
}
