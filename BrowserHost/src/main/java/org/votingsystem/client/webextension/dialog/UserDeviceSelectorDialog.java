package org.votingsystem.client.webextension.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.MediaTypeVS;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserDeviceSelectorDialog extends DialogVS {

    public interface Listener {
        public void setSelectedDevice(DeviceVSDto device);
    }

    private static Logger log = Logger.getLogger(UserDeviceSelectorDialog.class.getName());

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
        ProgressDialog.show(new DeviceLoaderTask(), message);
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        acceptButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK));
        acceptButton.setText(ContextVS.getMessage("acceptLbl"));
        footerBox.getChildren().remove(acceptButton);
    }

    private void updateDeviceList(Collection<DeviceVSDto> deviceList) {
        PlatformImpl.runLater(() -> {
            try {
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
                for(DeviceVSDto deviceVSDto: deviceList) {
                    if(BrowserSessionService.getInstance().getDevice().getDeviceId().equals(deviceVSDto.getDeviceId())) {
                        deviceFound = true;
                        RadioButton radioButton = new RadioButton(deviceVSDto.getDeviceName());
                        radioButton.setUserData(deviceVSDto);
                        radioButton.setToggleGroup(deviceToggleGroup);
                        deviceListBox.getChildren().add(radioButton);
                    }
                }
                if(!deviceFound) {
                    setCaption(ContextVS.getMessage("deviceListEmptyMsg"));
                    mainPane.getChildren().remove(messageLbl);
                }
                mainPane.getScene().getWindow().sizeToScene();
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

    public void acceptButton(ActionEvent actionEvent) {
        if(deviceToggleGroup != null && deviceToggleGroup.getSelectedToggle() != null)
            listener.setSelectedDevice((DeviceVSDto) deviceToggleGroup.getSelectedToggle().getUserData());
        hide();
    }

    public  class DeviceLoaderTask extends Task<Void> {

        public DeviceLoaderTask( ) { }

        @Override protected Void call() throws Exception {
            try {
                UserVSDto userVSDto = HttpHelper.getInstance().getData(UserVSDto.class,
                        ContextVS.getInstance().getCurrencyServer().getDeviceVSConnectedServiceURL(
                                BrowserSessionService.getInstance().getUserVS().getNif()), MediaTypeVS.JSON);
                updateDeviceList(userVSDto.getConnectedDevices());
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
            return null;
        }

    }

    public static void show(String caption, String message, Listener listener) {
        Platform.runLater(() -> {
            try {
                UserDeviceSelectorDialog dialog = new UserDeviceSelectorDialog(caption, message, listener);
                dialog.show();
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

}