package org.votingsystem.client.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSSessionUtils {

    private static Logger log = Logger.getLogger(BrowserVSSessionUtils.class);

    private UserVS userVS;
    private File sessionFile;
    private JSONObject sessionDataJSON;

    private static final BrowserVSSessionUtils INSTANCE = new BrowserVSSessionUtils();

    private BrowserVSSessionUtils() {
        try {
            sessionFile = new File(ContextVS.APPDIR + File.separator + ContextVS.BROWSER_SESSION_FILE_NAME);
            if(sessionFile.createNewFile()) {
                sessionDataJSON = new JSONObject();
            } else sessionDataJSON = (JSONObject) JSONSerializer.toJSON(FileUtils.getStringFromFile(sessionFile));
            sessionDataJSON.put("isConnected", false);
            userVS = UserVS.parse((java.util.Map) sessionDataJSON.get("userVS"));
            flush();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public static BrowserVSSessionUtils getInstance() {
        return INSTANCE;
    }

    public void setIsConnected(boolean isConnected) {
        sessionDataJSON.put("isConnected", isConnected);
        flush();
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) throws Exception {
        this.userVS = userVS;
        JSONArray userVSList = null;
        if(sessionDataJSON.has("userVSList")) {
            userVSList = sessionDataJSON.getJSONArray("userVSList");
            boolean updated = false;
            for(int i = 0; i < userVSList.size(); i++) {
                JSONObject user = (JSONObject) userVSList.get(i);
                if(user.getString("nif").equals(userVS.getNif())) {
                    userVSList.remove(i);
                    userVSList.add(userVS.toJSON());
                    updated = true;
                }
            }
            if(!updated) userVSList.add(userVS.toJSON());
        } else {
            userVSList = new JSONArray();
            userVSList.add(userVS.toJSON());
            sessionDataJSON.put("userVSList", userVSList);
        }
        sessionDataJSON.put("isConnected", true);
        sessionDataJSON.put("userVS", userVS.toJSON());
        flush();
    }

    public JSONObject getSessionData() {
        return sessionDataJSON;
    }

    private void flush() {
        try {
            FileUtils.copyStreamToFile(new ByteArrayInputStream(sessionDataJSON.toString().getBytes()), sessionFile);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}
