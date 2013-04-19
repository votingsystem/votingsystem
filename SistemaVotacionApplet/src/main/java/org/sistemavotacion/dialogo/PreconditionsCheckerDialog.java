package org.sistemavotacion.dialogo;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import org.sistemavotacion.AppletFirma;
import static org.sistemavotacion.AppletFirma.CERT_CHAIN_URL_SUFIX;
import org.sistemavotacion.Contexto;
import static org.sistemavotacion.Contexto.getString;
import org.sistemavotacion.FirmaDialog;
import org.sistemavotacion.VotacionDialog;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.pdf.PdfFormHelper;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.VotacionHelper;
import org.sistemavotacion.worker.ObtenerArchivoWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class PreconditionsCheckerDialog 
        extends JDialog implements VotingSystemWorkerListener {
    
    private static Logger logger = 
            LoggerFactory.getLogger(PreconditionsCheckerDialog.class);

    private static Map<String, X509Certificate> certsMap = 
            new HashMap<String, X509Certificate>();
    private Operacion operacion;
    private AtomicBoolean checking = new AtomicBoolean(true);
    private boolean preconditionsOK = false;
    private String message = "ERROR";
    private Frame frame = null;
    private static final int CHECK_ACCES_CONTROL_CERT = 0;
    private Boolean accessControlCertChecked = null;
    private static final int CHECK_CONTROL_CENTER_CERT = 1;
    private static final int CHECK_SERVER_CERT = 2;
    private Boolean controlCenterCertChecked = null;
    private Boolean signCertChecked = null;
    

    public PreconditionsCheckerDialog(Frame parent, 
            boolean modal, Operacion operacion) {
        super(parent, modal);
        frame = parent;
        this.operacion = operacion;
        initComponents();
        setLocationRelativeTo(null);  
        acceptButton.setVisible(false);
        setTitle(getString("preconditionsCheckerDialogCaption"));
        
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug(" - window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                logger.debug(" - window closing event received");
                dispose();
            }
        });
        Runnable runnable = new Runnable() {
            public void run() { 
                try {
                    checkConditions();
                } catch (Exception e) {
                    e.printStackTrace();
                } 
            }
        };
        new Thread(runnable).start();
        pack();
    }
    
    public void checkConditions() {
        logger.debug("checkConditions");
        try {
            switch(operacion.getTipo()) {
                case ENVIO_VOTO_SMIME:
                    checkCert(operacion.getEvento().getCentroControl().
                            getServerURL(), CHECK_CONTROL_CENTER_CERT);
                    checkCert(operacion.getEvento().getControlAcceso().
                            getServerURL(),CHECK_ACCES_CONTROL_CERT);
                    break;
                case PUBLICACION_MANIFIESTO_PDF:
                case FIRMA_MANIFIESTO_PDF:
                case PUBLICACION_RECLAMACION_SMIME:
                case FIRMA_RECLAMACION_SMIME:
                case PUBLICACION_VOTACION_SMIME:
                case CANCELAR_EVENTO:
                case ASOCIAR_CENTRO_CONTROL_SMIME:
                case ANULAR_SOLICITUD_ACCESO:
                case ANULAR_VOTO: 
                case SOLICITUD_COPIA_SEGURIDAD:
                    checkCert(operacion.getUrlServer(), CHECK_SERVER_CERT);
                    break;
                default:
                    checking.set(true);
                    preconditionsOK = true;
            }
        } catch(final Exception ex) {
            logger.error(ex.getMessage(), ex);
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    MensajeDialog errorDialog = new MensajeDialog(frame, true);
                    errorDialog.setMessage(ex.getMessage(), getString("errorLbl"));
                }
            });
        }
        while(checking.get()){}
        logger.debug("preconditionsOK: " + preconditionsOK);
        if(preconditionsOK == true) {
            processOperation();
            dispose();
        } else {
            acceptButton.setVisible(true);
            progressLabel.setText(message);
            progressBar.setVisible(false);
            AppletFirma.INSTANCIA.responderCliente(
                    Operacion.SC_ERROR_EJECUCION, 
                    Contexto.getString("votingPreconditionsErrorMsg", message));
        }
        pack();
    }
    
    private void processOperation() {
        logger.debug("processOperation");
        switch(operacion.getTipo()) {
            case GUARDAR_RECIBO_VOTO:
                Operacion resultado = VotacionHelper.guardarRecibo(
                        operacion.getArgs()[0], frame);
                AppletFirma.INSTANCIA.responderCliente(
                        resultado.getCodigoEstado(), resultado.getMensaje());
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
            case PUBLICACION_MANIFIESTO_PDF:
            case FIRMA_MANIFIESTO_PDF:
            case PUBLICACION_RECLAMACION_SMIME:
            case FIRMA_RECLAMACION_SMIME:
            case PUBLICACION_VOTACION_SMIME:
            case CANCELAR_EVENTO:
            case ASOCIAR_CENTRO_CONTROL_SMIME:
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
                byte[] bytesPDF = null; 
                try {
                    File file = PdfFormHelper.obtenerSolicitudCopia(
                            operacion.getEvento().getEventoId().toString(),
                            operacion.getEvento().getAsunto(), 
                            operacion.getEmailSolicitante());
                    bytesPDF = FileUtils.getBytesFromFile(file);
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
                firmaDialog.inicializarSinDescargarPDF(bytesPDF);
                break;
            default:
                logger.debug("Operación desconocida: " + operacion.getTipo().toString());
        }
    }

    public static X509Certificate getCert(String serverURL) {
        logger.debug(".getCert(...)", " - getCert - serverURL: " + serverURL);
        return certsMap.get(serverURL);
    }
     
     
    public void checkCert(String serverURL, Integer operationId) throws Exception {
        logger.debug(".checkCert(...)", " - checkCert - serverURL: " + serverURL 
                + " - operationId: " + operationId);
        if(serverURL == null) throw new Exception("Missing cert url");
        X509Certificate cert = certsMap.get(serverURL);
        if(cert == null) {
            if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
            String serverCertURL = serverURL + CERT_CHAIN_URL_SUFIX;
            logger.debug(" - getNetworkCert - serverCertURL: " + serverCertURL);
            new ObtenerArchivoWorker(operationId, serverCertURL, this).execute();
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

        acceptButton = new javax.swing.JButton();
        messagePanel = new javax.swing.JPanel();
        progressLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        progressLabel1 = new javax.swing.JLabel();

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

        progressLabel1.setFont(new java.awt.Font("DejaVu Sans", 1, 14)); // NOI18N
        progressLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        progressLabel1.setText(bundle.getString("PreconditionsCheckerDialog.progressLabel1.text")); // NOI18N

        javax.swing.GroupLayout messagePanelLayout = new javax.swing.GroupLayout(messagePanel);
        messagePanel.setLayout(messagePanelLayout);
        messagePanelLayout.setHorizontalGroup(
            messagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(messagePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(messagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(messagePanelLayout.createSequentialGroup()
                        .addGroup(messagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(progressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 411, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(progressLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 411, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        messagePanelLayout.setVerticalGroup(
            messagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(messagePanelLayout.createSequentialGroup()
                .addComponent(progressLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressLabel1)
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
    }//GEN-LAST:event_acceptButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton acceptButton;
    private javax.swing.JPanel messagePanel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JLabel progressLabel1;
    // End of variables declaration//GEN-END:variables

    @Override
    public void process(List<String> messages) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void setVotingPreconditions() {
         logger.debug(" - setVotingPreconditions - accessControlCertChecked: " + accessControlCertChecked 
                 + " - controlCenterCertChecked: " + controlCenterCertChecked);
        if(accessControlCertChecked == true && controlCenterCertChecked == true) 
            preconditionsOK = true;
        checking.set(false);
    }
    
    private void setSignPreconditions() {
         logger.debug(" - setSignPreconditions - signCertChecked: " + signCertChecked);
        if(signCertChecked == true) preconditionsOK = true;
        checking.set(false);
    }
    
    @Override
    public void showResult(VotingSystemWorker votingSystemWorker) {
        logger.debug(" - showResult - ");
        ObtenerArchivoWorker fileWorker = null;
        try {
            switch(votingSystemWorker.getId()) {
                case CHECK_ACCES_CONTROL_CERT:
                    fileWorker = (ObtenerArchivoWorker)votingSystemWorker;
                    if(Respuesta.SC_OK == fileWorker.getStatusCode()) {
                        Collection<X509Certificate> certChain = CertUtil.
                        fromPEMToX509CertCollection(fileWorker.getBytesArchivo());
                        X509Certificate serverCert = certChain.iterator().next();
                        certsMap.put(operacion.getEvento().getControlAcceso().
                                getServerURL(), serverCert);
                        accessControlCertChecked = true;                        
                    } else {
                        accessControlCertChecked = false;
                        message = fileWorker.getMessage();
                    } 
                    if(controlCenterCertChecked != null) setVotingPreconditions();
                    break;
                case CHECK_CONTROL_CENTER_CERT:
                    fileWorker = (ObtenerArchivoWorker)votingSystemWorker;
                    if(Respuesta.SC_OK == votingSystemWorker.getStatusCode()) {
                        Collection<X509Certificate> certChain = CertUtil.
                        fromPEMToX509CertCollection(fileWorker.getBytesArchivo());
                        X509Certificate serverCert = certChain.iterator().next();
                        certsMap.put(operacion.getEvento().getCentroControl().
                                getServerURL(), serverCert);
                        controlCenterCertChecked = true;
                    } else {
                        controlCenterCertChecked = false;
                        message = fileWorker.getMessage();
                    } 
                    if(accessControlCertChecked != null) setVotingPreconditions();
                    break;
                case CHECK_SERVER_CERT:
                    fileWorker = (ObtenerArchivoWorker)votingSystemWorker;
                    if(Respuesta.SC_OK == votingSystemWorker.getStatusCode()) {
                        Collection<X509Certificate> certChain = CertUtil.
                        fromPEMToX509CertCollection(fileWorker.getBytesArchivo());
                        X509Certificate serverCert = certChain.iterator().next();
                        certsMap.put(operacion.getUrlServer(), serverCert);
                        signCertChecked = true;
                    } else {
                        signCertChecked = false;
                        message = fileWorker.getMessage();
                    } 
                    setSignPreconditions();
                    break;
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
