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
import org.votingsystem.model.User;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.crypto.SignedFile;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CMSSignersPane extends GridPane {

    private static Logger log = Logger.getLogger(CMSSignersPane.class.getName());
    private TabPane tabPane;

    public CMSSignersPane(SignedFile signedFile) {
        tabPane = new TabPane();
        tabPane.setRotateGraphic(false);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        tabPane.setSide(Side.TOP);
        setStyle("-fx-max-width: 900px;");

        Button closeButton = new Button(ContextVS.getMessage("closeLbl"));
                closeButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.SAVE)));
        closeButton.setOnAction(actionEvent -> CMSSignersPane.this.setVisible(false));
        Tab newTab = null;
        try {
            Set<User> signersVS = signedFile.getCMS().getSigners();
            log.info("Num. signers: " + signersVS.size());
            for (User signerVS:signersVS) {
                SignatureInfoPane signerVSPanel = new SignatureInfoPane(signerVS, signedFile.getCMS());
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
            CMSSignersPane signersPane = new CMSSignersPane(signedFile);
            new DialogVS(signersPane).show();
        });
    }


}
