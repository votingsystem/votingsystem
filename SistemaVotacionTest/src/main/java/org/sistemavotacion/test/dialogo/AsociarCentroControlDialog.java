package org.sistemavotacion.test.dialogo;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.callable.InfoGetter;
import org.sistemavotacion.callable.SMIMESignedSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AsociarCentroControlDialog extends JDialog implements KeyListener {

    private static Logger logger = LoggerFactory.getLogger(AsociarCentroControlDialog.class);

    private static BlockingQueue<Future<Respuesta>> queue = 
        new LinkedBlockingQueue<Future<Respuesta>>(3);
    
    private static final int CONTROL_CENTER_GETTER    = 0;
    private static final int ASSOCIATE_CONTROL_CENTER = 1;


    public enum Estado {DESCONECTADO, CONECTANDO, CONECTADO_CENTRO_CONTROL, 
        ASOCIANDO_CENTRO_CONTROL, CENTRO_CONTROL_ASOCIADO;}
    
    private Future<Respuesta> tareaEnEjecucion;
    private Estado estado = Estado.DESCONECTADO;
    private ActorConIP controlCenter = null;
    private Border normalTextBorder;
    private Frame parentFrame;
    private final AtomicBoolean done = new AtomicBoolean(false);
    
    /**
     * Creates new form AsociarCentroControlDialog
     */
    public AsociarCentroControlDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(null);
        this.parentFrame = parent;
        normalTextBorder = new JTextField().getBorder();
        mensajePanel.setVisible(false);
        controlCenterTextField.addKeyListener(this);
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                done.set(true);
                if (tareaEnEjecucion != null) {
                    tareaEnEjecucion.cancel(true);
                }
            }
            public void windowClosing(WindowEvent e) { 
                done.set(true);
            }
        });
        ContextoPruebas.INSTANCE.submit(new Runnable() {
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
        while (!done.get()) {
            try {
                Future<Respuesta> future = queue.take();
                Respuesta respuesta = future.get();
                switch(respuesta.getId()) {
                    case CONTROL_CENTER_GETTER:
                        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                            try {
                                controlCenter = ActorConIP.parse(respuesta.getMensaje());
                                if(controlCenter.getTipo() != ActorConIP.Tipo.CENTRO_CONTROL) {
                                    mostrarMensajeUsuario(ContextoPruebas.INSTANCE.
                                            getString("controlCenterErrorMsg"));
                                    controlCenterTextField.setBorder(new LineBorder(Color.RED,2));
                                    infoServidorButton.setIcon(null);
                                    infoServidorButton.setText(ContextoPruebas.INSTANCE.
                                            getString("associateLbl"));
                                    estado = Estado.DESCONECTADO;
                                    return;
                                }
                                if(ActorConIP.EnvironmentMode.DEVELOPMENT !=  
                                        controlCenter.getEnvironmentMode()) {
                                    String msg = ContextoPruebas.INSTANCE.getString(
                                            "serverModeErrorMsg", controlCenter.getEnvironmentMode());
                                    logger.error("### ERROR - " + msg);
                                    mostrarMensajeUsuario(msg);
                                    controlCenterTextField.setBorder(new LineBorder(Color.RED,2));
                                    infoServidorButton.setIcon(null);
                                    infoServidorButton.setText(ContextoPruebas.INSTANCE.
                                            getString("associateLbl"));
                                    estado = Estado.DESCONECTADO;                        
                                    return;
                                }
                                mostrarMensajeUsuario(null);
                                controlCenterTextField.setBorder(normalTextBorder);
                                estado = Estado.CONECTADO_CENTRO_CONTROL;
                                sendAssociationRequest();
                            } catch(Exception ex) {
                                logger.error(ex.getMessage(), ex);
                                infoServidorButton.setIcon(new javax.swing.ImageIcon(
                                        getClass().getResource("/images/pair_16x16.png")));
                                infoServidorButton.setText(ContextoPruebas.INSTANCE.
                                        getString("associateLbl"));
                                mostrarMensajeUsuario(ex.getMessage());
                            }
                        } else {
                            mostrarMensajeUsuario(respuesta.getMensaje());
                            infoServidorButton.setIcon(null);
                            infoServidorButton.setText("Asociar");
                        }  
                        break;
                    case ASSOCIATE_CONTROL_CENTER:
                        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                            estado = Estado.CENTRO_CONTROL_ASOCIADO;
                            infoServidorButton.setText("Información del servidor");
                            infoServidorButton.setIcon(new ImageIcon(getClass()
                                    .getResource("/images/information-white.png")));
                            ContextoPruebas.INSTANCE.setControlCenter(controlCenter);
                            dispose();
                        } else {
                            mostrarMensajeUsuario(respuesta.getMensaje());
                            infoServidorButton.setIcon(null);
                            infoServidorButton.setText("Asociar");
                        }
                        break;
                }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
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

        urlCentroControlPanel = new javax.swing.JPanel();
        centroControlLabel = new javax.swing.JLabel();
        controlCenterTextField = new javax.swing.JTextField();
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

        controlCenterTextField.setText(bundle.getString("AsociarCentroControlDialog.controlCenterTextField.text")); // NOI18N

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
                .addComponent(controlCenterTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(infoServidorButton, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        urlCentroControlPanelLayout.setVerticalGroup(
            urlCentroControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, urlCentroControlPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(urlCentroControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(centroControlLabel)
                    .addComponent(controlCenterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                .addComponent(cerrarButton, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
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
        if("".equals(controlCenterTextField.getText().trim())) return; 
        switch(estado) {
            case DESCONECTADO:
                logger.debug("Conectando con Control Acceso");
                estado = Estado.CONECTANDO;
                infoServidorButton.setIcon(new javax.swing.ImageIcon(getClass().
                        getResource("/images/loading.gif")));
                infoServidorButton.setText("Conectando");
                String urlServidor = StringUtils.prepararURL(
                        controlCenterTextField.getText().trim());
                String urlInfoServidor = ContextoPruebas.getURLInfoServidor(urlServidor);
                InfoGetter infoGetter = new InfoGetter(
                        CONTROL_CENTER_GETTER,urlInfoServidor, null);
                Future<Respuesta> future = ContextoPruebas.INSTANCE.submit(infoGetter);
                tareaEnEjecucion = future;
                controlCenterTextField.setText(controlCenterTextField.getText().trim());
                try {
                    queue.put(future);
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
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
                    parentFrame, false, controlCenter);
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
    private javax.swing.JButton cerrarButton;
    private javax.swing.JLabel closeLabel;
    private javax.swing.JTextField controlCenterTextField;
    private javax.swing.JButton infoServidorButton;
    private javax.swing.JLabel mensajeLabel;
    private javax.swing.JPanel mensajePanel;
    private javax.swing.JPanel urlCentroControlPanel;
    // End of variables declaration//GEN-END:variables

    private void sendAssociationRequest() {
        estado = Estado.ASOCIANDO_CENTRO_CONTROL;
        try {
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    ContextoPruebas.INSTANCE.getUserTest().getKeyStore(),
                    ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                    ContextoPruebas.DEFAULTS.PASSWORD.toCharArray(),
                    ContextoPruebas.VOTE_SIGN_MECHANISM);

            String documentoAsociacion = ActorConIP.getAssociationDocumentJSON(
                    controlCenter.getServerURL()).toString();
            String msgSubject = ContextoPruebas.INSTANCE.getString(
                    "associateControlCenterMsgSubject");
            SMIMEMessageWrapper smimeDocument = signedMailGenerator.genMimeMessage(
                    ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                    Contexto.INSTANCE.getAccessControl().getNombreNormalizado(), 
                    documentoAsociacion, msgSubject, null);
            SMIMESignedSender signedSender = new SMIMESignedSender(ASSOCIATE_CONTROL_CENTER, 
                    smimeDocument, ContextoPruebas.INSTANCE.getURLAsociarActorConIP(), 
                    null, null);
            Future<Respuesta> future = ContextoPruebas.INSTANCE.
                    submit(signedSender);
            queue.put(future);
            tareaEnEjecucion = future;
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

    @Override public void keyTyped(KeyEvent ke) { }

    @Override public void keyPressed(KeyEvent ke) {
        int key = ke.getKeyCode();
        if (key == KeyEvent.VK_ENTER) {
            Toolkit.getDefaultToolkit().beep();
            infoServidorButton.doClick();
        }
    }

    @Override public void keyReleased(KeyEvent ke) { }
        
}
