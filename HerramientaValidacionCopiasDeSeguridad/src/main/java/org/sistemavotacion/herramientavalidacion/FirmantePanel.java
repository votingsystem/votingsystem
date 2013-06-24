package org.sistemavotacion.herramientavalidacion;

import org.sistemavotacion.herramientavalidacion.util.Formateadora;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JFrame;
import org.bouncycastle.tsp.TimeStampToken;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class FirmantePanel extends javax.swing.JPanel implements ItemListener {

    private static Logger logger = LoggerFactory.getLogger(FirmantePanel.class);
    
    private Usuario firmante;
    
    /** Creates new form FirmantePanel */
    public FirmantePanel() {
        initComponents();
    }

    
    public FirmantePanel(Usuario firmante) throws Exception {
        initComponents();
        textoEditorPane.setEditable(false);
        textoEditorPane.setContentType("text/html");
        firmanteEditorPane.setEditable(false);
        firmanteEditorPane.setContentType("text/html");        
        this.firmante = firmante;
        valorAlgoritmoFirmaLabel.setText(firmante.getEncryptiontId() + " - " + 
                firmante.getDigestId());
        valorFechaFirmaLabel.setText(DateUtils.
                getSpanishFormattedStringFromDate(firmante.getFechaFirma()));
        firmanteEditorPane.setText(firmante.getInfoCert());
        textoEditorPane.setText(firmante.getContenidoFirmado());
        hashBase64TextField.setText(firmante.getContentDigestBase64());
        hashHexadecimalTextField.setText(firmante.getContentDigestHex());
        firmaBase64TextField.setText(firmante.getFirmaBase64());
        firmaHexadecimalTextField.setText(firmante.getFirmaHex());
        if(firmante.getTimeStampToken() == null) timeStampButton.setVisible(false);
        itemStateChanged(null);
        checkBox.addItemListener(this);
    }
    
    
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        algoritmoFirmaLabel = new javax.swing.JLabel();
        valorAlgoritmoFirmaLabel = new javax.swing.JLabel();
        textoLabel = new javax.swing.JLabel();
        textoScrollPane = new javax.swing.JScrollPane();
        textoEditorPane = new javax.swing.JEditorPane();
        hashHexadecimalLabel = new javax.swing.JLabel();
        hashHexadecimalTextField = new javax.swing.JTextField();
        hashBase64Label = new javax.swing.JLabel();
        hashBase64TextField = new javax.swing.JTextField();
        firmaLabel = new javax.swing.JLabel();
        firmaHexadecimalTextField = new javax.swing.JTextField();
        firmaBase64Label = new javax.swing.JLabel();
        firmaBase64TextField = new javax.swing.JTextField();
        fechaFirmaLabel = new javax.swing.JLabel();
        valorFechaFirmaLabel = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        firmanteScrollPane = new javax.swing.JScrollPane();
        firmanteEditorPane = new javax.swing.JEditorPane();
        checkBox = new javax.swing.JCheckBox();
        timeStampButton = new javax.swing.JButton();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/herramientavalidacion/Bundle"); // NOI18N
        jLabel1.setText(bundle.getString("FirmantePanel.jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        algoritmoFirmaLabel.setText(bundle.getString("FirmantePanel.algoritmoFirmaLabel.text")); // NOI18N
        algoritmoFirmaLabel.setName("algoritmoFirmaLabel"); // NOI18N

        valorAlgoritmoFirmaLabel.setName("valorAlgoritmoFirmaLabel"); // NOI18N

        textoLabel.setText(bundle.getString("FirmantePanel.textoLabel.text")); // NOI18N
        textoLabel.setName("textoLabel"); // NOI18N

        textoScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        textoScrollPane.setName("textoScrollPane"); // NOI18N

        textoEditorPane.setBackground(java.awt.Color.white);
        textoEditorPane.setName("textoEditorPane"); // NOI18N
        textoScrollPane.setViewportView(textoEditorPane);

        hashHexadecimalLabel.setText(bundle.getString("FirmantePanel.hashHexadecimalLabel.text")); // NOI18N
        hashHexadecimalLabel.setName("hashHexadecimalLabel"); // NOI18N

        hashHexadecimalTextField.setName("hashHexadecimalTextField"); // NOI18N

        hashBase64Label.setText(bundle.getString("FirmantePanel.hashBase64Label.text")); // NOI18N
        hashBase64Label.setName("hashBase64Label"); // NOI18N

        hashBase64TextField.setName("hashBase64TextField"); // NOI18N

        firmaLabel.setText(bundle.getString("FirmantePanel.firmaLabel.text")); // NOI18N
        firmaLabel.setName("firmaLabel"); // NOI18N

        firmaHexadecimalTextField.setName("firmaHexadecimalTextField"); // NOI18N

        firmaBase64Label.setText(bundle.getString("FirmantePanel.firmaBase64Label.text")); // NOI18N
        firmaBase64Label.setName("firmaBase64Label"); // NOI18N

        firmaBase64TextField.setName("firmaBase64TextField"); // NOI18N

        fechaFirmaLabel.setText(bundle.getString("FirmantePanel.fechaFirmaLabel.text")); // NOI18N
        fechaFirmaLabel.setName("fechaFirmaLabel"); // NOI18N

        valorFechaFirmaLabel.setName("valorFechaFirmaLabel"); // NOI18N

        jLabel2.setText(bundle.getString("FirmantePanel.jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        firmanteScrollPane.setName("firmanteScrollPane"); // NOI18N

        firmanteEditorPane.setBackground(java.awt.Color.white);
        firmanteEditorPane.setName("firmanteEditorPane"); // NOI18N
        firmanteScrollPane.setViewportView(firmanteEditorPane);

        checkBox.setText(bundle.getString("FirmantePanel.checkBox.text")); // NOI18N
        checkBox.setName("checkBox"); // NOI18N

        timeStampButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/history_16x16.png"))); // NOI18N
        timeStampButton.setText(bundle.getString("FirmantePanel.timeStampButton.text")); // NOI18N
        timeStampButton.setActionCommand(bundle.getString("FirmantePanel.timeStampButton.actionCommand")); // NOI18N
        timeStampButton.setName("timeStampButton"); // NOI18N
        timeStampButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timeStampButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(textoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(checkBox))
                    .addComponent(firmanteScrollPane)
                    .addComponent(firmaBase64TextField)
                    .addComponent(hashBase64Label, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(firmaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(firmaHexadecimalTextField)
                    .addComponent(hashBase64TextField)
                    .addComponent(hashHexadecimalTextField)
                    .addComponent(firmaBase64Label, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hashHexadecimalLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textoScrollPane, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(algoritmoFirmaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(valorAlgoritmoFirmaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(40, 40, 40)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(timeStampButton)
                            .addComponent(fechaFirmaLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(valorFechaFirmaLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(algoritmoFirmaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(valorAlgoritmoFirmaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(fechaFirmaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(valorFechaFirmaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(timeStampButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(firmanteScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBox)
                    .addComponent(textoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textoScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(hashHexadecimalLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(hashHexadecimalTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(hashBase64Label, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(hashBase64TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(firmaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(firmaHexadecimalTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(firmaBase64Label, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(firmaBase64TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void timeStampButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_timeStampButtonActionPerformed
        TimeStampToken timeStampToken = firmante.getTimeStampToken();
        if(timeStampToken == null) {
            logger.debug("TimeStampToken NULL");
            return;
        }
        Frame frame;
        Frame[] frames = JFrame.getFrames();
        if(frames.length == 0 || frames[0] == null) frame = new javax.swing.JFrame();
        else frame = frames[0];
        TimeStampDialog timeStampDialog = new TimeStampDialog(
                frame, true, timeStampToken);
       timeStampDialog.setVisible(true);
    }//GEN-LAST:event_timeStampButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel algoritmoFirmaLabel;
    private javax.swing.JCheckBox checkBox;
    private javax.swing.JLabel fechaFirmaLabel;
    private javax.swing.JLabel firmaBase64Label;
    private javax.swing.JTextField firmaBase64TextField;
    private javax.swing.JTextField firmaHexadecimalTextField;
    private javax.swing.JLabel firmaLabel;
    private javax.swing.JEditorPane firmanteEditorPane;
    private javax.swing.JScrollPane firmanteScrollPane;
    private javax.swing.JLabel hashBase64Label;
    private javax.swing.JTextField hashBase64TextField;
    private javax.swing.JLabel hashHexadecimalLabel;
    private javax.swing.JTextField hashHexadecimalTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JEditorPane textoEditorPane;
    private javax.swing.JLabel textoLabel;
    private javax.swing.JScrollPane textoScrollPane;
    private javax.swing.JButton timeStampButton;
    private javax.swing.JLabel valorAlgoritmoFirmaLabel;
    private javax.swing.JLabel valorFechaFirmaLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void itemStateChanged(ItemEvent e) {
        logger.debug("itemStateChanged - itemStateChanged");
        String textoConFormato = null;
        if (!checkBox.isSelected()) {
            try {
                textoConFormato = Formateadora.procesar(firmante.getContenidoFirmado()); 
                textoEditorPane.setText(textoConFormato);
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                return;
            }
        } else textoEditorPane.setText(firmante.getContenidoFirmado());  
    }
}
