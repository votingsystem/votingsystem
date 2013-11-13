package org.votingsystem.applet.votingtool;

import java.awt.Frame;
import java.io.File;
import javax.swing.JFileChooser;
import org.apache.log4j.Logger;
import org.votingsystem.model.VoteVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.applet.model.AppletOperation;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.FileUtils;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SaveReceiptDialog extends javax.swing.JDialog {
    
    private static Logger logger = Logger.getLogger(SaveReceiptDialog.class);

    private Frame parentFrame = null;
    
    public SaveReceiptDialog(Frame parent, boolean modal) {
        super(parent, modal);
        setLocationRelativeTo(null);
        this.parentFrame = parent;
        initComponents();
        parent.setLocationRelativeTo(null);
    }
    
    public void show(final String hashCertificadoVotoBase64) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                guardarRecibo(hashCertificadoVotoBase64);
            }
        });   
        setVisible(true);
    }
    
    public void guardarRecibo(String hashCertificadoVotoBase64) { 
        logger.debug(" - guardarRecibo - ");
        VoteVS recibo = VotingToolContext.INSTANCE.getVoteReceipt(hashCertificadoVotoBase64);
        AppletOperation operationVS = new AppletOperation(ResponseVS.SC_CANCELLED);
        parentFrame.setLocationRelativeTo(null);
        if(recibo != null) {
            String resultado = ContextVS.INSTANCE.getString("operacionCancelada");
            try {
                final JFileChooser chooser = new JFileChooser();
                File reciboFile = FileUtils.getFileFromBytes(recibo.getReceipt().getBytes());
                chooser.setSelectedFile(reciboFile);
                int returnVal = chooser.showSaveDialog(parentFrame);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    if (file.getName().indexOf(".") == -1) {
                        String fileName = file.getAbsolutePath();
                        file = new File(fileName);
                    }
                    if (file != null) {
                        reciboFile.renameTo(file);
                        resultado = file.getAbsolutePath();
                        operationVS.setStatusCode(ResponseVS.SC_OK);
                    }
                }
                reciboFile.delete();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            operationVS.setMessage(resultado);
            logger.debug("- guardarRecibo - resultado: " + operationVS.toJSON().toString());
        } else {
            logger.debug(" - Receipt Null - ");
            operationVS.setStatusCode(ResponseVS.SC_ERROR_REQUEST);
            operationVS.setMessage(ContextVS.INSTANCE.getString(
                            "receiptNotFoundMsg", hashCertificadoVotoBase64));
        }
        ContextVS.INSTANCE.sendMessageToHost(operationVS);
        dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("DejaVu Sans", 1, 16)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/votingsystem/applet/votingtool/Bundle"); // NOI18N
        jLabel1.setText(bundle.getString("SaveReceiptDialog.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
}
