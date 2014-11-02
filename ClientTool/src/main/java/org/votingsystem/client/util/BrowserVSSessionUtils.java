package org.votingsystem.client.util;

import com.sun.javafx.application.PlatformImpl;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.UUID;

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
                sessionDataJSON.put("deviceId", UUID.randomUUID().toString());
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

    public String getDeviceId() {
        return sessionDataJSON.getString("deviceId");
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

    public void setCSRRequest(Long requestId, Encryptor.EncryptedBundle bundle) {
        try {
            File csrFile = new File(ContextVS.APPDIR + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
            csrFile.createNewFile();
            JSONObject jsonData = bundle.toJSON();
            jsonData.put("requestId", requestId);
            flush();
            FileUtils.copyStreamToFile(new ByteArrayInputStream(jsonData.toString().getBytes()), csrFile);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }


    public void checkCSRRequest() {
        PlatformImpl.runLater(new Runnable() { @Override public void run() { checkCSR(); } });
    }

    private void checkCSR() {
        File csrFile = new File(ContextVS.APPDIR + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
        if(csrFile.exists()) {
            log.debug("csr request found");
            try {
                JSONObject jsonData = (JSONObject) JSONSerializer.toJSON(FileUtils.getStringFromFile(csrFile));
                String serviceURL = ContextVS.getInstance().getAccessControl().getUserCSRServiceURL(
                        jsonData.getLong("requestId"));
                ResponseVS responseVS = HttpHelper.getInstance().getData(serviceURL, null);
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(
                            responseVS.getMessage().getBytes());
                    X509Certificate userCert = certificates.iterator().next();
                    UserVS user = UserVS.getUserVS(userCert);
                    setUserVS(user, false);
                    log.debug("user: " + user.getNif() + " - certificates.size(): " + certificates.size());
                    X509Certificate[] certsArray = new X509Certificate[certificates.size()];
                    certificates.toArray(certsArray);
                    String passwd = null;
                    byte[] serializedCertificationRequest = null;
                    while(passwd == null) {
                        PasswordDialog passwordDialog = new PasswordDialog();
                        passwordDialog.show(ContextVS.getMessage("csrPasswMsg"));
                        passwd = passwordDialog.getPassword();
                        if(passwd == null) {
                            showMessage(ContextVS.getMessage("certPendingMissingPasswdMsg"));
                            return;
                        }
                        Encryptor.EncryptedBundle bundle = Encryptor.EncryptedBundle.parse(jsonData);
                        try {
                            serializedCertificationRequest = Encryptor.pbeAES_Decrypt(passwd, bundle);
                        } catch (Exception ex) {
                            passwd = null;
                            showMessage(ContextVS.getMessage("cryptoTokenPasswdErrorMsg"));
                        }
                    }

                    CertificationRequestVS certificationRequest =
                            (CertificationRequestVS) ObjectUtils.deSerializeObject(serializedCertificationRequest);
                    KeyStore userKeyStore = KeyStore.getInstance("JKS");
                    userKeyStore.load(null);
                    userKeyStore.setKeyEntry(ContextVS.KEYSTORE_USER_CERT_ALIAS, certificationRequest.getPrivateKey(),
                            passwd.toCharArray(), certsArray);
                    ContextVS.saveUserKeyStore(userKeyStore, passwd);
                    ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN,
                            CryptoTokenVS.JKS_KEYSTORE.toString());
                    showMessage(ContextVS.getMessage("certInstallOKMsg"));
                } else showMessage(ContextVS.getMessage("certPendingMsg"));
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                showMessage(ContextVS.getMessage("errorStoringKeyStoreMsg"));
            }
        }

    }

    public void showMessage(final String message) {
        PlatformImpl.runLater(new Runnable() {
            @Override
            public void run() {
                new MessageDialog().showHtmlMessage(message);
            }
        });
    }

    private void flush() {
        try {
            FileUtils.copyStreamToFile(new ByteArrayInputStream(sessionDataJSON.toString().getBytes()), sessionFile);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}
