package org.votingsystem.client.util;

import com.sun.javafx.application.PlatformImpl;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.dnie.DNIeContentSigner;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SMIMESignedGeneratorVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.*;

import javax.mail.Header;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSSessionUtils {

    private static Logger log = Logger.getLogger(BrowserVSSessionUtils.class);

    private UserVS userVS;
    private File sessionFile;
    private JSONObject browserSessionDataJSON;
    private JSONObject sessionDataJSON;
    private static CountDownLatch countDownLatch;
    private static WebSocketUtils.RequestBundle requestBundle;
    private static SMIMEMessage smimeMessage;
    private static ResponseVS<SMIMEMessage> messageToDeviceResponse;

    private static final BrowserVSSessionUtils INSTANCE = new BrowserVSSessionUtils();

    private BrowserVSSessionUtils() {
        try {
            sessionFile = new File(ContextVS.APPDIR + File.separator + ContextVS.BROWSER_SESSION_FILE_NAME);
            if(sessionFile.createNewFile()) {
                sessionDataJSON = new JSONObject();
                browserSessionDataJSON = new JSONObject();
                browserSessionDataJSON.put("deviceId", UUID.randomUUID().toString());
                sessionDataJSON.put("browserSession", browserSessionDataJSON);
            } else {
                sessionDataJSON = (JSONObject) JSONSerializer.toJSON(FileUtils.getStringFromFile(sessionFile));
                browserSessionDataJSON = sessionDataJSON.getJSONObject("browserSession");
            }
            browserSessionDataJSON.put("isConnected", false);
            if(browserSessionDataJSON.get("userVS") != null) userVS = UserVS.parse((java.util.Map) browserSessionDataJSON.get("userVS"));
            flush();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public static BrowserVSSessionUtils getInstance() {
        return INSTANCE;
    }

    public void setIsConnected(boolean isConnected) {
        browserSessionDataJSON.put("isConnected", isConnected);
        flush();
    }

    public void setCSRRequestId(Long id) {
        browserSessionDataJSON.put("csrRequestId", id);
        flush();
    }

    public void setMobileCryptoToken(JSONObject deviceDataJSON) {
        sessionDataJSON.put("mobileCryptoToken", deviceDataJSON);
        flush();
    }

    //{"id":,"deviceName":"","certPEM":""}
    public JSONObject getMobileCryptoToken() {
        return sessionDataJSON.getJSONObject("mobileCryptoToken");
    }

    public String getMobileCryptoName() {
        return sessionDataJSON.getJSONObject("mobileCryptoToken").getString("deviceName");
    }

    public Long getCSRRequestId() {
        return browserSessionDataJSON.getLong("csrRequestId");
    }

    public String getDeviceId() {
        return browserSessionDataJSON.getString("deviceId");
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS, boolean isConnected) throws Exception {
        this.userVS = userVS;
        JSONArray userVSList = null;
        if(browserSessionDataJSON.has("userVSList")) {
            userVSList = browserSessionDataJSON.getJSONArray("userVSList");
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
            browserSessionDataJSON.put("userVSList", userVSList);
        }
        browserSessionDataJSON.put("isConnected", isConnected);
        browserSessionDataJSON.put("userVS", userVS.toJSON());
        flush();
    }

    public JSONObject getBrowserSessionData() {
        return browserSessionDataJSON;
    }

    public void setCSRRequest(Long requestId, Encryptor.EncryptedBundle bundle) {
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


    public void checkCSRRequest() {
        PlatformImpl.runLater(new Runnable() { @Override public void run() { checkCSR(); } });
    }

    private void deleteCSR() {
        log.debug("deleteCSR");
        File csrFile = new File(ContextVS.APPDIR + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
        if(csrFile.exists()) csrFile.delete();
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
                            Button optionButton = new Button(ContextVS.getMessage("deletePendingCsrMsg"));
                            optionButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
                            optionButton.setOnAction(new EventHandler<ActionEvent>() {
                                @Override public void handle(ActionEvent actionEvent) {
                                    deleteCSR();
                                }});
                            showMessage(ContextVS.getMessage("certPendingMissingPasswdMsg"), optionButton);
                            return;
                        }
                        Encryptor.EncryptedBundle bundle = Encryptor.EncryptedBundle.parse(jsonData);
                        try {
                            serializedCertificationRequest = Encryptor.pbeAES_Decrypt(passwd, bundle);
                        } catch (Exception ex) {
                            passwd = null;
                            showMessage(ContextVS.getMessage("cryptoTokenPasswdErrorMsg"), null);
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
                    showMessage(ContextVS.getMessage("certInstallOKMsg"), null);
                    csrFile.delete();
                } else showMessage(ContextVS.getMessage("certPendingMsg"), null);
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                showMessage(ContextVS.getMessage("errorStoringKeyStoreMsg"), null);
            }
        }

    }

    public static SMIMEMessage getSMIME(String fromUser, String toUser, String textToSign,
                    char[] password, String subject, Header... headers) throws Exception {
        String  tokenType = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.DNIe.toString());
        log.debug("getSMIME - tokenType: " + tokenType);
        switch(CryptoTokenVS.valueOf(tokenType)) {
            case JKS_KEYSTORE:
                KeyStore keyStore = ContextVS.getUserKeyStore(password);
                SMIMESignedGeneratorVS signedGenerator = new SMIMESignedGeneratorVS(keyStore,
                        ContextVS.KEYSTORE_USER_CERT_ALIAS, password, ContextVS.DNIe_SIGN_MECHANISM);
                return signedGenerator.getSMIME(fromUser, toUser, textToSign, subject, headers);
            case DNIe:
                return DNIeContentSigner.getSMIME(fromUser, toUser, textToSign, password, subject, headers);
            case MOBILE:
                countDownLatch = new CountDownLatch(1);
                JSONObject mobileTokenJSON = getInstance().getMobileCryptoToken();//{"id":,"deviceName":"","certPEM":""}
                Long deviceToId = mobileTokenJSON.getLong("id");
                String deviceToName = mobileTokenJSON.getString("deviceName");
                String deviceFromName = InetAddress.getLocalHost().getHostName();
                String certPEM = mobileTokenJSON.getString("certPEM");
                X509Certificate deviceToCert =  CertUtils.fromPEMToX509CertCollection(certPEM.getBytes()).iterator().next();
                requestBundle = WebSocketUtils.getSignRequest(deviceToId, deviceToName,
                    deviceFromName, toUser, textToSign, subject, ContextVS.getInstance().getLocale().getLanguage(),
                    deviceToCert, headers);

                BrowserVS.getInstance().sendWebSocketMessage(requestBundle.getRequest().toString());

                countDownLatch.await();
                ResponseVS<SMIMEMessage> responseVS = getMessageToDeviceResponse();
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
                else return responseVS.getData();
            default: return null;
        }
    }

    public static void setWebSocketMessage(WebSocketMessage message) {
        switch (message.getOperation()) {
            case MESSAGEVS_TO_DEVICE:
                if(ResponseVS.SC_ERROR == message.getStatusCode()) {
                    messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR, message.getMessage());
                    countDownLatch.countDown();
                }
                break;
            case MESSAGEVS_FROM_DEVICE:
                try {
                    smimeMessage = message.getSignResponse(requestBundle);
                    messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_OK, null, smimeMessage);
                } catch(Exception ex) {
                    log.error(ex.getMessage(), ex);
                    messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR, ex.getMessage());
                }
                countDownLatch.countDown();
                break;
        }
    }

    public static ResponseVS getMessageToDeviceResponse() {
        return messageToDeviceResponse;
    }

    public static CryptoTokenVS getCryptoTokenType () {
        String  tokenType = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.DNIe.toString());
        return CryptoTokenVS.valueOf(tokenType);
    }

    public void showMessage(final String message, final Button optionButton) {
        PlatformImpl.runLater(new Runnable() {
            @Override
            public void run() {
                new MessageDialog().showHtmlMessage(message, optionButton);
            }
        });
    }

    private void flush() {
        log.debug("flush");
        try {
            sessionDataJSON.put("browserSession", browserSessionDataJSON);
            FileUtils.copyStreamToFile(new ByteArrayInputStream(sessionDataJSON.toString().getBytes()), sessionFile);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}
