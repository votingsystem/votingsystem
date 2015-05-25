package org.votingsystem.client;

import javafx.application.Application;
import javafx.stage.Stage;
import org.votingsystem.client.service.BrowserSessionService;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.ContextVS;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VotingSystemApp extends Application {

    private static Logger log = Logger.getLogger(VotingSystemApp.class.getSimpleName());

    private static VotingSystemApp INSTANCE;
    private Map<String, String> smimeMessageMap;

    // Create a trust manager that does not validate certificate chains
    private static TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                System.out.print("trustAllCerts - getAcceptedIssuers");
                try {
                    return ContextVS.getInstance().getVotingSystemSSLCerts().toArray(new X509Certificate[]{});
                } catch (Exception ex) { log.log(Level.SEVERE,ex.getMessage(), ex);}
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                System.out.print("trustAllCerts - checkClientTrusted");
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType ) throws CertificateException {
                System.out.print("trustAllCerts - checkServerTrusted");
                try {
                    CertUtils.verifyCertificate(ContextVS.getInstance().getVotingSystemSSLTrustAnchors(), false,
                            Arrays.asList(certs));
                } catch(Exception ex) {
                    throw new CertificateException(ex.getMessage());
                }
            }
        }
    };

    public String getSMIME(String smimeMessageURL) {
        if(smimeMessageMap ==  null) return null;
        else return smimeMessageMap.get(smimeMessageURL);
    }

    public void setSMIME(String smimeMessageURL, String smimeMessageStr) {
        if(smimeMessageMap ==  null) {
            smimeMessageMap = new HashMap<String, String>();
        }
        smimeMessageMap.put(smimeMessageURL, smimeMessageStr);
    }

    @Override public void stop() {
        log.info("stop");
        System.exit(0);//Platform.exit();
    }

    public static VotingSystemApp getInstance() {
        return INSTANCE;
    }

    @Override public void start(final Stage primaryStage) throws Exception {
        INSTANCE = this;
        Browser browser = Browser.init(primaryStage);
        new Thread(() -> {
                boolean loadedFromJar = false;
                if(VotingSystemApp.class.getResource(VotingSystemApp.this.getClass().getSimpleName() + ".class").
                        toString().contains("jar:file")) {
                    loadedFromJar = true;
                }
                log.info("start - loadedFromJar: " + loadedFromJar + " - JavaFX version: " +
                        com.sun.javafx.runtime.VersionInfo.getRuntimeVersion());
                try {
                    SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                } catch (GeneralSecurityException ex) { log.log(Level.SEVERE,ex.getMessage(), ex); }
                String accessControlServerURL = null;
                String currencyServerURL = null;
                if(loadedFromJar) {
                    accessControlServerURL = ContextVS.getMessage("prodAccessControlServerURL");
                    currencyServerURL = ContextVS.getMessage("prodCurrencyServerURL");
                } else {
                    accessControlServerURL = ContextVS.getMessage("devAccessControlServerURL");
                    currencyServerURL = ContextVS.getMessage("devCurrencyServerURL");
                }
                ResponseVS responseVS = null;
                try {
                    responseVS = Utils.checkServer(accessControlServerURL);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        browser.setVotingSystemAvailable(true);
                        ContextVS.getInstance().setAccessControl((AccessControlVS) responseVS.getData());
                        BrowserSessionService.getInstance().checkCSRRequest();
                    }
                } catch(Exception ex) {log.log(Level.SEVERE,ex.getMessage(), ex);}
                try {
                    responseVS = Utils.checkServer(currencyServerURL);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        browser.setCurrencyServerAvailable(true);
                        ContextVS.getInstance().setCurrencyServer((CurrencyServer) responseVS.getData());
                    } else browser.setCurrencyServerAvailable(false);
                } catch(Exception ex) {
                    log.log(Level.SEVERE,ex.getMessage());
                    browser.setCurrencyServerAvailable(false);
                }
        }).start();
        browser.show();
    }

    public static void main(String[] args) {
        ContextVS.initSignatureClient("clientToolMessages", Locale.getDefault().getLanguage());
        if(args.length > 0) ContextVS.getInstance().initDirs(args[0]);
        launch(args);
    }

}