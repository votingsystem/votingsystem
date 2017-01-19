package org.votingsystem.client.pane;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.util.Formatter;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.voting.ElectionStatsDto;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Messages;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ElectionInfoPane extends VBox {

    private static Logger log = Logger.getLogger(ElectionInfoPane.class.getName());

    private ElectionStatsDto electionStats;
    private Button validateBackupButton;

    public ElectionInfoPane(final ElectionStatsDto electionStats, String decompressedBackupBaseDir) throws Exception {
        this.electionStats = electionStats;
        Label subjectLabel = new Label(Messages.currentInstance().get("subjectLbl") + ": ");
        subjectLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 1.3em;-fx-font-style: italic;");
        Label subjectValueLabel = new Label(electionStats.getSubject());
        subjectValueLabel.setStyle("-fx-font-size: 1.3em;-fx-font-style: italic;");

        HBox subjectHBox = new HBox();
        subjectHBox.getChildren().addAll(subjectLabel, subjectValueLabel);
        VBox.setMargin(subjectHBox, new Insets(5, 15, 10, 15));

        HBox dateBeginHBox = new HBox();
        Label dateBeginLabel = new Label(Messages.currentInstance().get("dateBeginLbl") + ": ");
        dateBeginLabel.setStyle("-fx-font-weight: bold;");
        Label dateBeginValueLabel = new Label(DateUtils.getDateStr(electionStats.getDateBegin()));
        dateBeginHBox.getChildren().addAll(dateBeginLabel, dateBeginValueLabel);
        VBox.setMargin(dateBeginHBox, new Insets(10, 15, 10, 15));

        WebView webView = new WebView();
        webView.getEngine().loadContent(Formatter.formatElectionStats(electionStats));
        Utils.browserLinkListener(webView);


        validateBackupButton = new Button(Messages.currentInstance().get("validateBackupLbl"));
        validateBackupButton.setOnAction(actionEvent -> BackupValidatorPane.validateBackup(decompressedBackupBaseDir,
                electionStats, getScene().getWindow()));
        validateBackupButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.FILE_TEXT_ALT));
        HBox.setMargin(validateBackupButton, new Insets(0, 0, 0, 10));
        dateBeginHBox.getChildren().add(validateBackupButton);
        this.getChildren().addAll(subjectHBox, dateBeginHBox, webView);
        HBox.setHgrow(this, Priority.ALWAYS);
        VBox.setVgrow(this, Priority.ALWAYS);
        VBox.setVgrow(webView, Priority.ALWAYS);
    }

}