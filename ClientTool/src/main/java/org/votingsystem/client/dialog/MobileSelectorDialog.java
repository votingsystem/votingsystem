package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.NifUtils;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MobileSelectorDialog {

    public interface Listener {
        public void setSelectedDevice(JSONObject deviceDataJSON);
    }

    private static Logger log = Logger.getLogger(MobileSelectorDialog.class);

    @FXML private VBox mainPane;
    @FXML private Button acceptButton;
    @FXML private Button cancelButton;
    @FXML private Button searchDeviceButton;
    @FXML private Label messageLbl;
    @FXML private TextField nifTextField;
    @FXML private VBox devicesBox;
    @FXML private ProgressBar progressBar;
    @FXML private VBox deviceListBox;
    private Listener listener;
    private ToggleGroup deviceToggleGroup;

    private MobileSelectorDialog(Listener listener) {
        this.listener = listener;
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        mainPane.getChildren().removeAll(devicesBox);
        messageLbl.setText(ContextVS.getMessage("setMobileSignatureMechanismAdv"));
        acceptButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK));
        acceptButton.setText(ContextVS.getMessage("acceptLbl"));
        cancelButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        cancelButton.setText(ContextVS.getMessage("cancelLbl"));
        nifTextField.setPromptText(ContextVS.getMessage("nifLbl"));
        searchDeviceButton.setText(ContextVS.getMessage("searchDevicesLbl"));
        searchDeviceButton.setGraphic(Utils.getImage(FontAwesome.Glyph.SEARCH));
    }

    public void searchButton(ActionEvent actionEvent) {
        try {
            final String nif = NifUtils.validate(nifTextField.getText());
            if(!mainPane.getChildren().contains(devicesBox)) mainPane.getChildren().add(1, devicesBox);
            if(!devicesBox.getChildren().contains(progressBar)) devicesBox.getChildren().add(0, progressBar);
            mainPane.getScene().getWindow().sizeToScene();
            new Thread(new Runnable() {
                @Override public void run() {
                    ResponseVS responseVS = HttpHelper.getInstance().getData(
                            ContextVS.getInstance().getVicketServer().getDeviceListByNifServiceURL(nif), ContentTypeVS.JSON);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        updateDeviceList((JSONArray) ((JSONObject)responseVS.getMessageJSON()).getJSONArray("deviceList"));
                    }
                }
            }).start();
        } catch(Exception ex) {
            showMessage(ex.getMessage());
        }

    }

    private void updateDeviceList(JSONArray deviceArray) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                if(devicesBox.getChildren().contains(progressBar)) devicesBox.getChildren().remove(progressBar);
                if(!deviceListBox.getChildren().isEmpty()) deviceListBox.getChildren().removeAll(
                        deviceListBox.getChildren());
                deviceToggleGroup = new ToggleGroup();
                for(int i = 0; i < deviceArray.size() ; i++) {
                    JSONObject deviceData = (JSONObject) deviceArray.get(i);
                    RadioButton radioButton = new RadioButton(deviceData.getString("deviceName"));
                    radioButton.setUserData(deviceData);
                    radioButton.setToggleGroup(deviceToggleGroup);
                    devicesBox.getChildren().add(radioButton);
                }
            }
        });
    }

    public void acceptButton(ActionEvent actionEvent) {
        listener.setSelectedDevice((JSONObject) deviceToggleGroup.getSelectedToggle().getUserData());
    }

    public void cancelButton(ActionEvent actionEvent) {
        mainPane.getScene().getWindow().hide();
    }

    public void showMessage(String message) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                new MessageDialog().showMessage(null, message);
            }
        });
    }

    public static void show(Listener listener) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    MobileSelectorDialog dialog = new MobileSelectorDialog(listener);
                    Stage stage = new Stage();
                    stage.centerOnScreen();
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/MobileSelectorDialog.fxml"));
                    fxmlLoader.setController(dialog);
                    stage.setScene(new Scene(fxmlLoader.load()));
                    stage.setTitle(ContextVS.getMessage("setMobileSignatureMechanismMsg"));
                    stage.initModality(Modality.WINDOW_MODAL);
                    stage.show();
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        });
    }

}