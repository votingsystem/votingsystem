package org.votingsystem.client.pane;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.dialog.SettingsDialog;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MainOptionsPane extends VBox {

    private static Logger log = Logger.getLogger(MainOptionsPane.class);

    private VBox votingSystemOptionsBox;
    private VBox cooinOptionsBox;
    private HBox headerButtonsBox;

    public MainOptionsPane() {
        headerButtonsBox = new HBox(20);
        VBox.setMargin(headerButtonsBox, new Insets(0, 0, 20, 0));
        ChoiceBox documentChoiceBox = new ChoiceBox();
        documentChoiceBox.setPrefWidth(150);
        final String[] documentOptions = new String[]{ContextVS.getMessage("documentsLbl"),
                ContextVS.getMessage("openFileButtonLbl"),
                ContextVS.getMessage("signDocumentButtonLbl")};
        documentChoiceBox.getItems().addAll(documentOptions);
        documentChoiceBox.getSelectionModel().selectFirst();
        documentChoiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue ov, Number value, Number new_value) {
                log.debug("value: " + value + " -new_value: " + new_value + " - option: " + documentOptions[new_value.intValue()]);
                String selectedOption = documentOptions[new_value.intValue()];
                if(ContextVS.getMessage("openFileButtonLbl").equals(selectedOption)) {
                    Platform.runLater(() -> BrowserVS.getInstance().showDocumentVS(null, null, null));
                } else if(ContextVS.getMessage("signDocumentButtonLbl").equals(selectedOption)) {
                    SignDocumentFormPane.showDialog();
                }
                documentChoiceBox.getSelectionModel().select(0);
            }
        });
        votingSystemOptionsBox = new VBox(15);
        Button voteButton = new Button(ContextVS.getMessage("voteButtonLbl"));
        voteButton.setGraphic(Utils.getImage(FontAwesome.Glyph.ENVELOPE));
        voteButton.setOnAction(event -> {
            openVotingSystemURL(ContextVS.getInstance().getAccessControl().getVotingPageURL(),
                    ContextVS.getMessage("voteButtonLbl"));
        });
        voteButton.setPrefWidth(500);

        Button selectRepresentativeButton = new Button(ContextVS.getMessage("selectRepresentativeButtonLbl"));
        selectRepresentativeButton.setGraphic(Utils.getImage(FontAwesome.Glyph.HAND_ALT_RIGHT));
        selectRepresentativeButton.setOnAction(event -> {
            openVotingSystemURL(ContextVS.getInstance().getAccessControl().getSelectRepresentativePageURL(),
                    ContextVS.getMessage("selectRepresentativeButtonLbl"));
        });
        selectRepresentativeButton.setPrefWidth(500);
        votingSystemOptionsBox.getChildren().addAll(voteButton, selectRepresentativeButton);
        cooinOptionsBox = new VBox(15);
        Button cooinUsersProceduresButton = new Button(ContextVS.getMessage("financesLbl"));
        cooinUsersProceduresButton.setGraphic(Utils.getImage(FontAwesome.Glyph.BAR_CHART_ALT));
        cooinUsersProceduresButton.setOnAction(event -> {
            openCooinURL(ContextVS.getInstance().getCooinServer().getUserDashBoardURL(),
                    ContextVS.getMessage("cooinUsersLbl"));
        });
        cooinUsersProceduresButton.setPrefWidth(500);
        Button walletButton = new Button(ContextVS.getMessage("walletLbl"));
        walletButton.setGraphic(Utils.getImage(FontAwesome.Glyph.MONEY));
        walletButton.setPrefWidth(500);
        walletButton.setOnAction(event -> {
            openCooinURL(ContextVS.getInstance().getCooinServer().getWalletURL(),
                    ContextVS.getMessage("walletLbl"));
        });
        cooinOptionsBox.getChildren().addAll(cooinUsersProceduresButton, walletButton);
        cooinOptionsBox.setStyle("-fx-alignment: center;");
        ChoiceBox adminChoiceBox = new ChoiceBox();
        adminChoiceBox.setPrefWidth(180);
        final String[] adminOptions = new String[]{ContextVS.getMessage("adminLbl"),
                ContextVS.getMessage("settingsLbl"),
                ContextVS.getMessage("cooinAdminLbl"),
                ContextVS.getMessage("votingSystemProceduresLbl")};
        adminChoiceBox.getItems().addAll(adminOptions);
        adminChoiceBox.getSelectionModel().selectFirst();
        adminChoiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue ov, Number value, Number new_value) {
                log.debug("value: " + value + " - new_value: " + new_value + " - option: " +
                        adminOptions[new_value.intValue()]);
                String selectedOption = adminOptions[new_value.intValue()];
                if(ContextVS.getMessage("settingsLbl").equals(selectedOption)) SettingsDialog.showDialog();
                else if(ContextVS.getMessage("cooinAdminLbl").equals(selectedOption)) {
                    openCooinURL(ContextVS.getInstance().getCooinServer().getAdminDashBoardURL(),
                            ContextVS.getMessage("cooinAdminLbl"));
                } else if(ContextVS.getMessage("votingSystemProceduresLbl").equals(selectedOption)) {
                    openVotingSystemURL(ContextVS.getInstance().getAccessControl().getDashBoardURL(),
                            ContextVS.getMessage("votingSystemProceduresLbl"));
                }
                adminChoiceBox.getSelectionModel().select(0);
            }
        });
        getChildren().add(0, headerButtonsBox);
        headerButtonsBox.getChildren().addAll(adminChoiceBox, documentChoiceBox);
        getStyleClass().add("modal-dialog");
        setPrefWidth(550);
    }

    private void openVotingSystemURL(final String URL, final String caption) {
        log.debug("openVotingSystemURL: " + URL);
        if(ContextVS.getInstance().getAccessControl() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"), ContextVS.getMessage("errorLbl"));
            return;
        }
        Platform.runLater(() -> BrowserVS.getInstance().newTab(URL, caption, null));
    }

    private void openCooinURL(final String URL, final String caption) {
        log.debug("openCooinURL: " + URL);
        if(ContextVS.getInstance().getCooinServer() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"), ContextVS.getMessage("errorLbl"));
            return;
        }
        Platform.runLater(() -> BrowserVS.getInstance().newTab(URL, caption, null));
    }

    public void setCooinServerAvailable(final boolean available) {
        PlatformImpl.runLater(() -> {
            if (available) getChildren().add((getChildren().size() - 1), cooinOptionsBox);
            else if (getChildren().contains(cooinOptionsBox)) getChildren().remove(cooinOptionsBox);
            //getScene().getWindow().sizeToScene();
        });
    }

    public void setVotingSystemAvailable(final boolean available) {
        PlatformImpl.runLater(() -> {
            if(available) getChildren().add(1, votingSystemOptionsBox);
            else if(getChildren().contains(votingSystemOptionsBox)) getChildren().remove(votingSystemOptionsBox);
            //getScene().getWindow().sizeToScene();
        });
    }
}
