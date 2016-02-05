package org.votingsystem.client.webextension.pane;

import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.dialog.DialogVS;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.SignedFile;
import org.votingsystem.util.ContextVS;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SMIMESignersPane extends GridPane {

    private static Logger log = Logger.getLogger(SMIMESignersPane.class.getName());
    private TabPane tabPane;

    public SMIMESignersPane(SignedFile signedFile) {
        tabPane = new TabPane();
        tabPane.setRotateGraphic(false);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        tabPane.setSide(Side.TOP);
        setStyle("-fx-max-width: 900px;");

        Button closeButton = new Button(ContextVS.getMessage("closeLbl"));
                closeButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.SAVE)));
        closeButton.setOnAction(actionEvent -> SMIMESignersPane.this.setVisible(false));
        Tab newTab = null;
        try {
            Set<UserVS> signersVS = signedFile.getSMIME().getSigners();
            log.info("Num. signers: " + signersVS.size());
            for (UserVS signerVS:signersVS) {
                SignatureInfoPane signerVSPanel = new SignatureInfoPane(signerVS, signedFile.getSMIME());
                String tabName = ContextVS.getMessage("signerLbl");
                if(signerVS.getNif() != null) tabName = signerVS.getNif();
                newTab = new Tab();
                newTab.setText(tabName);
                newTab.setContent(signerVSPanel);
                tabPane.getTabs().add(newTab);
            }
            add(tabPane, 0, 0);
            setHgrow(tabPane, Priority.ALWAYS);
            setVgrow(tabPane, Priority.ALWAYS);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }


    public static void showDialog(final SignedFile signedFile) {
        Platform.runLater(() -> {
            SMIMESignersPane signersPane = new SMIMESignersPane(signedFile);
            new DialogVS(signersPane).show();
        });
    }


}
