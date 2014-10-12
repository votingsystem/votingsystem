package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;
import org.votingsystem.client.model.SignedFile;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;

import java.util.Set;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SMIMESignersPane extends GridPane {

    private static Logger log = Logger.getLogger(SMIMESignersPane.class);
    private TabPane tabPane;

    public SMIMESignersPane(SignedFile signedFile) {
        tabPane = new TabPane();
        tabPane.setRotateGraphic(false);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        tabPane.setSide(Side.TOP);
        setStyle("-fx-max-width: 900px;");

        Button closeButton = new Button(ContextVS.getMessage("closeLbl"));
        closeButton.setGraphic((new ImageView(Utils.getImage(this, "save_data"))));
        closeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                SMIMESignersPane.this.setVisible(false);
            }
        });

        Tab newTab = null;
        try {
            Set<UserVS> signersVS = signedFile.getSMIMEMessage().getSigners();
            log.debug("Num. signers: " + signersVS.size());
            for (UserVS signerVS:signersVS) {
                SignatureInfoPane signerVSPanel = new SignatureInfoPane(signerVS, signedFile.getSMIMEMessage());
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
            log.error(ex.getMessage(), ex);
        }
    }


    public static void showDialog(final SignedFile signedFile) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                Stage stage = new Stage();
                stage.initModality(Modality.WINDOW_MODAL);
                //stage.initOwner(window);

                stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
                    @Override public void handle(WindowEvent window) { }
                });
                SMIMESignersPane SMIMESignersPane = new SMIMESignersPane(signedFile);
                stage.setScene(new Scene(SMIMESignersPane));
                stage.setTitle(ContextVS.getMessage("signersLbl"));
                stage.centerOnScreen();
                stage.show();
            }
        });
    }


}
