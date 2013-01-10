package org.sistemavotacion.smime;

import com.sun.mail.util.BASE64DecoderStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESigned;

/**
 *
 * @author jgzornoza
 */
public class SMIMESignedValidator {
    
    public static void validate (MimeMessage msg) throws IOException, 
            MessagingException, CMSException, SMIMEException {
        MimeMultipart mimeMultipart;
        SMIMESigned smimeSigned;
        if (msg.getContent() instanceof BASE64DecoderStream) {
            smimeSigned = new SMIMESigned(msg); 
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ((CMSProcessable)smimeSigned.getSignedContent()).write(baos);
   

        } else {
            smimeSigned = new SMIMESigned((MimeMultipart)msg.getContent());
            MimeBodyPart content = smimeSigned.getContent();
        } 

            
         /*   
            System.out.println("Content:");
            Object  cont = content.getContent();
            if (cont instanceof String) {
                System.out.println("Is String:" + (String)cont);
            }

            else if (cont instanceof Multipart){
                Multipart   mp = (Multipart)cont;
                int count = mp.getCount();
                for (int i = 0; i < count; i++) {
                    BodyPart    m = mp.getBodyPart(i);
                    Object      part = m.getContent();

                    System.out.println("Part " + i);
                    System.out.println("---------------------------");

                    if (part instanceof String) {
                        System.out.println((String)part);
                    }
                    else  {
                        System.out.println("can't print...");
                    }
                }
            }

            System.out.println("Status:");

            verify(smimeSigned);
        
        } catch (Exception ex) {
            Logger.getLogger(CreateSignedMail.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }
    
}
