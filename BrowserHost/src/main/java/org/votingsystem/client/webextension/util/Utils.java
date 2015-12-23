package org.votingsystem.client.webextension.util;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
import org.votingsystem.client.webextension.dialog.PasswordDialog;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.model.voting.ControlCenterVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.*;
import org.votingsystem.util.currency.Wallet;
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

    private static Logger log = Logger.getLogger(Utils.class.getSimpleName());

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

    public static ResponseVS<ActorVS> checkServer(String serverURL) throws Exception {
        log.info(" - checkServer: " + serverURL);
        ActorVS actorVS = ContextVS.getInstance().checkServer(serverURL.trim());
        if (actorVS == null) {
            String serverInfoURL = ActorVS.getServerInfoURL(serverURL);
            ResponseVS responseVS = HttpHelper.getInstance().getData(serverInfoURL, ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                actorVS = ((ActorVSDto) responseVS.getMessage(ActorVSDto.class)).getActorVS();
                responseVS.setData(actorVS);
                log.log(Level.INFO,"checkServer - adding " + serverURL.trim() + " to sever map");
                switch (actorVS.getType()) {
                    case ACCESS_CONTROL:
                        ContextVS.getInstance().setAccessControl((AccessControlVS) actorVS);
                        break;
                    case CURRENCY:
                        ContextVS.getInstance().setCurrencyServer((CurrencyServer) actorVS);
                        ContextVS.getInstance().setTimeStampServerCert(actorVS.getTimeStampCert());
                        break;
                    case CONTROL_CENTER:
                        ContextVS.getInstance().setControlCenter((ControlCenterVS) actorVS);
                        break;
                    default:
                        log.info("Unprocessed actor:" + actorVS.getType());
                }
            } else if (ResponseVS.SC_NOT_FOUND == responseVS.getStatusCode()) {
                responseVS.setMessage(ContextVS.getMessage("serverNotFoundMsg", serverURL.trim()));
            }
            return responseVS;
        } else {
            ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK);
            responseVS.setData(actorVS);
            return responseVS;
        }
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
                        UserVS userVS = UserVS.getUserVS((X509Certificate)
                                userKeyStore.getCertificate("UserTestKeysStore"));
                        PasswordDialog.Listener passwordListener = new PasswordDialog.Listener() {
                            @Override public void setPassword(TypeVS passwordType, char[] password) {
                                if(password == null) return;
                                try {
                                    ContextVS.saveUserKeyStore(userKeyStore, password);
                                    ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN,
                                            CryptoTokenVS.JKS_KEYSTORE.toString());
                                    BrowserSessionService.getInstance().setUserVS(userVS, false);
                                    if(operationVS != null) BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(
                                            ResponseVS.SC_OK, null, operationVS.getCallerCallback(), UserVSDto.COMPLETE(userVS)));
                                } catch (Exception ex) {
                                    BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                                }
                            }
                        };
                        PasswordDialog.showWithPasswordConfirm(null, passwordListener, ContextVS.getMessage("newKeyStorePasswordMsg"));
                    } catch(Exception ex) {
                        log.log(Level.SEVERE,ex.getMessage(), ex);
                        if(operationVS != null) BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(
                                ResponseVS.SC_ERROR, ex.getMessage(), operationVS.getCallerCallback(), null));
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
                                operationVS.getCallerCallback(), null));
                    } else BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_OK,
                            selectedImage.getAbsolutePath(), operationVS.getCallerCallback(), null));
                } else BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_ERROR,
                        null, operationVS.getCallerCallback(), null));
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_ERROR,
                        ex.getMessage(), operationVS.getCallerCallback(), null));
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
                    null, operation.getCallerCallback(), null));
        } else BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_ERROR,
                null, operation.getCallerCallback(), null));
    }

    public static void saveReceiptAnonymousDelegation(OperationVS operation) throws Exception{
        log.info("saveReceiptAnonymousDelegation - hashCertVSBase64: " + operation.getMessage() +
                " - callbackId: " + operation.getCallerCallback());
        Platform.runLater(() -> {
            ResponseVS responseVS = ContextVS.getInstance().getHashCertVSData(operation.getMessage());
            if (responseVS == null) {
                log.log(Level.SEVERE,"Missing receipt data for hash: " + operation.getMessage());
                BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_ERROR,
                        null, operation.getCallerCallback(), null));
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
                                null, operation.getCallerCallback(), null));
                    } else BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_ERROR,
                            null, operation.getCallerCallback(), null));
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        });
    }

    public static File getReceiptBundle(ResponseVS responseVS) throws Exception {
        Map delegationDataMap = (Map) responseVS.getData();
        java.util.List<File> fileList = new ArrayList<File>();
        File smimeTempFile = File.createTempFile(ContextVS.RECEIPT_FILE_NAME, ContentTypeVS.SIGNED.getExtension());
        smimeTempFile.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(responseVS.getSMIME().getBytes()), smimeTempFile);
        File certVSDataFile = File.createTempFile(ContextVS.CANCEL_DATA_FILE_NAME, "");
        certVSDataFile.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(
                JSON.getMapper().writeValueAsString(delegationDataMap).getBytes("UTF-8")), certVSDataFile);
        fileList.add(certVSDataFile);
        fileList.add(smimeTempFile);
        File outputZip = File.createTempFile(ContextVS.CANCEL_BUNDLE_FILE_NAME, ".zip");
        outputZip.deleteOnExit();
        FileUtils.packZip(outputZip, fileList);
        return outputZip;
    }

    public static void createNewWallet() {
        PlatformImpl.runLater(() -> {
            PasswordDialog.Listener passwordListener = new PasswordDialog.Listener() {
                @Override public void setPassword(TypeVS passwordType, char[] password) {
                    if(password != null) {
                        try {
                            Wallet.createWallet(new ArrayList<>(), password);
                        } catch (Exception ex) { log.log(Level.SEVERE,ex.getMessage(), ex); }
                    }
                }
            };
            PasswordDialog.showWithPasswordConfirm(null, passwordListener, ContextVS.getMessage("newWalletPinMsg"));
        });

    }

    public static void showWalletNotFoundMessage() {
        Button optionButton = new Button(ContextVS.getMessage("newWalletButton"));
        optionButton.setOnAction(event -> createNewWallet());
        BrowserHost.showMessage(ContextVS.getMessage("walletNotFoundMessage"), optionButton);
    }

    static class Delta { double x, y; }

}
