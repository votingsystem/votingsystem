package org.sistemavotacion.dialogo;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.SwingWorker;
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

    public PreconditionsCheckerDialog(Frame parent, boolean modal) {
        super(parent, modal);
        frame = parent;
        this.operacion = Contexto.INSTANCE.getPendingOperation();
        initComponents();
        setLocationRelativeTo(null);  
        setTitle(Contexto.INSTANCE.getString("preconditionsCheckerDialogCaption"));
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug("PreconditionsCheckerDialog window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                logger.debug(" - window closing event received");
                sendResponse(Operacion.SC_CANCELADO,
                        Contexto.INSTANCE.getString("operacionCancelada"));
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
        progressLabel.setText(Contexto.INSTANCE.getString("errorLbl"));
        progressBar.setVisible(false);
        waitLabel.setText(message);
        operacion.setCodigoEstado(status);
        operacion.setMensaje(message);
        Contexto.INSTANCE.sendMessageToHost(operacion);
        dispose();
    }
    
    class PreconditionsCheckerWorker extends SwingWorker<Respuesta, Object> {
       
        @Override public Respuesta doInBackground() {
            logger.debug("PreconditionsCheckerWorker.doInBackground - operation:" + 
                    operacion.getTipo());
            Respuesta respuesta = null;
            try {
            switch(operacion.getTipo()) {
                case ENVIO_VOTO_SMIME:
                    String accessControlURL = operacion.getEvento().
                            getControlAcceso().getServerURL().trim();
                    respuesta = checkActorConIP(accessControlURL);
                    if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
                        return respuesta;
                    } else {
                        Contexto.INSTANCE.setAccessControl(
                                (ActorConIP)respuesta.getData());
                    }
                    String controlCenterURL = operacion.getEvento().
                            getCentroControl().getServerURL().trim();
                    respuesta = checkActorConIP(controlCenterURL);
                    if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                        Contexto.INSTANCE.setControlCenter(
                            (ActorConIP)respuesta.getData());
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
                    String serverURL = operacion.getUrlServer().trim();
                    respuesta = checkActorConIP(serverURL);
                    if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                        Contexto.INSTANCE.setAccessControl(
                                (ActorConIP)respuesta.getData());
                    }
                    break;
                default: 
                    logger.error(" ################# UNKNOWN OPERATION -> " +  
                            operacion.getTipo());
                    respuesta = new Respuesta(Respuesta.SC_ERROR, 
                            Contexto.INSTANCE.getString("unknownOperationErrorMsg") +  
                            operacion.getTipo());
                    break;
            }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
            }
            return respuesta;
        }

        @Override protected void done() {
            try{
                Respuesta respuesta = get();
                logger.debug("PreconditionsCheckerWorker.done - operation:" + 
                        operacion.getTipo() + " - status: " + respuesta.getCodigoEstado());
                if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    switch(operacion.getTipo()) {
                        case GUARDAR_RECIBO_VOTO:
                            SaveReceiptDialog saveReceiptDialog = 
                                new SaveReceiptDialog(frame, true);
                            dispose();
                            saveReceiptDialog.show(operacion.getArgs()[0]);
                            break;
                        case NEW_REPRESENTATIVE:
                            RepresentativeDataDialog representativeDialog = 
                                new RepresentativeDataDialog(frame, true);
                            dispose();
                            representativeDialog.show(operacion);
                            break;
                        case ENVIO_VOTO_SMIME:
                            VotacionDialog votacionDialog = new VotacionDialog(
                                    frame, true);
                            dispose();
                            votacionDialog.setVisible(true);
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
                            FirmaDialog firmaDialog = new FirmaDialog(frame, true);
                            dispose();
                            firmaDialog.mostrar();
                            break;
                        case SOLICITUD_COPIA_SEGURIDAD:
                            FirmaDialog firmaDialog1 = new FirmaDialog(frame, true);
                            byte[] bytesPDF = PdfFormHelper.getBackupRequest(
                                    operacion.getEvento().getEventoId().toString(),
                                    operacion.getEvento().getAsunto(), 
                                    operacion.getEmailSolicitante());
                            dispose();
                            firmaDialog1.inicializarSinDescargarPDF(bytesPDF);
                            break;
                        default:
                            logger.debug("############ UNKNOWN OPERATION -> " + 
                            operacion.getTipo().toString());
                            sendResponse(Respuesta.SC_ERROR, Contexto.INSTANCE.
                                    getString("unknownOperationErrorMsg") +  
                                    operacion.getTipo());
                    }            
                } else {
                    logger.debug("respuesta.getMensaje(): " + respuesta.getMensaje());
                    sendResponse(respuesta.getCodigoEstado(), respuesta.getMensaje());
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                sendResponse(Respuesta.SC_ERROR, ex.getMessage());
            }
        }
    }

    
    private Respuesta<ActorConIP> checkActorConIP(String serverURL) throws Exception {
        logger.debug(" - checkActorConIP: " + serverURL);
        ActorConIP actorConIp = actorMap.get(serverURL.trim());
        if(actorConIp == null) { 
            String serverInfoURL = serverURL;
            if (!serverInfoURL.endsWith("/")) serverInfoURL = serverInfoURL + "/";
            serverInfoURL = serverInfoURL + SERVER_INFO_URL_SUFIX;
            InfoGetter infoGetter = new InfoGetter(null, serverInfoURL, null);
            Respuesta respuesta = infoGetter.call();
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                ActorConIP actorConIP = ActorConIP.parse(respuesta.getMensaje());
                respuesta.setData(actorConIP);
                logger.error("checkActorConIP - adding " + serverURL.trim() + 
                        " to actor map");
                actorMap.put(serverURL.trim(), actorConIP);
            }
            return respuesta;
        } else {
            Respuesta respuesta = new Respuesta(Respuesta.SC_OK);
            respuesta.setData(actorConIp);
            return respuesta;
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
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/Bundle"); // NOI18N
        progressLabel.setText(bundle.getString("VotacionDialog.progressLabel.text")); // NOI18N

        progressBar.setIndeterminate(true);

        waitLabel.setFont(new java.awt.Font("DejaVu Sans", 1, 14)); // NOI18N
        waitLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        java.util.ResourceBundle bundle1 = java.util.ResourceBundle.getBundle("org/sistemavotacion/dialogo/Bundle"); // NOI18N
        waitLabel.setText(bundle1.getString("PreconditionsCheckerDialog.waitLabel.text")); // NOI18N

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
