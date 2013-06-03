package org.sistemavotacion.test.panel;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.UserBaseSimulationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class UserBasePanel extends javax.swing.JPanel {
    
    private static Logger logger = LoggerFactory.getLogger(UserBasePanel.class);

    private Border normalTextBorder;
        
    public UserBasePanel() {
        initComponents();
        normalTextBorder = numRepTextField.getBorder();
    }

    public UserBaseSimulationData getData() {
        numRepTextField.setBorder(normalTextBorder);
        numVotesRepTextField.setBorder(normalTextBorder);
        
        numUsuWithRepresentativeTextField.setBorder(normalTextBorder);
        numVotesUsersWithRepresentativeTextField.setBorder(normalTextBorder);
        
        numVotesUsersWithoutRepresentativeTextField.setBorder(normalTextBorder);
        numUsuWithoutRepresentativeTextField.setBorder(normalTextBorder);

        List<JTextField> textFiels = Arrays.asList(userIndexTextField, 
                numRepTextField, numVotesRepTextField, numUsuWithRepresentativeTextField, 
                numVotesUsersWithRepresentativeTextField, numUsuWithoutRepresentativeTextField,
                numVotesUsersWithoutRepresentativeTextField);
        
        UserBaseSimulationData userBaseData = checkEmptyFields(textFiels);
        if(userBaseData != null) return userBaseData;
        userBaseData = checkNumericFields(textFiels);
        if(userBaseData != null) return userBaseData;
        
        Integer userIndex = new Integer(userIndexTextField.getText().trim());
        
        Integer numRep =  new Integer(numRepTextField.getText().trim());
        Integer numVotesRep = new Integer(numVotesRepTextField.getText().trim());

        Integer numUserWithRepresentative = new Integer(
                numUsuWithRepresentativeTextField.getText().trim());
        Integer numVotesUserWithRepresentative = new Integer(
                numVotesUsersWithRepresentativeTextField.getText().trim());
        
        Integer numUserWithoutRepresentative = new Integer(
                numUsuWithoutRepresentativeTextField.getText().trim());
        Integer numVotesUserWithoutRepresentative = new Integer(
                numVotesUsersWithoutRepresentativeTextField.getText().trim());
       
        if(!(userIndex > 0)) {
            userIndexTextField.setBorder(new LineBorder(Color.RED,2));
            return new UserBaseSimulationData(Respuesta.SC_ERROR_PETICION, 
                        ContextoPruebas.INSTANCE.getString("numericFieldErrorMsg"));
        }
        
        if(numVotesRep > numRep) {
            numRepTextField.setBorder(new LineBorder(Color.RED,2));
            numVotesRepTextField.setBorder(new LineBorder(Color.RED,2));
            return new UserBaseSimulationData(Respuesta.SC_ERROR_PETICION, 
                    ContextoPruebas.INSTANCE.getString("numberUserVotesErrorMsg"));
        }
        
        if(numVotesUserWithRepresentative > numUserWithRepresentative) {
            numUsuWithRepresentativeTextField.setBorder(new LineBorder(Color.RED,2));
            numVotesUsersWithRepresentativeTextField.setBorder(new LineBorder(Color.RED,2));
            return new UserBaseSimulationData(Respuesta.SC_ERROR_PETICION, 
                    ContextoPruebas.INSTANCE.getString("numberUserVotesErrorMsg"));
        }

        if(numVotesUserWithoutRepresentative > numUserWithoutRepresentative) {
            numUsuWithoutRepresentativeTextField.setBorder(new LineBorder(Color.RED,2));
            numVotesUsersWithoutRepresentativeTextField.setBorder(new LineBorder(Color.RED,2));
            return new UserBaseSimulationData(Respuesta.SC_ERROR_PETICION, 
                    ContextoPruebas.INSTANCE.getString("numberUserVotesErrorMsg"));
        }
        if(!(numRep > 0)) {
            numRepTextField.setBorder(new LineBorder(Color.RED,2));
            return new UserBaseSimulationData(Respuesta.SC_ERROR_PETICION, 
                    ContextoPruebas.INSTANCE.getString("repNumberErrorMsg"));
        }
        if(numUserWithRepresentative > 0 && !(numRep > 0)) {
            numUsuWithRepresentativeTextField.setBorder(new LineBorder(Color.RED,2));
            numRepTextField.setBorder(new LineBorder(Color.RED,2));
            return new UserBaseSimulationData(Respuesta.SC_ERROR_PETICION,
                    ContextoPruebas.INSTANCE.getString("repNumberErrorMsg"));
        }

        
        userBaseData = new UserBaseSimulationData();
        userBaseData.setUserIndex(userIndex);
        
        userBaseData.setNumRepresentatives(numRep);
        userBaseData.setNumVotesRepresentatives(numVotesRep);
        
        userBaseData.setNumUsersWithRepresentative(numUserWithRepresentative);
        userBaseData.setNumVotesUsersWithRepresentative(numVotesUserWithRepresentative);
        
        userBaseData.setNumUsersWithoutRepresentative(numUserWithoutRepresentative);
        userBaseData.setNumVotesUsersWithoutRepresentative(numVotesUserWithoutRepresentative);
        
        userBaseData.setStatusCode(Respuesta.SC_OK);
        
        return userBaseData;
    }
    
    private UserBaseSimulationData checkNumericFields(List<JTextField> textFiels) {
        for(JTextField textField:textFiels) {
            try{
                int numvalue = new Integer(textField.getText().trim());
            } catch (Exception ex) {
                textField.setBorder(new LineBorder(Color.RED,2));
                return new UserBaseSimulationData(Respuesta.SC_ERROR_PETICION, 
                        ContextoPruebas.INSTANCE.getString("numericFieldErrorMsg"));
            } 
        }
        return null;
    }
    
    private UserBaseSimulationData checkEmptyFields(List<JTextField> textFiels) {
        for(JTextField textField:textFiels) {
            if (textField.getText() == null || "".equals(textField.getText().trim())) {
                textField.setBorder(new LineBorder(Color.RED,2));
                return new UserBaseSimulationData(Respuesta.SC_ERROR_PETICION, 
                        ContextoPruebas.INSTANCE.getString("emptyFieldErrorMsg"));
            } 
        }
        return null;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        usersPanel = new javax.swing.JPanel();
        numRepLabel = new javax.swing.JLabel();
        numRepTextField = new javax.swing.JTextField();
        numVotesRepLabel = new javax.swing.JLabel();
        numVotesRepTextField = new javax.swing.JTextField();
        representativesPanel = new javax.swing.JPanel();
        numUsuWithRepresentativeLabel = new javax.swing.JLabel();
        numUsuWithRepresentativeTextField = new javax.swing.JTextField();
        numUsuWithoutRepresentativeLabel = new javax.swing.JLabel();
        numUsuWithoutRepresentativeTextField = new javax.swing.JTextField();
        numVotesUsersWithoutRepresentativeLabel = new javax.swing.JLabel();
        numVotesUsersWithoutRepresentativeTextField = new javax.swing.JTextField();
        numVotesUsersWithRepresentativeLabel = new javax.swing.JLabel();
        numVotesUsersWithRepresentativeTextField = new javax.swing.JTextField();
        userIndexLabel = new javax.swing.JLabel();
        userIndexTextField = new javax.swing.JTextField();

        usersPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/test/dialogo/Bundle"); // NOI18N
        numRepLabel.setText(bundle.getString("RepresentativesDialog.numRepLabel.text")); // NOI18N

        numRepTextField.setText(bundle.getString("RepresentativesDialog.numRepTextField.text")); // NOI18N
        numRepTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numRepTextFieldActionPerformed(evt);
            }
        });

        numVotesRepLabel.setText(bundle.getString("RepresentativesDialog.numRepAbstentionLabel.text")); // NOI18N
        numVotesRepLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        numVotesRepTextField.setText(bundle.getString("RepresentativesDialog.numRepAbstentionTextField.text")); // NOI18N
        numVotesRepTextField.setToolTipText(bundle.getString("RepresentativesDialog.numRepAbstentionTextField.toolTipText")); // NOI18N
        numVotesRepTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numVotesRepTextFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout usersPanelLayout = new javax.swing.GroupLayout(usersPanel);
        usersPanel.setLayout(usersPanelLayout);
        usersPanelLayout.setHorizontalGroup(
            usersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(usersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(usersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(numVotesRepLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(numRepLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(usersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(numRepTextField)
                    .addComponent(numVotesRepTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE))
                .addContainerGap())
        );
        usersPanelLayout.setVerticalGroup(
            usersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(usersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(usersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(numRepTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(numRepLabel))
                .addGroup(usersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(usersPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(numVotesRepTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(usersPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(numVotesRepLabel))))
        );

        representativesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        numUsuWithRepresentativeLabel.setText(bundle.getString("RepresentativesDialog.numUsuTotalLabel.text")); // NOI18N
        numUsuWithRepresentativeLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        numUsuWithRepresentativeTextField.setText(bundle.getString("RepresentativesDialog.numUsuTotalTextField.text")); // NOI18N

        numUsuWithoutRepresentativeLabel.setText(bundle.getString("RepresentativesDialog.numUsuWithoutRepresentativeLabel.text")); // NOI18N
        numUsuWithoutRepresentativeLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        numUsuWithoutRepresentativeTextField.setText(bundle.getString("RepresentativesDialog.numUsuWithoutRepresentativeTextField.text")); // NOI18N

        numVotesUsersWithoutRepresentativeLabel.setText(bundle.getString("RepresentativesDialog.numUsuAbstentionLabel.text")); // NOI18N
        numVotesUsersWithoutRepresentativeLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        numVotesUsersWithoutRepresentativeLabel.setVerticalTextPosition(javax.swing.SwingConstants.TOP);

        numVotesUsersWithoutRepresentativeTextField.setText(bundle.getString("RepresentativesDialog.numUsuAbstentionTextField.text")); // NOI18N

        java.util.ResourceBundle bundle1 = java.util.ResourceBundle.getBundle("org/sistemavotacion/test/panel/Bundle"); // NOI18N
        numVotesUsersWithRepresentativeLabel.setText(bundle1.getString("UserBasePanel.numVotesUsersWithRepresentativeLabel.text")); // NOI18N
        numVotesUsersWithRepresentativeLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        numVotesUsersWithRepresentativeTextField.setText(bundle1.getString("UserBasePanel.numVotesUsersWithRepresentativeTextField.text")); // NOI18N

        javax.swing.GroupLayout representativesPanelLayout = new javax.swing.GroupLayout(representativesPanel);
        representativesPanel.setLayout(representativesPanelLayout);
        representativesPanelLayout.setHorizontalGroup(
            representativesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(representativesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(representativesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(numUsuWithRepresentativeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(numVotesUsersWithRepresentativeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(numUsuWithoutRepresentativeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(numVotesUsersWithoutRepresentativeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addGroup(representativesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(numVotesUsersWithoutRepresentativeTextField)
                    .addComponent(numUsuWithoutRepresentativeTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE)
                    .addComponent(numUsuWithRepresentativeTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE)
                    .addComponent(numVotesUsersWithRepresentativeTextField))
                .addContainerGap())
        );
        representativesPanelLayout.setVerticalGroup(
            representativesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(representativesPanelLayout.createSequentialGroup()
                .addGroup(representativesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(representativesPanelLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(numUsuWithRepresentativeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(representativesPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(numUsuWithRepresentativeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(representativesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(representativesPanelLayout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(numVotesUsersWithRepresentativeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(representativesPanelLayout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addComponent(numVotesUsersWithRepresentativeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(representativesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(representativesPanelLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(numUsuWithoutRepresentativeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(numUsuWithoutRepresentativeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(representativesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(representativesPanelLayout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(numVotesUsersWithoutRepresentativeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(representativesPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(numVotesUsersWithoutRepresentativeLabel)
                        .addContainerGap())))
        );

        userIndexLabel.setText(bundle1.getString("UserBasePanel.userIndexLabel.text")); // NOI18N

        userIndexTextField.setText(bundle1.getString("UserBasePanel.userIndexTextField.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(usersPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(representativesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(userIndexLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(34, 34, 34)
                .addComponent(userIndexTextField)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(userIndexTextField)
                    .addComponent(userIndexLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(usersPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(representativesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void numRepTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_numRepTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_numRepTextFieldActionPerformed

    private void numVotesRepTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_numVotesRepTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_numVotesRepTextFieldActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel numRepLabel;
    private javax.swing.JTextField numRepTextField;
    private javax.swing.JLabel numUsuWithRepresentativeLabel;
    private javax.swing.JTextField numUsuWithRepresentativeTextField;
    private javax.swing.JLabel numUsuWithoutRepresentativeLabel;
    private javax.swing.JTextField numUsuWithoutRepresentativeTextField;
    private javax.swing.JLabel numVotesRepLabel;
    private javax.swing.JTextField numVotesRepTextField;
    private javax.swing.JLabel numVotesUsersWithRepresentativeLabel;
    private javax.swing.JTextField numVotesUsersWithRepresentativeTextField;
    private javax.swing.JLabel numVotesUsersWithoutRepresentativeLabel;
    private javax.swing.JTextField numVotesUsersWithoutRepresentativeTextField;
    private javax.swing.JPanel representativesPanel;
    private javax.swing.JLabel userIndexLabel;
    private javax.swing.JTextField userIndexTextField;
    private javax.swing.JPanel usersPanel;
    // End of variables declaration//GEN-END:variables
}
