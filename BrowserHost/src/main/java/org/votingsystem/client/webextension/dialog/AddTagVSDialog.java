package org.votingsystem.client.webextension.dialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.javafx.application.PlatformImpl;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AddTagVSDialog extends DialogVS {

    private static Logger log = Logger.getLogger(AddTagVSDialog.class.getSimpleName());

    public interface Listener {
        public void addTagVS(String tagName);
    }

    private VBox mainPane;
    private TextField textField;
    private Listener listener;
    private Label messageLabel;
    private ListView<String> tagListView;
    private ObservableList<String> data;
    private static AddTagVSDialog dialog;

    public AddTagVSDialog() {
        super(new VBox(10));
        mainPane = (VBox) getContentPane();
        data = FXCollections.observableArrayList();
        tagListView = new ListView<>(data);
        messageLabel = new Label();
        messageLabel.setWrapText(true);
        textField = new TextField();
        textField.setPromptText(ContextVS.getMessage("searchTextLbl"));
        HBox.setHgrow(textField, Priority.ALWAYS);
        Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(actionEvent -> {
            if ("".equals(textField.getText().trim())) {
                BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("emptyFieldErrorMsg",
                        ContextVS.getMessage("searchTextLbl")));
                return;
            }
            try {
                ProgressDialog.show(new TagVSLoader(textField.getText()), ContextVS.getMessage("sendingMoneyLbl"), null);
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
        textField.setOnKeyPressed(ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) {
                acceptButton.fire();
            }
        });
        acceptButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK));
        HBox footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), acceptButton);

        tagListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable,
                                String oldValue, String newValue) {
                if(newValue != null) listener.addTagVS(newValue);
                hide();
            }
        });
        tagListView.setMaxHeight(300);
        mainPane.getChildren().addAll(messageLabel, textField, footerButtonsBox);
        mainPane.getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
        mainPane.getStyleClass().add("modal-dialog");
        mainPane.setStyle("-fx-pref-width: 400px;");
    }

    public void showMessage(String title, Listener listener) throws JsonProcessingException {
        this.listener = listener;
        messageLabel.setText(title);
        textField.setText("");
        show();
    }

    public class TagVSLoader extends Task<ResponseVS> {

        private String searchParam;

        public TagVSLoader(String searchParam) throws Exception {
            this.searchParam = searchParam;
        }

        @Override protected ResponseVS call() throws Exception {
            updateProgress(1, 10);
            updateMessage(ContextVS.getMessage("addTagVSLbl"));
            updateProgress(3, 10);
            //String targetURL = ContextVS.getInstance().getCurrencyServer().getTagVSSearchServiceURL(searchParam);
            String targetURL = "http://currency:8080/CurrencyServer/rest/tagVS?tag=" + searchParam;
            ResponseVS responseVS  = HttpHelper.getInstance().getData(targetURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                ResultListDto<TagVSDto> resultListDto = (ResultListDto<TagVSDto>) responseVS.getMessage(
                        new TypeReference<ResultListDto<TagVSDto>>() { });
                PlatformImpl.runLater(() -> {
                    data.clear();
                    for(TagVSDto tagVSDto : resultListDto.getResultList()) {
                        data.add(tagVSDto.getName().trim());
                    }
                    tagListView.setItems(data);
                    if(!(mainPane.getChildren().get(2) instanceof ListView)) {
                        mainPane.getChildren().add(2, tagListView);
                        mainPane.getScene().getWindow().sizeToScene();
                    }
                });
            }
            return responseVS;
        }
    }

    public static void show(String message, Listener listener) {
        PlatformImpl.runLater(() -> {
            try {
                if(dialog == null) dialog = new AddTagVSDialog();
                dialog.data.clear();
                if(dialog.mainPane.getChildren().get(2) instanceof ListView) {
                    dialog.mainPane.getChildren().remove(2);
                }
                dialog.showMessage(message, listener);
            } catch (JsonProcessingException ex) {
               log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

}