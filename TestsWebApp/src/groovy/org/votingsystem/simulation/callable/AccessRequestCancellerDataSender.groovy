package org.votingsystem.simulation.callable

import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.smime.ValidationResult
import org.votingsystem.simulation.ContextService
import org.votingsystem.simulation.model.AccessRequestBackup
import org.votingsystem.simulation.util.HttpHelper
import org.votingsystem.simulation.util.SimulationUtils
import org.votingsystem.util.ApplicationContextHolder

import javax.mail.internet.MimeMessage
import java.security.KeyStore
import java.util.concurrent.Callable
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestCancellerDataSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(AccessRequestCancellerDataSender.class);

    private AccessRequestBackup request;
    private ContextService contextService = null;
    
    public AccessRequestCancellerDataSender(AccessRequestBackup request) throws Exception {
        this.request = request;
        contextService = ApplicationContextHolder.getSimulationContext();
    }

    @Override  public ResponseVS call() throws Exception {
        KeyStore mockDnie = contextService.generateTestDNIe(request.getUserVS().getNif());
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, contextService.END_ENTITY_ALIAS,
                contextService.PASSWORD.toCharArray(), contextService.VOTE_SIGN_MECHANISM);
        String subject = contextService.getMessage("cancelAccessRequestMsgSubject") + request.getEventVSId();
        String voteCancellerFileName = contextService.CANCEL_VOTE_FILE + request.getEventVSId() +"_" +
                request.getUserVS().getNif() + ".p7m"
        byte[] messageBytes = null;
        synchronized(this) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MimeMessage mimeMessage = signedMailGenerator.genMimeMessage(request.getUserVS().getNif(),
                    contextService.getAccessControl().getNameNormalized(), request.toJSON().toString(), subject, null);
            mimeMessage.writeTo(baos);
            messageBytes = baos.toByteArray();
            baos.close();
            contextService.copyFileToSimDir(messageBytes, SimulationUtils.getUserDirPath(request.getUserVS().getNif()),
                    voteCancellerFileName)
        }
        ResponseVS responseVS = HttpHelper.getInstance().sendByteArray(messageBytes, ContentTypeVS.SIGNED,
                contextService.getAccessControl().getVoteCancellerServiceURL())
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            SMIMEMessageWrapper mimeMessage = new SMIMEMessageWrapper(
                    new ByteArrayInputStream(responseVS.getMessageBytes()));
            ValidationResult validationResult = mimeMessage.verify(contextService.getSessionPKIXParameters());
            if (!validationResult.isValidSignature()) {
                logger.error(" #### Error validating receipt");
            }
            responseVS.setData(request);
        }
        return responseVS;
    }
    
}