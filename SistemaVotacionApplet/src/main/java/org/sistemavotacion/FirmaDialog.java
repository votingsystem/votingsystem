package org.sistemavotacion;

import static org.sistemavotacion.Contexto.IS_TIME_STAMPED_SIGNATURE;
import static org.sistemavotacion.Contexto.NOMBRE_ARCHIVO_FIRMADO;
import static org.sistemavotacion.Contexto.TIMESTAMP_DNIe_HASH;
import static org.sistemavotacion.Contexto.getString;

import java.awt.Desktop;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.SwingWorker;

import org.sistemavotacion.dialogo.MensajeDialog;
import org.sistemavotacion.dialogo.PasswordDialog;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.pdf.PDFSignerDNIe;
import org.sistemavotacion.smime.DNIeSignedMailGenerator;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.worker.EnviarDocumentoFirmadoWorker;
import org.sistemavotacion.worker.ObtenerArchivoWorker;
import org.sistemavotacion.worker.PDFSignerDNIeWorker;
import org.sistemavotacion.worker.TimeStampWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.pdf.PdfReader;
import java.util.logging.Level;
import org.sistemavotacion.dialogo.PreconditionsCheckerDialog;
import org.sistemavotacion.seguridad.EncryptionHelper;
import org.sistemavotacion.worker.VotingSystemWorker;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class FirmaDialog extends JDialog implements VotingSystemWorkerListener {

    private static final long serialVersionUID = 1L;

    private static Logger logger = LoggerFactory.getLogger(FirmaDialog.class);
    
    private static final int PDF_SIGNER_DNIE_WORKER          = 0;
    private static final int ENVIAR_DOCUMENTO_FIRMADO_WORKER = 1;
    private static final int OBTENER_ARCHIVO_WORKER          = 2;
    private static final int TIME_STAMP_WORKER               = 3;

    
    private byte[] bytesDocumento;
    private volatile boolean mostrandoPantallaEnvio = false;
    private Frame parentFrame;
    private SwingWorker tareaEnEjecucion;
    private File documentoFirmado;
    private AppletFirma appletFirma;
    private Operacion operacion;
    private SMIMEMessageWrapper timeStampedDocument;
    private static FirmaDialog INSTANCIA;
    
    public FirmaDialog(Frame parent, boolean modal, final AppletFirma appletFirma) {
        super(parent, modal);
        this.parentFrame = parent;
        this.appletFirma = appletFirma;
        //parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);       
        initComponents();
        INSTANCIA = this;
        operacion = appletFirma.getOperacionEnCurso();
        if(operacion != null && operacion.getContenidoFirma() != null) {
            bytesDocumento = operacion.getContenidoFirma().toString().getBytes();
        }
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug("FirmaDialog window closed event received");
                appletFirma.cancelarOperacion();
            }

            public void windowClosing(WindowEvent e) {
                logger.debug("FirmaDialog window closing event received");
                appletFirma.cancelarOperacion();
                if(AppletFirma.ModoEjecucion.APLICACION == 
                        AppletFirma.modoEjecucion){
                    logger.debug(" ------ System.exit(0) ------ ");
                    System.exit(0);
                }
            }
        });
        Operacion.Tipo tipoOperacion = appletFirma.
                getOperacionEnCurso().getTipo();
        setTitle(tipoOperacion.getCaption());
        switch(tipoOperacion) {
            case FIRMA_MANIFIESTO_PDF:
            case PUBLICACION_MANIFIESTO_PDF:
                verDocumentoButton.setIcon(new ImageIcon(getClass().
                        getResource("/resources/images/pdf_16x16.png"))); 
                obtenerPDFFirma(appletFirma.
                        getOperacionEnCurso().getUrlDocumento());
                break;             
            case SOLICITUD_COPIA_SEGURIDAD:
                verDocumentoButton.setIcon(new ImageIcon(getClass().
                        getResource("/resources/images/pdf_16x16.png"))); 
                break;
            default:
                logger.debug("No se ha encontrado la operación");
                progressBarPanel.setVisible(false);
                 pack();
                setVisible(true);
                break;
        }
        progressBarPanel.setVisible(false);
        pack();
    }
    
    public void obtenerPDFFirma (String urlDocumento) {
        logger.debug("obtnerePDFFirma - urlDocumento: " + urlDocumento);
        progressLabel.setText("<html>" + getString("obteniendoDocumento") +"</html>");
        mostrarPantallaEnvio(true);
        new ObtenerArchivoWorker(OBTENER_ARCHIVO_WORKER, urlDocumento, this).execute();
        setVisible(true);
    }
    
    public void inicializarSinDescargarPDF (byte[] bytesPDF) {
        logger.debug("inicializarSinDescargarPDF");
        bytesDocumento = bytesPDF;
        setVisible(true);
    }
    
    public void mostrarPantallaEnvio (boolean visibility) {
        logger.debug("mostrarPantallaEnvio - " + visibility);
        mostrandoPantallaEnvio = visibility;
        progressBarPanel.setVisible(visibility);
        enviarButton.setVisible(!visibility);
        confirmacionPanel.setVisible(!visibility);
        if (mostrandoPantallaEnvio) cerrarButton.setText(getString("cancelar"));
        else cerrarButton.setText(getString("cerrar"));
        pack();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cerrarButton = new javax.swing.JButton();
        confirmacionPanel = new javax.swing.JPanel();
        mensajeLabel = new javax.swing.JLabel();
        verDocumentoButton = new javax.swing.JButton();
        progressBarPanel = new javax.swing.JPanel();
        progressLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        enviarButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        cerrarButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/cancel_16x16.png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/Bundle"); // NOI18N
        cerrarButton.setText(bundle.getString("FirmaDialog.cerrarButton.text")); // NOI18N
        cerrarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cerrarButtonActionPerformed(evt);
            }
        });

        confirmacionPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        mensajeLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        mensajeLabel.setText(bundle.getString("FirmaDialog.mensajeLabel.text")); // NOI18N
        mensajeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        verDocumentoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/fileopen16x16.png"))); // NOI18N
        verDocumentoButton.setText(bundle.getString("FirmaDialog.verDocumentoButton.text")); // NOI18N
        verDocumentoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verDocumentoButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout confirmacionPanelLayout = new javax.swing.GroupLayout(confirmacionPanel);
        confirmacionPanel.setLayout(confirmacionPanelLayout);
        confirmacionPanelLayout.setHorizontalGroup(
            confirmacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(confirmacionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(confirmacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mensajeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(confirmacionPanelLayout.createSequentialGroup()
                        .addComponent(verDocumentoButton)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        confirmacionPanelLayout.setVerticalGroup(
            confirmacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(confirmacionPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(mensajeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(verDocumentoButton)
                .addContainerGap())
        );

        progressLabel.setText(bundle.getString("FirmaDialog.progressLabel.text")); // NOI18N

        progressBar.setIndeterminate(true);

        javax.swing.GroupLayout progressBarPanelLayout = new javax.swing.GroupLayout(progressBarPanel);
        progressBarPanel.setLayout(progressBarPanelLayout);
        progressBarPanelLayout.setHorizontalGroup(
            progressBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(progressBarPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(progressBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        progressBarPanelLayout.setVerticalGroup(
            progressBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(progressBarPanelLayout.createSequentialGroup()
                .addComponent(progressLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        enviarButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/signature-ok_16x16.png"))); // NOI18N
        enviarButton.setText(bundle.getString("FirmaDialog.enviarButton.text")); // NOI18N
        enviarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enviarButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressBarPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(enviarButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cerrarButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(confirmacionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressBarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(confirmacionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(enviarButton)
                    .addComponent(cerrarButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cerrarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cerrarButtonActionPerformed
        logger.debug("cerrarButtonActionPerformed - mostrandoPantallaEnvio: " + mostrandoPantallaEnvio);
        if (mostrandoPantallaEnvio) {
            if (tareaEnEjecucion != null) tareaEnEjecucion.cancel(true);
            mostrarPantallaEnvio(false);
            return;
        }
        dispose();
    }//GEN-LAST:event_cerrarButtonActionPerformed

    private void enviarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enviarButtonActionPerformed
        String password = null;
        if (Contexto.getDNIePassword() == null) {
            PasswordDialog dialogoPassword = new PasswordDialog (parentFrame, true);
            dialogoPassword.setVisible(true);
            password = dialogoPassword.getPassword();
            if (password == null) return;
        }
        final String finalPassword = password;
        mostrarPantallaEnvio(true);
        progressLabel.setText("<html>" + getString("progressLabel")+ "</html>");
        final EnviarDocumentoFirmadoWorker lanzador = new EnviarDocumentoFirmadoWorker(
                ENVIAR_DOCUMENTO_FIRMADO_WORKER, operacion.getUrlEnvioDocumento(), this);
        tareaEnEjecucion = lanzador;
        Runnable runnable = new Runnable() {
            public void run() {  
                try {
                    switch(operacion.getTipo()) {
                        case ANULAR_VOTO:
                        case ANULAR_SOLICITUD_ACCESO:
                        case CAMBIO_ESTADO_CENTRO_CONTROL_SMIME:
                        case ASOCIAR_CENTRO_CONTROL_SMIME:
                        case FIRMA_RECLAMACION_SMIME:
                        case PUBLICACION_RECLAMACION_SMIME:
                        case CANCELAR_EVENTO:
                        case PUBLICACION_VOTACION_SMIME:
                        	documentoFirmado = new File(FileUtils.APPTEMPDIR + NOMBRE_ARCHIVO_FIRMADO);
                            documentoFirmado = DNIeSignedMailGenerator.genFile(null,
                                operacion.getNombreDestinatarioFirmaNormalizado(),
                                operacion.getContenidoFirma().toString(),
                                finalPassword.toCharArray(), operacion.getAsuntoMensajeFirmado(), 
                                documentoFirmado);
                            if(IS_TIME_STAMPED_SIGNATURE) {                                
                                setTimeStampDocument(documentoFirmado, TIMESTAMP_DNIe_HASH);
                                return;
                            }
                            break;
                        case SOLICITUD_COPIA_SEGURIDAD:
                        case FIRMA_MANIFIESTO_PDF:
                        case PUBLICACION_MANIFIESTO_PDF:
                            documentoFirmado = new File(FileUtils.APPTEMPDIR +
                                operacion.getTipo().getNombreArchivoEnDisco());
                            PdfReader readerManifiesto = new PdfReader(bytesDocumento);
                            if(IS_TIME_STAMPED_SIGNATURE) {
                                String reason = null;
                                String location = null;
                                new PDFSignerDNIeWorker(PDF_SIGNER_DNIE_WORKER, 
                                        operacion.getUrlTimeStampServer(),
                                        INSTANCIA, reason, location, finalPassword.toCharArray(), 
                                        readerManifiesto, documentoFirmado).execute();
                                return;
                            } else {
                                PDFSignerDNIe.sign(null, null, finalPassword.toCharArray(), 
                                    readerManifiesto, new FileOutputStream(documentoFirmado));
                            }
                            if(documentoFirmado == null) return;
                            break;
                        default:
                            logger.debug("No se ha encontrado la operación " + operacion.getTipo().toString());
                            break;
                    }
                    processDocument(documentoFirmado);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    mostrarPantallaEnvio(false);
                    String mensajeError = null;
                    if ("CKR_PIN_INCORRECT".equals(ex.getMessage())) {
                        Contexto.setDNIePassword(null);
                        mensajeError = getString("MENSAJE_ERROR_PASSWORD");
                    } else mensajeError = ex.getMessage();
                    MensajeDialog errorDialog = new MensajeDialog(parentFrame, true);
                    errorDialog.setMessage(mensajeError, getString("errorLbl"));
                    return;
                }    
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
        pack();       
    }//GEN-LAST:event_enviarButtonActionPerformed

    private void verDocumentoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verDocumentoButtonActionPerformed
        if (!Desktop.isDesktopSupported()) {
            logger.debug("No hay soporte de escritorio");
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            logger.debug("No se puede editar archivos");
        }
        try {
            File documento = new File(FileUtils.APPTEMPDIR + appletFirma.
                    getOperacionEnCurso().getTipo().getNombreArchivoEnDisco());
            documento.deleteOnExit();
            FileUtils.copyStreamToFile(new ByteArrayInputStream(bytesDocumento), documento);
            logger.info("documento.getAbsolutePath(): " + documento.getAbsolutePath());
            desktop.open(documento);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }//GEN-LAST:event_verDocumentoButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cerrarButton;
    private javax.swing.JPanel confirmacionPanel;
    private javax.swing.JButton enviarButton;
    private javax.swing.JLabel mensajeLabel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel progressBarPanel;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JButton verDocumentoButton;
    // End of variables declaration//GEN-END:variables

    
    private void setTimeStampDocument(File document, String timeStampRequestAlg) {
        if(document == null) return;
        try {
            timeStampedDocument = new SMIMEMessageWrapper(null, document);
            new TimeStampWorker(TIME_STAMP_WORKER, operacion.getUrlTimeStampServer(),
                    this, timeStampedDocument.getTimeStampRequest(timeStampRequestAlg)).execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            appletFirma.responderCliente(
                    Respuesta.SC_ERROR_EJECUCION, ex.getMessage());
        }
    }
    
    
    private void processDocument(File document) {
        if(document == null) return;
        final Operacion operacion = appletFirma.getOperacionEnCurso();
        try {
            document.deleteOnExit();
            EncryptionHelper.encryptSMIMEFile(document, 
                    PreconditionsCheckerDialog.getCert(operacion.getUrlServer()));
            final EnviarDocumentoFirmadoWorker lanzador = new EnviarDocumentoFirmadoWorker(
                ENVIAR_DOCUMENTO_FIRMADO_WORKER, operacion.getUrlEnvioDocumento(), this);
            lanzador.setDocumentoEnviado(document).execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            appletFirma.responderCliente(
                    Respuesta.SC_ERROR_EJECUCION, ex.getMessage());
            MensajeDialog errorDialog = new MensajeDialog(parentFrame, true);
            errorDialog.setMessage(ex.getMessage(), getString("errorLbl"));
        }
    }
    
    
    @Override  public void process(List<String> messages) {
        logger.debug(" - process: " + messages.iterator().next());
        progressLabel.setText(messages.iterator().next());
    }

    
    @Override public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - worker: " + worker.getClass() + 
                " - statusCode: " + worker.getStatusCode());
        switch(worker.getId()) {
            case PDF_SIGNER_DNIE_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    processDocument(((PDFSignerDNIeWorker)worker).getSignedAndTimeStampedPDF());
                } else {
                    mostrarPantallaEnvio(false);
                    MensajeDialog errorDialog = new MensajeDialog(parentFrame, true);
                    errorDialog.setMessage(worker.getMessage(), getString("errorLbl"));
                }
                break;
            case OBTENER_ARCHIVO_WORKER:
                mostrarPantallaEnvio(false);
                if (Respuesta.SC_OK == worker.getStatusCode()) {    
                    bytesDocumento =((ObtenerArchivoWorker)worker).getBytesArchivo();
                    pack();
                } else {
                    dispose();
                    appletFirma.responderCliente(worker.getStatusCode(), 
                            getString("errorDescragandoDocumento") + " - " + worker.getMessage());
                }
                break;
            case ENVIAR_DOCUMENTO_FIRMADO_WORKER:
                dispose();
                if (Respuesta.SC_OK == worker.getStatusCode()) {
                    appletFirma.responderCliente(worker.getStatusCode(), null);
                    /*ResultadoFirmaDialog resultadoFirmaDialog =
                            new ResultadoFirmaDialog(parentFrame, true);
                    respuesta.setArchivo(documentoFirmado);
                    respuesta.setOperacion(operacion);
                    resultadoFirmaDialog.mostrarMensaje(respuesta);*/
                } else {
                    mostrarPantallaEnvio(false);
                    appletFirma.responderCliente(
                            worker.getStatusCode(), worker.getMessage());
                }
                break;
            case TIME_STAMP_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        processDocument(timeStampedDocument.setTimeStampToken(
                                (TimeStampWorker)worker));
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        mostrarPantallaEnvio(false);
                        MensajeDialog errorDialog = new MensajeDialog(parentFrame, true);
                        errorDialog.setMessage(ex.getMessage(), getString("errorLbl"));
                    }
                } else {
                    mostrarPantallaEnvio(false);
                    MensajeDialog errorDialog = new MensajeDialog(parentFrame, true);
                    errorDialog.setMessage(worker.getMessage(), getString("errorLbl"));
                }
                break;
            default:
                logger.debug("*** UNKNOWN WORKER ID: '" + worker.getId() + "'");
        }

    }

}