package org.votingsystem.client.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.GlyphIcon;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.votingsystem.client.Browser;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.service.BrowserSessionService;
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
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.Browser.showMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Utils {

    public static final String APPLICATION_ICON = "mail-mark-unread.png";

    public static final String COLOR_BUTTON_OK = "#888";
    public static final String COLOR_RESULT_OK = "#388746";
    public static final String COLOR_RED = "#ba0011";
    public static final String COLOR_RED_DARK = "#6c0404";
    public static final String COLOR_YELLOW_ALERT = "#fa1";


    public static final String EVENT_TYPE_CLICK = "click";
    public static final String EVENT_TYPE_MOUSEOVER = "mouseover";
    public static final String EVENT_TYPE_MOUSEOUT = "mouseclick";

    private static Logger log = Logger.getLogger(Utils.class.getSimpleName());

    public static Text getIcon(FontAwesomeIcon icon) {
        Text text = GlyphsDude.createIcon(icon, GlyphIcon.DEFAULT_FONT_SIZE);
        text.setFill(Color.web(COLOR_BUTTON_OK));
        return text;
    }

    public static Text getIcon(FontAwesomeIcon icon, String color, double size) {
        Text text = GlyphsDude.createIcon(icon, String.valueOf(size));
        text.setFill(Color.web(color));
        return text;
    }

    public static Text getIcon(FontAwesomeIcon icon, double size) {
        Text text = GlyphsDude.createIcon(icon, String.valueOf(size));
        return text;
    }

    public static Text getIcon(FontAwesomeIcon icon, String color) {
        Text text = GlyphsDude.createIcon(icon, GlyphIcon.DEFAULT_FONT_SIZE);
        text.setFill(Color.web(color));
        return text;
    }

    public static Image getIcon(Object baseObject, String key) {
        String iconPath = null;
        String iconName = null;
        Image image = null;
        if(key.endsWith("_16")) {
            iconName = key.substring(0, key.indexOf("_16"));
            iconPath = "/resources/icon_16/" + iconName + ".png";
        } else if(key.endsWith("_32")) {
            iconName = key.substring(0, key.indexOf("_32"));
            iconPath = "/resources/icon_32/" + iconName + ".png";
        } else {//defaults to 16x16 icons
            iconPath = "/resources/icon_16/" + key + ".png";
        }
        try {
            image = new Image(baseObject.getClass().getResourceAsStream(iconPath));
        } catch(Exception ex) {
            log.log(Level.SEVERE," ### iconPath: " + iconPath + " not found");
            image = new Image(baseObject.getClass().getResourceAsStream(
                    "/resources/icon_32/button_default.png"));
        }
        return image;
    }

    public static String getResource(String path) {
        return VotingSystemApp.class.getResource(path).toExternalForm();
    }

    public static Image getIconFromResources(String imageFilename) {
        return new Image(getResource("/images/" + imageFilename));
    }

    public static Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public static Region getSpacer(){
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public static Tab getTab(Pane content, String caption) {
        Tab newTab = new Tab();
        newTab.setText(caption);
        newTab.setContent(content);
        return newTab;
    }

    public static Button getToolBarButton(Text icon) {
        Button toolBarButton = new Button();
        toolBarButton.setGraphic(icon);
        toolBarButton.getStyleClass().add("toolbar-button");
        return toolBarButton;
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
                            Browser.getInstance().newTab(href, null, null);
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

    public static ResponseVS<ActorVS> checkServer(String serverURL) throws Exception {
        log.info(" - checkServer: " + serverURL);
        ActorVS actorVS = ContextVS.getInstance().checkServer(serverURL.trim());
        if (actorVS == null) {
            String serverInfoURL = ActorVS.getServerInfoURL(serverURL);
            ResponseVS responseVS = HttpHelper.getInstance().getData(serverInfoURL, ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                actorVS = ((ActorVSDto) responseVS.getMessage(ActorVSDto.class)).getActorVS();
                responseVS.setData(actorVS);
                log.log(Level.SEVERE,"checkServer - adding " + serverURL.trim() + " to sever map");
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

    public static String getTagForDescription(String tagName) {
        return ContextVS.getMessage("forLbl") + " " + MsgUtils.getTagDescription(tagName);
    }

    public static void saveReceiptAnonymousDelegation(OperationVS operation, BrowserVS browserVS) throws Exception{
        log.info("saveReceiptAnonymousDelegation - hashCertVSBase64: " + operation.getMessage() +
                " - callbackId: " + operation.getCallerCallback());
        Platform.runLater(() -> {
            ResponseVS responseVS = ContextVS.getInstance().getHashCertVSData(operation.getMessage());
            if (responseVS == null) {
                log.log(Level.SEVERE,"Missing receipt data for hash: " + operation.getMessage());
                try {
                    browserVS.invokeBrowserCallback(MessageDto.ERROR(null),
                            operation.getCallerCallback());
                } catch (JsonProcessingException ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
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
                        browserVS.invokeBrowserCallback(MessageDto.OK(null), operation.getCallerCallback());
                    } else browserVS.invokeBrowserCallback(MessageDto.ERROR(null), operation.getCallerCallback());
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        });
    }

    public static void selectImage(final OperationVS operationVS, BrowserVS browserVS) throws Exception {
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
                        browserVS.invokeBrowserCallback(MessageDto.ERROR(
                                        ContextVS.getMessage("fileSizeExceeded", ContextVS.IMAGE_MAX_FILE_SIZE_KB)),
                                operationVS.getCallerCallback());
                    } else browserVS.invokeBrowserCallback(MessageDto.OK(selectedImage.getAbsolutePath()),
                            operationVS.getCallerCallback());
                } else browserVS.invokeBrowserCallback(MessageDto.ERROR(null), operationVS.getCallerCallback());
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                try {
                    browserVS.invokeBrowserCallback(MessageDto.ERROR(ex.getMessage()),
                            operationVS.getCallerCallback());
                } catch (JsonProcessingException e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        });
    }

    public static void receiptCancellation(final OperationVS operationVS, BrowserVS browserVS) throws Exception {
        log.info("receiptCancellation");
        switch(operationVS.getType()) {
            case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION:
                PlatformImpl.runLater(() -> {
                    FileChooser fileChooser = new FileChooser();
                    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Zip (*.zip)",
                            "*" + ContentTypeVS.ZIP.getExtension());
                    fileChooser.getExtensionFilters().add(extFilter);
                    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                    File file = fileChooser.showSaveDialog(new Stage());
                    if(file != null){
                        operationVS.setFile(file);
                        browserVS.processOperationVS(operationVS, null);
                    } else try {
                        browserVS.invokeBrowserCallback(MessageDto.ERROR(null), operationVS.getCallerCallback());
                    } catch (JsonProcessingException ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                });
                break;
            default:
                log.info("receiptCancellation - unknown receipt type: " + operationVS.getType());
        }
    }


    public static void selectKeystoreFile(OperationVS operationVS, BrowserVS browserVS) {
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
                                    if(operationVS != null) browserVS.invokeBrowserCallback(
                                            UserVSDto.COMPLETE(userVS), operationVS.getCallerCallback());
                                } catch (Exception ex) {
                                    showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                                }
                            }
                        };
                        PasswordDialog.showWithPasswordConfirm(null, passwordListener, ContextVS.getMessage("newKeyStorePasswordMsg"));
                    } catch(Exception ex) {
                        log.log(Level.SEVERE,ex.getMessage(), ex);
                        if(operationVS != null) browserVS.invokeBrowserCallback(MessageDto.ERROR(ex.getMessage()),
                                operationVS.getCallerCallback());
                    }

                }
            } catch (Exception ex) {
                log.log(Level.SEVERE,ex.getMessage(), ex);
            }
        });
    }

    public static void saveReceipt(OperationVS operation, BrowserVS browserVS) throws Exception{
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
            browserVS.invokeBrowserCallback(MessageDto.OK(null), operation.getCallerCallback());
        } else browserVS.invokeBrowserCallback(MessageDto.ERROR(null), operation.getCallerCallback());
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

    public static ContextMenu createHistoryMenu(WebView webView) { // a menu of history items.
        final ContextMenu historyMenu = new ContextMenu();
        WebHistory history = webView.getEngine().getHistory();
        // determine an appropriate subset range of the history list to display.
        int minIdx = Math.max(0, history.getCurrentIndex() - 8); // min range (inclusive) of history items to show.
        int maxIdx = Math.min(history.getEntries().size(), history.getCurrentIndex() + 6); // min range (exclusive) of history items to show.
        // add menu items to the history list.
        for (int i = maxIdx - 1; i >= minIdx; i--) {
            final MenuItem nextMenuItem = new MenuItem(history.getEntries().get(i).getUrl());
            nextMenuItem.setOnAction(actionEvent -> webView.getEngine().load(nextMenuItem.getText()));
            historyMenu.getItems().add(nextMenuItem);
            if (i == history.getCurrentIndex()) {
                nextMenuItem.getStyleClass().add("current-menu");
            }
        }
        return historyMenu;
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

    public static void showWalletNotFoundMessage() {
        Button optionButton = new Button(ContextVS.getMessage("newWalletButton"));
        optionButton.setOnAction(event -> createNewWallet());
        showMessage(ContextVS.getMessage("walletNotFoundMessage"), optionButton);
    }

    static class Delta { double x, y; }
}
