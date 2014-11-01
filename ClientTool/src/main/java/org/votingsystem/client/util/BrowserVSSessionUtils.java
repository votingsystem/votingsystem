package org.votingsystem.client.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

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
            if(sessionDataJSON.get("userVS") != null) userVS = UserVS.parse((java.util.Map) sessionDataJSON.get("userVS"));
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

    public void setCSRRequestId(Long id) {
        sessionDataJSON.put("csrRequestId", id);
        flush();
    }

    public Long getCSRRequestId() {
        return sessionDataJSON.getLong("csrRequestId");
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS, boolean isConnected) throws Exception {
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
        sessionDataJSON.put("isConnected", isConnected);
        sessionDataJSON.put("userVS", userVS.toJSON());
        flush();
    }

    public JSONObject getSessionData() {
        return sessionDataJSON;
    }

    public static void setCSRRequest(Long requestId, Encryptor.EncryptedBundle bundle) {
        try {
            File csrFile = new File(ContextVS.APPDIR + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
            csrFile.createNewFile();
            JSONObject jsonData = bundle.toJSON();
            jsonData.put("requestId", requestId);
            FileUtils.copyStreamToFile(new ByteArrayInputStream(jsonData.toString().getBytes()), csrFile);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public static ResponseVS checkCSRRequest() {
        File csrFile = new File(ContextVS.APPDIR + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
        if(csrFile.exists()) {
            try {
                JSONObject jsonData = (JSONObject) JSONSerializer.toJSON(FileUtils.getStringFromFile(csrFile));
                String serviceURL = ContextVS.getInstance().getAccessControl().getUserCSRServiceURL(
                        jsonData.getLong("requestId"));
                ResponseVS responseVS = HttpHelper.getInstance().getData(serviceURL, null);
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                } else return responseVS;
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                if(csrFile != null && csrFile.exists()) csrFile.delete();
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("checkCertRequestErrorMsg"));
            }
        }
        return null;
    }

    private void flush() {
        try {
            FileUtils.copyStreamToFile(new ByteArrayInputStream(sessionDataJSON.toString().getBytes()), sessionFile);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}
