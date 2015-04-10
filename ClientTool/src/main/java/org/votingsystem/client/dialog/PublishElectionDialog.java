package org.votingsystem.client.dialog;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.votingsystem.client.Browser;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.Browser.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PublishElectionDialog implements AddVoteOptionDialog.Listener {

    private static Logger log = Logger.getLogger(PublishElectionDialog.class.getSimpleName());

    private static Stage stage;
    private OperationVS operationVS;

    private Set<String> optionSet;

    @FXML private TextField caption;
    @FXML private DatePicker datePicker;
    @FXML private Button addOptionButton;
    @FXML private Button publishButton;
    @FXML private HTMLEditor editor;
    @FXML private VBox optionsVBox;

    public PublishElectionDialog(OperationVS operationVS) {
        this.operationVS = operationVS;
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        optionSet = new HashSet<>();
        caption.setPromptText(ContextVS.getMessage("electionSubjectLbl"));
        addOptionButton.setText(ContextVS.getMessage("addVoteOptionLbl"));
        addOptionButton.setOnAction(actionEvent -> {
            AddVoteOptionDialog.show(this);
        });
        publishButton.setText(ContextVS.getMessage("publishElectionLbl"));
        publishButton.setOnAction(actionEvent -> {
            submitForm();
        });
        publishButton.setVisible(true);
        editor.setHtmlText("<html><body></body></html>");
    }

    private void submitForm(){
        try {
            if("".equals(caption.getText().trim())) {
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterSubjectLbl"));
                return;
            }
            if(datePicker.getValue() == null) {
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterDateLbl"));
                return;
            }
            if(StringUtils.isHTMLEmpty(editor.getHtmlText().trim())) {
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterDataLbl"));
            }
            LocalDate isoDate = datePicker.getValue();
            Instant instant = isoDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            Date dateBegin = Date.from(instant);
            Map mapToSign = new HashMap<>();
            mapToSign.put("subject", caption.getText().trim());
            mapToSign.put("content", editor.getHtmlText());
            mapToSign.put("dateBegin", dateBegin);
            List<Map> optionList = new ArrayList<>();
            for(String option : optionSet) {
                Map optionMap = new HashMap<>();
                optionMap.put("content", option);
                optionList.add(optionMap);
            }
            mapToSign.put("fieldsEventVS", optionList);
            operationVS.setDocumentToSignMap(mapToSign);
            Browser.getInstance().processOperationVS(operationVS, null);
            stage.hide();
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static void show(OperationVS operationVS, Window owner) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    PublishElectionDialog dialog = new PublishElectionDialog(operationVS);
                    if (stage == null) {
                        stage = new Stage(StageStyle.DECORATED);
                        stage.initOwner(owner);
                        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
                    }
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/PublishVoteEditor.fxml"));
                    fxmlLoader.setController(dialog);
                    stage.setScene(new Scene(fxmlLoader.load()));
                    stage.getScene().setFill(null);
                    Utils.addMouseDragSupport(stage);
                    stage.centerOnScreen();
                    stage.toFront();
                    stage.show();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        });
    }

    @Override public void addOption(String optionContent) {
        optionSet.add(optionContent);
        HBox optionHBox = new HBox();
        Button removeOptionButton = new Button(ContextVS.getMessage("deleteLbl"));
        removeOptionButton.setGraphic(Utils.getIcon(FontAwesomeIconName.TIMES, Utils.COLOR_RED_DARK));
        removeOptionButton.setOnAction(actionEvent -> {
            removeOption(optionHBox, optionContent);
        });
        Label optionContentLabel = new Label(optionContent);
        optionContentLabel.setStyle("-fx-font-size: 1.2em; -fx-font-weight: bold; -fx-font-style: italic; -fx-padding: 0 0 0 10;");
        optionHBox.getChildren().addAll(removeOptionButton, optionContentLabel);
        optionHBox.setStyle("-fx-padding: 10 10 0 10;");
        optionsVBox.getChildren().add(optionHBox);
        if(optionSet.size() >= 2) publishButton.setVisible(true);
        else publishButton.setVisible(false);
    }

    private void removeOption(HBox optionRemovedHBox, String optionRemoved) {
        optionSet.remove(optionRemoved);
        optionsVBox.getChildren().removeAll(optionRemovedHBox);
        if(optionSet.size() >= 2) publishButton.setVisible(true);
        else publishButton.setVisible(false);
    }

}