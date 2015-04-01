package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.votingsystem.client.service.SessionService;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.NifUtils;
import java.io.IOException;
import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MobileSelectorDialog extends DialogVS {

    public interface Listener {
        public void setSelectedDevice(Map deviceDataMap);
    }

    private static Logger log = Logger.getLogger(MobileSelectorDialog.class.getSimpleName());

    @FXML private VBox mainPane;
    @FXML private Button acceptButton;
    @FXML private Button searchDeviceButton;
    @FXML private Label messageLbl;
    @FXML private TextField nifTextField;
    @FXML private ProgressBar progressBar;
    @FXML private VBox deviceListBox;
    @FXML private HBox footerBox;
    private Listener listener;
    private ToggleGroup deviceToggleGroup;

    private MobileSelectorDialog(String caption, String message, Listener listener) throws IOException {
        super("/fxml/MobileSelectorDialog.fxml", caption);
        this.listener = listener;
        messageLbl.setText(message);
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        mainPane.getChildren().removeAll(progressBar);
        acceptButton.setGraphic(Utils.getIcon(FontAwesomeIconName.CHECK));
        acceptButton.setText(ContextVS.getMessage("acceptLbl"));
        nifTextField.setPromptText(ContextVS.getMessage("nifLbl"));
        searchDeviceButton.setText(ContextVS.getMessage("searchDevicesLbl"));
        searchDeviceButton.setGraphic(Utils.getIcon(FontAwesomeIconName.SEARCH));
        footerBox.getChildren().remove(acceptButton);
    }

    public void searchButton(ActionEvent actionEvent) {
        try {
            final String nif = NifUtils.validate(nifTextField.getText());
            if(!mainPane.getChildren().contains(progressBar)) mainPane.getChildren().add(1, progressBar);
            mainPane.getScene().getWindow().sizeToScene();
            new Thread(new Runnable() {
                @Override public void run() {
                    ResponseVS responseVS = HttpHelper.getInstance().getData(
                            ContextVS.getInstance().getCurrencyServer().getDeviceListByNifServiceURL(nif), ContentTypeVS.JSON);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        try {
                            updateDeviceList((List) responseVS.getMessageMap().get("deviceList"));
                        } catch (IOException ex) {
                            log.log(Level.SEVERE, ex.getMessage(), ex);
                        }
                    }
                }
            }).start();
        } catch(Exception ex) {
            showMessage(ResponseVS.SC_ERROR, ex.getMessage());
        }

    }

    private void updateDeviceList(List deviceList) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                if(mainPane.getChildren().contains(progressBar)) mainPane.getChildren().remove(progressBar);
                if(!deviceListBox.getChildren().isEmpty()) deviceListBox.getChildren().removeAll(
                        deviceListBox.getChildren());
                deviceToggleGroup = new ToggleGroup();
                deviceToggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                    public void changed(ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) {
                        if (deviceToggleGroup.getSelectedToggle() != null) {
                            if(!footerBox.getChildren().contains(acceptButton))
                                footerBox.getChildren().add(2, acceptButton);
                        } else footerBox.getChildren().remove(acceptButton);
                    }
                });
                for(int i = 0; i < deviceList.size() ; i++) {
                    Map deviceData = (Map) deviceList.get(i);
                    if(SessionService.getInstance().getDeviceId() == null ||
                            !SessionService.getInstance().getDeviceId().equals(deviceData.get("deviceId"))) {
                        RadioButton radioButton = new RadioButton((String) deviceData.get("deviceName"));
                        radioButton.setUserData(deviceData);
                        radioButton.setToggleGroup(deviceToggleGroup);
                        deviceListBox.getChildren().add(radioButton);
                    }
                }
                mainPane.getScene().getWindow().sizeToScene();
            }
        });
    }

    public void acceptButton(ActionEvent actionEvent) {
        if(deviceToggleGroup != null && deviceToggleGroup.getSelectedToggle() != null)
            listener.setSelectedDevice((Map) deviceToggleGroup.getSelectedToggle().getUserData());
        hide();
    }

    public void onEnterNifTextField(ActionEvent actionEvent) {
        searchButton(actionEvent);
    }

    public static void show(String caption, String message, Listener listener) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    MobileSelectorDialog dialog = new MobileSelectorDialog(caption, message, listener);
                    dialog.show();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        });
    }

}