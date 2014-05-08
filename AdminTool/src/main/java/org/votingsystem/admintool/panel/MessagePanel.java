package org.votingsystem.admintool.panel;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class MessagePanel extends JPanel {

    private JLabel iconLabel;
    private JLabel messageLabel;

    public MessagePanel() {
        setLayout(new MigLayout("fill", "[][center]"));
    }

    public void setMessage(String message, Icon icon) {
        if(iconLabel != null) {
            remove(iconLabel);
            iconLabel = null;
        }
        if(messageLabel != null) {
            remove(messageLabel);
            messageLabel = null;
        }
        if(icon != null) {
            iconLabel =  new JLabel(icon);
            add(iconLabel, "cell 0 0, width 50:50:50");
        }
        if(message != null) {
            messageLabel = new JLabel("<html><b>" + message + "</b></html>");
            add(messageLabel, "cell 1 0, width 500:500:500, gapleft 15");
        }
    }

}