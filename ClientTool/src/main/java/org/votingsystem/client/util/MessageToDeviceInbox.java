package org.votingsystem.client.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.WebSocketMessage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageToDeviceInbox {

    private static Logger log = Logger.getLogger(MessageToDeviceInbox.class);

    private List<WebSocketMessage> webSocketMessageList = new ArrayList<>();
    private File sessionFile;
    private static final MessageToDeviceInbox INSTANCE = new MessageToDeviceInbox();


    public static MessageToDeviceInbox getInstance() {
        return INSTANCE;
    }

    private MessageToDeviceInbox() {
        JSONArray messageArray = null;
        try {
            sessionFile = new File(ContextVS.APPDIR + File.separator + ContextVS.INBOX_FILE);
            if(sessionFile.createNewFile()) {
                messageArray = new JSONArray();
                flush();
            }
            else messageArray = (JSONArray) JSONSerializer.toJSON(FileUtils.getStringFromFile(sessionFile));
            for(int i = 0; i < messageArray.size(); i++) {
                webSocketMessageList.add(new WebSocketMessage((net.sf.json.JSONObject) messageArray.get(i)));
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void addMessage(WebSocketMessage webSocketMessage) {
        webSocketMessage.setDate(Calendar.getInstance().getTime());
        webSocketMessageList.add(webSocketMessage);
        flush();
    }

    public List<WebSocketMessage> getMessageList() {
        return new ArrayList<>(webSocketMessageList);
    }

    public void resetMessageList() {
        webSocketMessageList = new ArrayList<>();
    }

    private void flush() {
        log.debug("flush");
        try {
            JSONArray messageArray = new JSONArray();
            for(WebSocketMessage webSocketMessage: webSocketMessageList) {
                messageArray.add(webSocketMessage.getMessageJSON());
            }
            FileUtils.copyStreamToFile(new ByteArrayInputStream(messageArray.toString().getBytes()), sessionFile);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}
