package org.votingsystem.applet.votingtool.panel;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.Border;
import net.miginfocom.swing.MigLayout;

public class ProgressBarPanel extends JPanel {
    
    private JLabel messageLabel      = null;
    private JProgressBar progressBar = null;
    
    public ProgressBarPanel () {
        setLayout(new MigLayout("fill"));
        messageLabel = new JLabel();
        messageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        //Border border = BorderFactory.createLineBorder(Color.BLUE, 1);
        //messageLabel.setBorder(border);
        add(messageLabel, "growx, wrap"); 
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        add(progressBar, "growx, wrap"); 
    }
    
    public void setMessage(String message) {
        if(message != null || "".equals(message)) {
            messageLabel.setText("<html><div style=\"margin: 5px 0 5px 0;\"><b>" + message + "</b></div></html>");
        }
        progressBar.setVisible(true);
    }

    public void setMaximum(Integer value) {
        progressBar.setMaximum(value);
    }

    public void setValue(Integer value) {
        progressBar.setValue(value);
    }

    public void setBarMessage(String barMessage) {
        progressBar.setStringPainted(true);
        progressBar.setString(barMessage);
    }

    public void setResultMessage(String message) {
        progressBar.setVisible(false);
        messageLabel.setText("<html><div style=\"margin: 5px 0 5px 0;\"><b>" + 
            message + "</b></div></html>");
    }
    
}