package org.sistemavotacion.dialogo;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.AppletFirma;
import static org.sistemavotacion.AppletFirma.SERVER_INFO_URL_SUFIX;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.FirmaDialog;
import org.sistemavotacion.RepresentativeDataDialog;
import org.sistemavotacion.SaveReceiptDialog;
import org.sistemavotacion.VotacionDialog;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.pdf.PdfFormHelper;
import org.sistemavotacion.worker.InfoGetterWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class PreconditionsCheckerDialog 
        extends JDialog implements VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(
            PreconditionsCheckerDialog.class);
    
    private static final int CHECK_ACCES_CONTROL_CERT = 0;
    private static final int CHECK_CONTROL_CENTER_CERT = 1;

    private static final Map<String, ActorConIP> actorMap = 
            new HashMap<String, ActorConIP>();
    private Operacion operacion;
    
    private final CountDownLatch checkLatch = new CountDownLatch(1);
    private AtomicBoolean preconditionsOK = new AtomicBoolean(false);
    private Frame frame = null;
    
    private Boolean accessControlCertChecked = null;
    private Boolean controlCenterCertChecked = null;
    
    private String message = null;

    public PreconditionsCheckerDialog(Frame parent, 
            boolean modal, Operacion operacion) {
        super(parent, modal);
        frame = parent;
        this.operacion = operacion;
        initComponents();
        setLocationRelativeTo(null);  
        acceptButton.setVisible(false);
        setTitle(Contexto.INSTANCE.getString("preconditionsCheckerDialogCaption"));
        
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug("PreconditionsCheckerDialog window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                logger.debug(" - window closing event received");
                dispose();
                AppletFirma.INSTANCIA.cancelarOperacion();
            }
        });
        Runnable runnable = new Runnable() {
            public void run() { 
                try {
                    checkConditions();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                } 
            }
        };
        new Thread(runnable).start();
        pack();
    }
    
    public void checkConditions() throws InterruptedException {
        logger.debug("checkConditions");
        try {
            switch(operacion.getTipo()) {
                case ENVIO_VOTO_SMIME:
                    boolean accessControlChecked = checkCert(operacion.getEvento().
                            getCentroControl().getServerURL(), CHECK_CONTROL_CENTER_CERT);
                    boolean controlCenterChecked = checkCert(operacion.getEvento().
                            getControlAcceso().getServerURL(),CHECK_ACCES_CONTROL_CERT);
                    if(accessControlChecked && controlCenterChecked) {
                        preconditionsOK.set(true);
                        checkLatch.countDown();
                    }
                    logger.debug("controlCenterCertChecked: " + controlCenterCertChecked + 
                            " - accessControlCertChecked: " + accessControlCertChecked);
                    break;
                case REPRESENTATIVE_REVOKE:
                case REPRESENTATIVE_ACCREDITATIONS_REQUEST:
                case REPRESENTATIVE_VOTING_HISTORY_REQUEST:
                case NEW_REPRESENTATIVE:
                case REPRESENTATIVE_SELECTION:
                case PUBLICACION_MANIFIESTO_PDF:
                case FIRMA_MANIFIESTO_PDF:
                case PUBLICACION_RECLAMACION_SMIME:
                case FIRMA_RECLAMACION_SMIME:
                case PUBLICACION_VOTACION_SMIME:
                case CANCELAR_EVENTO:
                case ASOCIAR_CENTRO_CONTROL:
                case ANULAR_SOLICITUD_ACCESO:
                case ANULAR_VOTO: 
                case SOLICITUD_COPIA_SEGURIDAD:
                    accessControlCertChecked = checkCert(
                            operacion.getUrlServer(), CHECK_ACCES_CONTROL_CERT);
                    if(accessControlCertChecked) {
                        preconditionsOK.set(true);
                        checkLatch.countDown();
                    }
                    break;
                default:
                    logger.error(" ################# UNKNOWN OPERATION -> " +  
                            operacion.getTipo());
                    checkLatch.countDown();
                    preconditionsOK.set(true);
            }
        } catch(final Exception ex) {
            logger.error(ex.getMessage(), ex);
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    MensajeDialog errorDialog = new MensajeDialog(frame, true);
                    errorDialog.setMessage(ex.getMessage(), 
                            Contexto.INSTANCE.getString("errorLbl"));
                }
            });
        }
        checkLatch.await();
        logger.debug("preconditionsOK: " + preconditionsOK);
        if(preconditionsOK.get() == true) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    processOperation();
                }
            });
            dispose();
        } else {
            acceptButton.setVisible(true);
            progressLabel.setText(Contexto.INSTANCE.getString("errorLbl"));
            progressBar.setVisible(false);
            waitLabel.setText(message);
            AppletFirma.INSTANCIA.responderCliente(
                    Operacion.SC_ERROR, 
                    Contexto.INSTANCE.getString("votingPreconditionsErrorMsg", 
                    Contexto.INSTANCE.getString("errorLbl")));
        }
        pack();
    }
    
    private void processOperation() {
        logger.debug("processOperation: " + operacion.getTipo());
        switch(operacion.getTipo()) {
            case GUARDAR_RECIBO_VOTO:
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        SaveReceiptDialog saveReceiptDialog = 
                                new SaveReceiptDialog(frame, true);
                        saveReceiptDialog.show(operacion.getArgs()[0]);
                    }
                });   
                break;
            case NEW_REPRESENTATIVE:
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        RepresentativeDataDialog representativeDialog = 
                                new RepresentativeDataDialog(frame, true);
                        representativeDialog.show(operacion);
                    }
                });  
                break;
            case ENVIO_VOTO_SMIME:
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        VotacionDialog votacionDialog = new VotacionDialog(
                                    frame, true, AppletFirma.INSTANCIA);
                        votacionDialog.setVisible(true);
                    }
                });    
                break;
            case REPRESENTATIVE_REVOKE:
            case REPRESENTATIVE_ACCREDITATIONS_REQUEST:
            case REPRESENTATIVE_VOTING_HISTORY_REQUEST:
            case REPRESENTATIVE_SELECTION:
            case PUBLICACION_MANIFIESTO_PDF:
            case FIRMA_MANIFIESTO_PDF:
            case PUBLICACION_RECLAMACION_SMIME:
            case FIRMA_RECLAMACION_SMIME:
            case PUBLICACION_VOTACION_SMIME:
            case CANCELAR_EVENTO:
            case ASOCIAR_CENTRO_CONTROL:
            case ANULAR_SOLICITUD_ACCESO:
            case ANULAR_VOTO: 
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        FirmaDialog firmaDialog = new FirmaDialog(frame, true, AppletFirma.INSTANCIA);
                    }
                });
                break;
            case SOLICITUD_COPIA_SEGURIDAD:
                FirmaDialog firmaDialog = new FirmaDialog(frame, true, AppletFirma.INSTANCIA);
                try {
                    byte[] bytesPDF = PdfFormHelper.getBackupRequest(
                            operacion.getEvento().getEventoId().toString(),
                            operacion.getEvento().getAsunto(), 
                            operacion.getEmailSolicitante());
                    firmaDialog.inicializarSinDescargarPDF(bytesPDF);
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
                break;
            default:
                logger.debug("################# UNKNOWN OPERATION -> " + 
                        operacion.getTipo().toString());
        }
    }
     
    private boolean checkCert(String serverURL, Integer operationId) throws Exception {
        logger.debug(" - checkCert - serverURL: " + serverURL 
                + " - operationId: " + operationId);
        if(serverURL == null) throw new Exception("Missing cert url");
        ActorConIP actorConIp = actorMap.get(serverURL);
        if(actorConIp == null) {
            if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
            String serverInfoURL = serverURL + SERVER_INFO_URL_SUFIX;
            new InfoGetterWorker(operationId, serverInfoURL, null, this).execute();
            return false;
        } else return true;
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        acceptButton = new javax.swing.JButton();
        messagePanel = new javax.swing.JPanel();
        progressLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        waitLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        acceptButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/accept_16x16.png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/dialogo/Bundle"); // NOI18N
        acceptButton.setText(bundle.getString("PreconditionsCheckerDialog.acceptButton.text")); // NOI18N
        acceptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acceptButtonActionPerformed(evt);
            }
        });

        progressLabel.setFont(new java.awt.Font("DejaVu Sans", 1, 14)); // NOI18N
        progressLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        java.util.ResourceBundle bundle1 = java.util.ResourceBundle.getBundle("org/sistemavotacion/Bundle"); // NOI18N
        progressLabel.setText(bundle1.getString("VotacionDialog.progressLabel.text")); // NOI18N

        progressBar.setIndeterminate(true);

        waitLabel.setFont(new java.awt.Font("DejaVu Sans", 1, 14)); // NOI18N
        waitLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        waitLabel.setText(bundle.getString("PreconditionsCheckerDialog.waitLabel.text")); // NOI18N

        javax.swing.GroupLayout messagePanelLayout = new javax.swing.GroupLayout(messagePanel);
        messagePanel.setLayout(messagePanelLayout);
        messagePanelLayout.setHorizontalGroup(
            messagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(messagePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(messagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(progressLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE)
                    .addComponent(waitLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(acceptButton)
                .addContainerGap())
            .addComponent(messagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(messagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(acceptButton)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void acceptButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acceptButtonActionPerformed
        dispose();
        if(AppletFirma.ModoEjecucion.APLICACION == 
                AppletFirma.modoEjecucion){
            logger.debug(" ------ System.exit(0) ------ ");
            System.exit(0);
        }
        AppletFirma.INSTANCIA.cancelarOperacion();
    }//GEN-LAST:event_acceptButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton acceptButton;
    private javax.swing.JPanel messagePanel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JLabel waitLabel;
    // End of variables declaration//GEN-END:variables

    @Override public void processVotingSystemWorkerMsg(List<String> messages) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void setVotingPreconditions() {
         logger.debug(" - setVotingPreconditions - accessControlCertChecked: " 
                 + accessControlCertChecked 
                 + " - controlCenterCertChecked: " + controlCenterCertChecked);
        if(accessControlCertChecked == true && controlCenterCertChecked == true) 
            preconditionsOK.set(true);
        checkLatch.countDown();
    }
    
    @Override
    public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
                " - worker: " + worker.getClass().getSimpleName() + 
                " - workerId:" + worker.getId());
        InfoGetterWorker infoWorker = null;
        try {
            switch(worker.getId()) {
                case CHECK_ACCES_CONTROL_CERT:
                    infoWorker = (InfoGetterWorker)worker;
                    if(Respuesta.SC_OK == worker.getStatusCode()) {
                        JSONObject actorConIPJSON = (JSONObject) JSONSerializer.
                                toJSON(infoWorker.getMessage());
                        ActorConIP actorConIP = ActorConIP.parse(actorConIPJSON);
                        actorMap.put(operacion.getUrlServer(), actorConIP);
                        Contexto.INSTANCE.setAccessControl(actorConIP);
                        accessControlCertChecked = true;                        
                    } else {
                        accessControlCertChecked = false;
                        message = Contexto.INSTANCE.getString("checkAccessControlErrorMsg");
                    } 
                    if(operacion.getTipo() == Operacion.Tipo.ENVIO_VOTO_SMIME) {
                        if(controlCenterCertChecked != null) setVotingPreconditions();
                        else return;
                    } else {
                        if(Respuesta.SC_OK == worker.getStatusCode()) {
                            preconditionsOK.set(true);
                        } else preconditionsOK.set(false);
                        checkLatch.countDown();
                    }
                    logger.debug("CHECK_ACCES_CONTROL_CERT - url: " + operacion.getUrlServer());
                    break;
                case CHECK_CONTROL_CENTER_CERT: 
                    infoWorker = (InfoGetterWorker)worker;
                    if(Respuesta.SC_OK == worker.getStatusCode()) {
                        JSONObject actorConIPJSON = (JSONObject) JSONSerializer.
                                toJSON(infoWorker.getMessage());
                        ActorConIP controlCenter = ActorConIP.parse(actorConIPJSON);
                        Contexto.INSTANCE.setControlCenter(controlCenter);
                        actorMap.put(operacion.getEvento().getCentroControl().
                                getServerURL(), controlCenter);
                        controlCenterCertChecked = true;
                    } else {
                        controlCenterCertChecked = false;
                        message = Contexto.INSTANCE.getString("checkControlCenterErrorMsg");
                    } 
                    if(accessControlCertChecked != null) setVotingPreconditions();
                    break;
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
