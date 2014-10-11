package org.votingsystem.client.dialog;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import org.apache.log4j.Logger;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.ContentSignerHelper;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.FileUtils;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SettingsDialog {

    private static Logger log = Logger.getLogger(SettingsDialog.class);

    private Stage stage;
    private KeyStore userKeyStore;
    private Label keyStoreLabel;
    private VBox keyStoreVBox;
    private GridPane gridPane;
    private RadioButton signWithDNIeRb;
    private RadioButton signWithKeystoreRb;

    public SettingsDialog() {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        final Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));

        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent window) {      }
        });

        gridPane = new GridPane();
        gridPane.setVgap(10);

        ToggleGroup tg = new ToggleGroup();

        signWithDNIeRb = new RadioButton(ContextVS.getMessage("setDNIeSignatureMechanismMsg"));
        signWithDNIeRb.setToggleGroup(tg);
        signWithDNIeRb.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                changeSignatureMode(actionEvent);
            }});

        signWithKeystoreRb = new RadioButton(ContextVS.getMessage("setJksKeyStoreSignatureMechanismMsg"));
        signWithKeystoreRb.setToggleGroup(tg);
        signWithKeystoreRb.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                changeSignatureMode(actionEvent);
            }});
        keyStoreVBox = new VBox(10);

        Button selectKeyStoreButton = new Button(ContextVS.getMessage("setKeyStoreLbl"));
        selectKeyStoreButton.setGraphic(new ImageView(Utils.getImage(this, "fa-key")));
        selectKeyStoreButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                selectKeystoreFile();
            }});
        keyStoreLabel = new Label(ContextVS.getMessage("selectKeyStoreLbl"));
        keyStoreLabel.setContentDisplay(ContentDisplay.LEFT);
        keyStoreVBox.getChildren().addAll(selectKeyStoreButton, keyStoreLabel);
        VBox.setMargin(keyStoreVBox, new Insets(10, 10, 10, 10));
        keyStoreVBox.getStyleClass().add("settings-vbox");

        HBox footerButtonsBox = new HBox(10);

        Button cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setGraphic(new ImageView(Utils.getImage(this, "cancel")));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                stage.close();
            }});

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        acceptButton.setGraphic(new ImageView(Utils.getImage(this, "accept")));
        acceptButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                validateForm();
            }});
        footerButtonsBox.getChildren().addAll(acceptButton, spacer, cancelButton);
        gridPane.setMargin(footerButtonsBox, new Insets(20, 20, 0, 20));

        gridPane.add(signWithDNIeRb,0,0);
        gridPane.add(signWithKeystoreRb,0,4);
        gridPane.add(footerButtonsBox,0,6);
        gridPane.getStyleClass().add("modal-dialog");
        stage.setScene(new Scene(gridPane, Color.TRANSPARENT));
        stage.getScene().getStylesheets().add(getClass().getResource("/resources/css/modal-dialog.css").toExternalForm());
        // allow the dialog to be dragged around.
        final Node root = stage.getScene().getRoot();
        final Delta dragDelta = new Delta();
        root.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = stage.getX() - mouseEvent.getScreenX();
                dragDelta.y = stage.getY() - mouseEvent.getScreenY();
            }
        });
        root.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                stage.setX(mouseEvent.getScreenX() + dragDelta.x);
                stage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });
        gridPane.setMinWidth(600);
    }

    private void changeSignatureMode(ActionEvent evt) {
        log.debug("changeSignatureMode");
        gridPane.getChildren().remove(keyStoreVBox);
        if(evt.getSource() == signWithKeystoreRb) {
            gridPane.add(keyStoreVBox, 0, 5);
        }
        stage.sizeToScene();
    }

    public void show() {
        log.debug("show");
        String cryptoTokenStr = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN,
                ContentSignerHelper.CryptoToken.DNIe.toString());
        ContentSignerHelper.CryptoToken cryptoToken = ContentSignerHelper.CryptoToken.valueOf(cryptoTokenStr);
        gridPane.getChildren().remove(keyStoreVBox);
        switch(cryptoToken) {
            case DNIe:
                signWithDNIeRb.setSelected(true);
                break;
            case JKS_KEYSTORE:
                signWithKeystoreRb.setSelected(true);
                gridPane.add(keyStoreVBox, 0, 5);
                break;
        }
        stage.centerOnScreen();
        stage.show();
    }

    private void selectKeystoreFile() {
        log.debug("selectKeystoreFile");
        try {
            final FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                File selectedKeystore = new File(file.getAbsolutePath());
                byte[] keystoreBytes = FileUtils.getBytesFromFile(selectedKeystore);
                try {
                    userKeyStore = KeyStoreUtil.getKeyStoreFromBytes(keystoreBytes, null);
                } catch(Exception ex) {
                    MessageDialog messageDialog = new MessageDialog();
                    messageDialog.showMessage(ContextVS.getMessage("errorLbl") + " " +
                            ContextVS.getMessage("keyStoreNotValidErrorMsg"));
                }
                //PrivateKey privateKeySigner = (PrivateKey)userKeyStore.getKey("UserTestKeysStore", null);
                X509Certificate certSigner = (X509Certificate) userKeyStore.getCertificate("UserTestKeysStore");
                keyStoreLabel.setText(certSigner.getSubjectDN().toString());
            } else {
                keyStoreLabel.setText(ContextVS.getMessage("selectKeyStoreLbl"));
            }

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void validateForm() {
        log.debug("validateForm");
        String cryptoTokenStr = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN,
                ContentSignerHelper.CryptoToken.DNIe.toString());
        ContentSignerHelper.CryptoToken cryptoToken = ContentSignerHelper.CryptoToken.valueOf(cryptoTokenStr);
        if(signWithKeystoreRb.isSelected() &&  ContentSignerHelper.CryptoToken.JKS_KEYSTORE != cryptoToken) {
            if(userKeyStore == null) {
                MessageDialog messageDialog = new MessageDialog();
                messageDialog.showMessage(ContextVS.getMessage("errorLbl") + " " +
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
                            ContentSignerHelper.CryptoToken.JKS_KEYSTORE.toString());
                } catch(Exception ex) {
                    MessageDialog messageDialog = new MessageDialog();
                    messageDialog.showMessage(ContextVS.getMessage("errorLbl") + " " +
                            ContextVS.getMessage("errorStoringKeyStoreMsg"));
                    return;
                }
            }
        }
        if(signWithDNIeRb.isSelected()) {
            ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN,
                    ContentSignerHelper.CryptoToken.DNIe.toString());
        }
        stage.close();
    }

    class Delta { double x, y; }

}