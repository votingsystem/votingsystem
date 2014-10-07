package org.votingsystem.client.util;

import javafx.scene.image.Image;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by jgzornoza on 10/05/14.
 */
public class Utils {

    private static Logger logger = Logger.getLogger(Utils.class);

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
            logger.error(" ### iconPath: " + iconPath + " not found");
            image = new Image(baseObject.getClass().getResourceAsStream(
                    "/resources/icon_32/button_default.png"));
        }
        return image;
    }

    public static File getReceiptBundle(ResponseVS responseVS) throws Exception {
        Map delegationDataMap = (Map) responseVS.getData();
        JSONObject messageJSON = (JSONObject) JSONSerializer.toJSON(delegationDataMap);
        java.util.List<File> fileList = new ArrayList<File>();
        File smimeTempFile = File.createTempFile(ContextVS.RECEIPT_FILE_NAME, ContentTypeVS.SIGNED.getExtension());
        smimeTempFile.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(responseVS.getSmimeMessage().getBytes()), smimeTempFile);
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

}
