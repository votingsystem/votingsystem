package org.votingsystem.applet.validationtool.panel;

import net.miginfocom.swing.MigLayout;
import org.votingsystem.model.ContextVS;

import javax.swing.*;

/**
 * Created by jgzornoza on 23/11/13.
 */
public class MessagePanel extends JPanel {

    private JLabel iconLabel;
    private JLabel messageLabel;

    public MessagePanel() {
        setLayout(new MigLayout("fill", "[][]"));
    }

    public void setMessage(String message, Icon icon) {
        if(icon == null) {
            if(iconLabel != null) {
                remove(iconLabel);
                iconLabel = null;
            }
        } else {
            iconLabel =  new JLabel(icon);
            add(iconLabel, "cell 0 0");
        }
        if(message == null) {
            if(messageLabel != null) {
                remove(messageLabel);
                messageLabel = null;
            }
        } else {
            messageLabel = new JLabel();
            add(messageLabel, "cell 0 1");
            messageLabel.setText("<html><b><u>" + message + "</u></b></html>");
        }

    }

}