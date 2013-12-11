package org.votingsystem.applet.votingtool.dialog;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import net.miginfocom.swing.MigLayout;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.applet.votingtool.panel.ProgressBarPanel;
import org.votingsystem.model.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class PreconditionsCheckerDialog extends JDialog {
    
    private static Logger logger = Logger.getLogger(PreconditionsCheckerDialog.class);
    
    private static final Map<String, ActorVS> actorMap = new HashMap<String, ActorVS>();
    private OperationVS operationVS;
    
    private ProgressBarPanel progressBarPanel;
    private Container container;
    
    public PreconditionsCheckerDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(null);  
        setTitle(ContextVS.getMessage("preconditionsCheckerDialogCaption"));
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug("PreconditionsCheckerDialog window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                logger.debug(" - window closing event received");
                sendResponse(ResponseVS.SC_CANCELLED, ContextVS.getMessage("operationCancelled"));
            }
        });
        pack();
    }
    
    private void initComponents() {
        logger.debug("initComponents");
        container = getContentPane();   
        container.setLayout(new MigLayout("fill"));
        progressBarPanel = new ProgressBarPanel();
        container.add(progressBarPanel, "width :400:, wrap"); 
    }
    
    public void checkOperation(OperationVS operationVS) {
        logger.debug("checkOperation");
        this.operationVS = operationVS;
        PreconditionsCheckerWorker worker = new PreconditionsCheckerWorker();
        worker.execute();
        setVisible(true);
    }
        
    //we know this is done in a background thread
    private ResponseVS<ActorVS> checkActorVS(String serverURL) throws Exception {
        logger.debug(" - checkActorVS: " + serverURL);
        ActorVS actorVS = actorMap.get(serverURL.trim());
        if(actorVS == null) { 
            String serverInfoURL = ActorVS.getServerInfoURL(serverURL);
            ResponseVS responseVS = HttpHelper.getInstance().getData(serverInfoURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONObject jsonObject = (JSONObject)JSONSerializer.toJSON(responseVS.getMessage());
                actorVS = ActorVS.populate(jsonObject);
                responseVS.setData(actorVS);
                logger.error("checkActorVS - adding " + serverURL.trim() + " to actor map");
                actorMap.put(serverURL.trim(), actorVS);
            }
            return responseVS;
        } else {
            ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK);
            responseVS.setData(actorVS);
            return responseVS;
        }
    }
    
    private void sendResponse(int status, String message) {
        progressBarPanel.setResultMessage(message);
        if(operationVS != null) {
            operationVS.setStatusCode(status);
            operationVS.setMessage(message);
            ContextVS.getInstance().sendMessageToHost(operationVS);
        } else logger.debug(" --- operationVS null ---");
        dispose();
    }
  
    
    class PreconditionsCheckerWorker extends SwingWorker<ResponseVS, Object> {
       
        @Override public ResponseVS doInBackground() {
            logger.debug("PreconditionsCheckerWorker.doInBackground - operation:" + 
                    operationVS.getType());
            ResponseVS responseVS = null;
            try {
            switch(operationVS.getType()) {
                case SEND_SMIME_VOTE:
                    String accessControlURL = operationVS.getEventVS().getAccessControlVS().getServerURL();
                    responseVS = checkActorVS(accessControlURL);
                    if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
                    else ContextVS.getInstance().setAccessControl((AccessControlVS)responseVS.getData());
                    String controlCenterURL = operationVS.getEventVS().getControlCenterVS().getServerURL();
                    responseVS = checkActorVS(controlCenterURL);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        ContextVS.getInstance().setControlCenter((ControlCenterVS)responseVS.getData());
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
                    String serverURL = operationVS.getUrlServer().trim();
                    responseVS = checkActorVS(serverURL);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        ContextVS.getInstance().setAccessControl((AccessControlVS)responseVS.getData());
                    }
                    break;
                default: 
                    logger.error(" ################# UNKNOWN OPERATION -> " +  
                            operationVS.getType());
                    responseVS = new ResponseVS(ResponseVS.SC_ERROR,
                            ContextVS.getInstance().getMessage("unknownOperationErrorMsg") +  operationVS.getType());
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
                logger.debug("PreconditionsCheckerWorker.done - operationVS:" + 
                        operationVS.getType() + " - status: " + responseVS.getStatusCode());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    switch(operationVS.getType()) {
                        case SAVE_VOTE_RECEIPT:
                            SaveReceiptDialog saveReceiptDialog = new SaveReceiptDialog(new JFrame(), true);
                            dispose();
                            saveReceiptDialog.show(operationVS.getArgs()[0]);
                            break;
                        case NEW_REPRESENTATIVE:
                            RepresentativeFormDialog representativeDialog = new RepresentativeFormDialog(new JFrame(), true);
                            dispose();
                            representativeDialog.show(operationVS);
                            break;
                        case SEND_SMIME_VOTE:
                            ElectionDialog votacionDialog = new ElectionDialog(new JFrame(), true);
                            dispose();
                            votacionDialog.show(operationVS);
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
                            SignatureDialog signatureDialog = new SignatureDialog(new JFrame(), true);
                            dispose();
                            signatureDialog.show(operationVS);
                            break;
                        default:
                            logger.debug("############ UNKNOWN OPERATION -> " + operationVS.getType().toString());
                            sendResponse(ResponseVS.SC_ERROR, ContextVS.getInstance().
                                    getMessage("unknownOperationErrorMsg") + operationVS.getType());
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
    
    public static void main(String[] args) {

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ContextVS.initSignatureApplet(null, "log4j.properties", "messages_", "es");
                    UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                    PreconditionsCheckerDialog dialog = new PreconditionsCheckerDialog(new JFrame(), true);
                    dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override public void windowClosing(java.awt.event.WindowEvent e) {
                            System.exit(0);
                        }
                    });
                    dialog.setVisible(true);
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });

    }   

}
