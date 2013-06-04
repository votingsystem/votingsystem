package org.sistemavotacion.test.dialogo;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import javax.mail.internet.MimeMessage;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.MainFrame;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.DocumentSenderWorker;
import org.sistemavotacion.worker.InfoGetterWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AsociarCentroControlDialog extends JDialog implements 
        VotingSystemWorkerListener, KeyListener {

    private static Logger logger = LoggerFactory.getLogger(AsociarCentroControlDialog.class);

    private static final int INFO_GETTER_WORKER                       = 0;
    private static final int ASSOCIATE_CONTROL_CENTER_WORKER          = 1;

    @Override public void processVotingSystemWorkerMsg(List<String> messages) {
        String msg = null;
        for(String message : messages) {
            if(msg == null ) msg = message;
            else msg = msg + "<br/>" + message; 
        }
        mostrarMensajeUsuario(msg);
    }

    @Override
    public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
                " - worker: " + worker.getClass().getSimpleName() + 
                " - workerId:" + worker.getId());
        switch(worker.getId()) {
            case INFO_GETTER_WORKER:
            if(Respuesta.SC_OK == worker.getStatusCode()) {
                try {
                    ActorConIP actorConIP = ActorConIP.parse(worker.getMessage());
                    if(actorConIP.getTipo() != ActorConIP.Tipo.CENTRO_CONTROL) {
                        mostrarMensajeUsuario("El servidor no es un Centro de Control");
                        centroControlTextField.setBorder(new LineBorder(Color.RED,2));
                        infoServidorButton.setIcon(null);
                        infoServidorButton.setText("Asociar");
                        estado = Estado.DESCONECTADO;
                        return;
                    }
                    mostrarMensajeUsuario(null);
                    centroControlTextField.setBorder(normalTextBorder);
                    centroControl = actorConIP;
                    estado = Estado.CONECTADO_CENTRO_CONTROL;
                    
                    
                    firmarEnviarSolicitudAsociacionCentroControl();
                    /* ====== if(ActorConIP.EnvironmentMode.TEST.equals(
                            centroControl.getEnvironmentMode())) {
                        firmarEnviarSolicitudAsociacionCentroControl();
                    } else {
                         mostrarMensajeUsuario(
                                 "Para poder hacer las pruebas el servidor tiene que ser arrancado en modo TEST");
                    }*/
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    infoServidorButton.setIcon(new javax.swing.ImageIcon(
                            getClass().getResource("/images/pair_16x16.png")));
                    infoServidorButton.setText("Asociar");
                    mostrarMensajeUsuario(ex.getMessage());
                }
            } else {
                mostrarMensajeUsuario(worker.getMessage());
                infoServidorButton.setIcon(null);
                infoServidorButton.setText("Asociar");
            }
            break;
            case ASSOCIATE_CONTROL_CENTER_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    estado = Estado.CENTRO_CONTROL_ASOCIADO;
                    infoServidorButton.setText("Información del servidor");
                    infoServidorButton.setIcon(new ImageIcon(getClass()
                            .getResource("/images/information-white.png")));
                    MainFrame.INSTANCIA.cargarControlAcceso();
                    dispose();
                } else {
                    mostrarMensajeUsuario(worker.getMessage());
                    infoServidorButton.setIcon(null);
                    infoServidorButton.setText("Asociar");
                }
                break;
        }
    }
    
    public enum Estado {DESCONECTADO, CONECTANDO, CONECTADO_CENTRO_CONTROL, 
    ASOCIANDO_CENTRO_CONTROL, CENTRO_CONTROL_ASOCIADO;}
    
    private SwingWorker tareaEnEjecucion;
    private Estado estado = Estado.DESCONECTADO;
    private ActorConIP centroControl = null;
    private Border normalTextBorder;
        
    /**
     * Creates new form AsociarCentroControlDialog
     */
    public AsociarCentroControlDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(null);
        normalTextBorder = new JTextField().getBorder();
        mensajePanel.setVisible(false);
        centroControlTextField.addKeyListener(this);
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

        urlCentroControlPanel = new javax.swing.JPanel();
        centroControlLabel = new javax.swing.JLabel();
        centroControlTextField = new javax.swing.JTextField();
        infoServidorButton = new javax.swing.JButton();
        cerrarButton = new javax.swing.JButton();
        mensajePanel = new javax.swing.JPanel();
        mensajeLabel = new javax.swing.JLabel();
        closeLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/test/dialogo/Bundle"); // NOI18N
        setTitle(bundle.getString("AsociarCentroControlDialog.title")); // NOI18N

        centroControlLabel.setText(bundle.getString("AsociarCentroControlDialog.centroControlLabel.text")); // NOI18N
        centroControlLabel.setToolTipText(bundle.getString("AsociarCentroControlDialog.centroControlLabel.toolTipText")); // NOI18N

        centroControlTextField.setText(bundle.getString("AsociarCentroControlDialog.centroControlTextField.text")); // NOI18N

        infoServidorButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/pair_16x16.png"))); // NOI18N
        infoServidorButton.setText(bundle.getString("AsociarCentroControlDialog.infoServidorButton.text")); // NOI18N
        infoServidorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                asociarServidorButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout urlCentroControlPanelLayout = new javax.swing.GroupLayout(urlCentroControlPanel);
        urlCentroControlPanel.setLayout(urlCentroControlPanelLayout);
        urlCentroControlPanelLayout.setHorizontalGroup(
            urlCentroControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(urlCentroControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(centroControlLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(centroControlTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 272, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(infoServidorButton)
                .addContainerGap())
        );
        urlCentroControlPanelLayout.setVerticalGroup(
            urlCentroControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, urlCentroControlPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(urlCentroControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(centroControlLabel)
                    .addComponent(centroControlTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(infoServidorButton))
                .addContainerGap())
        );

        cerrarButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/cancel_16x16.png"))); // NOI18N
        cerrarButton.setText(bundle.getString("AsociarCentroControlDialog.cerrarButton.text")); // NOI18N
        cerrarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cerrarButtonActionPerformed(evt);
            }
        });

        mensajePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        mensajeLabel.setFont(new java.awt.Font("DejaVu Sans", 1, 15)); // NOI18N
        mensajeLabel.setForeground(java.awt.Color.red);
        mensajeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mensajeLabel.setText(bundle.getString("AsociarCentroControlDialog.mensajeLabel.text")); // NOI18N

        closeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        closeLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/close.gif"))); // NOI18N
        closeLabel.setText(bundle.getString("AsociarCentroControlDialog.closeLabel.text")); // NOI18N
        closeLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                closeLabelcloseMensajeUsuario(evt);
            }
        });

        javax.swing.GroupLayout mensajePanelLayout = new javax.swing.GroupLayout(mensajePanel);
        mensajePanel.setLayout(mensajePanelLayout);
        mensajePanelLayout.setHorizontalGroup(
            mensajePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mensajePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mensajeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(closeLabel))
        );
        mensajePanelLayout.setVerticalGroup(
            mensajePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mensajePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mensajeLabel)
                .addContainerGap(16, Short.MAX_VALUE))
            .addGroup(mensajePanelLayout.createSequentialGroup()
                .addComponent(closeLabel)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mensajePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(urlCentroControlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(cerrarButton)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(mensajePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(urlCentroControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(cerrarButton)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void asociarServidorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_asociarServidorButtonActionPerformed
        logger.debug("asociarServidorButtonActionPerformed - estado: " + estado.toString());
        if("".equals(centroControlTextField.getText().trim())) return; 
        switch(estado) {
            case DESCONECTADO:
                logger.debug("Conectando con Control Acceso");
                estado = Estado.CONECTANDO;
                infoServidorButton.setIcon(new javax.swing.ImageIcon(getClass().
                        getResource("/images/loading.gif")));
                infoServidorButton.setText("Conectando");
                String urlServidor = StringUtils.prepararURL(
                        centroControlTextField.getText().trim());
                String urlInfoServidor = ContextoPruebas.getURLInfoServidor(urlServidor);
                tareaEnEjecucion = new InfoGetterWorker(INFO_GETTER_WORKER,
                    urlInfoServidor, null, this);
                tareaEnEjecucion.execute();
                centroControlTextField.setText(centroControlTextField.getText().trim());
                break;
            case CONECTANDO:
                tareaEnEjecucion.cancel(true);
                infoServidorButton.setIcon(null);
                infoServidorButton.setText("Conectar");
                estado = Estado.DESCONECTADO;
                break;
            case ASOCIANDO_CENTRO_CONTROL:
                tareaEnEjecucion.cancel(true);
                infoServidorButton.setIcon(null);
                infoServidorButton.setText("Conectar");
                estado =  Estado.DESCONECTADO;
                break;
            case CENTRO_CONTROL_ASOCIADO:
                InfoServidorDialog infoServidorDialog = new InfoServidorDialog(
                    MainFrame.INSTANCIA.getFrames()[0], false, centroControl);
                infoServidorDialog.setVisible(true);
                break;
        }
        
    }//GEN-LAST:event_asociarServidorButtonActionPerformed

    private void cerrarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cerrarButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cerrarButtonActionPerformed

    private void closeLabelcloseMensajeUsuario(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_closeLabelcloseMensajeUsuario
        mostrarMensajeUsuario(null);
    }//GEN-LAST:event_closeLabelcloseMensajeUsuario

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(AsociarCentroControlDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AsociarCentroControlDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AsociarCentroControlDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AsociarCentroControlDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the dialog
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                AsociarCentroControlDialog dialog = new AsociarCentroControlDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {

                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel centroControlLabel;
    private javax.swing.JTextField centroControlTextField;
    private javax.swing.JButton cerrarButton;
    private javax.swing.JLabel closeLabel;
    private javax.swing.JButton infoServidorButton;
    private javax.swing.JLabel mensajeLabel;
    private javax.swing.JPanel mensajePanel;
    private javax.swing.JPanel urlCentroControlPanel;
    // End of variables declaration//GEN-END:variables

    private void firmarEnviarSolicitudAsociacionCentroControl() {
        estado = Estado.ASOCIANDO_CENTRO_CONTROL;
        try {
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    ContextoPruebas.INSTANCE.getUserTest().getKeyStore(),
                    ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                    ContextoPruebas.DEFAULTS.PASSWORD.toCharArray(),
                    ContextoPruebas.VOTE_SIGN_MECHANISM);
            File solicitudAsociacion = new File(FileUtils.APPTEMPDIR + "SolicitudAsociacion");
            String documentoAsociacion = ActorConIP.getAssociationDocumentJSON(
                    centroControl.getServerURL()).toString();
            MimeMessage mimeMessage = signedMailGenerator.genMimeMessage(
                    ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                    Contexto.INSTANCE.getAccessControl().getNombreNormalizado(), 
                    documentoAsociacion, "Solicitud Asociacion de Centro de Control", null);
            mimeMessage.writeTo(new FileOutputStream(solicitudAsociacion));
            tareaEnEjecucion = new DocumentSenderWorker(
                    ASSOCIATE_CONTROL_CENTER_WORKER, solicitudAsociacion, 
                    Contexto.SIGNED_CONTENT_TYPE,
                    ContextoPruebas.getURLAsociarActorConIP(
                    Contexto.INSTANCE.getAccessControl().getServerURL()), this);
            tareaEnEjecucion.execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            infoServidorButton.setIcon(null);
            infoServidorButton.setText("Asociar");
            mostrarMensajeUsuario(ex.getMessage());
        }
    }
    
    public void mostrarMensajeUsuario(String mensaje) {
        if(mensaje == null) {
            mensajePanel.setVisible(false);
        }else {
            mensajeLabel.setText(mensaje);
            mensajePanel.setVisible(true);
        }
        pack();
    }

    @Override
    public void keyTyped(KeyEvent ke) { }

    @Override
    public void keyPressed(KeyEvent ke) {
        int key = ke.getKeyCode();
        if (key == KeyEvent.VK_ENTER) {
            Toolkit.getDefaultToolkit().beep();
            infoServidorButton.doClick();
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) { }
}
