package org.votingsystem.simulation.callable

import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.smime.ValidationResult
import org.votingsystem.simulation.SignatureVSService
import org.votingsystem.simulation.model.AccessRequestBackup
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.StringUtils

import javax.mail.internet.MimeMessage
import java.security.KeyStore
import java.util.concurrent.Callable
/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class AccessRequestCancellerDataSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(AccessRequestCancellerDataSender.class);

    private AccessRequestBackup request;
    
    public AccessRequestCancellerDataSender(AccessRequestBackup request) throws Exception {
        this.request = request;
    }

    @Override  public ResponseVS call() throws Exception {
        SignatureVSService signatureVSService = (SignatureVSService)ApplicationContextHolder.getBean("signatureVSService")
        KeyStore mockDnie = signatureVSService.generateKeyStore(request.getUserVS().getNif())
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, ContextVS.END_ENTITY_ALIAS,
                ContextVS.PASSWORD.toCharArray(), ContextVS.VOTE_SIGN_MECHANISM);
        String subject = ApplicationContextHolder.getInstance().getMessage("cancelAccessRequestMsgSubject") + request.getEventVSId();
        String voteCancellerFileName = ContextVS.CANCEL_DATA_FILE_NAME + request.getEventVSId() +"_" +
                request.getUserVS().getNif() + ".p7m"
        byte[] messageBytes = null;
        synchronized(this) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MimeMessage mimeMessage = signedMailGenerator.genMimeMessage(request.getUserVS().getNif(),
                    ContextVS.getInstance().getAccessControl().getNameNormalized(), request.toJSON().toString(), subject, null);
            mimeMessage.writeTo(baos);
            messageBytes = baos.toByteArray();
            baos.close();
            ContextVS.getInstance().copyFile(messageBytes, StringUtils.getUserDirPath(request.getUserVS().getNif()),
                    voteCancellerFileName)
        }
        ResponseVS responseVS = HttpHelper.getInstance().sendData(messageBytes, ContentTypeVS.SIGNED,
                ContextVS.getInstance().getAccessControl().getVoteCancellerServiceURL())
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            SMIMEMessageWrapper mimeMessage = new SMIMEMessageWrapper(
                    new ByteArrayInputStream(responseVS.getMessageBytes()));
            ValidationResult validationResult = mimeMessage.verify(ContextVS.getInstance().getSessionPKIXParameters());
            if (!validationResult.isValidSignature()) {
                logger.error(" #### Error validating receipt");
            }
            responseVS.setData(request);
        }
        return responseVS;
    }
    
}