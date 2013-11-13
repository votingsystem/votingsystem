package org.votingsystem.applet.votingtool.dialog;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.SwingWorker;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import static org.votingsystem.applet.votingtool.Applet.SERVER_INFO_URL_SUFIX;
import org.votingsystem.model.ContextVS;
import org.votingsystem.applet.votingtool.FirmaDialog;
import org.votingsystem.applet.votingtool.RepresentativeDataDialog;
import org.votingsystem.applet.votingtool.SaveReceiptDialog;
import org.votingsystem.applet.votingtool.VotacionDialog;
import org.votingsystem.applet.model.AppletOperation;
import static org.votingsystem.applet.model.AppletOperation.Type.ACCESS_REQUEST_CANCELLATION;
import static org.votingsystem.applet.model.AppletOperation.Type.VOTE_CANCELLATION;
import static org.votingsystem.applet.model.AppletOperation.Type.CONTROL_CENTER_ASSOCIATION;
import static org.votingsystem.applet.model.AppletOperation.Type.EVENT_CANCELLATION;
import static org.votingsystem.applet.model.AppletOperation.Type.SEND_SMIME_VOTE;
import static org.votingsystem.applet.model.AppletOperation.Type.MANIFEST_SIGN;
import static org.votingsystem.applet.model.AppletOperation.Type.SMIME_CLAIM_SIGNATURE;
import static org.votingsystem.applet.model.AppletOperation.Type.NEW_REPRESENTATIVE;
import static org.votingsystem.applet.model.AppletOperation.Type.MANIFEST_PUBLISHING;
import static org.votingsystem.applet.model.AppletOperation.Type.CLAIM_PUBLISHING;
import static org.votingsystem.applet.model.AppletOperation.Type.VOTING_PUBLISHING;
import static org.votingsystem.applet.model.AppletOperation.Type.REPRESENTATIVE_ACCREDITATIONS_REQUEST;
import static org.votingsystem.applet.model.AppletOperation.Type.REPRESENTATIVE_REVOKE;
import static org.votingsystem.applet.model.AppletOperation.Type.REPRESENTATIVE_SELECTION;
import static org.votingsystem.applet.model.AppletOperation.Type.REPRESENTATIVE_VOTING_HISTORY_REQUEST;
import static org.votingsystem.applet.model.AppletOperation.Type.BACKUP_REQUEST;
import org.votingsystem.applet.pdf.PdfFormHelper;
import org.votingsystem.applet.callable.InfoGetter;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.apache.log4j.Logger;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class PreconditionsCheckerDialog extends JDialog {
    
    private static Logger logger = Logger.getLogger(
            PreconditionsCheckerDialog.class);
    
    private static final Map<String, ActorVS> actorMap = 
            new HashMap<String, ActorVS>();
    private AppletOperation operation;
    private Frame frame = null;

    public PreconditionsCheckerDialog(AppletOperation operation, 
            Frame parent, boolean modal) {
        super(parent, modal);
        frame = parent;
        this.operation = operation;
        initComponents();
        setLocationRelativeTo(null);  
        setTitle(ContextVS.INSTANCE.getString("preconditionsCheckerDialogCaption"));
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug("PreconditionsCheckerDialog window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                logger.debug(" - window closing event received");
                sendResponse(ResponseVS.SC_CANCELLED,
                        ContextVS.INSTANCE.getString("operacionCancelada"));
            }
        });
        pack();
    }
    
    public void showDialog() {
        logger.debug("showDialog");
        PreconditionsCheckerWorker worker = new PreconditionsCheckerWorker();
        worker.execute();
        setVisible(true);
    }
    
    private void sendResponse(int status, String message) {
        progressLabel.setText(ContextVS.INSTANCE.getString("errorLbl"));
        progressBar.setVisible(false);
        waitLabel.setText(message);
        operation.setStatusCode(status);
        operation.setMessage(message);
        ContextVS.INSTANCE.sendMessageToHost(operation);
        dispose();
    }
    
    class PreconditionsCheckerWorker extends SwingWorker<ResponseVS, Object> {
       
        @Override public ResponseVS doInBackground() {
            logger.debug("PreconditionsCheckerWorker.doInBackground - operation:" + 
                    operation.getType());
            ResponseVS responseVS = null;
            try {
            switch(operation.getType()) {
                case SEND_SMIME_VOTE:
                    String accessControlURL = operation.getEvento().
                            getControlAcceso().getServerURL().trim();
                    responseVS = checkActorConIP(accessControlURL);
                    if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                        return responseVS;
                    } else {
                        ContextVS.INSTANCE.setAccessControl(
                                (ActorVS)responseVS.getData());
                    }
                    String controlCenterURL = operation.getEvento().
                            getCentroControl().getServerURL().trim();
                    responseVS = checkActorConIP(controlCenterURL);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        ContextVS.INSTANCE.setControlCenter(
                            (ActorVS)responseVS.getData());
                    }
                    break;
                case REPRESENTATIVE_REVOKE:
                case REPRESENTATIVE_ACCREDITATIONS_REQUEST:
                case REPRESENTATIVE_VOTING_HISTORY_REQUEST:
                case NEW_REPRESENTATIVE:
                case REPRESENTATIVE_SELECTION:
                case MANIFEST_PUBLISHING:
                case MANIFEST_SIGN:
                case CLAIM_PUBLISHING:
                case SMIME_CLAIM_SIGNATURE:
                case VOTING_PUBLISHING:
                case EVENT_CANCELLATION:
                case CONTROL_CENTER_ASSOCIATION:
                case ACCESS_REQUEST_CANCELLATION:
                case VOTE_CANCELLATION: 
                case BACKUP_REQUEST:
                    String serverURL = operation.getUrlServer().trim();
                    responseVS = checkActorConIP(serverURL);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        ContextVS.INSTANCE.setAccessControl(
                                (ActorVS)responseVS.getData());
                    }
                    break;
                default: 
                    logger.error(" ################# UNKNOWN OPERATION -> " +  
                            operation.getType());
                    responseVS = new ResponseVS(ResponseVS.SC_ERROR, 
                            ContextVS.INSTANCE.getString("unknownOperationErrorMsg") +  
                            operation.getType());
                    break;
            }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
            return responseVS;
        }

        @Override protected void done() {
            try{
                ResponseVS responseVS = get();
                logger.debug("PreconditionsCheckerWorker.done - operation:" + 
                        operation.getType() + " - status: " + responseVS.getStatusCode());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    switch(operation.getType()) {
                        case SAVE_VOTE_RECEIPT:
                            SaveReceiptDialog saveReceiptDialog = 
                                new SaveReceiptDialog(frame, true);
                            dispose();
                            saveReceiptDialog.show(operation.getArgs()[0]);
                            break;
                        case NEW_REPRESENTATIVE:
                            RepresentativeDataDialog representativeDialog = 
                                new RepresentativeDataDialog(frame, true);
                            dispose();
                            representativeDialog.show(operation);
                            break;
                        case SEND_SMIME_VOTE:
                            VotacionDialog votacionDialog = new VotacionDialog(
                                    frame, true);
                            dispose();
                            votacionDialog.show(operation);
                            break;
                        case REPRESENTATIVE_REVOKE:
                        case REPRESENTATIVE_ACCREDITATIONS_REQUEST:
                        case REPRESENTATIVE_VOTING_HISTORY_REQUEST:
                        case REPRESENTATIVE_SELECTION:
                        case MANIFEST_PUBLISHING:
                        case MANIFEST_SIGN:
                        case CLAIM_PUBLISHING:
                        case SMIME_CLAIM_SIGNATURE:
                        case VOTING_PUBLISHING:
                        case EVENT_CANCELLATION:
                        case CONTROL_CENTER_ASSOCIATION:
                        case ACCESS_REQUEST_CANCELLATION:
                        case VOTE_CANCELLATION:
                        case BACKUP_REQUEST:
                            FirmaDialog firmaDialog = new FirmaDialog(frame, true);
                            dispose();
                            firmaDialog.show(operation);
                            break;
                        default:
                            logger.debug("############ UNKNOWN OPERATION -> " + 
                            operation.getType().toString());
                            sendResponse(ResponseVS.SC_ERROR, ContextVS.INSTANCE.
                                    getString("unknownOperationErrorMsg") +  
                                    operation.getType());
                    }            
                } else {
                    logger.debug("responseVS.getMessage(): " + responseVS.getMessage());
                    sendResponse(responseVS.getStatusCode(), responseVS.getMessage());
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                sendResponse(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }
    }

    
    private ResponseVS<ActorVS> checkActorConIP(String serverURL) throws Exception {
        logger.debug(" - checkActorConIP: " + serverURL);
        ActorVS actorConIp = actorMap.get(serverURL.trim());
        if(actorConIp == null) { 
            String serverInfoURL = serverURL;
            if (!serverInfoURL.endsWith("/")) serverInfoURL = serverInfoURL + "/";
            serverInfoURL = serverInfoURL + SERVER_INFO_URL_SUFIX;
            InfoGetter infoGetter = new InfoGetter(null, serverInfoURL, null);
            ResponseVS responseVS = infoGetter.call();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONObject jsonObject = (JSONObject)JSONSerializer.toJSON(responseVS.getMessage());
                ActorVS actorConIP = ActorVS.populate(jsonObject);
                responseVS.setData(actorConIP);
                logger.error("checkActorConIP - adding " + serverURL.trim() + 
                        " to actor map");
                actorMap.put(serverURL.trim(), actorConIP);
            }
            return responseVS;
        } else {
            ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK);
            responseVS.setData(actorConIp);
            return responseVS;
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        messagePanel = new javax.swing.JPanel();
        progressLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        waitLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        progressLabel.setFont(new java.awt.Font("DejaVu Sans", 1, 14)); // NOI18N
        progressLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/votingsystem/applet/votingtool/Bundle"); // NOI18N
        progressLabel.setText(bundle.getString("VotacionDialog.progressLabel.text")); // NOI18N

        progressBar.setIndeterminate(true);

        waitLabel.setFont(new java.awt.Font("DejaVu Sans", 1, 14)); // NOI18N
        waitLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        java.util.ResourceBundle bundle1 = java.util.ResourceBundle.getBundle("org/votingsystem/applet/votingtool/Bundle"); // NOI18N
        waitLabel.setText(bundle1.getString("PreconditionsCheckerDialog.waitLabel.text")); // NOI18N

        javax.swing.GroupLayout messagePanelLayout = new javax.swing.GroupLayout(messagePanel);
        messagePanel.setLayout(messagePanelLayout);
        messagePanelLayout.setHorizontalGroup(
            messagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(messagePanelLayout.createSequentialGroup()
                .addGroup(messagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(messagePanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(messagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE)
                            .addComponent(waitLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(messagePanelLayout.createSequentialGroup()
                        .addGap(44, 44, 44)
                        .addComponent(progressLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 436, Short.MAX_VALUE)))
                .addContainerGap())
        );
        messagePanelLayout.setVerticalGroup(
            messagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(messagePanelLayout.createSequentialGroup()
                .addComponent(progressLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(waitLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(messagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(messagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel messagePanel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JLabel waitLabel;
    // End of variables declaration//GEN-END:variables

}
