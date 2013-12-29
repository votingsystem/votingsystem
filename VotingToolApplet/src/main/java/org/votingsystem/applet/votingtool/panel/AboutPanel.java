package org.votingsystem.applet.votingtool.panel;

import net.miginfocom.swing.MigLayout;
import org.votingsystem.model.ContextVS;

import javax.swing.*;

/**
 * Created by jgzornoza on 23/11/13.
 */
public class AboutPanel  extends JPanel {



    public AboutPanel () {
        setLayout(new MigLayout("wrap 2"));

        JLabel iconLabel =  new JLabel("");
        iconLabel.setIcon(ContextVS.getIcon(this, "application-certificate_32"));
        add(iconLabel, "span 1 3");


        JLabel messageLabel = new JLabel("<html><b><u>" + ContextVS.getMessage("appDescriptionLbl") + "</u></b></html>");
        add(messageLabel);
        JLabel sourceInfoLabel = new JLabel("<html><b><u>" + ContextVS.getMessage("appSourceRepository") + "</u></b></html>");
        sourceInfoLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        add(sourceInfoLabel);
        JLabel appVersionLabel = new JLabel("<html><b><u>" + ContextVS.getMessage("appVersionLbl") + "</u></b> " +
                ContextVS.getMessage("appVersionValue") + "</html>");
        add(appVersionLabel);

    }




}