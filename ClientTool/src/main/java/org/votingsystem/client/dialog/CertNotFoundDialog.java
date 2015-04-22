package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcons;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import org.votingsystem.client.Browser;
import org.votingsystem.client.pane.DecoratedPane;
import org.votingsystem.client.util.Utils;
import org.votingsystem.util.ContextVS;

import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertNotFoundDialog extends VBox {

    private static Logger log = Logger.getLogger(CertNotFoundDialog.class.getSimpleName());


    public CertNotFoundDialog() {
        setPrefWidth(500);
        Label messageLabel = new Label(ContextVS.getMessage("newUserMsg"));
        messageLabel.setStyle("-fx-font-size: 16;-fx-text-fill: #888;");
        messageLabel.setWrapText(true);
        Button importCertButton = new Button(ContextVS.getMessage("importCertLbl"));
        importCertButton.setOnAction(actionEvent -> {
            Utils.selectKeystoreFile(null, Browser.getInstance());
            getScene().getWindow().hide();
        });
        importCertButton.setGraphic(Utils.getIcon(FontAwesomeIcons.CHECK));
        Button requestCertButton = new Button(ContextVS.getMessage("requestCertLbl"));
        requestCertButton.setOnAction(actionEvent -> {
                Browser.getInstance().openVotingSystemURL(
                        ContextVS.getInstance().getAccessControl().getCertRequestServiceURL(),
                        ContextVS.getMessage("requestCertLbl"));
                getScene().getWindow().hide();
            } );
        requestCertButton.setGraphic(Utils.getIcon(FontAwesomeIcons.TIMES));
        HBox footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), importCertButton, requestCertButton);
        setStyle("-fx-padding: 30;-fx-spacing: 20;-fx-alignment: center;-fx-background-color: #fff;");
        getChildren().addAll(messageLabel, footerButtonsBox);
    }

    public static void show(Window owner) {
        PlatformImpl.runLater(() -> {
            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(true);
            stage.initOwner(owner);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
            stage.setScene(new Scene(new DecoratedPane(ContextVS.getMessage("newUserLbl"), null, new CertNotFoundDialog(), stage)));
            stage.centerOnScreen();
            stage.show();
            stage.toFront();
        });
    }

}