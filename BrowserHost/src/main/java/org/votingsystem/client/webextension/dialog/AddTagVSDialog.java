package org.votingsystem.client.webextension.dialog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.javafx.application.PlatformImpl;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;

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
    //private TextField textField;
    private Listener listener;
    private ListView<String> tagListView;
    private ObservableList<String> data;
    private static AddTagVSDialog INSTANCE;

    public AddTagVSDialog() {
        super(new VBox(10));
        setCaption(ContextVS.getMessage("addTagVSLbl"));
        mainPane = (VBox) getContentPane();
        data = FXCollections.observableArrayList();
        tagListView = new ListView<>(data);
        /*textField = new TextField();
        textField.setPromptText(ContextVS.getMessage("searchTextLbl"));
        HBox.setHgrow(textField, Priority.ALWAYS);
        Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"),Utils.getIcon(FontAwesome.Glyph.CHECK));
        acceptButton.setOnAction(actionEvent -> {
            if ("".equals(textField.getText().trim())) {
                BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("emptyFieldErrorMsg",
                        ContextVS.getMessage("searchTextLbl")));
                return;
            }
            try {
                ProgressDialog.show(new TagVSLoader(null), ContextVS.getMessage("addTagVSLbl"));
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
        textField.setOnKeyPressed(ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) { acceptButton.fire(); }
        });
        HBox footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), acceptButton);*/
        tagListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if(newValue != null) listener.addTagVS(newValue);
                hide();
            }
        });
        tagListView.setMaxHeight(300);
        mainPane.getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
        mainPane.getStyleClass().add("modal-dialog");
        mainPane.setStyle("-fx-pref-width: 400px;");
        mainPane.getChildren().add(tagListView);
    }

    private void loadTags() {
        try {
            ProgressDialog.show(new TagVSLoader(null), ContextVS.getMessage("addTagVSLbl"));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            String targetURL = ContextVS.getInstance().getCurrencyServer().getTagVSServiceURL();
            ResponseVS responseVS  = HttpHelper.getInstance().getData(targetURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                ResultListDto<TagVSDto> resultListDto = (ResultListDto<TagVSDto>) responseVS.getMessage(
                        new TypeReference<ResultListDto<TagVSDto>>() { });
                PlatformImpl.runLater(() -> {
                    data.clear();
                    for(TagVSDto tagVSDto : resultListDto.getResultList()) {
                        if(!TagVS.WILDTAG.equals(tagVSDto.getName().trim())) data.add(tagVSDto.getName().trim());
                    }
                    tagListView.setItems(data);
                    mainPane.getScene().getWindow().sizeToScene();
                });
            }
            return responseVS;
        }
    }

    public static void show(Listener listener) {
        PlatformImpl.runLater(() -> {
            if(INSTANCE == null) INSTANCE = new AddTagVSDialog();
            //INSTANCE.data.clear();
            INSTANCE.listener = listener;
            //INSTANCE.textField.setText("");
            if(INSTANCE.data.size() == 0) INSTANCE.loadTags();
            INSTANCE.show();
        });
    }

}