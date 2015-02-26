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
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.votingsystem.client.service.SessionService;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.HttpHelper;

import java.io.IOException;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserDeviceSelectorDialog extends DialogVS {

    public interface Listener {
        public void setSelectedDevice(JSONObject deviceDataJSON);
    }

    private static Logger log = Logger.getLogger(UserDeviceSelectorDialog.class);

    @FXML private VBox mainPane;
    @FXML private Button acceptButton;
    @FXML private Label messageLbl;
    @FXML private ProgressBar progressBar;
    @FXML private VBox deviceListBox;
    @FXML private HBox footerBox;
    private Listener listener;
    private ToggleGroup deviceToggleGroup;

    private UserDeviceSelectorDialog(String caption, String message, Listener listener) throws IOException {
        super("/fxml/UserDeviceSelectorDialog.fxml", caption);
        this.listener = listener;
        messageLbl.setText(message);
        new Thread(new Runnable() {
            @Override public void run() {
                ResponseVS responseVS = HttpHelper.getInstance().getData(
                        ContextVS.getInstance().getCooinServer().getConnectedDeviceListByNifServiceURL(
                        SessionService.getInstance().getUserVS().getNif()), ContentTypeVS.JSON);
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    updateDeviceList((JSONArray) ((JSONObject)responseVS.getMessageJSON()).getJSONArray("deviceList"));
                }
            }
        }).start();
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        acceptButton.setGraphic(Utils.getIcon(FontAwesomeIconName.CHECK));
        acceptButton.setText(ContextVS.getMessage("acceptLbl"));
        footerBox.getChildren().remove(acceptButton);
    }

    private void updateDeviceList(JSONArray deviceArray) {
        PlatformImpl.runLater(() -> {
            if(mainPane.getChildren().contains(progressBar)) mainPane.getChildren().remove(progressBar);
            if(!deviceListBox.getChildren().isEmpty()) deviceListBox.getChildren().removeAll(
                    deviceListBox.getChildren());
            deviceToggleGroup = new ToggleGroup();
            deviceToggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                public void changed(ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) {
                    if (deviceToggleGroup.getSelectedToggle() != null) {
                        if(!footerBox.getChildren().contains(acceptButton)) {
                            footerBox.getChildren().add(acceptButton);
                            getStage().sizeToScene();
                        }
                    } else footerBox.getChildren().remove(acceptButton);
                }
            });
            boolean deviceFound = false;
            for(int i = 0; i < deviceArray.size() ; i++) {
                JSONObject deviceData = (JSONObject) deviceArray.get(i);
                if(!SessionService.getInstance().getDeviceId().equals(deviceData.getString("deviceId"))) {
                    deviceFound = true;
                    RadioButton radioButton = new RadioButton(deviceData.getString("deviceName"));
                    radioButton.setUserData(deviceData);
                    radioButton.setToggleGroup(deviceToggleGroup);
                    deviceListBox.getChildren().add(radioButton);
                }
            }
            if(!deviceFound) {
                setCaption(ContextVS.getMessage("deviceListEmptyMsg"));
                mainPane.getChildren().remove(messageLbl);
            }
            mainPane.getScene().getWindow().sizeToScene();
        });
    }

    public void acceptButton(ActionEvent actionEvent) {
        if(deviceToggleGroup != null && deviceToggleGroup.getSelectedToggle() != null)
            listener.setSelectedDevice((JSONObject) deviceToggleGroup.getSelectedToggle().getUserData());
        hide();
    }

    public static void show(String caption, String message, Listener listener) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    UserDeviceSelectorDialog dialog = new UserDeviceSelectorDialog(caption, message, listener);
                    dialog.show();
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        });
    }

}