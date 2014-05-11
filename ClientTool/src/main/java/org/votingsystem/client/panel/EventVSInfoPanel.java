package org.votingsystem.client.panel;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.model.MetaInf;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class EventVSInfoPanel extends JPanel {

    private static Logger logger = Logger.getLogger(EventVSInfoPanel.class);

    private MetaInf metaInf = null;


    public EventVSInfoPanel(MetaInf metaInf) throws Exception {
        this.metaInf = metaInf;
        initComponents();
    }


    private JLabel createBoldLabel(String labelContent) {
        return new JLabel("<html><b>" + labelContent + "</b></html>");
    }

    private void initComponents() throws URISyntaxException {
        setLayout(new MigLayout("fill", "", "[][][][]"));

        JLabel subjectLabel = createBoldLabel(ContextVS.getMessage("subjectLbl") + ": ");
        add(subjectLabel);
        JLabel subjectValueLabel = new JLabel(metaInf.getSubject());
        add(subjectValueLabel, "span 3, wrap");

        JLabel dateInitLabel = createBoldLabel(ContextVS.getMessage("dateInitLbl") + ": ");
        add(dateInitLabel);
        JLabel dateInitValueLabel = new JLabel(DateUtils.getShortStringFromDate(metaInf.getDateInit()));
        add(dateInitValueLabel, "");

        JLabel dateFinishLabel = createBoldLabel(ContextVS.getMessage("dateFinishLbl") + ": ");
        add(dateFinishLabel, "gapleft 30");
        JLabel dateFinishValueLabel = new JLabel(DateUtils.getShortStringFromDate(metaInf.getDateFinish()));
        add(dateFinishValueLabel, "wrap");

        JScrollPane contentScrollPane = new JScrollPane();
        JEditorPane contentPane = new JEditorPane();
        contentPane.setEditable(false);
        contentPane.setContentType("text/html");
        contentPane.setBackground(java.awt.Color.white);
        contentPane.setText(metaInf.getFormattedInfo());
        contentPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if(Desktop.isDesktopSupported()) {
                        try {Desktop.getDesktop().browse(e.getURL().toURI());}
                        catch(Exception ex) {logger.error(ex.getMessage(), ex);}
                    }
                }
            }
        });
        contentScrollPane.setViewportView(contentPane);
        add(contentScrollPane, "height 400::, grow, span 4, wrap");
        if(metaInf.getType() == TypeVS.VOTING_EVENT) {
            JButton representativesButton = new JButton(ContextVS.getMessage("representativesDetailsLbl"));
            representativesButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) { showRepresentativeDetails();}
            });
            add(representativesButton, "cell 0 5, span 2");
        }
    }

    
    private void showRepresentativeDetails() {
        MessageDialog messageDialog = new MessageDialog(new JFrame(), true);
        messageDialog.showMessage(metaInf.getRepresentativesHTML(), ContextVS.getMessage("representativesDetailsLbl"));
    }

    private static void open(URI uri) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(uri);
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        } else {
            MessageDialog messageDialog = new MessageDialog(new Frame(), true);
            messageDialog.showMessage(ContextVS.getInstance().getMessage(
                    "platformNotSupportedErrorMsg"), ContextVS.getInstance().getMessage("errorLbl"));
        }
    }
}
