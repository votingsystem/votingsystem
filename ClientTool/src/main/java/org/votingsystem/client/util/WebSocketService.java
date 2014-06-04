package org.votingsystem.client.util;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.FileUtils;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;


import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.websocket.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class WebSocketService extends Service<ResponseVS> {

    private static Logger logger = Logger.getLogger(WebSocketService.class);

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private final ClientManager client = ClientManager.createClient();
    private final String keyStorePassw = "";
    private String targetService;

    public WebSocketService(X509Certificate serverCert, String targetService) {
        this.targetService = targetService;
        try {
            KeyStore p12Store = KeyStore.getInstance("PKCS12");
            p12Store.load(null, null);
            p12Store.setCertificateEntry(serverCert.getSubjectDN().toString(), serverCert);
            byte[] p12KeyStoreBytes = KeyStoreUtil.getBytes(p12Store, keyStorePassw.toCharArray());

            // Grizzly ssl configuration
            SSLContextConfigurator sslContext = new SSLContextConfigurator();
            sslContext.setTrustStoreType("PKCS12");
            sslContext.setTrustStoreBytes(p12KeyStoreBytes);
            sslContext.setTrustStorePass(keyStorePassw);
            SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslContext, true, false, false);
            client.getProperties().put(SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override protected Task<ResponseVS> createTask() {
        return new WebSocketTask();
    }
    class WebSocketTask extends Task<ResponseVS> {

        @Override protected ResponseVS call() throws Exception {
            try {
                logger.debug("WebSocketTask - Connecting ... to " + targetService);
                client.connectToServer(new Endpoint() {
                    @Override public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        try {
                            session.addMessageHandler(new MessageHandler.Whole<String>() {
                                @Override public void onMessage(String message) {
                                    logger.debug("WebSocketTask - onMessage: " + message);
                                }
                            });
                            logger.debug("WebSocketTask - Client onOpen");
                            session.getBasicRemote().sendText("Here we come");
                        } catch (IOException e) {
                            // do nothing
                        }
                    }
                }, ClientEndpointConfig.Builder.create().build(), URI.create(targetService));

            }catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            return null;
        }
    }

}
