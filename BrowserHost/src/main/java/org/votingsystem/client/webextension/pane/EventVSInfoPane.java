package org.votingsystem.client.webextension.pane;

import com.sun.javafx.application.PlatformImpl;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.dialog.DialogVS;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.voting.MetaInf;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSInfoPane extends VBox {

    private static Logger log = Logger.getLogger(EventVSInfoPane.class.getName());

    private MetaInf metaInf = null;
    private Button validateBackupButton;

    public EventVSInfoPane(final MetaInf metaInf, String decompressedBackupBaseDir) throws Exception {
        this.metaInf = metaInf;
        Label subjectLabel = new Label(ContextVS.getMessage("subjectLbl") + ": ");
        subjectLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 1.3em;-fx-font-style: italic;");
        Label subjectValueLabel = new Label(metaInf.getSubject());
        subjectValueLabel.setStyle("-fx-font-size: 1.3em;-fx-font-style: italic;");

        HBox subjectHBox = new HBox();
        subjectHBox.getChildren().addAll(subjectLabel, subjectValueLabel);
        VBox.setMargin(subjectHBox, new Insets(5, 15, 10, 15));

        HBox dateBeginHBox = new HBox();
        Label dateBeginLabel = new Label(ContextVS.getMessage("dateBeginLbl") + ": ");
        dateBeginLabel.setStyle("-fx-font-weight: bold;");
        Label dateBeginValueLabel = new Label(DateUtils.getDayWeekDateStr(metaInf.getDateBegin(), "HH:mm"));
        dateBeginHBox.getChildren().addAll(dateBeginLabel, dateBeginValueLabel);
        VBox.setMargin(dateBeginHBox, new Insets(10, 15, 10, 15));

        WebView webView = new WebView();
        webView.getEngine().loadContent(metaInf.getFormattedInfo());
        Utils.browserVSLinkListener(webView);

        if(metaInf.getType() == TypeVS.VOTING_EVENT) {
            Button representativesButton = new Button(ContextVS.getMessage("representativesDetailsLbl"));
            representativesButton.setOnAction(actionEvent ->  {
                PlatformImpl.runLater(() -> {
                    new DialogVS(new HTMLPane(metaInf.getRepresentativesHTML()), null).setCaption(
                            ContextVS.getMessage("representativesDetailsLbl")).show();
                });
            });
            representativesButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.GROUP));
            dateBeginHBox.getChildren().addAll(Utils.getSpacer(), representativesButton);
        }
        validateBackupButton = new Button(ContextVS.getMessage("validateBackupLbl"));
        validateBackupButton.setOnAction(actionEvent -> BackupValidatorPane.validateBackup(decompressedBackupBaseDir,
                metaInf, getScene().getWindow()));
        validateBackupButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.FILE_TEXT_ALT));
        HBox.setMargin(validateBackupButton, new Insets(0, 0, 0, 10));
        dateBeginHBox.getChildren().add(validateBackupButton);
        this.getChildren().addAll(subjectHBox, dateBeginHBox, webView);
        HBox.setHgrow(this, Priority.ALWAYS);
        VBox.setVgrow(this, Priority.ALWAYS);
        VBox.setVgrow(webView, Priority.ALWAYS);
    }

}
