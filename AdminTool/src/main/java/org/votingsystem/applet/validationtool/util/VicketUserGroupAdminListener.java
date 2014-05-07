package org.votingsystem.applet.validationtool.util;

import com.sun.javafx.application.PlatformImpl;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.dialog.PasswordDialog;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;

import javax.swing.*;


public class VicketUserGroupAdminListener implements BrowserVSOperator {

    private static Logger logger = Logger.getLogger(VicketUserGroupAdminListener.class);

    private BrowserVS browserVS;

    public VicketUserGroupAdminListener(BrowserVS browserVS){
        this.browserVS = browserVS;
        browserVS.setBrowserVSOperator(this);
    }

    @Override
    public void processOperationVS(final OperationVS operation) {

        PlatformImpl.startup(new Runnable() {@Override public void run() {browserVS.showProgressDialog(
                ContextVS.getMessage("creatingVicketServerGroupMsg"), true);}});
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    PasswordDialog passwordDialog = new PasswordDialog (new JFrame(), true);
                    passwordDialog.setVisible(true);
                    String password = passwordDialog.getPassword();
                    if (password != null) processNewGroupOperation(password, operation);
                } catch(final Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    browserVS.showMessage(ContextVS.getMessage("errorLbl") + " - " + ex.getMessage());
                } finally {
                    browserVS.showProgressDialog(null, false);
                }
            }
        });
    }

    private void processNewGroupOperation(String password, OperationVS operation) throws Exception {
        JSONObject documentToSignJSON = (JSONObject)JSONSerializer.toJSON(operation.getDocumentToSignMap());
        SMIMEMessageWrapper smimeMessage = SMIMEContentSigner.genMimeMessage(null,
                operation.getNormalizedReceiverName(), documentToSignJSON.toString(),
                password.toCharArray(), operation.getSignedMessageSubject(), null);
        SMIMESignedSender senderWorker = new SMIMESignedSender(smimeMessage, operation.getServiceURL(),
                ContextVS.getInstance().getVicketServer().getTimeStampServiceURL(),
                ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED, null,
                ContextVS.getInstance().getVicketServer().getX509Certificate());
        ResponseVS responseVS =  senderWorker.call();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            browserVS.showMessage(responseVS.getMessage());
        } else {
            browserVS.showMessage(ContextVS.getMessage("errorLbl") + " - " + responseVS.getMessage());
        }
    }

}