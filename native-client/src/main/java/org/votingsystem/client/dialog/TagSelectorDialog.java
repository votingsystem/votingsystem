package org.votingsystem.client.dialog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.javafx.application.PlatformImpl;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import org.votingsystem.client.MainApp;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TagDto;
import org.votingsystem.http.ContentType;
import org.votingsystem.http.HttpConn;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.Messages;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TagSelectorDialog extends AppDialog {

    private static Logger log = Logger.getLogger(TagSelectorDialog.class.getName());

    public interface Listener {
        public void addTagVS(String tagName);
    }

    private VBox mainPane;
    //private TextField textField;
    private Listener listener;
    private ListView<String> tagListView;
    private ObservableList<String> data;
    private static TagSelectorDialog INSTANCE;

    public TagSelectorDialog() {
        super(new VBox(10));
        setCaption(Messages.currentInstance().get("addTagLbl"));
        mainPane = (VBox) getContentPane();
        data = FXCollections.observableArrayList();
        tagListView = new ListView<>(data);
        /*textField = new TextField();
        textField.setPromptText(Messages.currentInstance().get("searchTextLbl"));
        HBox.setHgrow(textField, Priority.ALWAYS);
        Button acceptButton = new Button(Messages.currentInstance().get("acceptLbl"),Utils.getIcon(FontAwesome.Glyph.CHECK));
        acceptButton.setOnAction(actionEvent -> {
            if ("".equals(textField.getText().trim())) {
                MainApp.showMessage(ResponseDto.SC_ERROR, Messages.currentInstance().get("emptyFieldErrorMsg",
                        Messages.currentInstance().get("searchTextLbl")));
                return;
            }
            try {
                ProgressDialog.show(new TagVSLoader(null), Messages.currentInstance().get("addTagLbl"));
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
            ProgressDialog.show(new TagLoader(null), Messages.currentInstance().get("addTagLbl"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class TagLoader extends Task<ResponseDto> {

        private String searchParam;

        public TagLoader(String searchParam) throws Exception {
            this.searchParam = searchParam;
        }

        @Override protected ResponseDto call() throws Exception {
            updateProgress(1, 10);
            updateMessage(Messages.currentInstance().get("addTagLbl"));
            updateProgress(3, 10);
            String targetURL = CurrencyOperation.GET_TAG.getUrl(MainApp.instance().getCurrencyServiceEntityId());
            ResponseDto responseVS  = HttpConn.getInstance().doGetRequest(targetURL, ContentType.JSON.name());
            if(ResponseDto.SC_OK == responseVS.getStatusCode()) {
                ResultListDto<TagDto> resultListDto = (ResultListDto<TagDto>) responseVS.getMessage(
                        new TypeReference<ResultListDto<TagDto>>() { });
                PlatformImpl.runLater(() -> {
                    data.clear();
                    for(TagDto tagVSDto : resultListDto.getResultList()) {
                        if(!TagDto.WILDTAG.equals(tagVSDto.getName().trim())) data.add(tagVSDto.getName().trim());
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
            if(INSTANCE == null) INSTANCE = new TagSelectorDialog();
            //INSTANCE.data.clear();
            INSTANCE.listener = listener;
            //INSTANCE.textField.setText("");
            if(INSTANCE.data.size() == 0) INSTANCE.loadTags();
            INSTANCE.show();
        });
    }

}