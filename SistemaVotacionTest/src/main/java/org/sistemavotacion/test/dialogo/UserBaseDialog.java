package org.sistemavotacion.test.dialogo;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import static org.sistemavotacion.Contexto.getString;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.UserBaseData;
import org.sistemavotacion.test.simulacion.CreacionBaseUsuarios;
import org.sistemavotacion.test.simulacion.SimulationListener;
import org.sistemavotacion.test.simulacion.Simulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class UserBaseDialog extends JDialog 
    implements SimulationListener<UserBaseData> {

    private static Logger logger = LoggerFactory.getLogger(UserBaseDialog.class);  
    
    private static final int ENVIAR_DOCUMENTO_FIRMADO_WORKER = 0;
    private static final int TIME_STAMP_WORKER = 1;
    
    private Border normalTextBorder;
    private CreacionBaseUsuarios creacionBaseUsuarios = null;
    private Frame parentFrame;
    private List<String> errors = null;
    MensajeDialog errorDialog = null;
    
    /**
     * Creates new form RepresentativesDialog
     */
    public UserBaseDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.parentFrame = parent;
        initComponents();
        setLocationRelativeTo(null);
        /*
        numRepAbstentionLabel.setVisible(false);
        numRepAbstentionTextField.setVisible(false);
        numUsuWithoutRepresentativeLabel.setVisible(false);
        numUsuWithoutRepresentativeTextField.setVisible(false);
        numUsuAbstentionLabel.setVisible(false);
        numUsuAbstentionTextField.setVisible(false);*/
        progressBarPanel.setVisible(false);
        validacionPanel.setVisible(false);
        editorPane.setContentType("text/html");
        scrollPane.setVisible(false);
        editorPane.setEditable(false);
        errorButton.setVisible(false);
        pack();
    }

    public void setMessage(String mensaje) {
        if(mensaje == null || "".equals(mensaje)) {
            mensajeValidacionLabel.setText("");
            validacionPanel.setVisible(false);
        }else {
            mensajeValidacionLabel.setText(mensaje);
            validacionPanel.setVisible(true);
        }
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

        validacionPanel = new javax.swing.JPanel();
        mensajeValidacionLabel = new javax.swing.JLabel();
        closePanelLabel = new javax.swing.JLabel();
        createUsersButton = new javax.swing.JButton();
        progressBarPanel = new javax.swing.JPanel();
        progressLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        cancelButton = new javax.swing.JButton();
        userBasePanel = new org.sistemavotacion.test.panel.UserBasePanel();
        scrollPane = new javax.swing.JScrollPane();
        editorPane = new javax.swing.JEditorPane();
        errorButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/test/dialogo/Bundle"); // NOI18N
        setTitle(bundle.getString("UserBaseDialog.title")); // NOI18N

        validacionPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        mensajeValidacionLabel.setBackground(java.awt.Color.white);
        mensajeValidacionLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mensajeValidacionLabel.setText(bundle.getString("UserBaseDialog.mensajeValidacionLabel.text")); // NOI18N

        closePanelLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/close.gif"))); // NOI18N
        closePanelLabel.setText(bundle.getString("DatosSimulacionDialog.closePanelLabel.text")); // NOI18N
        closePanelLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                closePanelLabelcloseMensajeUsuario(evt);
            }
        });

        javax.swing.GroupLayout validacionPanelLayout = new javax.swing.GroupLayout(validacionPanel);
        validacionPanel.setLayout(validacionPanelLayout);
        validacionPanelLayout.setHorizontalGroup(
            validacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(validacionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mensajeValidacionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(closePanelLabel))
        );
        validacionPanelLayout.setVerticalGroup(
            validacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(validacionPanelLayout.createSequentialGroup()
                .addComponent(closePanelLabel)
                .addGap(0, 29, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, validacionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mensajeValidacionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        createUsersButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Group_16x16.png"))); // NOI18N
        createUsersButton.setText(bundle.getString("UserBaseDialog.createUsersButton.text")); // NOI18N
        createUsersButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createUsersButtonActionPerformed(evt);
            }
        });

        progressLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        progressLabel.setText(bundle.getString("UserBaseDialog.progressLabel.text")); // NOI18N

        progressBar.setIndeterminate(true);

        javax.swing.GroupLayout progressBarPanelLayout = new javax.swing.GroupLayout(progressBarPanel);
        progressBarPanel.setLayout(progressBarPanelLayout);
        progressBarPanelLayout.setHorizontalGroup(
            progressBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(progressBarPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(progressBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 362, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(progressLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        progressBarPanelLayout.setVerticalGroup(
            progressBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(progressBarPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(progressLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        cancelButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/cancel_16x16.png"))); // NOI18N
        cancelButton.setText(bundle.getString("UserBaseDialog.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        scrollPane.setViewportView(editorPane);

        errorButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/error.png"))); // NOI18N
        errorButton.setText(bundle.getString("UserBaseDialog.errorButton.text")); // NOI18N
        errorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                errorButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(validacionPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(progressBarPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(userBasePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 386, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(errorButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(createUsersButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(validacionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(userBasePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(createUsersButton)
                    .addComponent(cancelButton)
                    .addComponent(errorButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closePanelLabelcloseMensajeUsuario(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_closePanelLabelcloseMensajeUsuario
        setMessage(null);
    }//GEN-LAST:event_closePanelLabelcloseMensajeUsuario

    private void createUsersButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createUsersButtonActionPerformed
        UserBaseData userBaseData = userBasePanel.getData();
        if(Respuesta.SC_OK != userBaseData.getCodigoEstado()) {
            logger.debug("createUsersButtonActionPerformedv - message " 
                    + userBaseData.getMessage());
            setMessage(userBaseData.getMessage());
            return;
        }
        setMessage(null);
        createUsersButton.setVisible(false);
        scrollPane.setVisible(false);
        userBasePanel.setVisible(false);
        progressBarPanel.setVisible(true);
        pack();
        creacionBaseUsuarios = new CreacionBaseUsuarios(
               userBaseData, this);
        creacionBaseUsuarios.lanzar();
    }//GEN-LAST:event_createUsersButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        if(progressBarPanel.isVisible()) {
            creacionBaseUsuarios.finalizar();
            setSimulationResult(null, creacionBaseUsuarios.getUserBaseData());
        } else this.dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void errorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_errorButtonActionPerformed
        String resultMessange = "";
        int numError = 0;
        for(String error: errors) {
            resultMessange = resultMessange + "<br/> *** " + ++numError + " - " + error;
        }
        if(errorDialog == null) errorDialog = new MensajeDialog(parentFrame, false);
        errorDialog.setMessage("<html>" + resultMessange + "</html>", 
                ContextoPruebas.getString("userBaseErrorCaption"));
    }//GEN-LAST:event_errorButtonActionPerformed


    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(UserBaseDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(UserBaseDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(UserBaseDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(UserBaseDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                UserBaseDialog dialog = new UserBaseDialog(new javax.swing.JFrame(), true);
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
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel closePanelLabel;
    private javax.swing.JButton createUsersButton;
    private javax.swing.JEditorPane editorPane;
    private javax.swing.JButton errorButton;
    private javax.swing.JLabel mensajeValidacionLabel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel progressBarPanel;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JScrollPane scrollPane;
    private org.sistemavotacion.test.panel.UserBasePanel userBasePanel;
    private javax.swing.JPanel validacionPanel;
    // End of variables declaration//GEN-END:variables



    @Override public void setSimulationResult(
            Simulator simulator, final UserBaseData data) {
        logger.debug("setResult");
        userBasePanel.setVisible(true);
        progressBarPanel.setVisible(false);
        createUsersButton.setVisible(false);
        ContextoPruebas.setUserBaseData(data);
        setMessage(ContextoPruebas.getString("userBaseDataInContextMsg"));
        final String result = data.operationResultHtml();
        try {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    editorPane.setText(result);
                    scrollPane.setVisible(true);
                    pack();
                }
            });
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        /*Document doc = editorPane.getDocument();
        try {
            doc.insertString(0, data.toHtmlString(), null);
        } catch (BadLocationException ex) {
            logger.error(ex.getMessage(), ex);
        }*/
        pack();
    }

    @Override public void setSimulationErrorMessage(String message) {
        if(errors == null) {
            errors = new ArrayList<String>();
            errorButton.setVisible(true);
        } 
        errors.add(message);
        errorButton.setText(errors.size() + " errores");
    }

    @Override
    public void setSimulationMessage(String message) {
        progressLabel.setText(message);
        pack();
    }

}
