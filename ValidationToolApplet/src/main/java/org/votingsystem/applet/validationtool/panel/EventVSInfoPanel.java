package org.votingsystem.applet.validationtool.panel;

import java.awt.Desktop;
import java.awt.Frame;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.dialog.MessageDialog;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;
import javax.swing.*;
import org.votingsystem.applet.validationtool.model.MetaInf;

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
        String eventPathPart = null;
        String metaInfURL = metaInf.getServerURL();
        String numSignaturesLblStr = null;
        String numSignaturesValueLblStr = null;
        switch (metaInf.getType()) {
            case MANIFEST_EVENT:
                eventPathPart = "/eventVSManifest";
                numSignaturesLblStr = ContextVS.getInstance().getMessage("numSignaturesLabel");
                numSignaturesValueLblStr = String.valueOf(metaInf.getNumSignatures());
                break;
            case CLAIM_EVENT:
                eventPathPart = "/eventVSClaim";
                numSignaturesLblStr = ContextVS.getInstance().getMessage("numSignaturesLabel");
                numSignaturesValueLblStr = String.valueOf(metaInf.getNumSignatures());
                break;
            case VOTING_EVENT:
                eventPathPart = "/eventVSElection";
                numSignaturesLblStr = ContextVS.getInstance().getMessage("numVotesVSLabel");
                numSignaturesValueLblStr = String.valueOf(metaInf.getNumVotes());
                break;
        }
        if(eventPathPart != null) {
            metaInfURL = metaInfURL.concat(eventPathPart);
            if(metaInf.getId() != null) metaInfURL = metaInfURL.concat("/"+ metaInf.getId());
        }

        setLayout(new MigLayout("fill"));
        JLabel subjectLabel = createBoldLabel(ContextVS.getMessage("subjectLbl") + ": ");
        add(subjectLabel);
        JLabel subjectValueLabel = new JLabel(metaInf.getSubject());
        add(subjectValueLabel, "wrap");

        JLabel dateInitLabel = createBoldLabel(ContextVS.getMessage("dateInitLbl") + ": ");
        add(dateInitLabel);
        JLabel dateInitValueLabel = new JLabel(DateUtils.getShortStringFromDate(metaInf.getDateInit()));
        add(dateInitValueLabel, "");

        JLabel dateFinishLabel = createBoldLabel(ContextVS.getMessage("dateFinishLbl") + ": ");
        add(dateFinishLabel);
        JLabel dateFinishValueLabel = new JLabel(DateUtils.getShortStringFromDate(metaInf.getDateFinish()));
        add(dateFinishValueLabel, "wrap");

        JLabel numSignaturesLabel = createBoldLabel(numSignaturesLblStr + ": ");
        add(numSignaturesLabel);
        JLabel numSignaturesValueLabel = new JLabel(numSignaturesValueLblStr);
        add(dateInitValueLabel, "wrap");

        JLabel numAccessRequestsLabel = createBoldLabel(ContextVS.getInstance().getMessage("accessRequestLabel") +": ");
        add(numAccessRequestsLabel);
        JLabel numAccessRequestsValueLabel = new JLabel(String.valueOf(metaInf.getNumAccessRequest()));
        add(numAccessRequestsValueLabel, "wrap");

        JScrollPane contentScrollPane = new JScrollPane();
        JEditorPane contentPane = new JEditorPane();
        contentPane.setEditable(false);
        contentPane.setContentType("text/html");
        contentPane.setBackground(java.awt.Color.white);
        contentPane.setText(metaInf.getFormattedInfo());
        contentScrollPane.setViewportView(contentPane);
        add(contentScrollPane, "grow, wrap");

        final URI uriToOpen = new URI(metaInfURL);
        JButton webInfoButton = new JButton(ContextVS.getMessage("webInfoButtonLbl"));
        webInfoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { open(uriToOpen);}
        });
        add(webInfoButton, "");


        if(TypeVS.VOTING_EVENT == metaInf.getType()) {
            JButton representativesButton = new JButton(ContextVS.getMessage("representativesDetailsLbl"));
            representativesButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) { showRepresentativeDetails();}
            });
            add(representativesButton, "");
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
