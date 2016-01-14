package org.votingsystem.client.webextension.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.util.ContextVS;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertNotFoundDialog extends DialogVS {

    private static Logger log = Logger.getLogger(CertNotFoundDialog.class.getSimpleName());


    public CertNotFoundDialog() {
        super(new VBox(10));
        VBox mainDialog = (VBox) getContentPane();
        mainDialog.getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
        mainDialog.getStyleClass().add("modal-dialog");
        Label messageLabel = new Label(ContextVS.getMessage("newUserMsg"));
        messageLabel.setStyle("-fx-font-size: 16;-fx-text-fill: #888;");
        messageLabel.setWrapText(true);
        Button importCertButton = Utils.createButton(ContextVS.getMessage("importCertLbl"), Utils.getIcon(FontAwesome.Glyph.CHECK));
        importCertButton.setOnAction(actionEvent -> {
            Utils.selectKeystoreFile(null);
            hide();
        });
        Button requestCertButton = Utils.createButton(ContextVS.getMessage("requestCertLbl"), Utils.getIcon(FontAwesome.Glyph.TIMES));
        requestCertButton.setOnAction(actionEvent -> {
                BrowserHost.sendMessageToBrowser(MessageDto.NEW_TAB(
                        ContextVS.getInstance().getAccessControl().getCertRequestServiceURL()));
                hide();
            } );
        HBox footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), importCertButton, requestCertButton);
        mainDialog.getChildren().addAll(messageLabel, footerButtonsBox);
    }

    public static void showDialog() {
        PlatformImpl.runLater(() -> {
            new CertNotFoundDialog().show();
        });
    }

}