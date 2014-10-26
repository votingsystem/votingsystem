package org.votingsystem.client.util;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import org.votingsystem.model.*;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Utils {

    public static final String COLOR_BUTTON_OK = "#388746";
    public static final String COLOR_RED_DARK = "#6c0404";

    private static Logger log = Logger.getLogger(Utils.class);

    private static GlyphFont fontAwesome = new GlyphFont("FontAwesome", 16,
           Utils.class.getResourceAsStream("/css/fontawesome-webfont.ttf"));


    public static Glyph getImage(FontAwesome.Glyph font) {
        return fontAwesome.create(font).color(Color.web(COLOR_BUTTON_OK));
    }

    public static Glyph getImage(String unicode) {
        return fontAwesome.create(unicode).color(Color.web(COLOR_BUTTON_OK));
    }

    public static Glyph getImage(FontAwesome.Glyph font, double size) {
        return fontAwesome.create(font).color(Color.web(COLOR_BUTTON_OK)).size(size);
    }

    public static Glyph getImage(FontAwesome.Glyph font, String color) {
        return fontAwesome.create(font).color(Color.web(color));
    }

    public static Glyph getImage(FontAwesome.Glyph font, String color, double size) {
        return fontAwesome.create(font).color(Color.web(color)).size(size);
    }

    public static Image getImage(Object baseObject, String key) {
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


    public static Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
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
                    case VICKETS:
                        ContextVS.getInstance().setVicketServer((VicketServer) actorVS);
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

    public static String getTagDescription(String tagName) {
        if(TagVS.WILDTAG.equals(tagName)) return ContextVS.getMessage("wildTagLbl");
        else return tagName;

    }

    public static String getTagForDescription(String tagName) {
        return ContextVS.getMessage("forLbl") + " " + getTagDescription(tagName);
    }

    public static void saveReceiptAnonymousDelegation(OperationVS operation, WebKitHost webKitHost) throws Exception{
        log.debug("saveReceiptAnonymousDelegation - hashCertVSBase64: " + operation.getMessage() +
                " - callbackId: " + operation.getCallerCallback());
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ResponseVS responseVS = ContextVS.getInstance().getHashCertVSData(operation.getMessage());
                if (responseVS == null) {
                    log.error("Missing receipt data for hash: " + operation.getMessage());
                    webKitHost.sendMessageToBrowser(ResponseVS.SC_ERROR, null, operation.getCallerCallback());
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
                            webKitHost.sendMessageToBrowser(ResponseVS.SC_OK, null, operation.getCallerCallback());
                        } else
                            webKitHost.sendMessageToBrowser(ResponseVS.SC_ERROR, null, operation.getCallerCallback());
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
            }
        });
    }

    public static void selectImage(final OperationVS operationVS, WebKitHost webKitHost) throws Exception {
        PlatformImpl.runLater(new Runnable() {
            @Override
            public void run() {
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
                            webKitHost.sendMessageToBrowser(ResponseVS.SC_ERROR,
                                    ContextVS.getMessage("fileSizeExceeded", ContextVS.IMAGE_MAX_FILE_SIZE_KB),
                                    operationVS.getCallerCallback());
                        } else webKitHost.sendMessageToBrowser(ResponseVS.SC_OK, selectedImage.getAbsolutePath(),
                                operationVS.getCallerCallback());
                    } else webKitHost.sendMessageToBrowser(ResponseVS.SC_ERROR, null, operationVS.getCallerCallback());
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    webKitHost.sendMessageToBrowser(ResponseVS.SC_ERROR, ex.getMessage(), operationVS.getCallerCallback());
                }
            }
        });
    }

    public static void receiptCancellation(final OperationVS operationVS, WebKitHost webKitHost) throws Exception {
        log.debug("receiptCancellation");
        switch(operationVS.getType()) {
            case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED:
                PlatformImpl.runLater(new Runnable() {
                    @Override public void run() {
                        FileChooser fileChooser = new FileChooser();
                        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Zip (*.zip)",
                                "*" + ContentTypeVS.ZIP.getExtension());
                        fileChooser.getExtensionFilters().add(extFilter);
                        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                        File file = fileChooser.showSaveDialog(new Stage());
                        if(file != null){
                            operationVS.setFile(file);
                            webKitHost.processOperationVS(operationVS);
                        } else webKitHost.sendMessageToBrowser(ResponseVS.SC_ERROR, null, operationVS.getCallerCallback());
                    }
                });
                break;
            default:
                log.debug("receiptCancellation - unknown receipt type: " + operationVS.getType());
        }
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
            webKitHost.sendMessageToBrowser(ResponseVS.SC_OK, null, operation.getCallerCallback());
        } else webKitHost.sendMessageToBrowser(ResponseVS.SC_ERROR, null, operation.getCallerCallback());
    }

}
