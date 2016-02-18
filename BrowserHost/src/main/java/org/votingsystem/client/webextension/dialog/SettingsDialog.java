package org.votingsystem.client.webextension.dialog;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.util.ContextVS;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SettingsDialog extends DialogVS implements DeviceSelectorDialog.Listener {

    private static Logger log = Logger.getLogger(SettingsDialog.class.getName());

    private HBox mobileDeviceInfo;
    private Label mobileDeviceLbl;
    private VBox mainPane;
    private DeviceVSDto deviceVSDto;

    public SettingsDialog() {
        super(new VBox(15));
        mainPane = (VBox) getContentPane();



        mobileDeviceInfo = new HBox(15);
        mobileDeviceInfo.setAlignment(Pos.CENTER);
        mobileDeviceLbl = new Label();
        mobileDeviceLbl.setStyle("-fx-text-fill: #888;");
        Button mobileDeviceButton = new Button(ContextVS.getMessage("changeMobileDeviceLbl"), Utils.getIcon(FontAwesome.Glyph.EXCHANGE));
        mobileDeviceButton.setOnAction(actionEvent -> DeviceSelectorDialog.show(ContextVS.getMessage(
                "setMobileSignatureMechanismMsg"), ContextVS.getMessage("setMobileSignatureMechanismAdv"),
                SettingsDialog.this));
        mobileDeviceInfo.getChildren().addAll(mobileDeviceLbl, mobileDeviceButton);

        Button matchDeviceButton = new Button(ContextVS.getMessage("matchDeviceLbl"), Utils.getIcon(FontAwesome.Glyph.KEY));




        /*Button requestCertButton = new Button(ContextVS.getMessage("requestCertLbl"), Utils.getIcon(FontAwesome.Glyph.CERTIFICATE));
        requestCertButton.setOnAction(actionEvent -> {
            if (ContextVS.getInstance().getAccessControl() != null) {
                Platform.runLater(() -> {
                    BrowserHost.sendMessageToBrowser(MessageDto.NEW_TAB(
                            ContextVS.getInstance().getAccessControl().getCertRequestServiceURL()));
                    getStage().close();
                });
            } else {
                log.log(Level.SEVERE, "missing 'access control'");
                BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("connectionErrorMsg"));
            }
        });*/
        HBox footerButtonsBox = new HBox(10);
        Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"), Utils.getIcon(FontAwesome.Glyph.CHECK));
        acceptButton.setOnAction(actionEvent -> validateForm());
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), matchDeviceButton, acceptButton);
        mainPane.setMargin(footerButtonsBox, new Insets(20, 20, 0, 20));
        mainPane.setMargin(mobileDeviceInfo, new Insets(0, 0, 0, 20));

        mainPane.getChildren().add(footerButtonsBox);
        mainPane.getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
        mainPane.getStyleClass().add("modal-dialog");
        mainPane.setMinWidth(550);

        DeviceVSDto cryptToken = BrowserSessionService.getInstance().getCryptoToken();
        if(cryptToken != null) {

        }
    }


    @Override public void show() {
        log.info("show");
        getStage().show();
    }

    public static void showDialog() {
        Platform.runLater(() -> {
            SettingsDialog settingsDebug = new SettingsDialog();
            settingsDebug.show();
        });
    }


    private void validateForm() {
        log.info("validateForm");
        close();
    }

    private void close() {
        log.info("close");
        hide();
    }

    @Override public void setSelectedDevice(DeviceVSDto selectedDevice) {
        log.info("setSelectedDevice: " + selectedDevice.getDeviceName());

        getStage().sizeToScene();
    }

    @Override public void cancelSelection() {
        log.info("cancelSelection");
    }

}