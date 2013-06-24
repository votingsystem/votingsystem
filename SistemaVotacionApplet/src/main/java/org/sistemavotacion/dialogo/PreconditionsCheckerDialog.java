package org.sistemavotacion.dialogo;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import javax.swing.JDialog;
import org.sistemavotacion.AppletFirma;
import static org.sistemavotacion.AppletFirma.SERVER_INFO_URL_SUFIX;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.FirmaDialog;
import org.sistemavotacion.RepresentativeDataDialog;
import org.sistemavotacion.SaveReceiptDialog;
import org.sistemavotacion.VotacionDialog;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Operacion;
import static org.sistemavotacion.modelo.Operacion.Tipo.ANULAR_SOLICITUD_ACCESO;
import static org.sistemavotacion.modelo.Operacion.Tipo.ANULAR_VOTO;
import static org.sistemavotacion.modelo.Operacion.Tipo.ASOCIAR_CENTRO_CONTROL;
import static org.sistemavotacion.modelo.Operacion.Tipo.CANCELAR_EVENTO;
import static org.sistemavotacion.modelo.Operacion.Tipo.ENVIO_VOTO_SMIME;
import static org.sistemavotacion.modelo.Operacion.Tipo.FIRMA_MANIFIESTO_PDF;
import static org.sistemavotacion.modelo.Operacion.Tipo.FIRMA_RECLAMACION_SMIME;
import static org.sistemavotacion.modelo.Operacion.Tipo.NEW_REPRESENTATIVE;
import static org.sistemavotacion.modelo.Operacion.Tipo.PUBLICACION_MANIFIESTO_PDF;
import static org.sistemavotacion.modelo.Operacion.Tipo.PUBLICACION_RECLAMACION_SMIME;
import static org.sistemavotacion.modelo.Operacion.Tipo.PUBLICACION_VOTACION_SMIME;
import static org.sistemavotacion.modelo.Operacion.Tipo.REPRESENTATIVE_ACCREDITATIONS_REQUEST;
import static org.sistemavotacion.modelo.Operacion.Tipo.REPRESENTATIVE_REVOKE;
import static org.sistemavotacion.modelo.Operacion.Tipo.REPRESENTATIVE_SELECTION;
import static org.sistemavotacion.modelo.Operacion.Tipo.REPRESENTATIVE_VOTING_HISTORY_REQUEST;
import static org.sistemavotacion.modelo.Operacion.Tipo.SOLICITUD_COPIA_SEGURIDAD;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.pdf.PdfFormHelper;
import org.sistemavotacion.callable.InfoGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class PreconditionsCheckerDialog extends JDialog {
    
    private static Logger logger = LoggerFactory.getLogger(
            PreconditionsCheckerDialog.class);
    
    private static final Map<String, ActorConIP> actorMap = 
            new HashMap<String, ActorConIP>();
    private Operacion operacion;
    
    private Frame frame = null;
    private final AppletFirma appletFirma;

    public PreconditionsCheckerDialog(Frame parent, 
            boolean modal, Operacion operacion, final AppletFirma appletFirma) {
        super(parent, modal);
        frame = parent;
        this.appletFirma = appletFirma;
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
                appletFirma.cancelarOperacion();
            }
        });
        
        Contexto.INSTANCE.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    checkConditions();
                } catch (final Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            MensajeDialog errorDialog = new MensajeDialog(frame, true);
                            errorDialog.setMessage(ex.getMessage(), 
                                    Contexto.INSTANCE.getString("errorLbl"));
                        }
                    });
                }
            }
        });
        pack();
    }
    
    public void checkConditions() throws Exception {
        logger.debug("checkConditions");
        Respuesta respuesta = null;
        switch(operacion.getTipo()) {
            case ENVIO_VOTO_SMIME:
                respuesta = checkActorConIP(operacion.getEvento().
                        getControlAcceso().getServerURL());
                if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
                    logger.error("ERROR checking ACCESS CONTROL");
                    cancelOperation(respuesta.getMensaje());
                    return;
                } else {
                    ActorConIP accessControl = ActorConIP.parse(respuesta.getMensaje());
                    Contexto.INSTANCE.setAccessControl(accessControl);
                }
                respuesta = checkActorConIP(operacion.getEvento().
                        getCentroControl().getServerURL());
                if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
                    logger.error("ERROR checking CONTROL CENTER");
                    cancelOperation(respuesta.getMensaje());
                    return;
                } else {
                    ActorConIP controlCenter = ActorConIP.parse(respuesta.getMensaje());
                    Contexto.INSTANCE.setControlCenter(controlCenter);
                }
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
                respuesta = checkActorConIP(operacion.getUrlServer());
                if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
                    logger.error("ERROR checking ACCESS CONTROL - msg:" + 
                            respuesta.getMensaje());
                    cancelOperation(respuesta.getMensaje());
                    return;
                } else {
                    ActorConIP accessControl = ActorConIP.parse(respuesta.getMensaje());
                    Contexto.INSTANCE.setAccessControl(accessControl);
                }
            default: 
                logger.error(" ################# UNKNOWN OPERATION -> " +  
                        operacion.getTipo());
                break;
        }
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                processOperation();
            }
        });
        pack();
    }
    
    private void cancelOperation(String message) {
        acceptButton.setVisible(true);
        progressLabel.setText(Contexto.INSTANCE.getString("errorLbl"));
        progressBar.setVisible(false);
        waitLabel.setText(message);
        appletFirma.responderCliente(
                Operacion.SC_ERROR, 
                Contexto.INSTANCE.getString("votingPreconditionsErrorMsg", 
                Contexto.INSTANCE.getString("errorLbl")));
        pack();
    }
    
    private void processOperation() {
        logger.debug("processOperation: " + operacion.getTipo());
        switch(operacion.getTipo()) {
            case GUARDAR_RECIBO_VOTO:
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        SaveReceiptDialog saveReceiptDialog = 
                                new SaveReceiptDialog(frame, true, appletFirma);
                        dispose();
                        saveReceiptDialog.show(operacion.getArgs()[0]);
                    }
                });   
                break;
            case NEW_REPRESENTATIVE:
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        RepresentativeDataDialog representativeDialog = 
                                new RepresentativeDataDialog(frame, true, appletFirma);
                        dispose();
                        representativeDialog.show(operacion);
                    }
                });  
                break;
            case ENVIO_VOTO_SMIME:
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        VotacionDialog votacionDialog = new VotacionDialog(
                                    frame, true, appletFirma);
                        dispose();
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
                        FirmaDialog firmaDialog = new FirmaDialog(frame, true, appletFirma);
                        dispose();
                        firmaDialog.setVisible(true);
                    }
                });
                break;
            case SOLICITUD_COPIA_SEGURIDAD:
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            FirmaDialog firmaDialog = new FirmaDialog(frame, true, appletFirma);
                            byte[] bytesPDF = PdfFormHelper.getBackupRequest(
                                    operacion.getEvento().getEventoId().toString(),
                                    operacion.getEvento().getAsunto(), 
                                    operacion.getEmailSolicitante());
                            dispose();
                            firmaDialog.inicializarSinDescargarPDF(bytesPDF);
                        } catch(Exception ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    }
                });
                

                break;
            default:
                logger.debug("################# UNKNOWN OPERATION -> " + 
                        operacion.getTipo().toString());
        }
    }
    
    private Respuesta checkActorConIP(String serverURL) throws Exception {
        logger.debug(" - checkActorConIP: " + serverURL);
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        ActorConIP actorConIp = actorMap.get(serverURL);
        if(actorConIp == null) { 
            String serverInfoURL = serverURL + SERVER_INFO_URL_SUFIX;
            InfoGetter infoGetter = new InfoGetter(null, serverInfoURL, null);
            Future<Respuesta> future = Contexto.INSTANCE.submit(infoGetter);
            return future.get();
        } else return new Respuesta(Respuesta.SC_OK);
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
        appletFirma.cancelarOperacion();
    }//GEN-LAST:event_acceptButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton acceptButton;
    private javax.swing.JPanel messagePanel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JLabel waitLabel;
    // End of variables declaration//GEN-END:variables

}
