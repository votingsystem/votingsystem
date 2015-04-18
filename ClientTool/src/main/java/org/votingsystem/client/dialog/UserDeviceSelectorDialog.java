package org.votingsystem.client.dialog;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.votingsystem.client.service.SessionService;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.MediaTypeVS;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserDeviceSelectorDialog extends DialogVS {

    public interface Listener {
        public void setSelectedDevice(DeviceVSDto device);
    }

    private static Logger log = Logger.getLogger(UserDeviceSelectorDialog.class.getSimpleName());

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
        new Thread(() -> {
            try {
                ResultListDto<DeviceVSDto> deviceList = HttpHelper.getInstance().getData(
                        new TypeReference<ResultListDto<DeviceVSDto>>(){},
                        ContextVS.getInstance().getCurrencyServer().getConnectedDeviceListByNifServiceURL(
                                SessionService.getInstance().getUserVS().getNif()), MediaTypeVS.JSON);
                updateDeviceList(deviceList.getResultList());
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }).start();
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        acceptButton.setGraphic(Utils.getIcon(FontAwesomeIconName.CHECK));
        acceptButton.setText(ContextVS.getMessage("acceptLbl"));
        footerBox.getChildren().remove(acceptButton);
    }

    private void updateDeviceList(List<DeviceVSDto> deviceList) {
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
            for(DeviceVSDto deviceVSDto: deviceList) {
                if(!SessionService.getInstance().getDeviceVS().getDeviceId().equals(deviceVSDto.getDeviceId())) {
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
        });
    }

    public void acceptButton(ActionEvent actionEvent) {
        if(deviceToggleGroup != null && deviceToggleGroup.getSelectedToggle() != null)
            listener.setSelectedDevice((DeviceVSDto) deviceToggleGroup.getSelectedToggle().getUserData());
        hide();
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