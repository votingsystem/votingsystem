package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.util.BrowserVSSessionUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SettingsDialog extends DialogVS  implements MobileSelectorDialog.Listener {

    private static Logger log = Logger.getLogger(SettingsDialog.class);

    private KeyStore userKeyStore;
    private Label keyStoreLbl;
    private HBox mobileDeviceInfo;
    private Label mobileDeviceLbl;
    private VBox keyStoreVBox;
    private GridPane gridPane;
    private RadioButton signWithDNIeRb;
    private RadioButton signWithMobileRb;
    private RadioButton signWithKeystoreRb;
    private JSONObject deviceDataJSON;

    public SettingsDialog() {
        super(new GridPane());
        final Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        gridPane = (GridPane) getRootNode();
        gridPane.setVgap(15);
        ToggleGroup tg = new ToggleGroup();
        signWithDNIeRb = new RadioButton(ContextVS.getMessage("setDNIeSignatureMechanismMsg"));
        signWithDNIeRb.setToggleGroup(tg);
        signWithDNIeRb.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                changeSignatureMode(actionEvent);
            }});

        signWithMobileRb = new RadioButton(ContextVS.getMessage("setMobileSignatureMechanismMsg"));
        signWithMobileRb.setToggleGroup(tg);
        signWithMobileRb.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                changeSignatureMode(actionEvent);
            }
        });

        mobileDeviceInfo = new HBox(15);
        mobileDeviceInfo.setAlignment(Pos.CENTER);
        mobileDeviceLbl = new Label();
        mobileDeviceLbl.setStyle("-fx-text-fill: #888;");
        Button mobileDeviceButton = new Button(ContextVS.getMessage("changeMobileDeviceLbl"));
        mobileDeviceButton.setGraphic(Utils.getImage(FontAwesome.Glyph.EXCHANGE, Utils.COLOR_BUTTON_OK));
        mobileDeviceButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                MobileSelectorDialog.show(SettingsDialog.this);
            }
        });
        mobileDeviceInfo.getChildren().addAll(mobileDeviceLbl, mobileDeviceButton);

        signWithKeystoreRb = new RadioButton(ContextVS.getMessage("setJksKeyStoreSignatureMechanismMsg"));
        signWithKeystoreRb.setToggleGroup(tg);
        signWithKeystoreRb.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                changeSignatureMode(actionEvent);
            }});
        keyStoreVBox = new VBox(10);
        Button selectKeyStoreButton = new Button(ContextVS.getMessage("setKeyStoreLbl"));
        selectKeyStoreButton.setGraphic(Utils.getImage(FontAwesome.Glyph.KEY));
        selectKeyStoreButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                selectKeystoreFile();
            }});
        keyStoreLbl = new Label(ContextVS.getMessage("selectKeyStoreLbl"));
        keyStoreLbl.setContentDisplay(ContentDisplay.LEFT);
        UserVS lastAuthenticatedUser = BrowserVSSessionUtils.getInstance().getUserVS();
        if(lastAuthenticatedUser != null) keyStoreLbl.setText(
                lastAuthenticatedUser.getCertificate().getSubjectDN().toString());
        keyStoreVBox.getChildren().addAll(selectKeyStoreButton, keyStoreLbl);
        VBox.setMargin(keyStoreVBox, new Insets(10, 10, 10, 10));
        keyStoreVBox.getStyleClass().add("settings-vbox");

        Button requestCertButton = new Button(ContextVS.getMessage("requestCertLbl"));
        requestCertButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CERTIFICATE, Utils.COLOR_BUTTON_OK));
        requestCertButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                if (ContextVS.getInstance().getAccessControl() != null) {
                    Platform.runLater(new Runnable() {
                        @Override public void run() {
                            BrowserVS.getInstance().newTab(
                                    ContextVS.getInstance().getAccessControl().getCertRequestServiceURL(),null, null);
                            getStage().close();
                        }
                    });
                } else {
                    log.error("missing 'access control'");
                    showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("connectionErrorMsg"));
                }
            }
        });

        HBox footerButtonsBox = new HBox(10);
        Button cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                getStage().close();
            }});
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        acceptButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK));
        acceptButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                validateForm();
            }});
        footerButtonsBox.getChildren().addAll(spacer, cancelButton, acceptButton);
        gridPane.setMargin(footerButtonsBox, new Insets(20, 20, 0, 20));
        gridPane.add(requestCertButton,0,0);
        gridPane.setMargin(requestCertButton, new Insets(10, 20, 20, 20));
        gridPane.add(signWithMobileRb,0,1);
        gridPane.setMargin(mobileDeviceInfo, new Insets(0, 0, 0, 20));
        gridPane.add(signWithDNIeRb,0,3);
        gridPane.add(signWithKeystoreRb,0,5);
        gridPane.add(footerButtonsBox,0,7);
        gridPane.getStyleClass().add("modal-dialog");
        getStage().setScene(new Scene(gridPane, Color.TRANSPARENT));
        getStage().getScene().getStylesheets().add(getClass().getResource("/css/modal-dialog.css").toExternalForm());
        gridPane.setMinWidth(600);
    }

    private void changeSignatureMode(ActionEvent evt) {
        log.debug("changeSignatureMode");
        if(gridPane.getChildren().contains(keyStoreVBox)) gridPane.getChildren().remove(keyStoreVBox);
        if(gridPane.getChildren().contains(mobileDeviceInfo)) gridPane.getChildren().remove(mobileDeviceInfo);
        if(evt.getSource() == signWithKeystoreRb) {
            gridPane.add(keyStoreVBox, 0, 6);
        }
        if(evt.getSource() == signWithMobileRb && deviceDataJSON != null) {
            gridPane.add(mobileDeviceInfo, 0, 2);
        }
        if(evt.getSource() == signWithMobileRb) MobileSelectorDialog.show(SettingsDialog.this);
        getStage().sizeToScene();
    }

    @Override public void show() {
        log.debug("show");
        String cryptoTokenStr = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN,
                CryptoTokenVS.DNIe.toString());
        CryptoTokenVS cryptoTokenVS = CryptoTokenVS.valueOf(cryptoTokenStr);
        gridPane.getChildren().remove(keyStoreVBox);
        switch(cryptoTokenVS) {
            case DNIe:
                signWithDNIeRb.setSelected(true);
                break;
            case JKS_KEYSTORE:
                signWithKeystoreRb.setSelected(true);
                gridPane.add(keyStoreVBox, 0, 6);
                break;
            case MOBILE:
                signWithMobileRb.setSelected(true);
                mobileDeviceLbl.setText(BrowserVSSessionUtils.getInstance().getMobileCryptoToken().getString("deviceName"));
                gridPane.add(mobileDeviceInfo, 0, 2);
                break;
        }
        getStage().show();
    }

    private void selectKeystoreFile() {
        log.debug("selectKeystoreFile");
        try {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(ContextVS.getMessage("selectKeyStore"));
            File file = fileChooser.showOpenDialog(getStage());
            if (file != null) {
                File selectedKeystore = new File(file.getAbsolutePath());
                byte[] keystoreBytes = FileUtils.getBytesFromFile(selectedKeystore);
                try {
                    userKeyStore = KeyStoreUtil.getKeyStoreFromBytes(keystoreBytes, null);
                } catch(Exception ex) {
                    showMessage(null, ContextVS.getMessage("errorLbl") + " " +
                            ContextVS.getMessage("keyStoreNotValidErrorMsg"));
                }
                X509Certificate certSigner = (X509Certificate) userKeyStore.getCertificate("UserTestKeysStore");
                keyStoreLbl.setText(certSigner.getSubjectDN().toString());
            } else {
                keyStoreLbl.setText(ContextVS.getMessage("selectKeyStoreLbl"));
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void showMessage(Integer statusCode, String message) {
        PlatformImpl.runLater(new Runnable() { @Override public void run() {
            new MessageDialog().showMessage(statusCode, message);}});
    }

    private void validateForm() {
        log.debug("validateForm");
        String cryptoTokenStr = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN,
                CryptoTokenVS.DNIe.toString());
        CryptoTokenVS cryptoTokenVS = CryptoTokenVS.valueOf(cryptoTokenStr);
        if(signWithKeystoreRb.isSelected() &&  CryptoTokenVS.JKS_KEYSTORE != cryptoTokenVS) {
            if(userKeyStore == null) {
                showMessage(null, ContextVS.getMessage("errorLbl") + " " +
                        ContextVS.getMessage("keyStoreNotSelectedErrorLbl"));
                return;
            }
        }
        if(userKeyStore != null) {
            if(signWithKeystoreRb.isSelected()) {
                try {
                    PasswordDialog passwordDialog = new PasswordDialog();
                    passwordDialog.show(ContextVS.getMessage("newKeyStorePasswordMsg"));
                    String password = passwordDialog.getPassword();
                    ContextVS.saveUserKeyStore(userKeyStore, password);
                    ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN,
                            CryptoTokenVS.JKS_KEYSTORE.toString());
                } catch(Exception ex) {
                    showMessage(null, ContextVS.getMessage("errorStoringKeyStoreMsg"));
                    return;
                }
            }
        }
        if(signWithDNIeRb.isSelected()) {
            ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.DNIe.toString());
        }
        if(signWithMobileRb.isSelected() && deviceDataJSON == null) {
            showMessage(null, ContextVS.getMessage("deviceDataMissingErrorMsg"));
            return;
        } else if(signWithMobileRb.isSelected()) {
            ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.MOBILE.toString());
            BrowserVSSessionUtils.getInstance().setMobileCryptoToken(deviceDataJSON);
        }
        hide();
    }

    @Override public void setSelectedDevice(JSONObject deviceDataJSON) {
        log.debug("setSelectedDevice: " + deviceDataJSON.toString());
        this.deviceDataJSON = deviceDataJSON;
        if(!gridPane.getChildren().contains(mobileDeviceInfo)) gridPane.add(mobileDeviceInfo, 0, 2);
        mobileDeviceLbl.setText(deviceDataJSON.getString("deviceName"));
        getStage().sizeToScene();
    }

}