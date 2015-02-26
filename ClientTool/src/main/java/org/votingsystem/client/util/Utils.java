package org.votingsystem.client.util;

import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.GlyphIcon;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
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
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.service.SessionService;
import org.votingsystem.model.*;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.Wallet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Utils {

    public static final String APPLICATION_ICON = "mail-mark-unread.png";

    public static final String COLOR_BUTTON_OK = "#888";
    public static final String COLOR_RED = "#ba0011";
    public static final String COLOR_RED_DARK = "#6c0404";
    public static final String COLOR_YELLOW_ALERT = "#fa1";


    public static final String EVENT_TYPE_CLICK = "click";
    public static final String EVENT_TYPE_MOUSEOVER = "mouseover";
    public static final String EVENT_TYPE_MOUSEOUT = "mouseclick";

    private static Logger log = Logger.getLogger(Utils.class);

    public static Text getIcon(FontAwesomeIconName icon) {
        Text text = GlyphsDude.createIcon(icon, GlyphIcon.DEFAULT_ICON_SIZE);
        text.setFill(Color.web(COLOR_BUTTON_OK));
        return text;
    }

    public static Text getIcon(FontAwesomeIconName icon, String color, double size) {
        Text text = GlyphsDude.createIcon(icon, String.valueOf(size));
        text.setFill(Color.web(color));
        return text;
    }

    public static Text getIcon(FontAwesomeIconName icon, double size) {
        Text text = GlyphsDude.createIcon(icon, String.valueOf(size));
        return text;
    }

    public static Text getIcon(FontAwesomeIconName icon, String color) {
        Text text = GlyphsDude.createIcon(icon, GlyphIcon.DEFAULT_ICON_SIZE);
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
            log.error(" ### iconPath: " + iconPath + " not found");
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
                            String href = ((Element)evt.getTarget()).getAttribute("href");
                            evt.preventDefault();
                            BrowserVS.getInstance().newTab(href, null, null);
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
        log.debug(" - checkServer: " + serverURL);
        ActorVS actorVS = ContextVS.getInstance().checkServer(serverURL.trim());
        if (actorVS == null) {
            String serverInfoURL = ActorVS.getServerInfoURL(serverURL);
            ResponseVS responseVS = HttpHelper.getInstance().getData(serverInfoURL, ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                actorVS = ActorVS.parse((Map) responseVS.getMessageJSON());
                responseVS.setData(actorVS);
                log.error("checkServer - adding " + serverURL.trim() + " to sever map");
                switch (actorVS.getType()) {
                    case ACCESS_CONTROL:
                        ContextVS.getInstance().setAccessControl((AccessControlVS) actorVS);
                        break;
                    case COOINS:
                        ContextVS.getInstance().setCooinServer((CooinServer) actorVS);
                        ContextVS.getInstance().setTimeStampServerCert(actorVS.getTimeStampCert());
                        break;
                    case CONTROL_CENTER:
                        ContextVS.getInstance().setControlCenter((ControlCenterVS) actorVS);
                        break;
                    default:
                        log.debug("Unprocessed actor:" + actorVS.getType());
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
        JSONObject messageJSON = (JSONObject) JSONSerializer.toJSON(delegationDataMap);
        java.util.List<File> fileList = new ArrayList<File>();
        File smimeTempFile = File.createTempFile(ContextVS.RECEIPT_FILE_NAME, ContentTypeVS.SIGNED.getExtension());
        smimeTempFile.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(responseVS.getSMIME().getBytes()), smimeTempFile);
        File certVSDataFile = File.createTempFile(ContextVS.CANCEL_DATA_FILE_NAME, "");
        certVSDataFile.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(messageJSON.toString().getBytes("UTF-8")), certVSDataFile);
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

    public static void saveReceiptAnonymousDelegation(OperationVS operation, WebKitHost webKitHost) throws Exception{
        log.debug("saveReceiptAnonymousDelegation - hashCertVSBase64: " + operation.getMessage() +
                " - callbackId: " + operation.getCallerCallback());
        Platform.runLater(() -> {
            ResponseVS responseVS = ContextVS.getInstance().getHashCertVSData(operation.getMessage());
            if (responseVS == null) {
                log.error("Missing receipt data for hash: " + operation.getMessage());
                webKitHost.invokeBrowserCallback(Utils.getMessageToBrowser(ResponseVS.SC_ERROR, null),
                        operation.getCallerCallback());
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
                        webKitHost.invokeBrowserCallback(Utils.getMessageToBrowser(ResponseVS.SC_OK, null),
                                operation.getCallerCallback());
                    } else webKitHost.invokeBrowserCallback(Utils.getMessageToBrowser(ResponseVS.SC_ERROR, null),
                            operation.getCallerCallback());
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        });
    }

    public static String getSessionCoreSignalJSCommand(JSONObject sessionDataJSON) {
        JSONObject coreSignal = new JSONObject();
        //this.fire('core-signal', {name: "vs-session-data", data: sessionDataJSON});
        coreSignal.put("name", "vs-session-data");
        coreSignal.put("data", sessionDataJSON);
        String jsCommand = null;
        try {
            jsCommand = "fireCoreSignal('" + Base64.getEncoder().encodeToString(
                    coreSignal.toString().getBytes("UTF-8")) + "')";
        } catch (UnsupportedEncodingException ex) { log.error(ex.getMessage(), ex); }
        return jsCommand;
    }

    public static void selectImage(final OperationVS operationVS, WebKitHost webKitHost) throws Exception {
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
                    log.debug(" - imageFileBytes.length: " + imageFileBytes.length);
                    if (imageFileBytes.length > ContextVS.IMAGE_MAX_FILE_SIZE) {
                        log.debug(" - MAX_FILE_SIZE exceeded ");
                        webKitHost.invokeBrowserCallback(getMessageToBrowser(ResponseVS.SC_ERROR,
                                        ContextVS.getMessage("fileSizeExceeded", ContextVS.IMAGE_MAX_FILE_SIZE_KB)),
                                operationVS.getCallerCallback());
                    } else webKitHost.invokeBrowserCallback(getMessageToBrowser(ResponseVS.SC_OK,
                            selectedImage.getAbsolutePath()), operationVS.getCallerCallback());
                } else webKitHost.invokeBrowserCallback(getMessageToBrowser(ResponseVS.SC_ERROR, null),
                        operationVS.getCallerCallback());
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                webKitHost.invokeBrowserCallback(getMessageToBrowser(ResponseVS.SC_ERROR, ex.getMessage()),
                        operationVS.getCallerCallback());
            }
        });
    }

    public static void receiptCancellation(final OperationVS operationVS, WebKitHost webKitHost) throws Exception {
        log.debug("receiptCancellation");
        switch(operationVS.getType()) {
            case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED:
                PlatformImpl.runLater(() -> {
                    FileChooser fileChooser = new FileChooser();
                    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Zip (*.zip)",
                            "*" + ContentTypeVS.ZIP.getExtension());
                    fileChooser.getExtensionFilters().add(extFilter);
                    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                    File file = fileChooser.showSaveDialog(new Stage());
                    if(file != null){
                        operationVS.setFile(file);
                        webKitHost.processOperationVS(operationVS, null);
                    } else webKitHost.invokeBrowserCallback(getMessageToBrowser(ResponseVS.SC_ERROR, null),
                            operationVS.getCallerCallback());
                });
                break;
            default:
                log.debug("receiptCancellation - unknown receipt type: " + operationVS.getType());
        }
    }

    public static void selectKeystoreFile(OperationVS operationVS, WebKitHost webKitHost) {
        log.debug("selectKeystoreFile");
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
                        PasswordDialog passwordDialog = new PasswordDialog();
                        passwordDialog.show(ContextVS.getMessage("newKeyStorePasswordMsg"));
                        String password = passwordDialog.getPassword();
                        ContextVS.saveUserKeyStore(userKeyStore, password);
                        ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN,
                                CryptoTokenVS.JKS_KEYSTORE.toString());
                        SessionService.getInstance().setUserVS(userVS, false);
                        JSONObject userDataJSON = userVS.toJSON();
                        userDataJSON.put("statusCode", ResponseVS.SC_OK);
                        if(operationVS != null) webKitHost.invokeBrowserCallback(
                                userDataJSON, operationVS.getCallerCallback());
                    } catch(Exception ex) {
                        log.error(ex.getMessage(), ex);
                        if(operationVS != null) webKitHost.invokeBrowserCallback(getMessageToBrowser(ResponseVS.SC_ERROR,
                                ex.getMessage()), operationVS.getCallerCallback());
                    }

                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        });
    }

    public static void saveReceipt(OperationVS operation, WebKitHost webKitHost) throws Exception{
        log.debug("saveReceipt");
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                ContextVS.getMessage("signedFileFileFilterMsg"), "*" + ContentTypeVS.SIGNED.getExtension());
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.setInitialFileName(ContextVS.getMessage("genericReceiptFileName"));
        File file = fileChooser.showSaveDialog(new Stage());
        if(file != null){
            FileUtils.copyStringToFile(operation.getMessage(), file);
            webKitHost.invokeBrowserCallback(getMessageToBrowser(ResponseVS.SC_OK, null), operation.getCallerCallback());
        } else webKitHost.invokeBrowserCallback(getMessageToBrowser(ResponseVS.SC_ERROR, null),
                operation.getCallerCallback());
    }

    public static JSONObject getMessageToBrowser(int statusCode, String message) {
        Map resultMap = new HashMap();
        resultMap.put("statusCode", statusCode);
        resultMap.put("message", message);
        return (JSONObject)JSONSerializer.toJSON(resultMap);
    }

    public static void createNewWallet() {
        PlatformImpl.runLater(() -> {
            PasswordDialog passwordDialog = new PasswordDialog();
            passwordDialog.show(ContextVS.getMessage("newWalletPinMsg"));
            String password = passwordDialog.getPassword();
            if(password != null) {
                try {
                    Wallet.createWallet(new JSONArray(), password);
                } catch (Exception ex) { log.error(ex.getMessage(), ex); }
            }
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
