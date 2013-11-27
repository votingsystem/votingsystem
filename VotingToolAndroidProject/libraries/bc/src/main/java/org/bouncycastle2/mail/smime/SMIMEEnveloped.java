package org.bouncycastle2.mail.smime;

import org.bouncycastle2.cms.CMSEnvelopedData;
import org.bouncycastle2.cms.CMSException;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import java.io.IOException;
import java.io.InputStream;

/**
 * containing class for an S/MIME pkcs7-mime encrypted MimePart.
 */
public class SMIMEEnveloped
    extends CMSEnvelopedData
{
    MimePart                message;

    private static InputStream getInputStream(
        Part    bodyPart)
        throws MessagingException
    {
        try
        {
            return bodyPart.getInputStream();
        }
        catch (IOException e)
        {
            throw new MessagingException("can't extract input stream: " + e);
        }
    }

    public SMIMEEnveloped(
        MimeBodyPart    message) 
        throws MessagingException, CMSException
    {
        super(getInputStream(message));

        this.message = message;
    }

    public SMIMEEnveloped(
        MimeMessage    message) 
        throws MessagingException, CMSException
    {
        super(getInputStream(message));

        this.message = message;
    }

    public MimePart getEncryptedContent()
    {
        return message;
    }
}
