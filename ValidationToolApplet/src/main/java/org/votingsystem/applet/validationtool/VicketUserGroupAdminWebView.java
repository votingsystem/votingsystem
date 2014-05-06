package org.votingsystem.applet.validationtool;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.dialog.MessageDialog;
import org.votingsystem.applet.validationtool.dialog.PasswordDialog;
import org.votingsystem.applet.validationtool.util.BrowserVS;
import org.votingsystem.applet.validationtool.util.WebbAppListener;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.dnie.DNIeContentSigner;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;

import javax.swing.*;


public class VicketUserGroupAdminWebView implements WebbAppListener {

    private static Logger logger = Logger.getLogger(VicketUserGroupAdminWebView.class);

    private Stage stage;
    private BrowserVS browserVS;

    public VicketUserGroupAdminWebView(){
        Platform.setImplicitExit(false);
    }

    private void initComponents(){
        //Note: Key is that Scene needs to be created and run on "FX user thread" NOT on the AWT-EventQueue Thread
        //https://gist.github.com/anjackson/1640654

        PlatformImpl.startup(new Runnable() {
            @Override
            public void run() {
                stage = new Stage();
                stage.setTitle(ContextVS.getMessage("groupAdminButtonLbl"));
                stage.setResizable(true);
                stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                    @Override
                    public void handle(WindowEvent event) {
                        event.consume();
                        stage.hide();
                        logger.debug("stage.setOnCloseRequest");
                    }
                });
                browserVS = new BrowserVS();
                browserVS.setWebbAppListener(VicketUserGroupAdminWebView.this);
                Scene scene = new Scene(browserVS, 900, 700, Color.web("#666970"));
                stage.setScene(scene);
                stage.show();
                browserVS.loadURL("http://vickets/Vickets/groupVS/admin");
            }
        });
    }

    public void launch() {
        initComponents();
    }

    @Override
    public void setMessageToSignatureClient(String messageToSignatureClient) {
        logger.debug("messageToSignatureClient: " + messageToSignatureClient);
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(messageToSignatureClient);
        final OperationVS operation = OperationVS.populate(jsonObject);
        PlatformImpl.startup(new Runnable() {@Override public void run() {browserVS.showProgressDialog(true);}});
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    PasswordDialog passwordDialog = new PasswordDialog (new JFrame(), true);
                    passwordDialog.setVisible(true);
                    String password = passwordDialog.getPassword();
                    if (password != null) processNewGroupOperation(password, operation);
                } catch(Exception ex) {
                    MessageDialog messageDialog = new MessageDialog(new JFrame(), true);
                    messageDialog.showMessage(ex.getMessage(), ContextVS.getMessage("errorLbl"));
                } finally {
                    PlatformImpl.startup(new Runnable() {@Override public void run() {browserVS.showProgressDialog(false);}});
                }
            }
        });
    }

    private void processNewGroupOperation(String password, OperationVS operation) throws Exception {
        try {
            JSONObject documentToSignJSON = (JSONObject)JSONSerializer.toJSON(operation.getDocumentToSignMap());
            SMIMEMessageWrapper smimeMessage = DNIeContentSigner.genMimeMessage(null,
                    operation.getNormalizedReceiverName(), documentToSignJSON.toString(),
                    password.toCharArray(), operation.getSignedMessageSubject(), null);
            SMIMESignedSender senderWorker = new SMIMESignedSender(smimeMessage, operation.getServiceURL(),
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                    ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED, null, ContextVS.getInstance().getAccessControl().
                    getX509Certificate());
            ResponseVS responseVS =  senderWorker.call();
        } catch(Exception ex) {
            PlatformImpl.startup(new Runnable() {
                @Override
                public void run() {
                    browserVS.showProgressDialog(false);
                }
            });
            throw ex;
        }
    }

}