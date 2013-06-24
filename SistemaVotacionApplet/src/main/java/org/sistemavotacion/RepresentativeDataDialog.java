package org.sistemavotacion;

import java.awt.Desktop;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.MessageDigest;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.dialogo.MensajeDialog;
import org.sistemavotacion.dialogo.PasswordDialog;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.smime.DNIeSignedMailGenerator;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.ImagePreviewPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sistemavotacion.callable.RepresentativeDataSender;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeDataDialog extends JDialog  {
    
    private static Logger logger = LoggerFactory.getLogger(SaveReceiptDialog.class);

    private static BlockingQueue<Future<Respuesta>> queue = 
        new LinkedBlockingQueue<Future<Respuesta>>(3);
    
    private Frame parentFrame = null;
    private Operacion operacion = null;
    private File selectedImage = null;
    private Future<Respuesta> tareaEnEjecucion;
    private AtomicBoolean mostrandoPantallaEnvio = new AtomicBoolean(false);
    private final AppletFirma appletFirma;
        
    public RepresentativeDataDialog(Frame parent, boolean modal, 
            final AppletFirma appletFirma) {
        super(parent, modal);
        setLocationRelativeTo(null);
        this.parentFrame = parent;
        this.appletFirma = appletFirma;
        initComponents();
        parent.setLocationRelativeTo(null);
        setTitle(Contexto.INSTANCE.getString("NEW_REPRESENTATIVE"));
        validationPanel.setVisible(false);
        progressBarPanel.setVisible(false);
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug(" - window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                logger.debug(" - window closing event received");
                dispose();
                appletFirma.cancelarOperacion();
            }
        });
        Contexto.INSTANCE.submit(new Runnable() {
            @Override public void run() {
                try {
                    readFutures();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        pack();
    }
    
    public void readFutures () {
        logger.debug(" - readFutures");
        AtomicBoolean done = new AtomicBoolean(false);
        while (!done.get()) {
            try {
                Future<Respuesta> future = queue.take();
                Respuesta respuesta = future.get();
                logger.debug(" - readFutures - response status: " + respuesta.getCodigoEstado());
                if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    appletFirma.responderCliente(
                            respuesta.getCodigoEstado(), respuesta.getMensaje());
                } else {
                    mostrarPantallaEnvio(false);
                    appletFirma.responderCliente(
                            respuesta.getCodigoEstado(), respuesta.getMensaje());
                }
                dispose();
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }
    
    public void mostrarPantallaEnvio (boolean visibility) {
        logger.debug("mostrarPantallaEnvio - " + visibility);
        mostrandoPantallaEnvio.set(visibility);
        progressBarPanel.setVisible(visibility);
        enviarButton.setVisible(!visibility);
        imagePanel.setVisible(!visibility);
        confirmacionPanel.setVisible(!visibility);
        if (mostrandoPantallaEnvio.get()) cerrarButton.setText(
                Contexto.INSTANCE.getString("cancelar"));
        else cerrarButton.setText(Contexto.INSTANCE.getString("cerrar"));
        pack();
    }
        
    private void setMessage (String mensaje) {
        if (mensaje == null) validationPanel.setVisible(false);
        else {
            messageLabel.setText("<html>" + mensaje + "</html>");
            validationPanel.setVisible(true);
        }
        pack();
    }
    
    public void show(Operacion operacion) {
        this.operacion = operacion;
        setVisible(true);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        confirmacionPanel = new javax.swing.JPanel();
        mensajeLabel = new javax.swing.JLabel();
        verDocumentoButton = new javax.swing.JButton();
        validationPanel = new javax.swing.JPanel();
        messageLabel = new javax.swing.JLabel();
        progressBarPanel = new javax.swing.JPanel();
        progressLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        imagePanel = new javax.swing.JPanel();
        selectImageButton = new javax.swing.JButton();
        selectedImageLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        enviarButton = new javax.swing.JButton();
        cerrarButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        confirmacionPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        mensajeLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/Bundle"); // NOI18N
        mensajeLabel.setText(bundle.getString("RepresentativeDataDialog.mensajeLabel.text")); // NOI18N
        mensajeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        verDocumentoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/fileopen16x16.png"))); // NOI18N
        verDocumentoButton.setText(bundle.getString("RepresentativeDataDialog.verDocumentoButton.text")); // NOI18N
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

        validationPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        messageLabel.setFont(new java.awt.Font("DejaVu Sans", 1, 13)); // NOI18N
        messageLabel.setForeground(new java.awt.Color(215, 43, 13));
        messageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        messageLabel.setText(bundle.getString("RepresentativeDataDialog.messageLabel.text")); // NOI18N

        javax.swing.GroupLayout validationPanelLayout = new javax.swing.GroupLayout(validationPanel);
        validationPanel.setLayout(validationPanelLayout);
        validationPanelLayout.setHorizontalGroup(
            validationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(validationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(messageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        validationPanelLayout.setVerticalGroup(
            validationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(validationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(messageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)
                .addContainerGap())
        );

        progressLabel.setText(bundle.getString("RepresentativeDataDialog.progressLabel.text")); // NOI18N

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

        selectImageButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/Group_16x16.png"))); // NOI18N
        selectImageButton.setText(bundle.getString("RepresentativeDataDialog.selectImageButton.text")); // NOI18N
        selectImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectImageButtonActionPerformed(evt);
            }
        });

        selectedImageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        selectedImageLabel.setText(bundle.getString("RepresentativeDataDialog.selectedImageLabel.text")); // NOI18N

        javax.swing.GroupLayout imagePanelLayout = new javax.swing.GroupLayout(imagePanel);
        imagePanel.setLayout(imagePanelLayout);
        imagePanelLayout.setHorizontalGroup(
            imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(imagePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(imagePanelLayout.createSequentialGroup()
                        .addComponent(selectImageButton)
                        .addGap(0, 147, Short.MAX_VALUE))
                    .addComponent(selectedImageLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        imagePanelLayout.setVerticalGroup(
            imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(imagePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(selectImageButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectedImageLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        enviarButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/signature-ok_16x16.png"))); // NOI18N
        enviarButton.setText(bundle.getString("RepresentativeDataDialog.enviarButton.text")); // NOI18N
        enviarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enviarButtonActionPerformed(evt);
            }
        });

        cerrarButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/cancel_16x16.png"))); // NOI18N
        cerrarButton.setText(bundle.getString("RepresentativeDataDialog.cerrarButton.text")); // NOI18N
        cerrarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cerrarButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(enviarButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cerrarButton)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(enviarButton)
                    .addComponent(cerrarButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(imagePanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(confirmacionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(progressBarPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(validationPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressBarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(validationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(confirmacionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void selectImageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectImageButtonActionPerformed
        logger.debug(" - selectImageButtonActionPerformed - ");   
        try {
            final JFileChooser chooser = new JFileChooser();
            ImagePreviewPanel preview = new ImagePreviewPanel();
            chooser.setAccessory(preview);
            chooser.addPropertyChangeListener(preview);
                int returnVal = chooser.showSaveDialog(parentFrame);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    if ((file != null) &&
                            file.getName().toLowerCase().endsWith(".jpg") ||
                            file.getName().toLowerCase().endsWith(".jpeg") ||
                            file.getName().toLowerCase().endsWith(".gif") ||
                            file.getName().toLowerCase().endsWith(".png")) {
                        selectedImage = new File(file.getAbsolutePath());
                        byte[] imageFileBytes = FileUtils.getBytesFromFile(selectedImage);
                        logger.debug(" - imageFileBytes.length: " + 
                                imageFileBytes.length);
                        if(imageFileBytes.length > Contexto.IMAGE_MAX_FILE_SIZE) {
                            logger.debug(" - MAX_FILE_SIZE exceeded ");
                            setMessage(Contexto.INSTANCE.getString("fileSizeExceeded", 
                                    Contexto.IMAGE_MAX_FILE_SIZE_KB));
                            selectedImage = null;
                            selectedImageLabel.setText(
                                Contexto.INSTANCE.getString("imageNotSelectedMsg"));
                        } else {
                            selectedImageLabel.setText(file.getAbsolutePath());
                            MessageDigest messageDigest = MessageDigest.getInstance(
                                    Contexto.VOTING_DATA_DIGEST);
                            byte[] resultDigest =  messageDigest.digest(imageFileBytes);
                            String base64ResultDigest = new String(Base64.encode(resultDigest));
                            operacion.getContenidoFirma().put(
                                 "base64ImageHash", base64ResultDigest);
                            //String base64RepresentativeEncodedImage = new String(
                            //        Base64.encode(imageFileBytes));
                            // operacion.getContenidoFirma().put(
                            //     "base64RepresentativeEncodedImage", base64RepresentativeEncodedImage);
                            setMessage(null);
                        }
                    } else {
                        selectedImage = null;
                        selectedImageLabel.setText(
                            Contexto.INSTANCE.getString("imageNotSelectedMsg"));
                    } 
                }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }//GEN-LAST:event_selectImageButtonActionPerformed

    private void enviarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enviarButtonActionPerformed
        if(selectedImage == null) {
            setMessage(Contexto.INSTANCE.getString("imageMissingMsg"));
            return;
        } else setMessage(null);
        String password = null;
        PasswordDialog dialogoPassword = new PasswordDialog (parentFrame, true);
        dialogoPassword.setVisible(true);
        password = dialogoPassword.getPassword();
        if (password == null) return;
        final String finalPassword = password;
        mostrarPantallaEnvio(true);
        progressLabel.setText("<html>" + Contexto.INSTANCE.getString(
                "progressLabel")+ "</html>");

        Contexto.INSTANCE.submit(new Runnable() {
            public void run() {
                try {
                   SMIMEMessageWrapper representativeRequestSMIME = 
                           DNIeSignedMailGenerator.genMimeMessage(null,
                           operacion.getNombreDestinatarioFirmaNormalizado(),
                           operacion.getContenidoFirma().toString(),
                           finalPassword.toCharArray(), operacion.getAsuntoMensajeFirmado(), null);
                   RepresentativeDataSender dataSender = new RepresentativeDataSender( 
                           representativeRequestSMIME, selectedImage, 
                           operacion.getUrlEnvioDocumento(), Contexto.INSTANCE.
                           getAccessControl().getCertificate());
                   Future<Respuesta> future = Contexto.INSTANCE.submit(dataSender);
                   queue.put(future);
                   tareaEnEjecucion = future;
               } catch (Exception ex) {
                   logger.error(ex.getMessage(), ex);
                   mostrarPantallaEnvio(false);
                   String mensajeError = null;
                   if ("CKR_PIN_INCORRECT".equals(ex.getMessage())) {
                       mensajeError = Contexto.INSTANCE.getString("MENSAJE_ERROR_PASSWORD");
                   } else mensajeError = ex.getMessage();
                   MensajeDialog errorDialog = new MensajeDialog(parentFrame, true);
                   errorDialog.setMessage(mensajeError, 
                           Contexto.INSTANCE.getString("errorLbl"));
               }
            }
        });
        pack();
    }//GEN-LAST:event_enviarButtonActionPerformed

    private void cerrarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cerrarButtonActionPerformed
        logger.debug("cerrarButtonActionPerformed - mostrandoPantallaEnvio: " 
                + mostrandoPantallaEnvio);
        if (mostrandoPantallaEnvio.get()) {
            if (tareaEnEjecucion != null) tareaEnEjecucion.cancel(true);
            mostrarPantallaEnvio(false);
            return;
        }
        dispose();
        appletFirma.cancelarOperacion();
    }//GEN-LAST:event_cerrarButtonActionPerformed

    private void verDocumentoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verDocumentoButtonActionPerformed
        if (!Desktop.isDesktopSupported()) {
            logger.debug("No hay soporte de escritorio");
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            logger.debug("No se puede editar archivos");
        }
        try {
            File documento = new File(Contexto.DEFAULTS.APPTEMPDIR + operacion.
                    getTipo().getNombreArchivoEnDisco());
            documento.deleteOnExit();
            FileUtils.copyStreamToFile(new ByteArrayInputStream(
                    operacion.getContenidoFirma().toString().getBytes()), documento);
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
    private javax.swing.JPanel imagePanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel mensajeLabel;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel progressBarPanel;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JButton selectImageButton;
    private javax.swing.JLabel selectedImageLabel;
    private javax.swing.JPanel validationPanel;
    private javax.swing.JButton verDocumentoButton;
    // End of variables declaration//GEN-END:variables


}
