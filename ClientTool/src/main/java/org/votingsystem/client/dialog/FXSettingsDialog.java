package org.votingsystem.client.dialog;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.*;
import org.apache.log4j.Logger;
import org.votingsystem.client.util.FXUtils;
import org.votingsystem.client.util.SMIMEContentSigner;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.FileUtils;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class FXSettingsDialog {

    private static Logger logger = Logger.getLogger(FXSettingsDialog.class);

    private Stage stage;
    private KeyStore userKeyStore;
    private Label keyStoreLabel;
    private VBox keyStoreVBox;
    private VBox dialogVBox;
    private RadioButton signWithDNIeRb;
    private RadioButton signWithAndroidRb;
    private RadioButton signWithKeystoreRb;

    public FXSettingsDialog() {
        String cryptoTokenStr = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN,
                SMIMEContentSigner.CryptoToken.DNIe.toString());
        SMIMEContentSigner.CryptoToken cryptoToken = SMIMEContentSigner.CryptoToken.valueOf(cryptoTokenStr);


        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent window) {      }
        });

        dialogVBox = new VBox(10);
        ToggleGroup tg = new ToggleGroup();

        signWithDNIeRb = new RadioButton(ContextVS.getMessage("setDNIeSignatureMechanismMsg"));
        signWithDNIeRb.setToggleGroup(tg);
        signWithDNIeRb.setPrefWidth(600);
        signWithDNIeRb.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                changeSignatureMode(actionEvent);
            }});

        signWithAndroidRb = new RadioButton(ContextVS.getMessage("setAndroidSignatureMechanismMsg"));
        signWithAndroidRb.setToggleGroup(tg);
        signWithAndroidRb.setPrefWidth(600);
        signWithAndroidRb.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                changeSignatureMode(actionEvent);
            }});

        signWithKeystoreRb = new RadioButton(ContextVS.getMessage("setJksKeyStoreSignatureMechanismMsg"));
        signWithKeystoreRb.setToggleGroup(tg);
        signWithKeystoreRb.setPrefWidth(600);
        signWithKeystoreRb.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                changeSignatureMode(actionEvent);
            }});
        keyStoreVBox = new VBox(10);
        Button selectKeyStoreButton = new Button(ContextVS.getMessage("setKeyStoreLbl"));
        selectKeyStoreButton.setGraphic(new ImageView(FXUtils.getImage(this, "fa-key")));
        selectKeyStoreButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                selectKeystoreFile();
            }});
        keyStoreLabel = new Label(ContextVS.getMessage("selectKeyStoreLbl"));
        keyStoreLabel.setContentDisplay(ContentDisplay.LEFT);
        keyStoreVBox.getChildren().addAll(selectKeyStoreButton, keyStoreLabel);
        VBox.setMargin(keyStoreVBox, new Insets(10, 10, 10, 10));
        keyStoreVBox.getStyleClass().add("select-keystore-box");

        Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setGraphic(new ImageView(FXUtils.getImage(this, "accept")));
        acceptButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                stage.hide();
            }});
        dialogVBox.getChildren().addAll(signWithDNIeRb, signWithAndroidRb, signWithKeystoreRb, acceptButton);
        dialogVBox.getStyleClass().add("modal-dialog");
        stage.setScene(new Scene(dialogVBox, Color.TRANSPARENT));
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
        switch(cryptoToken) {
            case MOBILE:
                signWithAndroidRb.setSelected(true);
                break;
            case DNIe:
                signWithDNIeRb.setSelected(true);
                break;
            case JKS_KEYSTORE:
                signWithKeystoreRb.setSelected(true);
                break;
        }
    }

    private void changeSignatureMode(ActionEvent evt) {
        logger.debug("changeSignatureMode");
        if(evt.getSource() == signWithKeystoreRb) {
            dialogVBox.getChildren().add(3, keyStoreVBox);
        } else {
            dialogVBox.getChildren().remove(keyStoreVBox);
        }
        stage.sizeToScene();
    }

    public void show() {
        stage.centerOnScreen();
        stage.show();
    }

    private void selectKeystoreFile() {
        logger.debug("selectKeystoreFile");
        try {
            final FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                File selectedKeystore = new File(file.getAbsolutePath());
                byte[] keystoreBytes = FileUtils.getBytesFromFile(selectedKeystore);
                try {
                    userKeyStore = KeyStoreUtil.getKeyStoreFromBytes(keystoreBytes, null);
                } catch(Exception ex) {
                    FXMessageDialog messageDialog = new FXMessageDialog();
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
            logger.error(ex.getMessage(), ex);
        }
    }

    private void validateSettings() {
        logger.debug("validateSettings");
        String cryptoTokenStr = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN,
                SMIMEContentSigner.CryptoToken.DNIe.toString());
        SMIMEContentSigner.CryptoToken cryptoToken = SMIMEContentSigner.CryptoToken.valueOf(cryptoTokenStr);
        if(signWithKeystoreRb.isSelected() &&  SMIMEContentSigner.CryptoToken.JKS_KEYSTORE != cryptoToken) {
            if(signWithKeystoreRb.isSelected()) {
                if(userKeyStore == null) {
                    FXMessageDialog messageDialog = new FXMessageDialog();
                    messageDialog.showMessage(ContextVS.getMessage("errorLbl") + " " +
                            ContextVS.getMessage("keyStoreNotSelectedErrorLbl"));
                }
            }
            stage.hide();
        }
        if(userKeyStore != null) {
            if(signWithKeystoreRb.isSelected()) {
                try {
                    FXPasswordDialog passwordDialog = new FXPasswordDialog ();
                    passwordDialog.show(ContextVS.getMessage("newKeyStorePasswordMsg"),
                            ContextVS.getMessage("setKeyStoreLbl"));
                    String password = passwordDialog.getPassword();
                    ContextVS.saveUserKeyStore(userKeyStore, password);
                    ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN,
                            SMIMEContentSigner.CryptoToken.JKS_KEYSTORE.toString());
                } catch(Exception ex) {
                    FXMessageDialog messageDialog = new FXMessageDialog();
                    messageDialog.showMessage(ContextVS.getMessage("errorLbl") + " " +
                            ContextVS.getMessage("errorStoringKeyStoreMsg"));
                }
            }
        }
        stage.close();
    }

    class Delta { double x, y; }

}