package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.dialog.AppDialog;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.Signature;
import org.votingsystem.model.User;
import org.votingsystem.util.Messages;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignersPane extends GridPane {

    private static Logger log = Logger.getLogger(SignersPane.class.getName());

    private TabPane tabPane;

    public SignersPane(Set<Signature> signatures) {
        tabPane = new TabPane();
        tabPane.setRotateGraphic(false);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        tabPane.setSide(Side.TOP);
        setStyle("-fx-max-width: 900px;");

        Button closeButton = new Button(Messages.currentInstance().get("closeLbl"));
                closeButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.SAVE)));
        closeButton.setOnAction(actionEvent -> SignersPane.this.setVisible(false));
        Tab newTab = null;
        try {
            log.info("Num. signers: " + signatures.size());
            for (Signature signature : signatures) {
                User signer = User.FROM_X509_CERT(signature.getSigningCert());
                SignatureInfoPane signerVSPanel = new SignatureInfoPane(signature);
                String tabName = Messages.currentInstance().get("signerLbl");
                if(signer.getNumId() != null)
                    tabName = signer.getNumIdAndType();
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

    public static void showDialog(final Set<Signature> signatures) {
        Platform.runLater(() -> {
            SignersPane signersPane = new SignersPane(signatures);
            new AppDialog(signersPane).show();
        });
    }

}
