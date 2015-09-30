package org.votingsystem.client.dialog;

import com.google.common.eventbus.Subscribe;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Window;
import org.votingsystem.client.Browser;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.voting.FieldEventVS;
import org.votingsystem.service.EventBusService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.Browser.showMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ElectionEditorDialog extends DialogVS implements AddVoteOptionDialog.Listener {

    private static Logger log = Logger.getLogger(ElectionEditorDialog.class.getSimpleName());

    private OperationVS operationVS;

    private Set<String> optionSet;

    @FXML private TextField caption;
    @FXML private DatePicker datePicker;
    @FXML private Button addOptionButton;
    @FXML private Button publishButton;
    @FXML private HTMLEditor editor;
    @FXML private VBox optionsVBox;

    static class OperationVSListener {
        @Subscribe public void responseVSChange(ResponseVS responseVS) {
            switch(responseVS.getType()) {
                case CANCELED:
                    if(responseVS.getData() instanceof OperationVS) {
                        OperationVS operationVS = (OperationVS) responseVS.getData();
                        if(operationVS.getType() == TypeVS.VOTING_PUBLISHING && operationVS != null) {
                            show(operationVS, null);
                        }
                    }
                    break;
            }
        }
    }

    static {
        EventBusService.getInstance().register(new OperationVSListener());
    }

    public ElectionEditorDialog(OperationVS operationVS) throws IOException {
        super("/fxml/PublishVoteEditor.fxml", ContextVS.getMessage("publishElectionLbl"));
        setOperationVS(operationVS);
    }

    @FXML void initialize() {
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
        //publishButton.setVisible(false);
    }

    private void setOperationVS(OperationVS operationVS) {
        this.operationVS = operationVS;
        if(operationVS.getJsonStr() != null) {
            try {
                EventVSDto eventVSDto = operationVS.getDocumentToSign(EventVSDto.class);
                caption.setText(eventVSDto.getSubject());
                datePicker.setValue(eventVSDto.getDateBegin().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                editor.setHtmlText(new String(Base64.getDecoder().decode(eventVSDto.getContent()), StandardCharsets.UTF_8));
                for(FieldEventVS fieldEventVS : eventVSDto.getFieldsEventVS()) {
                    addOption(fieldEventVS.getContent());
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
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
            if(optionSet.size() < 2) {
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("missingOptionsErrorMsg"));
                return;
            }
            LocalDate isoDate = datePicker.getValue();
            Instant instant = isoDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            Date dateBegin = Date.from(instant);
            if(dateBegin.before(new Date())) {
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("dateBeforeCurrentErrorMsg"));
                return;
            }
            EventVSDto eventVSDto = new EventVSDto();
            eventVSDto.setSubject(caption.getText().trim());
            //this is to allow parsing json html fields with javascript
            eventVSDto.setContent(Base64.getEncoder().encodeToString(editor.getHtmlText().getBytes()));
            eventVSDto.setDateBegin(dateBegin);
            Set<FieldEventVS> fieldEventVSet = new HashSet<>();
            for(String option : optionSet) {
                FieldEventVS fieldEventVS = new FieldEventVS();
                fieldEventVS.setContent(option);
                fieldEventVSet.add(fieldEventVS);
            }
            eventVSDto.setFieldsEventVS(fieldEventVSet);
            operationVS.setJsonStr(JSON.getMapper().writeValueAsString(eventVSDto));
            operationVS.setSignedMessageSubject(ContextVS.getMessage("publishElectionLbl"));
            operationVS.setServiceURL(ContextVS.getInstance().getAccessControl().getPublishElectionURL());
            operationVS.setCallerCallback(null);
            hide();
            Browser.getInstance().processOperationVS(operationVS, null);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static void show(OperationVS operationVS, Window owner) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    ElectionEditorDialog dialog = new ElectionEditorDialog(operationVS);
                    dialog.show();
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
        removeOptionButton.setStyle("-fx-min-width:100px;");
        removeOptionButton.setGraphic(Utils.getIcon(FontAwesomeIcon.TIMES, Utils.COLOR_RED_DARK));
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
        getStage().sizeToScene();
    }

    private void removeOption(HBox optionRemovedHBox, String optionRemoved) {
        optionSet.remove(optionRemoved);
        optionsVBox.getChildren().removeAll(optionRemovedHBox);
        if(optionSet.size() >= 2) publishButton.setVisible(true);
        else publishButton.setVisible(false);
        getStage().sizeToScene();
    }

}