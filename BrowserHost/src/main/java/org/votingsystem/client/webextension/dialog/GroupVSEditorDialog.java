package org.votingsystem.client.webextension.dialog;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.web.HTMLEditor;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.service.EventBusService;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.util.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class GroupVSEditorDialog extends DialogVS implements AddTagVSDialog.Listener {

    private static Logger log = Logger.getLogger(GroupVSEditorDialog.class.getSimpleName());

    private OperationVS operationVS;
    private TagVS tagVS;
    private Long groupId;
    @FXML private Label adviceLbl;
    @FXML private TextField caption;
    @FXML private Button addTagVSButton;
    @FXML private Button tagVSButton;
    @FXML private Button publishButton;
    @FXML private HTMLEditor editor;
    private static GroupVSEditorDialog INSTANCE;

    static class OperationVSListener {
        @Subscribe public void call(ResponseVS responseVS) {
            switch(responseVS.getType()) {
                case CANCELED:
                    if(responseVS.getData() instanceof OperationVS) {
                        OperationVS operationVS = (OperationVS) responseVS.getData();
                        if((operationVS.getType() == TypeVS.CURRENCY_GROUP_NEW ||
                                operationVS.getType() == TypeVS.CURRENCY_GROUP_EDIT) && operationVS != null) {
                            show(operationVS);
                        }
                    }
                    break;
            }
        }
    }

    public GroupVSEditorDialog() throws IOException {
        super("/fxml/GroupVSEditor.fxml");
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        adviceLbl.setWrapText(true);
        adviceLbl.setText(" - " + ContextVS.getMessage("newGroupVSAdviceMsg3") + "\n" +
                " - " + ContextVS.getMessage("signatureRequiredMsg") + "\n" +
                " - " + ContextVS.getMessage("newGroupVSAdviceMsg2"));
        caption.setPromptText(ContextVS.getMessage("groupVSNameLbl"));
        publishButton.setOnAction(actionEvent -> {
            submitForm();
        });
        addTagVSButton.setText(ContextVS.getMessage("addTagVSLbl"));
        addTagVSButton.setStyle("-fx-min-width: 100px;");
        addTagVSButton.setOnAction(actionEvent -> {
            AddTagVSDialog.show(ContextVS.getMessage("addGroupVSTagMsg"), this);
        });
        tagVSButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        tagVSButton.setOnAction(actionEvent -> {
            tagVS = null;
            tagVSButton.setVisible(false);
        });
        publishButton.setVisible(true);
        publishButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK, Utils.COLOR_RESULT_OK));
        editor.setHtmlText("<html><body></body></html>");
    }

    @Override
    public void addTagVS(String tagName) {
        tagVSButton.setVisible(true);
        tagVSButton.setText(tagName);
        tagVS = new TagVS(tagName);
    }

    private void loadOperationData(OperationVS operationVS) {
        log.info("loadOperationData - type: " + operationVS.getType());
        this.operationVS = operationVS;
        caption.setText(null);
        editor.setHtmlText("<html><body></body></html>");
        tagVS = null;
        groupId = null;
        tagVSButton.setVisible(false);
        publishButton.setText(ContextVS.getMessage("saveLbl"));
        switch (operationVS.getType()) {
            case CURRENCY_GROUP_EDIT:
                ProgressDialog.show(new FetchGroupVSDataTask(operationVS.getMessage()),
                        ContextVS.getMessage("editGroupVSLbl"), null);
                setCaption(ContextVS.getMessage("editGroupVSLbl"));
                addTagVSButton.setVisible(false);
                tagVSButton.setDisable(true);
                caption.setEditable(false);
                break;
            case CURRENCY_GROUP_NEW:
                ProgressDialog.show(new FetchGroupVSDataTask(operationVS.getNif()),
                        ContextVS.getMessage("newGroupVSLbl"), null);
                setCaption(ContextVS.getMessage("newGroupVSLbl"));
                addTagVSButton.setVisible(true);
                tagVSButton.setVisible(false);
                tagVSButton.setDisable(false);
                caption.setEditable(true);
                break;
        }
        if(operationVS.getJsonStr() != null) {
            try {
                GroupVSDto dto = operationVS.getData(GroupVSDto.class);
                groupId = dto.getId();
                caption.setText(dto.getName());
                editor.setHtmlText(new String(Base64.getDecoder().decode(dto.getDescription()), StandardCharsets.UTF_8));
                addTagVS(dto.getTags().iterator().next().getName());
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        show();
    }

    private void submitForm(){
        try {
            if(caption.getText() == null || "".equals(caption.getText().trim())) {
                BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterGroupVSNameMsg"));
                return;
            }
            GroupVSDto dto = new GroupVSDto();
            dto.setOperation(operationVS.getOperation());
            dto.setId(groupId);
            dto.setName(caption.getText().trim());
            dto.setDescription(Base64.getEncoder().encodeToString(editor.getHtmlText().getBytes()));
            dto.setUUID(UUID.randomUUID().toString());
            if(tagVS != null) dto.setTags(new HashSet<>(Arrays.asList(tagVS)));
            operationVS.setJsonStr(JSON.getMapper().writeValueAsString(dto));
            operationVS.setSignedMessageSubject(ContextVS.getMessage("newGroupVSLbl"));
            operationVS.setServiceURL(ContextVS.getInstance().getCurrencyServer().getSaveGroupVSServiceURL());
            operationVS.setCallerCallback(null);
            operationVS.processOperationWithPassword(ContextVS.getMessage("newGroupVSLbl"));
            hide();
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    public static void show(OperationVS operationVS) {
        Platform.runLater(() -> {
            try {
                if(INSTANCE == null) {
                    INSTANCE =  new GroupVSEditorDialog();
                    EventBusService.getInstance().register(new OperationVSListener());
                }
                INSTANCE.loadOperationData(operationVS);
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

    public class FetchGroupVSDataTask extends Task<ResponseVS> {

        private String id;

        public FetchGroupVSDataTask(String groupId) {
            this.id = groupId;
        }

        @Override protected ResponseVS call() throws Exception {
            updateProgress(1, 10);
            updateMessage(ContextVS.getMessage("editGroupVSLbl"));
            String serviceURL = ContextVS.getInstance().getCurrencyServer().getUserVSURL(Long.valueOf(id));
            updateProgress(3, 10);
            ResponseVS responseVS = HttpHelper.getInstance().getData(serviceURL, ContentTypeVS.JSON);
            updateProgress(8, 10);
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                BrowserHost.showMessage(responseVS.getStatusCode(), responseVS.getMessage());
            } else {
                GroupVSDto dto = (GroupVSDto) responseVS.getMessage(GroupVSDto.class);
                groupId = dto.getId();
                String description = new String(Base64.getDecoder().decode(dto.getDescription()), StandardCharsets.UTF_8);
                Platform.runLater(() -> {
                    editor.setHtmlText(description);
                    caption.setText(dto.getName());
                    addTagVS(dto.getTags().iterator().next().getName());
                } );
            }
            return responseVS;
        }
    }
}