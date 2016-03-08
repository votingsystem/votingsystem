package org.votingsystem.client.webextension.util;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.dialog.PasswordDialog;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.service.WebSocketService;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.crypto.KeyStoreUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Utils {

    private static Logger log = Logger.getLogger(Utils.class.getName());

    public static final String APPLICATION_ICON = "mail-mark-unread.png";

    public static final String COLOR_BUTTON_OK = "#888";
    public static final String COLOR_RESULT_OK = "#388746";
    public static final String COLOR_RED = "#ba0011";
    public static final String COLOR_RED_DARK = "#6c0404";
    public static final String COLOR_YELLOW_ALERT = "#fa1";


    public static final String EVENT_TYPE_CLICK = "click";
    public static final String EVENT_TYPE_MOUSEOVER = "mouseover";
    public static final String EVENT_TYPE_MOUSEOUT = "mouseclick";

    private static GlyphFont fontAwesome;
    static {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("fontawesome-webfont.ttf");
        fontAwesome = new FontAwesome(is);
        GlyphFontRegistry.register(fontAwesome);
    }

    public static Glyph getIcon(FontAwesome.Glyph glyph) {
        return fontAwesome.create(glyph);
    }

    public static Glyph getIcon(FontAwesome.Glyph glyph, String color, double size) {
        return fontAwesome.create(glyph).color(Color.web(color)).size(size);
    }

    public static Glyph getIcon(FontAwesome.Glyph glyph, double size) {
        return fontAwesome.create(glyph).size(size);
    }

    public static Glyph getIcon(FontAwesome.Glyph glyph, String colorStr) {
        return Glyph.create( "FontAwesome|" + glyph.name()).color(Color.web(colorStr));
    }

    public static Image getIconFromResources(String imageFilename) {
        return new Image(getResource("/images/" + imageFilename));
    }

    public static String getResource(String path) {
        return BrowserHost.class.getResource(path).toExternalForm();
    }

    public static Region getSpacer(){
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    // allow the dialog to be dragged around.
    public static void addMouseDragSupport(Stage stage) {
        final Node root = stage.getScene().getRoot();
        final Delta dragDelta = new Delta();
        root.setOnMousePressed(mouseEvent -> {  // record a delta distance for the drag and drop operation.
            dragDelta.x = stage.getX() - mouseEvent.getScreenX();
            dragDelta.y = stage.getY() - mouseEvent.getScreenY();
        });
        root.setOnMouseDragged(mouseEvent -> {
            stage.setX(mouseEvent.getScreenX() + dragDelta.x);
            stage.setY(mouseEvent.getScreenY() + dragDelta.y);
        });
    }

    public static void selectKeystoreFile(OperationVS operationVS) {
        log.info("selectKeystoreFile");
        PlatformImpl.runLater(() -> {
            try {
                final FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle(ContextVS.getMessage("selectKeyStore"));
                File file = fileChooser.showOpenDialog(new Stage());
                if (file != null) {
                    File selectedKeystore = new File(file.getAbsolutePath());
                    byte[] keystoreBytes = FileUtils.getBytesFromFile(selectedKeystore);
                    try {
                        KeyStore userKeyStore = KeyStoreUtil.getKeyStoreFromBytes(keystoreBytes, null);
                        UserVS userVS = UserVS.FROM_X509_CERT((X509Certificate)
                                userKeyStore.getCertificate("UserTestKeysStore"));
                        PasswordDialog.showWithPasswordConfirm(password -> {
                            if(password == null) return;
                            try {
                                ContextVS.getInstance().saveUserKeyStore(userKeyStore, password);
                                BrowserSessionService.getInstance().setUserVS(userVS, false);
                                if(operationVS != null) BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(
                                        ResponseVS.SC_OK, JSON.getMapper().writeValueAsString(UserVSDto.COMPLETE(userVS)),
                                        operationVS.getTabId(),
                                        operationVS.getCallerCallback()));
                            } catch (Exception ex) {
                                BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                            }
                        }, ContextVS.getMessage("newKeyStorePasswordMsg"));
                    } catch(Exception ex) {
                        log.log(Level.SEVERE,ex.getMessage(), ex);
                        if(operationVS != null) BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(
                                ResponseVS.SC_ERROR, ex.getMessage(), operationVS.getTabId(), operationVS.getCallerCallback()));
                    }

                }
            } catch (Exception ex) {
                log.log(Level.SEVERE,ex.getMessage(), ex);
            }
        });
    }

    public static void browserVSLinkListener(WebView webView) {
        webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue ov, Worker.State oldState, Worker.State newState) {
                if (newState == Worker.State.SUCCEEDED) {
                    org.w3c.dom.events.EventListener listener = evt -> {
                        String domEventType = evt.getType();
                        if (domEventType.equals(EVENT_TYPE_CLICK)) {
                            webView.getEngine().getLoadWorker().cancel();
                            String href = ((Element) evt.getTarget()).getAttribute("href");
                            evt.preventDefault();
                            BrowserHost.sendMessageToBrowser(MessageDto.NEW_TAB(href));
                        }
                    };

                    Document doc = webView.getEngine().getDocument();
                    NodeList nodeList = doc.getElementsByTagName("a");
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        ((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, false);
                        //((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_MOUSEOVER, listener, false);
                        //((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_MOUSEOVER, listener, false);
                    }
                }
            }
        });
    }

    public static String getTagForDescription(String tagName) {
        return ContextVS.getMessage("forLbl") + " " + MsgUtils.getTagDescription(tagName);
    }

    public static void selectImage(final OperationVS operationVS) throws Exception {
        PlatformImpl.runLater(() -> {
            try {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter(
                        "JPG (*.jpg)", Arrays.asList("*.jpg", "*.JPG"));
                FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter(
                        "PNG (*.png)", Arrays.asList("*.png", "*.PNG"));
                fileChooser.getExtensionFilters().addAll(extFilterJPG, extFilterPNG);
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                File selectedImage = fileChooser.showOpenDialog(null);
                if (selectedImage != null) {
                    byte[] imageFileBytes = FileUtils.getBytesFromFile(selectedImage);
                    log.info(" - imageFileBytes.length: " + imageFileBytes.length);
                    if (imageFileBytes.length > ContextVS.IMAGE_MAX_FILE_SIZE) {
                        log.info(" - MAX_FILE_SIZE exceeded ");
                        BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK( ResponseVS.SC_ERROR,
                                ContextVS.getMessage("fileSizeExceeded", ContextVS.IMAGE_MAX_FILE_SIZE_KB),
                                operationVS.getTabId(),operationVS.getCallerCallback()));
                    } else BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_OK,
                            selectedImage.getAbsolutePath(), operationVS.getTabId(), operationVS.getCallerCallback()));
                } else BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_ERROR,
                        null, operationVS.getTabId(),operationVS.getCallerCallback()));
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_ERROR,
                        ex.getMessage(), operationVS.getTabId(), operationVS.getCallerCallback()));
            }
        });
    }

    public static void saveReceipt(OperationVS operation) throws Exception{
        log.info("saveReceipt");
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                ContextVS.getMessage("signedFileFileFilterMsg"), "*" + ContentTypeVS.SIGNED.getExtension());
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.setInitialFileName(ContextVS.getMessage("genericReceiptFileName"));
        File file = fileChooser.showSaveDialog(new Stage());
        if(file != null){
            FileUtils.copyStringToFile(operation.getMessage(), file);
            BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_OK,
                    null, operation.getTabId(), operation.getCallerCallback()));
        } else BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_ERROR,
                null, operation.getTabId(), operation.getCallerCallback()));
    }

    public static void saveReceiptAnonymousDelegation(OperationVS operation) throws Exception{
        log.info("saveReceiptAnonymousDelegation - hashCertVSBase64: " + operation.getMessage() +
                " - callbackId: " + operation.getCallerCallback());
        Platform.runLater(() -> {
            ResponseVS responseVS = ContextVS.getInstance().getHashCertVSData(operation.getMessage());
            if (responseVS == null) {
                log.log(Level.SEVERE,"Missing receipt data for hash: " + operation.getMessage());
                BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_ERROR,
                        null, operation.getTabId(), operation.getCallerCallback()));
            } else {
                File fileToSave = null;
                try {
                    fileToSave = Utils.getReceiptBundle(responseVS);
                    FileChooser fileChooser = new FileChooser();
                    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Zip (*.zip)",
                            "*" + ContentTypeVS.ZIP.getExtension());
                    fileChooser.getExtensionFilters().add(extFilter);
                    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                    fileChooser.setInitialFileName(ContextVS.getMessage("anonymousDelegationReceiptFileName"));
                    File file = fileChooser.showSaveDialog(new Stage());
                    if (file != null) {
                        FileUtils.copyStreamToFile(new FileInputStream(fileToSave), file);
                        BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_OK,
                                null, operation.getTabId(), operation.getCallerCallback()));
                    } else BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_ERROR,
                            null, operation.getTabId(), operation.getCallerCallback()));
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        });
    }

    public static File getReceiptBundle(ResponseVS responseVS) throws Exception {
        Map delegationDataMap = (Map) responseVS.getData();
        java.util.List<File> fileList = new ArrayList<File>();
        File cmsTempFile = File.createTempFile(ContextVS.RECEIPT_FILE_NAME, ContentTypeVS.SIGNED.getExtension());
        cmsTempFile.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(responseVS.getCMS().toPEM()), cmsTempFile);
        File certVSDataFile = File.createTempFile(ContextVS.CANCEL_DATA_FILE_NAME, "");
        certVSDataFile.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(
                JSON.getMapper().writeValueAsString(delegationDataMap).getBytes("UTF-8")), certVSDataFile);
        fileList.add(certVSDataFile);
        fileList.add(cmsTempFile);
        File outputZip = File.createTempFile(ContextVS.CANCEL_BUNDLE_FILE_NAME, ".zip");
        outputZip.deleteOnExit();
        FileUtils.packZip(outputZip, fileList);
        return outputZip;
    }

    public static void showWalletNotFoundMessage() {
        Button optionButton = new Button(ContextVS.getMessage("newWalletButton"));
        optionButton.setOnAction(event -> {
            PlatformImpl.runLater(() -> {
                PasswordDialog.showWithPasswordConfirm(password -> {
                    if(password != null) BrowserHost.getInstance().createEmptyWallet(password);
                }, ContextVS.getMessage("newWalletPinMsg"));
            });
        });
        BrowserHost.showMessage(ContextVS.getMessage("errorLbl"), ContextVS.getMessage("walletNotFoundMessage"),
                optionButton, null);
    }

    public static void addTextLimiter(final TextInputControl tf, final int maxLength) {
        tf.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(final ObservableValue<? extends String> ov, final String oldValue, final String newValue) {
                if (tf.getText().length() > maxLength) {
                    String s = tf.getText().substring(0, maxLength);
                    tf.setText(s);
                }
            }
        });
    }

    public static boolean checkConnection() {
        if(!WebSocketService.getInstance().isConnected()) {
            Button connectionButton = new Button(ContextVS.getMessage("connectLbl"),
                    Utils.getIcon(FontAwesome.Glyph.CLOUD_UPLOAD));
            connectionButton.setOnAction(event -> WebSocketService.getInstance().setConnectionEnabled(true));
            BrowserHost.showMessage(null, ContextVS.getMessage("authenticatedWebSocketConnectionRequiredMsg"),
                    connectionButton, null);
            return false;
        } else return true;
    }

    static class Delta { double x, y; }

}
