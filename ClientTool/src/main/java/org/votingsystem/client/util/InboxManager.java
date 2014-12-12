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
import java.util.stream.Collectors;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxManager {

    private static Logger log = Logger.getLogger(InboxManager.class);

    private List<WebSocketMessage> webSocketMessageList = new ArrayList<>();
    private File messagesFile;
    private static final InboxManager INSTANCE = new InboxManager();


    public static InboxManager getInstance() {
        return INSTANCE;
    }

    private InboxManager() {
        JSONArray messageArray = null;
        try {
            messagesFile = new File(ContextVS.APPDIR + File.separator + ContextVS.INBOX_FILE);
            if(messagesFile.createNewFile()) {
                messageArray = new JSONArray();
                flush();
            }
            else messageArray = (JSONArray) JSONSerializer.toJSON(FileUtils.getStringFromFile(messagesFile));
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

    public void removeMessage(WebSocketMessage webSocketMessage) {
        webSocketMessageList = webSocketMessageList.stream().filter(m -> !m.getUUID().equals(
                webSocketMessage.getUUID())).collect(Collectors.toList());
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
            FileUtils.copyStreamToFile(new ByteArrayInputStream(messageArray.toString().getBytes()), messagesFile);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}
